package cn.whaa.blog.application.service;

import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
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
        SysUserEntity user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "invalid credentials");
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
        pair.setUsername(user.getUsername());
        pair.setNickname(user.getNickname());
        return pair;
    }

    @Data
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
        private String username;
        private String nickname;
    }
}
