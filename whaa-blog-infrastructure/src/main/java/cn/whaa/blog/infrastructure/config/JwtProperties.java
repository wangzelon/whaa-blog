package cn.whaa.blog.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;

@Data
@ConfigurationProperties(prefix = "whaa.jwt")
public class JwtProperties {
    private String secret;
    private long accessTokenMinutes = 120;
    private long refreshTokenDays = 7;

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("WHA_JWT_SECRET must contain at least 32 bytes.");
        }
    }
}
