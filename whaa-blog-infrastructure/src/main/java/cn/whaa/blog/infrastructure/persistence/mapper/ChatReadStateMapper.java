package cn.whaa.blog.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ChatReadStateMapper {

    @Insert("INSERT IGNORE INTO chat_read_state (conversation_id, user_id, last_read_message_id) VALUES (#{conversationId}, #{userId}, 0)")
    int insertIfAbsent(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Update("UPDATE chat_read_state SET last_read_message_id = GREATEST(last_read_message_id, #{messageId}), updated_at = CURRENT_TIMESTAMP WHERE conversation_id = #{conversationId} AND user_id = #{userId}")
    int markRead(@Param("conversationId") Long conversationId,
                 @Param("userId") Long userId,
                 @Param("messageId") Long messageId);
}
