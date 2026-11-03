package cn.whaa.blog.infrastructure.security;

import cn.whaa.blog.domain.enums.UserStatus;
import cn.whaa.blog.infrastructure.persistence.mapper.SysUserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserMapper sysUserMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtTokenProvider.parseClaims(token);
                if ("access".equals(claims.get("type", String.class))) {
                    Long userId = Long.valueOf(claims.getSubject());
                    String role = claims.get("role", String.class);
                    Object versionClaim = claims.get("tokenVersion");
                    long tokenVersion = versionClaim instanceof Number number ? number.longValue() : 0L;
                    var userState = sysUserMapper.authenticationState(userId);
                    if (userState != null && UserStatus.ACTIVE.name().equals(userState.getStatus())
                            && role != null && role.equals(userState.getRole())
                            && tokenVersion == (userState.getTokenVersion() == null ? 0L : userState.getTokenVersion())) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                claims.getSubject(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
