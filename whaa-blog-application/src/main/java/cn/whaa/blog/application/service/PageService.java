package cn.whaa.blog.application.service;

import cn.whaa.blog.application.markdown.MarkdownService;
import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.infrastructure.persistence.entity.PageEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.PageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PageService {

    private final PageMapper pageMapper;
    private final MarkdownService markdownService;

    public PageVO getBySlug(String slug) {
        PageEntity entity = pageMapper.selectOne(new LambdaQueryWrapper<PageEntity>().eq(PageEntity::getSlug, slug));
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return toVo(entity);
    }

    public List<PageVO> listAll() {
        return pageMapper.selectList(null).stream().map(this::toVo).toList();
    }

    public Long save(PageSaveCommand cmd) {
        PageEntity entity;
        if (cmd.getId() != null) {
            entity = pageMapper.selectById(cmd.getId());
            if (entity == null) {
                throw new BizException(ResultCode.NOT_FOUND);
            }
        } else {
            entity = new PageEntity();
        }
        var rendered = markdownService.render(cmd.getContentMd());
        entity.setSlug(cmd.getSlug());
        entity.setTitle(cmd.getTitle());
        entity.setPageType(cmd.getPageType());
        entity.setIcon(cmd.getIcon());
        entity.setContentMd(cmd.getContentMd());
        entity.setContentHtml(rendered.html());
        entity.setAllowComment(cmd.getAllowComment() == null || cmd.getAllowComment());
        if (entity.getId() == null) {
            pageMapper.insert(entity);
        } else {
            pageMapper.updateById(entity);
        }
        return entity.getId();
    }

    public void delete(Long id) {
        pageMapper.deleteById(id);
    }

    private PageVO toVo(PageEntity entity) {
        PageVO vo = new PageVO();
        vo.setId(entity.getId());
        vo.setSlug(entity.getSlug());
        vo.setTitle(entity.getTitle());
        vo.setPageType(entity.getPageType());
        vo.setIcon(entity.getIcon());
        vo.setContentHtml(entity.getContentHtml());
        vo.setContentMd(entity.getContentMd());
        vo.setAllowComment(entity.getAllowComment());
        return vo;
    }

    @Data
    public static class PageVO {
        private Long id;
        private String slug;
        private String title;
        private String pageType;
        private String icon;
        private String contentHtml;
        private String contentMd;
        private Boolean allowComment;
    }

    @Data
    public static class PageSaveCommand {
        private Long id;
        private String slug;
        private String title;
        private String pageType;
        private String icon;
        private String contentMd;
        private Boolean allowComment;
    }
}
