package cn.whaa.blog.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> T getOrLoad(String key, Duration ttl, Class<T> type, Supplier<T> loader) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, type);
            } catch (Exception ignored) {
                redisTemplate.delete(key);
            }
        }
        T value = loader.get();
        if (value != null) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
            } catch (Exception ignored) {
                // cache write failure should not break request
            }
        }
        return value;
    }

    public <T> T getOrLoad(String key, Duration ttl, TypeReference<T> typeRef, Supplier<T> loader) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, typeRef);
            } catch (Exception ignored) {
                redisTemplate.delete(key);
            }
        }
        T value = loader.get();
        if (value != null) {
            try {
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
            } catch (Exception ignored) {
            }
        }
        return value;
    }

    public void evict(String key) {
        redisTemplate.delete(key);
    }

    public void evictPattern(String prefix) {
        var keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public boolean rateLimit(String key, int maxCount, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count != null && count <= maxCount;
    }

    public void incrementHyperLogLog(String key, String element) {
        redisTemplate.opsForHyperLogLog().add(key, element);
    }

    public long hyperLogLogCount(String key) {
        Long size = redisTemplate.opsForHyperLogLog().size(key);
        return size == null ? 0 : size;
    }
}
