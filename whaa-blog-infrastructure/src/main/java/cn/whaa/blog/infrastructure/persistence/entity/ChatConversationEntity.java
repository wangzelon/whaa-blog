package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_conversation")
public class ChatConversationEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lowUserId;
    private Long highUserId;
    private Long createdBy;
    private Long lastMessageId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
