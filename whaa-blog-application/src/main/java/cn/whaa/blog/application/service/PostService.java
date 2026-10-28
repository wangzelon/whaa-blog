package cn.whaa.blog.application.service;

import cn.whaa.blog.application.dto.*;
import cn.whaa.blog.application.markdown.MarkdownService;
import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.common.util.SlugUtils;
import cn.whaa.blog.domain.enums.PostStatus;
import cn.whaa.blog.infrastructure.cache.CacheService;
import cn.whaa.blog.infrastructure.persistence.entity.*;
import cn.whaa.blog.infrastructure.persistence.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final Duration LIST_CACHE_TTL = Duration.ofMinutes(5);

    private final PostMapper postMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;
    private final SysUserMapper sysUserMapper;
    private final MarkdownService markdownService;
    private final CacheService cacheService;

    public PageResult<PostSummaryVO> listPublished(long page, long size, Long categoryId, Long tagId) {
        String cacheKey = "posts:list:" + page + ":" + size + ":" + categoryId + ":" + tagId;
        return cacheService.getOrLoad(cacheKey, LIST_CACHE_TTL, new TypeReference<>() {
        }, () -> loadPublishedPage(page, size, categoryId, tagId));
    }

    private PageResult<PostSummaryVO> loadPublishedPage(long page, long size, Long categoryId, Long tagId) {
        LambdaQueryWrapper<PostEntity> wrapper = publishedWrapper();
        if (categoryId != null) {
            wrapper.eq(PostEntity::getCategoryId, categoryId);
        }
        if (tagId != null) {
            List<PostTagEntity> links = postTagMapper.selectList(new LambdaQueryWrapper<PostTagEntity>().eq(PostTagEntity::getTagId, tagId));
            Set<Long> postIds = links.stream().map(PostTagEntity::getPostId).collect(Collectors.toSet());
            if (postIds.isEmpty()) {
                return new PageResult<>(List.of(), 0, page, size);
            }
            wrapper.in(PostEntity::getId, postIds);
        }
        wrapper.orderByDesc(PostEntity::getPinned).orderByDesc(PostEntity::getPublishedAt);
        Page<PostEntity> result = postMapper.selectPage(new Page<>(page, size), wrapper);
        List<PostSummaryVO> records = result.getRecords().stream().map(this::toSummary).toList();
        return new PageResult<>(records, result.getTotal(), page, size);
    }

    public PostDetailVO getPublishedBySlug(String slug) {
        PostEntity entity = postMapper.selectOne(publishedWrapper().eq(PostEntity::getSlug, slug));
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND, "post not found");
        }
        incrementViewAsync(entity.getId());
        return toDetail(entity);
    }

    @Async
    public void incrementViewAsync(Long postId) {
        postMapper.incrementViewCount(postId);
    }

    public List<ArchiveGroupVO> listArchive() {
        return cacheService.getOrLoad("posts:archive", LIST_CACHE_TTL, new TypeReference<>() {
        }, this::loadArchive);
    }

    private List<ArchiveGroupVO> loadArchive() {
        List<PostEntity> posts = postMapper.selectList(
                publishedWrapper().orderByDesc(PostEntity::getPublishedAt)
        );
        Map<Integer, List<ArchiveItemVO>> grouped = new LinkedHashMap<>();
        for (PostEntity post : posts) {
            LocalDateTime publishedAt = post.getPublishedAt() != null ? post.getPublishedAt() : post.getCreatedAt();
            if (publishedAt == null) {
                continue;
            }
            int year = publishedAt.getYear();
            ArchiveItemVO item = new ArchiveItemVO();
            item.setSlug(post.getSlug());
            item.setTitle(post.getTitle());
            item.setPublishedAt(publishedAt);
            grouped.computeIfAbsent(year, y -> new ArrayList<>()).add(item);
        }
        return grouped.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<ArchiveItemVO>>comparingByKey().reversed())
                .map(e -> {
                    ArchiveGroupVO group = new ArchiveGroupVO();
                    group.setYear(e.getKey());
                    group.setItems(e.getValue());
                    return group;
                }).toList();
    }

    public List<PostSummaryVO> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        List<PostEntity> posts = postMapper.searchPublished(keyword.trim(), 50);
        return posts.stream().map(this::toSummary).toList();
    }

    @Transactional
    public Long saveAdmin(PostSaveCommand cmd, Long authorId) {
        MarkdownService.RenderResult rendered = markdownService.render(cmd.getContentMd());
        PostEntity entity;
        if (cmd.getId() != null) {
            entity = postMapper.selectById(cmd.getId());
            if (entity == null) {
                throw new BizException(ResultCode.NOT_FOUND);
            }
        } else {
            entity = new PostEntity();
            entity.setAuthorId(authorId);
            entity.setViewCount(0);
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setTitle(cmd.getTitle());
        entity.setSlug(StringUtils.hasText(cmd.getSlug()) ? cmd.getSlug() : SlugUtils.toSlug(cmd.getTitle()));
        entity.setDescription(cmd.getDescription());
        entity.setExcerpt(StringUtils.hasText(cmd.getExcerpt()) ? cmd.getExcerpt() : rendered.excerptMarkdown());
        entity.setContentMd(cmd.getContentMd());
        entity.setContentHtml(rendered.html());
        entity.setStatus(cmd.getStatus().name());
        entity.setCategoryId(cmd.getCategoryId());
        entity.setCoverUrl(cmd.getCoverUrl());
        entity.setPinned(Boolean.TRUE.equals(cmd.getPinned()));
        entity.setAllowComment(cmd.getAllowComment() == null || cmd.getAllowComment());
        entity.setHideTime(Boolean.TRUE.equals(cmd.getHideTime()));
        entity.setUpdatedAt(LocalDateTime.now());
        if (PostStatus.PUBLISHED.equals(cmd.getStatus())) {
            entity.setPublishedAt(cmd.getPublishedAt() != null ? cmd.getPublishedAt() : LocalDateTime.now());
        }
        if (entity.getId() == null) {
            postMapper.insert(entity);
        } else {
            postMapper.updateById(entity);
            postTagMapper.delete(new LambdaQueryWrapper<PostTagEntity>().eq(PostTagEntity::getPostId, entity.getId()));
        }
        saveTags(entity.getId(), cmd.getTagIds());
        cacheService.evictPattern("posts:");
        return entity.getId();
    }

    public PageResult<PostSummaryVO> listAdmin(long page, long size, String status) {
        LambdaQueryWrapper<PostEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(PostEntity::getStatus, status);
        }
        wrapper.orderByDesc(PostEntity::getUpdatedAt);
        Page<PostEntity> result = postMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toSummary).toList(), result.getTotal(), page, size);
    }

    public PostDetailVO getAdminById(Long id) {
        PostEntity entity = postMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return toDetail(entity);
    }

    @Transactional
    public void deleteAdmin(Long id) {
        postMapper.deleteById(id);
        postTagMapper.delete(new LambdaQueryWrapper<PostTagEntity>().eq(PostTagEntity::getPostId, id));
        cacheService.evictPattern("posts:");
    }

    private void saveTags(Long postId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            PostTagEntity link = new PostTagEntity();
            link.setPostId(postId);
            link.setTagId(tagId);
            postTagMapper.insert(link);
        }
    }

    private LambdaQueryWrapper<PostEntity> publishedWrapper() {
        return new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getStatus, PostStatus.PUBLISHED.name())
                .and(w -> w.isNull(PostEntity::getPublishedAt).or().le(PostEntity::getPublishedAt, LocalDateTime.now()));
    }

    private PostSummaryVO toSummary(PostEntity entity) {
        PostSummaryVO vo = new PostSummaryVO();
        vo.setId(entity.getId());
        vo.setSlug(entity.getSlug());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setExcerpt(entity.getExcerpt());
        vo.setCoverUrl(entity.getCoverUrl());
        vo.setPinned(entity.getPinned());
        vo.setHideTime(entity.getHideTime());
        vo.setPublishedAt(entity.getPublishedAt());
        if (entity.getCategoryId() != null) {
            CategoryEntity category = categoryMapper.selectById(entity.getCategoryId());
            if (category != null) {
                CategoryVO cvo = new CategoryVO();
                cvo.setId(category.getId());
                cvo.setName(category.getName());
                cvo.setSlug(category.getSlug());
                vo.setCategory(cvo);
            }
        }
        vo.setTags(loadTags(entity.getId()));
        return vo;
    }

    private PostDetailVO toDetail(PostEntity entity) {
        PostDetailVO vo = new PostDetailVO();
        PostSummaryVO summary = toSummary(entity);
        vo.setId(summary.getId());
        vo.setSlug(summary.getSlug());
        vo.setTitle(summary.getTitle());
        vo.setDescription(summary.getDescription());
        vo.setExcerpt(summary.getExcerpt());
        vo.setCoverUrl(summary.getCoverUrl());
        vo.setPinned(summary.getPinned());
        vo.setHideTime(summary.getHideTime());
        vo.setPublishedAt(summary.getPublishedAt());
        vo.setCategory(summary.getCategory());
        vo.setTags(summary.getTags());
        vo.setContentHtml(entity.getContentHtml());
        vo.setContentMd(entity.getContentMd());
        vo.setViewCount(entity.getViewCount());
        vo.setAllowComment(entity.getAllowComment());
        SysUserEntity author = sysUserMapper.selectById(entity.getAuthorId());
        if (author != null) {
            vo.setAuthorName(author.getNickname() != null ? author.getNickname() : author.getUsername());
        }
        return vo;
    }

    private List<TagVO> loadTags(Long postId) {
        List<PostTagEntity> links = postTagMapper.selectList(new LambdaQueryWrapper<PostTagEntity>().eq(PostTagEntity::getPostId, postId));
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> tagIds = links.stream().map(PostTagEntity::getTagId).toList();
        return tagMapper.selectBatchIds(tagIds).stream().map(t -> {
            TagVO vo = new TagVO();
            vo.setId(t.getId());
            vo.setName(t.getName());
            vo.setSlug(t.getSlug());
            return vo;
        }).toList();
    }
}
