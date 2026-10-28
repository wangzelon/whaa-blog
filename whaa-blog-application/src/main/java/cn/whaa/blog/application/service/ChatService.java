package cn.whaa.blog.application.service;

import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.infrastructure.persistence.entity.ChatConversationEntity;
import cn.whaa.blog.infrastructure.persistence.entity.ChatMessageEntity;
import cn.whaa.blog.infrastructure.persistence.entity.SysUserEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.ChatConversationMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.ChatMessageMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.ChatReadStateMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.ChatUserMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.SysUserMapper;
import cn.whaa.blog.infrastructure.persistence.projection.ChatConversationSummaryRow;
import cn.whaa.blog.infrastructure.persistence.projection.ChatMessageRow;
import cn.whaa.blog.infrastructure.persistence.projection.ChatUserRow;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SysUserMapper sysUserMapper;
    private final ChatUserMapper chatUserMapper;
    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatReadStateMapper readStateMapper;

    @Transactional(readOnly = true)
    public List<ContactVO> searchContacts(Long currentUserId, String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() > 64) normalizedQuery = normalizedQuery.substring(0, 64);
        return chatUserMapper.search(currentUserId, normalizedQuery).stream().map(this::toContact).toList();
    }

    @Transactional
    public ConversationVO openConversation(Long currentUserId, Long peerUserId) {
        if (peerUserId == null || peerUserId.equals(currentUserId)) {
            throw new BizException(ResultCode.BAD_REQUEST, "cannot start a conversation with yourself");
        }
        SysUserEntity peer = sysUserMapper.selectById(peerUserId);
        if (peer == null) throw new BizException(ResultCode.NOT_FOUND, "user not found");

        Long lowUserId = Math.min(currentUserId, peerUserId);
        Long highUserId = Math.max(currentUserId, peerUserId);
        conversationMapper.insertIfAbsent(lowUserId, highUserId, currentUserId);
        ChatConversationEntity conversation = conversationMapper.findByPair(lowUserId, highUserId);
        readStateMapper.insertIfAbsent(conversation.getId(), lowUserId);
        readStateMapper.insertIfAbsent(conversation.getId(), highUserId);
        return toConversation(conversationMapper.findForUser(conversation.getId(), currentUserId, false), currentUserId);
    }

    @Transactional(readOnly = true)
    public List<ConversationVO> listConversations(Long currentUserId, boolean isAdmin) {
        return conversationMapper.listForUser(currentUserId, isAdmin).stream()
                .map(row -> toConversation(row, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageVO> history(Long currentUserId, Long conversationId, Long beforeId,
                                   int requestedSize, boolean isAdmin) {
        requireVisible(currentUserId, conversationId, isAdmin);
        int size = Math.max(1, Math.min(requestedSize, 100));
        Long cursor = beforeId != null && beforeId > 0 ? beforeId : null;
        List<ChatMessageRow> rows = messageMapper.history(conversationId, cursor, size);
        Collections.reverse(rows);
        return rows.stream().map(this::toMessage).toList();
    }

    @Transactional
    public MessageVO sendMessage(Long currentUserId, Long conversationId, String rawContent) {
        requireParticipant(currentUserId, conversationId);
        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty() || content.length() > 2000) {
            throw new BizException(ResultCode.BAD_REQUEST, "message must contain 1-2000 characters");
        }

        ChatMessageEntity message = new ChatMessageEntity();
        message.setConversationId(conversationId);
        message.setSenderId(currentUserId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        conversationMapper.updateLastMessage(conversationId, message.getId());
        readStateMapper.markRead(conversationId, currentUserId, message.getId());

        SysUserEntity sender = sysUserMapper.selectById(currentUserId);
        return toMessage(message, sender);
    }

    @Transactional
    public void markRead(Long currentUserId, Long conversationId) {
        requireParticipant(currentUserId, conversationId);
        readStateMapper.markRead(conversationId, currentUserId, messageMapper.latestId(conversationId));
    }

    @Transactional(readOnly = true)
    public List<Long> participants(Long conversationId) {
        ChatConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) throw new BizException(ResultCode.NOT_FOUND, "conversation not found");
        return List.of(conversation.getLowUserId(), conversation.getHighUserId());
    }

    private ChatConversationEntity requireVisible(Long currentUserId, Long conversationId, boolean isAdmin) {
        ChatConversationEntity conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) throw new BizException(ResultCode.NOT_FOUND, "conversation not found");
        if (!isAdmin && !isParticipant(currentUserId, conversation)) {
            throw new BizException(ResultCode.FORBIDDEN, "you are not a participant of this conversation");
        }
        return conversation;
    }

    private ChatConversationEntity requireParticipant(Long currentUserId, Long conversationId) {
        return requireVisible(currentUserId, conversationId, false);
    }

    private boolean isParticipant(Long currentUserId, ChatConversationEntity conversation) {
        return currentUserId.equals(conversation.getLowUserId()) || currentUserId.equals(conversation.getHighUserId());
    }

    private ContactVO toContact(ChatUserRow row) {
        ContactVO vo = new ContactVO();
        vo.setId(row.getId());
        vo.setUsername(row.getUsername());
        vo.setNickname(row.getNickname());
        vo.setRole(row.getRole());
        return vo;
    }

    private ConversationVO toConversation(ChatConversationSummaryRow row, Long currentUserId) {
        if (row == null) throw new BizException(ResultCode.NOT_FOUND, "conversation not found");
        ConversationVO vo = new ConversationVO();
        vo.setId(row.getId());
        vo.setPeerUserId(row.getPeerUserId());
        vo.setPeerUsername(row.getPeerUsername());
        vo.setPeerNickname(row.getPeerNickname());
        vo.setPeerRole(row.getPeerRole());
        vo.setLastMessage(row.getLastMessage());
        vo.setLastMessageAt(row.getLastMessageAt());
        vo.setUnreadCount(row.getUnreadCount() == null ? 0 : row.getUnreadCount());
        boolean participant = currentUserId.equals(row.getParticipantOneId())
                || currentUserId.equals(row.getParticipantTwoId());
        String participantOne = displayName(row.getParticipantOneNickname(), row.getParticipantOneUsername());
        String participantTwo = displayName(row.getParticipantTwoNickname(), row.getParticipantTwoUsername());
        vo.setCanSend(participant);
        vo.setTitle(participant ? displayName(row.getPeerNickname(), row.getPeerUsername())
                : participantOne + " ↔ " + participantTwo);
        vo.setParticipantOneName(participantOne);
        vo.setParticipantTwoName(participantTwo);
        return vo;
    }

    private String displayName(String nickname, String username) {
        return nickname == null || nickname.isBlank() ? username : nickname;
    }

    private MessageVO toMessage(ChatMessageRow row) {
        MessageVO vo = new MessageVO();
        vo.setId(row.getId());
        vo.setConversationId(row.getConversationId());
        vo.setSenderId(row.getSenderId());
        vo.setSenderUsername(row.getSenderUsername());
        vo.setSenderNickname(row.getSenderNickname());
        vo.setContent(row.getContent());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    private MessageVO toMessage(ChatMessageEntity row, SysUserEntity sender) {
        MessageVO vo = new MessageVO();
        vo.setId(row.getId());
        vo.setConversationId(row.getConversationId());
        vo.setSenderId(row.getSenderId());
        vo.setSenderUsername(sender.getUsername());
        vo.setSenderNickname(sender.getNickname());
        vo.setContent(row.getContent());
        vo.setCreatedAt(row.getCreatedAt());
        return vo;
    }

    @Data
    public static class ContactVO {
        private Long id;
        private String username;
        private String nickname;
        private String role;
    }

    @Data
    public static class ConversationVO {
        private Long id;
        private Long peerUserId;
        private String peerUsername;
        private String peerNickname;
        private String peerRole;
        private String title;
        private String participantOneName;
        private String participantTwoName;
        private boolean canSend;
        private String lastMessage;
        private LocalDateTime lastMessageAt;
        private Long unreadCount;
    }

    @Data
    public static class MessageVO {
        private Long id;
        private Long conversationId;
        private Long senderId;
        private String senderUsername;
        private String senderNickname;
        private String content;
        private LocalDateTime createdAt;
    }
}
