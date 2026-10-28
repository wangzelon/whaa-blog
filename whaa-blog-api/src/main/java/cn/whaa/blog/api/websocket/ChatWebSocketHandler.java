package cn.whaa.blog.api.websocket;

import cn.whaa.blog.application.service.ChatService;
import cn.whaa.blog.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_FRAME_BYTES = 8 * 1024;
    private final ObjectMapper objectMapper;
    private final ChatService chatService;
    private final Map<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userId(session);
        sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (message.getPayloadLength() > MAX_FRAME_BYTES) {
            session.close(CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }
        Long senderId = userId(session);
        try {
            JsonNode body = objectMapper.readTree(message.getPayload());
            if (!"SEND".equals(body.path("type").asText())
                    || !body.path("conversationId").canConvertToLong()) {
                sendError(session, "invalid message");
                return;
            }
            var savedMessage = chatService.sendMessage(
                    senderId,
                    body.path("conversationId").asLong(),
                    body.path("content").asText("")
            );
            String event = objectMapper.writeValueAsString(Map.of("type", "MESSAGE", "message", savedMessage));
            Set<Long> deliveredTo = new HashSet<>(chatService.participants(savedMessage.getConversationId()));
            for (Long participantId : deliveredTo) {
                broadcast(participantId, event);
            }
            for (Map.Entry<Long, Set<WebSocketSession>> entry : sessionsByUser.entrySet()) {
                if (deliveredTo.contains(entry.getKey())) continue;
                boolean adminConnected = entry.getValue().stream()
                        .anyMatch(candidate -> Boolean.TRUE.equals(candidate.getAttributes()
                                .get(ChatHandshakeInterceptor.ADMIN_ATTRIBUTE)));
                if (adminConnected) broadcast(entry.getKey(), event);
            }
        } catch (Exception exception) {
            log.debug("WebSocket chat message rejected: {}", exception.getClass().getSimpleName());
            String messageText = exception instanceof BizException
                    ? exception.getMessage()
                    : "message was not sent; please try again";
            sendError(session, messageText);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userId(session);
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) sessionsByUser.remove(userId, sessions);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    private void broadcast(Long userId, String payload) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) return;
        for (WebSocketSession session : sessions) {
            send(session, payload);
        }
    }

    private void sendError(WebSocketSession session, String message) throws Exception {
        send(session, objectMapper.writeValueAsString(Map.of("type", "ERROR", "message", message)));
    }

    private void send(WebSocketSession session, String payload) {
        if (!session.isOpen()) return;
        try {
            synchronized (session) {
                if (session.isOpen()) session.sendMessage(new TextMessage(payload));
            }
        } catch (Exception exception) {
            log.debug("Could not deliver chat event to a WebSocket session");
        }
    }

    private Long userId(WebSocketSession session) {
        return Long.valueOf(session.getPrincipal().getName());
    }
}
