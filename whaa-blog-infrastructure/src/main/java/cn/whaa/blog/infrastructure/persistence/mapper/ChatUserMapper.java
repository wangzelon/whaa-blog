package cn.whaa.blog.infrastructure.persistence.mapper;

import cn.whaa.blog.infrastructure.persistence.projection.ChatUserRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatUserMapper {

    @Select("""
            SELECT id, username, nickname, role
            FROM sys_user
            WHERE id <> #{userId}
              AND (#{query} = '' OR username LIKE CONCAT('%', #{query}, '%')
                   OR nickname LIKE CONCAT('%', #{query}, '%'))
            ORDER BY CASE role WHEN 'ADMIN' THEN 0 ELSE 1 END, nickname, username
            LIMIT 20
            """)
    List<ChatUserRow> search(@Param("userId") Long userId, @Param("query") String query);
}
