package cn.whaa.blog.application.service;

import cn.whaa.blog.application.dto.TagVO;
import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.common.util.SlugUtils;
import cn.whaa.blog.infrastructure.cache.CacheService;
import cn.whaa.blog.infrastructure.persistence.entity.TagEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.TagMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;
    private final CacheService cacheService;

    public List<TagVO> listPublic() {
        return tagMapper.selectList(new LambdaQueryWrapper<TagEntity>().orderByAsc(TagEntity::getName))
                .stream().map(this::toVo).toList();
    }

    public List<TagVO> wordCloud() {
        return cacheService.getOrLoad("tags:cloud", Duration.ofMinutes(10), new TypeReference<>() {
        }, () -> tagMapper.selectTagCloud().stream().map(row -> {
            TagVO vo = new TagVO();
            vo.setId(((Number) row.get("id")).longValue());
            vo.setName((String) row.get("name"));
            vo.setSlug((String) row.get("slug"));
            Object weight = row.get("weight");
            vo.setWeight(weight == null ? 0L : ((Number) weight).longValue());
            return vo;
        }).toList());
    }

    public TagVO getBySlug(String slug) {
        TagEntity entity = tagMapper.selectOne(new LambdaQueryWrapper<TagEntity>().eq(TagEntity::getSlug, slug));
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return toVo(entity);
    }

    public Long save(TagEntity entity) {
        if (!StringUtils.hasText(entity.getSlug())) {
            entity.setSlug(SlugUtils.toSlug(entity.getName()));
        }
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
            tagMapper.insert(entity);
        } else {
            tagMapper.updateById(entity);
        }
        cacheService.evict("tags:cloud");
        return entity.getId();
    }

    public void delete(Long id) {
        tagMapper.deleteById(id);
        cacheService.evict("tags:cloud");
    }

    private TagVO toVo(TagEntity entity) {
        TagVO vo = new TagVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setSlug(entity.getSlug());
        return vo;
    }
}
