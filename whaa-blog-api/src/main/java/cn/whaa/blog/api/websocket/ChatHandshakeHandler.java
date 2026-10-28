package cn.whaa.blog.api.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.List;
import java.util.Map;

public class ChatHandshakeHandler extends DefaultHandshakeHandler {

    static final String CHAT_PROTOCOL = "whaa.chat.v1";

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object userId = attributes.get(ChatHandshakeInterceptor.USER_ID_ATTRIBUTE);
        return userId == null ? null : () -> String.valueOf(userId);
    }

    @Override
    protected String selectProtocol(List<String> requestedProtocols, WebSocketHandler wsHandler) {
        return requestedProtocols.contains(CHAT_PROTOCOL) ? CHAT_PROTOCOL : null;
    }
}
