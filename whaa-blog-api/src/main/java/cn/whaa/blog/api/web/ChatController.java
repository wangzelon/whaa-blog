package cn.whaa.blog.api.web;

import cn.whaa.blog.application.service.ChatService;
import cn.whaa.blog.common.result.Result;
import cn.whaa.blog.infrastructure.security.ChatTicketService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatTicketService ticketService;

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
