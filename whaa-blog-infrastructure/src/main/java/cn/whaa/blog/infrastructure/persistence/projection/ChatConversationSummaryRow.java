package cn.whaa.blog.infrastructure.persistence.projection;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatConversationSummaryRow {
    private Long id;
    private Long peerUserId;
    private String peerUsername;
    private String peerNickname;
    private String peerRole;
    private Long participantOneId;
    private String participantOneUsername;
    private String participantOneNickname;
    private String participantOneRole;
    private Long participantTwoId;
    private String participantTwoUsername;
    private String participantTwoNickname;
    private String participantTwoRole;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Long unreadCount;
}
