package cn.whaa.blog.infrastructure.persistence.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageRow {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderUsername;
    private String senderNickname;
    private String content;
    private LocalDateTime createdAt;
}
