package cn.whaa.blog.api.web;

import cn.whaa.blog.application.service.ChatService;
import cn.whaa.blog.common.result.Result;
import cn.whaa.blog.infrastructure.security.ChatTicketService;
import cn.whaa.blog.api.websocket.ChatWebSocketHandler;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatTicketService ticketService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @GetMapping("/contacts")
    public Result<List<ChatService.ContactVO>> contacts(
            @RequestParam(required = false, defaultValue = "") String q,
            Authentication authentication) {
        return Result.ok(chatService.searchContacts(currentUserId(authentication), q));
    }

    @GetMapping("/conversations")
    public Result<List<ChatService.ConversationVO>> conversations(Authentication authentication) {
        return Result.ok(chatService.listConversations(currentUserId(authentication), isAdmin(authentication)));
    }

    @PostMapping("/conversations")
    public Result<ChatService.ConversationVO> openConversation(
            @RequestBody OpenConversationRequest request, Authentication authentication) {
        return Result.ok(chatService.openConversation(currentUserId(authentication), request.getPeerUserId()));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<ChatService.MessageVO>> messages(
            @PathVariable Long id,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        return Result.ok(chatService.history(currentUserId(authentication), id, beforeId, size, isAdmin(authentication)));
    }

    @PostMapping("/conversations/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, Authentication authentication) {
        chatService.markRead(currentUserId(authentication), id);
        return Result.ok();
    }

    @DeleteMapping("/conversations/{id}/messages")
    public Result<Void> clearConversation(@PathVariable Long id, Authentication authentication) {
        chatService.clearConversation(currentUserId(authentication), id);
        chatWebSocketHandler.broadcastConversationCleared(id);
        return Result.ok();
    }

    @PostMapping(value = "/conversations/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ChatService.AttachmentVO> uploadAttachment(
            @PathVariable Long id, @RequestParam("file") MultipartFile file, Authentication authentication) {
        return Result.ok(chatService.uploadAttachment(currentUserId(authentication), id, file));
    }

    @GetMapping("/attachments/{attachmentId}/content")
    public ResponseEntity<InputStreamResource> attachmentContent(
            @PathVariable Long attachmentId, Authentication authentication) {
        ChatService.AttachmentContent content = chatService.openAttachment(
                currentUserId(authentication), attachmentId, isAdmin(authentication));
        ContentDisposition disposition = (content.mimeType().startsWith("image/")
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(content.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.mimeType()))
                .contentLength(content.sizeBytes())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(content.stream()));
    }

    @PostMapping("/ws-ticket")
    public Result<TicketVO> issueWebSocketTicket(Authentication authentication) {
        TicketVO vo = new TicketVO();
        vo.setTicket(ticketService.issue(currentUserId(authentication), isAdmin(authentication)));
        return Result.ok(vo);
    }

    private Long currentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    @Data
    public static class OpenConversationRequest {
        private Long peerUserId;
    }

    @Data
    public static class TicketVO {
        private String ticket;
    }
}
