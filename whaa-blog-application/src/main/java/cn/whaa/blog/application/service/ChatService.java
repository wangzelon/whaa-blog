package cn.whaa.blog.application.service;

import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.domain.enums.UserStatus;
import cn.whaa.blog.infrastructure.persistence.entity.ChatConversationEntity;
import cn.whaa.blog.infrastructure.persistence.entity.ChatMessageAttachmentEntity;
import cn.whaa.blog.infrastructure.persistence.entity.ChatMessageEntity;
import cn.whaa.blog.infrastructure.persistence.entity.SysUserEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.ChatConversationMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.ChatMessageAttachmentMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.ChatMessageMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.ChatReadStateMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.ChatUserMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.SysUserMapper;
import cn.whaa.blog.infrastructure.persistence.projection.ChatConversationSummaryRow;
import cn.whaa.blog.infrastructure.persistence.projection.ChatMessageRow;
import cn.whaa.blog.infrastructure.persistence.projection.ChatUserRow;
import cn.whaa.blog.infrastructure.storage.MinioStorageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_ATTACHMENTS_PER_MESSAGE = 5;
    private static final int MAX_PENDING_ATTACHMENTS_PER_USER = 10;
    private static final int PENDING_ATTACHMENT_MINUTES = 30;

    private final SysUserMapper sysUserMapper;
    private final ChatUserMapper chatUserMapper;
    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatMessageAttachmentMapper attachmentMapper;
    private final ChatReadStateMapper readStateMapper;
    private final ObjectProvider<MinioStorageService> storageProvider;

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
        if (peer == null || !UserStatus.ACTIVE.name().equals(peer.getStatus())) {
            throw new BizException(ResultCode.NOT_FOUND, "user not found");
        }

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
        if (rows.isEmpty()) return List.of();

        List<Long> messageIds = rows.stream().map(ChatMessageRow::getId).toList();
        Map<Long, List<ChatMessageAttachmentEntity>> attachmentsByMessage = new HashMap<>();
        attachmentMapper.selectList(new LambdaQueryWrapper<ChatMessageAttachmentEntity>()
                        .in(ChatMessageAttachmentEntity::getMessageId, messageIds)
                        .orderByAsc(ChatMessageAttachmentEntity::getId))
                .forEach(attachment -> attachmentsByMessage
                        .computeIfAbsent(attachment.getMessageId(), ignored -> new ArrayList<>()).add(attachment));
        return rows.stream().map(row -> toMessage(row,
                attachmentsByMessage.getOrDefault(row.getId(), List.of()))).toList();
    }

    public AttachmentVO uploadAttachment(Long currentUserId, Long conversationId, MultipartFile file) {
        requireParticipant(currentUserId, conversationId);
        LocalDateTime now = LocalDateTime.now();
        Long pendingCount = attachmentMapper.selectCount(new LambdaQueryWrapper<ChatMessageAttachmentEntity>()
                .eq(ChatMessageAttachmentEntity::getUploadedBy, currentUserId)
                .isNull(ChatMessageAttachmentEntity::getMessageId)
                .gt(ChatMessageAttachmentEntity::getExpiresAt, now));
        if (pendingCount != null && pendingCount >= MAX_PENDING_ATTACHMENTS_PER_USER) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, "too many pending attachments; try again later");
        }

        MinioStorageService storage = requireStorage();
        MinioStorageService.ChatUploadResult uploaded = storage.uploadChatAttachment(file);

        ChatMessageAttachmentEntity attachment = new ChatMessageAttachmentEntity();
        attachment.setConversationId(conversationId);
        attachment.setUploadedBy(currentUserId);
        attachment.setObjectKey(uploaded.objectKey());
        attachment.setOriginalName(uploaded.originalName());
        attachment.setMimeType(uploaded.mimeType());
        attachment.setSizeBytes(uploaded.sizeBytes());
        attachment.setCreatedAt(now);
        attachment.setExpiresAt(now.plusMinutes(PENDING_ATTACHMENT_MINUTES));
        try {
            attachmentMapper.insert(attachment);
        } catch (RuntimeException exception) {
            try {
                storage.deleteChatAttachment(uploaded.objectKey());
            } catch (RuntimeException cleanupException) {
                log.warn("Could not remove an unattached chat upload after persistence failed");
            }
            throw exception;
        }
        return toAttachment(attachment);
    }

    @Transactional
    public MessageVO sendMessage(Long currentUserId, Long conversationId, String rawContent,
                                 List<Long> requestedAttachmentIds) {
        requireParticipant(currentUserId, conversationId);
        String content = rawContent == null ? "" : rawContent.trim();
        List<ChatMessageAttachmentEntity> attachments = pendingAttachments(
                currentUserId, conversationId, requestedAttachmentIds);
        if (content.length() > 2000) {
            throw new BizException(ResultCode.BAD_REQUEST, "message text must be at most 2000 characters");
        }
        if (content.isEmpty() && attachments.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "message must contain text or attachments");
        }

        ChatMessageEntity message = new ChatMessageEntity();
        message.setConversationId(conversationId);
        message.setSenderId(currentUserId);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);

        for (ChatMessageAttachmentEntity attachment : attachments) {
            int updated = attachmentMapper.update(null, new LambdaUpdateWrapper<ChatMessageAttachmentEntity>()
                    .eq(ChatMessageAttachmentEntity::getId, attachment.getId())
                    .eq(ChatMessageAttachmentEntity::getConversationId, conversationId)
                    .eq(ChatMessageAttachmentEntity::getUploadedBy, currentUserId)
                    .isNull(ChatMessageAttachmentEntity::getMessageId)
                    .gt(ChatMessageAttachmentEntity::getExpiresAt, LocalDateTime.now())
                    .set(ChatMessageAttachmentEntity::getMessageId, message.getId())
                    .set(ChatMessageAttachmentEntity::getExpiresAt, null));
            if (updated != 1) {
                throw new BizException(ResultCode.BAD_REQUEST, "attachment expired or already used");
            }
            attachment.setMessageId(message.getId());
            attachment.setExpiresAt(null);
        }

        conversationMapper.updateLastMessage(conversationId, message.getId());
        readStateMapper.markRead(conversationId, currentUserId, message.getId());

        SysUserEntity sender = sysUserMapper.selectById(currentUserId);
        return toMessage(message, sender, attachments);
    }

    public AttachmentContent openAttachment(Long currentUserId, Long attachmentId, boolean isAdmin) {
        ChatMessageAttachmentEntity attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null || attachment.getMessageId() == null) {
            throw new BizException(ResultCode.NOT_FOUND, "attachment not found");
        }
        requireVisible(currentUserId, attachment.getConversationId(), isAdmin);
        InputStream stream = requireStorage().openChatAttachment(attachment.getObjectKey());
        return new AttachmentContent(stream, attachment.getOriginalName(), attachment.getMimeType(),
                attachment.getSizeBytes());
    }

    public void cleanupExpiredPendingAttachments() {
        List<ChatMessageAttachmentEntity> expired = attachmentMapper.selectList(
                new LambdaQueryWrapper<ChatMessageAttachmentEntity>()
                        .isNull(ChatMessageAttachmentEntity::getMessageId)
                        .lt(ChatMessageAttachmentEntity::getExpiresAt, LocalDateTime.now())
                        .orderByAsc(ChatMessageAttachmentEntity::getExpiresAt)
                        .last("LIMIT 100"));
        if (expired.isEmpty()) return;

        MinioStorageService storage = storageProvider.getIfAvailable();
        if (storage == null) return;
        for (ChatMessageAttachmentEntity attachment : expired) {
            try {
                storage.deleteChatAttachment(attachment.getObjectKey());
                attachmentMapper.deleteById(attachment.getId());
            } catch (RuntimeException exception) {
                log.warn("Could not clean up an expired chat attachment");
            }
        }
    }

    @Transactional
    public void markRead(Long currentUserId, Long conversationId) {
        requireParticipant(currentUserId, conversationId);
        readStateMapper.markRead(conversationId, currentUserId, messageMapper.latestId(conversationId));
    }

    @Transactional
    public void clearConversation(Long currentUserId, Long conversationId) {
        requireParticipant(currentUserId, conversationId);
        List<String> objectKeys = attachmentMapper.selectList(new LambdaQueryWrapper<ChatMessageAttachmentEntity>()
                        .eq(ChatMessageAttachmentEntity::getConversationId, conversationId))
                .stream().map(ChatMessageAttachmentEntity::getObjectKey).toList();

        attachmentMapper.update(null, new LambdaUpdateWrapper<ChatMessageAttachmentEntity>()
                .eq(ChatMessageAttachmentEntity::getConversationId, conversationId)
                .set(ChatMessageAttachmentEntity::getMessageId, null)
                .set(ChatMessageAttachmentEntity::getExpiresAt, LocalDateTime.now().minusMinutes(1)));
        messageMapper.delete(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getConversationId, conversationId));
        conversationMapper.clearLastMessage(conversationId);
        readStateMapper.resetForClearedConversation(conversationId);

        if (!objectKeys.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    MinioStorageService storage = storageProvider.getIfAvailable();
                    if (storage == null) return;
                    for (String objectKey : objectKeys) {
                        try {
                            storage.deleteChatAttachment(objectKey);
                        } catch (RuntimeException exception) {
                            log.warn("Could not immediately remove an attachment from a cleared conversation");
                        }
                    }
                }
            });
        }
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

    private MessageVO toMessage(ChatMessageRow row, List<ChatMessageAttachmentEntity> attachments) {
        MessageVO vo = new MessageVO();
        vo.setId(row.getId());
        vo.setConversationId(row.getConversationId());
        vo.setSenderId(row.getSenderId());
        vo.setSenderUsername(row.getSenderUsername());
        vo.setSenderNickname(row.getSenderNickname());
        vo.setContent(row.getContent());
        vo.setCreatedAt(row.getCreatedAt());
        vo.setAttachments(attachments.stream().map(this::toAttachment).toList());
        return vo;
    }

    private MessageVO toMessage(ChatMessageEntity row, SysUserEntity sender,
                                List<ChatMessageAttachmentEntity> attachments) {
        MessageVO vo = new MessageVO();
        vo.setId(row.getId());
        vo.setConversationId(row.getConversationId());
        vo.setSenderId(row.getSenderId());
        vo.setSenderUsername(sender.getUsername());
        vo.setSenderNickname(sender.getNickname());
        vo.setContent(row.getContent());
        vo.setCreatedAt(row.getCreatedAt());
        vo.setAttachments(attachments.stream().map(this::toAttachment).toList());
        return vo;
    }

    private List<ChatMessageAttachmentEntity> pendingAttachments(Long userId, Long conversationId,
                                                                  List<Long> requestedIds) {
        if (requestedIds == null || requestedIds.isEmpty()) return List.of();
        if (requestedIds.size() > MAX_ATTACHMENTS_PER_MESSAGE
                || requestedIds.stream().anyMatch(id -> id == null)
                || new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw new BizException(ResultCode.BAD_REQUEST, "a message can contain up to 5 unique attachments");
        }

        Map<Long, ChatMessageAttachmentEntity> byId = new HashMap<>();
        attachmentMapper.selectBatchIds(requestedIds).forEach(item -> byId.put(item.getId(), item));
        LocalDateTime now = LocalDateTime.now();
        List<ChatMessageAttachmentEntity> attachments = new ArrayList<>();
        for (Long id : requestedIds) {
            ChatMessageAttachmentEntity attachment = byId.get(id);
            if (attachment == null || !userId.equals(attachment.getUploadedBy())
                    || !conversationId.equals(attachment.getConversationId())
                    || attachment.getMessageId() != null || attachment.getExpiresAt() == null
                    || !attachment.getExpiresAt().isAfter(now)) {
                throw new BizException(ResultCode.BAD_REQUEST, "attachment expired or unavailable");
            }
            attachments.add(attachment);
        }
        return attachments;
    }

    private AttachmentVO toAttachment(ChatMessageAttachmentEntity attachment) {
        AttachmentVO vo = new AttachmentVO();
        vo.setId(attachment.getId());
        vo.setOriginalName(attachment.getOriginalName());
        vo.setMimeType(attachment.getMimeType());
        vo.setSizeBytes(attachment.getSizeBytes());
        return vo;
    }

    private MinioStorageService requireStorage() {
        MinioStorageService storage = storageProvider.getIfAvailable();
        if (storage == null) throw new BizException(ResultCode.INTERNAL_ERROR, "object storage unavailable");
        return storage;
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
        private List<AttachmentVO> attachments;
    }

    @Data
    public static class AttachmentVO {
        private Long id;
        private String originalName;
        private String mimeType;
        private Long sizeBytes;
    }

    public record AttachmentContent(InputStream stream, String originalName, String mimeType, long sizeBytes) {
    }
}
