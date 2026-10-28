package cn.whaa.blog.infrastructure.persistence.mapper;

import cn.whaa.blog.infrastructure.persistence.entity.ChatMessageEntity;
import cn.whaa.blog.infrastructure.persistence.projection.ChatMessageRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {

    @Select("""
            SELECT m.id AS id,
                   m.conversation_id AS conversationId,
                   m.sender_id AS senderId,
                   u.username AS senderUsername,
                   u.nickname AS senderNickname,
                   m.content AS content,
                   m.created_at AS createdAt
            FROM chat_message m
            JOIN sys_user u ON u.id = m.sender_id
            WHERE m.conversation_id = #{conversationId}
              AND (#{beforeId} IS NULL OR m.id < #{beforeId})
            ORDER BY m.id DESC
            LIMIT #{size}
            """)
    List<ChatMessageRow> history(@Param("conversationId") Long conversationId,
                                 @Param("beforeId") Long beforeId,
                                 @Param("size") int size);

    @Select("SELECT COALESCE(MAX(id), 0) FROM chat_message WHERE conversation_id = #{conversationId}")
    Long latestId(@Param("conversationId") Long conversationId);
}
