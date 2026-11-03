package cn.whaa.blog.api.web.admin;

import cn.whaa.blog.api.websocket.ChatWebSocketHandler;
import cn.whaa.blog.application.dto.PageResult;
import cn.whaa.blog.application.service.AdminUserService;
import cn.whaa.blog.common.result.Result;
import cn.whaa.blog.domain.enums.UserStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    @GetMapping
    public Result<PageResult<AdminUserService.UserVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status) {
        return Result.ok(adminUserService.list(page, size, q, status));
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request) {
        adminUserService.resetPassword(id, request == null ? null : request.getPassword());
        chatWebSocketHandler.disconnectUser(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody ChangeStatusRequest request) {
        UserStatus status = request == null ? null : request.getStatus();
        adminUserService.changeStatus(id, status);
        if (status == UserStatus.DISABLED) {
            chatWebSocketHandler.disconnectUser(id);
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        chatWebSocketHandler.disconnectUser(id);
        return Result.ok();
    }

    @Data
    public static class ResetPasswordRequest {
        private String password;
    }

    @Data
    public static class ChangeStatusRequest {
        private UserStatus status;
    }
}
