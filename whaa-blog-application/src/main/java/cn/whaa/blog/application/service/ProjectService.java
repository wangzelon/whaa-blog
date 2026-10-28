package cn.whaa.blog.application.service;

import cn.whaa.blog.infrastructure.persistence.entity.ProjectEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.ProjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;

    public List<ProjectEntity> listPublic(String categoryCode) {
        LambdaQueryWrapper<ProjectEntity> wrapper = new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getVisible, true)
                .orderByAsc(ProjectEntity::getSortOrder);
        if (StringUtils.hasText(categoryCode)) {
            wrapper.eq(ProjectEntity::getCategoryCode, categoryCode);
        }
        return projectMapper.selectList(wrapper);
    }

    public List<ProjectEntity> listAll() {
        return projectMapper.selectList(new LambdaQueryWrapper<ProjectEntity>().orderByAsc(ProjectEntity::getSortOrder));
    }

    public Long save(ProjectEntity entity) {
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
            projectMapper.insert(entity);
        } else {
            projectMapper.updateById(entity);
        }
        return entity.getId();
    }

    public void delete(Long id) {
        projectMapper.deleteById(id);
    }
}
