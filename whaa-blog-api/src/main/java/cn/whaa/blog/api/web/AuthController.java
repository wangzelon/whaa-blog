package cn.whaa.blog.api.web;

import cn.whaa.blog.application.service.AuthService;
import cn.whaa.blog.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<AuthService.TokenPair> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return Result.ok(authService.login(request.getUsername(), request.getPassword(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/refresh")
    public Result<AuthService.TokenPair> refresh(@RequestBody RefreshRequest request) {
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
}
