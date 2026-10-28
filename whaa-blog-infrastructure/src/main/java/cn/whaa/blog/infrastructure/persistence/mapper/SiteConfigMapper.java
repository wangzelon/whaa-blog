package cn.whaa.blog.infrastructure.persistence.mapper;

import cn.whaa.blog.infrastructure.persistence.entity.SiteConfigEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SiteConfigMapper extends BaseMapper<SiteConfigEntity> {
}
