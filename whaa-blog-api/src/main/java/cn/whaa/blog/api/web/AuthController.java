package cn.whaa.blog.api.web;

import cn.whaa.blog.application.service.AuthService;
import cn.whaa.blog.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<AuthService.TokenPair> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.ok(authService.login(request.getUsername(), request.getPassword(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/register")
    public Result<AuthService.TokenPair> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return Result.ok(authService.register(
                request.getUsername(), request.getPassword(), request.getNickname(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public Result<AuthService.TokenPair> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(authService.refresh(request.getRefreshToken()));
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 32)
        private String username;
        @NotBlank
        @Size(min = 12, max = 72)
        private String password;
        @Size(max = 64)
        private String nickname;
    }
}
