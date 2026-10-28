package cn.whaa.blog.infrastructure.persistence.mapper;

import cn.whaa.blog.infrastructure.persistence.entity.FriendLinkEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendLinkMapper extends BaseMapper<FriendLinkEntity> {

    @Select("SELECT * FROM friend_link WHERE visible = 1 ORDER BY RAND()")
    List<FriendLinkEntity> selectRandomVisible();

    @Select("SELECT * FROM friend_link WHERE visible = 1 ORDER BY sort_order ASC, id ASC")
    List<FriendLinkEntity> selectOrderedVisible();
}
