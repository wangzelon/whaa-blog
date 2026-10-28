package cn.whaa.blog.infrastructure.persistence.mapper;

import cn.whaa.blog.infrastructure.persistence.entity.SysUserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {
}
