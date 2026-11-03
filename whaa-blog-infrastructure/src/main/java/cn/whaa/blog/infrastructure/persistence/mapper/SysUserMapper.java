package cn.whaa.blog.infrastructure.persistence.mapper;

import cn.whaa.blog.infrastructure.persistence.entity.SysUserEntity;
import cn.whaa.blog.infrastructure.persistence.projection.SysUserAuthState;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

    @Select("SELECT role, status, token_version AS tokenVersion FROM sys_user WHERE id = #{userId}")
    SysUserAuthState authenticationState(@Param("userId") Long userId);
}
