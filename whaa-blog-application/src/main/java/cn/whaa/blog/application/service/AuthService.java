package cn.whaa.blog.application.service;

import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.domain.enums.UserRole;
import cn.whaa.blog.infrastructure.cache.CacheService;
import cn.whaa.blog.infrastructure.persistence.entity.SysUserEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.SysUserMapper;
import cn.whaa.blog.infrastructure.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CacheService cacheService;

    public TokenPair login(String username, String password, String ip) {
        if (!cacheService.rateLimit("login:ip:" + ip, 20, Duration.ofMinutes(10))) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS);
        }
        String normalizedUsername = username == null ? "" : username.trim();
        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, normalizedUsername));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenPair register(String username, String password, String nickname, String ip) {
        if (!cacheService.rateLimit("register:ip:" + ip, 5, Duration.ofMinutes(15))) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS);
        }

        String normalizedUsername = username == null ? "" : username.trim();
        if (!normalizedUsername.matches("[A-Za-z0-9._-]{3,32}")) {
            throw new BizException(ResultCode.BAD_REQUEST, "username must be 3-32 letters, digits, dots, dashes, or underscores");
        }
        if ("admin".equalsIgnoreCase(normalizedUsername)) {
            throw new BizException(ResultCode.BAD_REQUEST, "username is reserved");
        }
        if (password == null || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BizException(ResultCode.BAD_REQUEST, "password must be at least 12 characters and no more than 72 UTF-8 bytes");
        }
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, normalizedUsername)) > 0) {
            throw new BizException(ResultCode.BAD_REQUEST, "username is already taken");
        }

        String normalizedNickname = nickname == null || nickname.isBlank() ? normalizedUsername : nickname.trim();
        if (normalizedNickname.length() > 64) {
            throw new BizException(ResultCode.BAD_REQUEST, "nickname must be at most 64 characters");
        }

        SysUserEntity user = new SysUserEntity();
        user.setUsername(normalizedUsername);
        user.setNickname(normalizedNickname);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.USER.name());
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ResultCode.BAD_REQUEST, "username is already taken");
        }
        return issueTokens(user);
    }

    public TokenPair refresh(String refreshToken) {
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        Long userId = Long.parseLong(claims.getSubject());
        SysUserEntity user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return issueTokens(user);
    }

    private TokenPair issueTokens(SysUserEntity user) {
        String access = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refresh = jwtTokenProvider.createRefreshToken(user.getId());
        TokenPair pair = new TokenPair();
        pair.setAccessToken(access);
        pair.setRefreshToken(refresh);
        pair.setUserId(user.getId());
        pair.setUsername(user.getUsername());
        pair.setNickname(user.getNickname());
        pair.setRole(user.getRole());
        return pair;
    }

    @Data
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
        private Long userId;
        private String username;
        private String nickname;
        private String role;
    }
}
