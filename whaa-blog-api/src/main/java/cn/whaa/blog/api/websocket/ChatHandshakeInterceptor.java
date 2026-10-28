package cn.whaa.blog.api.websocket;

import cn.whaa.blog.infrastructure.security.ChatTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    static final String USER_ID_ATTRIBUTE = "chatUserId";
    static final String ADMIN_ATTRIBUTE = "chatAdmin";
    private static final String TICKET_PROTOCOL_PREFIX = "whaa.ticket.";

    private final ChatTicketService ticketService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        HttpHeaders headers = request.getHeaders();
        String ticket = headers.getOrDefault("Sec-WebSocket-Protocol", java.util.List.of()).stream()
                .filter(protocol -> protocol.startsWith(TICKET_PROTOCOL_PREFIX))
                .map(protocol -> protocol.substring(TICKET_PROTOCOL_PREFIX.length()))
                .findFirst()
                .orElse(null);
        ChatTicketService.TicketIdentity identity = ticketService.consume(ticket);
        if (identity == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(USER_ID_ATTRIBUTE, identity.userId());
        attributes.put(ADMIN_ATTRIBUTE, identity.admin());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing to release; tickets are single-use and expire automatically.
    }
}
