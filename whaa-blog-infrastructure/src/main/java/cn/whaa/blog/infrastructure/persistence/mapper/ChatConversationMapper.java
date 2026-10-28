package cn.whaa.blog.infrastructure.persistence.mapper;

import cn.whaa.blog.infrastructure.persistence.entity.ChatConversationEntity;
import cn.whaa.blog.infrastructure.persistence.projection.ChatConversationSummaryRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversationEntity> {

    @Insert("INSERT IGNORE INTO chat_conversation (low_user_id, high_user_id, created_by) VALUES (#{lowUserId}, #{highUserId}, #{createdBy})")
    int insertIfAbsent(@Param("lowUserId") Long lowUserId,
                       @Param("highUserId") Long highUserId,
                       @Param("createdBy") Long createdBy);

    @Select("SELECT * FROM chat_conversation WHERE low_user_id = #{lowUserId} AND high_user_id = #{highUserId}")
    ChatConversationEntity findByPair(@Param("lowUserId") Long lowUserId,
                                      @Param("highUserId") Long highUserId);

    @Update("UPDATE chat_conversation SET last_message_id = #{messageId}, updated_at = CURRENT_TIMESTAMP WHERE id = #{conversationId}")
    int updateLastMessage(@Param("conversationId") Long conversationId, @Param("messageId") Long messageId);

    @Select("""
            SELECT c.id AS id,
                   CASE WHEN c.low_user_id = #{userId} THEN high_user.id
                        WHEN c.high_user_id = #{userId} THEN low_user.id ELSE low_user.id END AS peerUserId,
                   CASE WHEN c.low_user_id = #{userId} THEN high_user.username
                        WHEN c.high_user_id = #{userId} THEN low_user.username ELSE low_user.username END AS peerUsername,
                   CASE WHEN c.low_user_id = #{userId} THEN high_user.nickname
                        WHEN c.high_user_id = #{userId} THEN low_user.nickname ELSE low_user.nickname END AS peerNickname,
                   CASE WHEN c.low_user_id = #{userId} THEN high_user.role
                        WHEN c.high_user_id = #{userId} THEN low_user.role ELSE low_user.role END AS peerRole,
                   low_user.id AS participantOneId,
                   low_user.username AS participantOneUsername,
                   low_user.nickname AS participantOneNickname,
                   low_user.role AS participantOneRole,
                   high_user.id AS participantTwoId,
                   high_user.username AS participantTwoUsername,
                   high_user.nickname AS participantTwoNickname,
                   high_user.role AS participantTwoRole,
                   lm.content AS lastMessage,
                   lm.created_at AS lastMessageAt,
                   CASE WHEN #{isAdmin} = TRUE
                             AND c.low_user_id <> #{userId} AND c.high_user_id <> #{userId} THEN 0
                        ELSE (SELECT COUNT(*) FROM chat_message unread
                              WHERE unread.conversation_id = c.id
                                AND unread.id > COALESCE(rs.last_read_message_id, 0)
                                AND unread.sender_id <> #{userId}) END AS unreadCount
            FROM chat_conversation c
            JOIN sys_user low_user ON low_user.id = c.low_user_id
            JOIN sys_user high_user ON high_user.id = c.high_user_id
            LEFT JOIN chat_message lm ON lm.id = c.last_message_id
            LEFT JOIN chat_read_state rs ON rs.conversation_id = c.id AND rs.user_id = #{userId}
            WHERE (c.low_user_id = #{userId} OR c.high_user_id = #{userId} OR #{isAdmin} = TRUE)
            ORDER BY COALESCE(lm.created_at, c.updated_at) DESC, c.id DESC
            """)
    List<ChatConversationSummaryRow> listForUser(@Param("userId") Long userId,
                                                 @Param("isAdmin") boolean isAdmin);

    @Select("""
            SELECT c.id AS id,
                   CASE WHEN c.low_user_id = #{userId} THEN high_user.id
                        WHEN c.high_user_id = #{userId} THEN low_user.id ELSE low_user.id END AS peerUserId,
                   CASE WHEN c.low_user_id = #{userId} THEN high_user.username
                        WHEN c.high_user_id = #{userId} THEN low_user.username ELSE low_user.username END AS peerUsername,
                   CASE WHEN c.low_user_id = #{userId} THEN high_user.nickname
                        WHEN c.high_user_id = #{userId} THEN low_user.nickname ELSE low_user.nickname END AS peerNickname,
                   CASE WHEN c.low_user_id = #{userId} THEN high_user.role
                        WHEN c.high_user_id = #{userId} THEN low_user.role ELSE low_user.role END AS peerRole,
                   low_user.id AS participantOneId,
                   low_user.username AS participantOneUsername,
                   low_user.nickname AS participantOneNickname,
                   low_user.role AS participantOneRole,
                   high_user.id AS participantTwoId,
                   high_user.username AS participantTwoUsername,
                   high_user.nickname AS participantTwoNickname,
                   high_user.role AS participantTwoRole,
                   lm.content AS lastMessage,
                   lm.created_at AS lastMessageAt,
                   CASE WHEN #{isAdmin} = TRUE
                             AND c.low_user_id <> #{userId} AND c.high_user_id <> #{userId} THEN 0
                        ELSE (SELECT COUNT(*) FROM chat_message unread
                              WHERE unread.conversation_id = c.id
                                AND unread.id > COALESCE(rs.last_read_message_id, 0)
                                AND unread.sender_id <> #{userId}) END AS unreadCount
            FROM chat_conversation c
            JOIN sys_user low_user ON low_user.id = c.low_user_id
            JOIN sys_user high_user ON high_user.id = c.high_user_id
            LEFT JOIN chat_message lm ON lm.id = c.last_message_id
            LEFT JOIN chat_read_state rs ON rs.conversation_id = c.id AND rs.user_id = #{userId}
            WHERE c.id = #{conversationId}
              AND (c.low_user_id = #{userId} OR c.high_user_id = #{userId} OR #{isAdmin} = TRUE)
            """)
    ChatConversationSummaryRow findForUser(@Param("conversationId") Long conversationId,
                                            @Param("userId") Long userId,
                                            @Param("isAdmin") boolean isAdmin);
}
