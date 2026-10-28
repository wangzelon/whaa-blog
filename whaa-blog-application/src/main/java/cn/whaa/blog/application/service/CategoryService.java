package cn.whaa.blog.application.service;

import cn.whaa.blog.application.dto.CategoryVO;
import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.common.util.SlugUtils;
import cn.whaa.blog.infrastructure.persistence.entity.CategoryEntity;
import cn.whaa.blog.infrastructure.persistence.entity.PostEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.CategoryMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final PostMapper postMapper;

    public List<CategoryVO> listPublic() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CategoryEntity>().orderByAsc(CategoryEntity::getSortOrder))
                .stream().map(this::toVo).toList();
    }

    public CategoryVO getBySlug(String slug) {
        CategoryEntity entity = categoryMapper.selectOne(new LambdaQueryWrapper<CategoryEntity>().eq(CategoryEntity::getSlug, slug));
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return toVo(entity);
    }

    public Long save(CategoryEntity entity) {
        if (!StringUtils.hasText(entity.getSlug())) {
            entity.setSlug(SlugUtils.toSlug(entity.getName()));
        }
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
            categoryMapper.insert(entity);
        } else {
            categoryMapper.updateById(entity);
        }
        return entity.getId();
    }

    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }

    private CategoryVO toVo(CategoryEntity entity) {
        CategoryVO vo = new CategoryVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setSlug(entity.getSlug());
        vo.setDescription(entity.getDescription());
        long count = postMapper.selectCount(new LambdaQueryWrapper<PostEntity>()
                .eq(PostEntity::getCategoryId, entity.getId())
                .eq(PostEntity::getStatus, "PUBLISHED"));
        vo.setPostCount(count);
        return vo;
    }
}
