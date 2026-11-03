package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message_attachment")
public class ChatMessageAttachmentEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long conversationId;
    private Long messageId;
    private Long uploadedBy;
    private String objectKey;
    private String originalName;
    private String mimeType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
