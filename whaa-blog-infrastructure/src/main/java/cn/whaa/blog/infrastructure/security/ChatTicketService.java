package cn.whaa.blog.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatTicketService {

    private static final String KEY_PREFIX = "chat:ws-ticket:";
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    public String issue(Long userId, boolean isAdmin) {
        String ticket = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + ticket, userId + ":" + (isAdmin ? "1" : "0"), TICKET_TTL);
        return ticket;
    }

    public TicketIdentity consume(String ticket) {
        if (ticket == null || !ticket.matches("[0-9a-fA-F-]{36}")) return null;
        String identity = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + ticket);
        if (identity == null) return null;
        try {
            String[] parts = identity.split(":", 2);
            if (parts.length != 2) return null;
            return new TicketIdentity(Long.valueOf(parts[0]), "1".equals(parts[1]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public record TicketIdentity(Long userId, boolean admin) { }
}
