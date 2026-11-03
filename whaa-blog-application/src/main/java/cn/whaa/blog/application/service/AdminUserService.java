package cn.whaa.blog.application.service;

import cn.whaa.blog.application.dto.PageResult;
import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.domain.enums.UserStatus;
import cn.whaa.blog.infrastructure.persistence.entity.SysUserEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.SysUserMapper;
import cn.whaa.blog.infrastructure.persistence.projection.SysUserAuthState;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String USER_ROLE = "USER";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResult<UserVO> list(long requestedPage, long requestedSize, String query, String statusFilter) {
        long pageNumber = Math.max(1, requestedPage);
        long pageSize = Math.max(1, Math.min(requestedSize, 100));
        String trimmedQuery = query == null ? "" : query.trim();
        String normalizedQuery = trimmedQuery.length() > 64 ? trimmedQuery.substring(0, 64) : trimmedQuery;

        LambdaQueryWrapper<SysUserEntity> wrapper = new LambdaQueryWrapper<SysUserEntity>()
                .select(SysUserEntity::getId, SysUserEntity::getUsername, SysUserEntity::getNickname,
                        SysUserEntity::getEmail, SysUserEntity::getRole, SysUserEntity::getStatus,
                        SysUserEntity::getCreatedAt, SysUserEntity::getDeletedAt)
                .eq(SysUserEntity::getRole, USER_ROLE)
                .orderByDesc(SysUserEntity::getId);
        if (!normalizedQuery.isBlank()) {
            wrapper.and(condition -> condition
                    .like(SysUserEntity::getUsername, normalizedQuery)
                    .or()
                    .like(SysUserEntity::getNickname, normalizedQuery));
        }
        if (statusFilter != null && !statusFilter.isBlank()) {
            wrapper.eq(SysUserEntity::getStatus, parseStatus(statusFilter).name());
        }

        Page<SysUserEntity> result = sysUserMapper.selectPage(new Page<>(pageNumber, pageSize), wrapper);
        return new PageResult<>(result.getRecords().stream().map(this::toUserVO).toList(),
                result.getTotal(), pageNumber, pageSize);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 8
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "password must be at least 8 characters and no more than 72 UTF-8 bytes");
        }
        requireManagedUser(userId);

        int updated = sysUserMapper.update(null, managedUserUpdate(userId)
                .set(SysUserEntity::getPasswordHash, passwordEncoder.encode(newPassword))
                .setSql("token_version = token_version + 1"));
        ensureUpdated(updated, userId);
    }

    @Transactional
    public void changeStatus(Long userId, UserStatus requestedStatus) {
        if (requestedStatus != UserStatus.ACTIVE && requestedStatus != UserStatus.DISABLED) {
            throw new BizException(ResultCode.BAD_REQUEST, "status must be ACTIVE or DISABLED");
        }
        SysUserAuthState user = requireManagedUser(userId);
        if (requestedStatus.name().equals(user.getStatus())) return;

        int updated = sysUserMapper.update(null, managedUserUpdate(userId)
                .set(SysUserEntity::getStatus, requestedStatus.name())
                .setSql("token_version = token_version + 1"));
        ensureUpdated(updated, userId);
    }

    @Transactional
    public void deleteUser(Long userId) {
        requireManagedUser(userId);
        String anonymizedUsername = "deleted_" + userId + "_" + UUID.randomUUID().toString().replace("-", "");
        String unusablePasswordHash = passwordEncoder.encode(
                UUID.randomUUID().toString() + UUID.randomUUID().toString());

        int updated = sysUserMapper.update(null, managedUserUpdate(userId)
                .set(SysUserEntity::getUsername, anonymizedUsername)
                .set(SysUserEntity::getNickname, "已注销用户")
                .set(SysUserEntity::getEmail, null)
                .set(SysUserEntity::getAvatar, null)
                .set(SysUserEntity::getPasswordHash, unusablePasswordHash)
                .set(SysUserEntity::getStatus, UserStatus.DELETED.name())
                .set(SysUserEntity::getDeletedAt, LocalDateTime.now())
                .setSql("token_version = token_version + 1"));
        ensureUpdated(updated, userId);
    }

    private LambdaUpdateWrapper<SysUserEntity> managedUserUpdate(Long userId) {
        return new LambdaUpdateWrapper<SysUserEntity>()
                .eq(SysUserEntity::getId, userId)
                .eq(SysUserEntity::getRole, USER_ROLE)
                .ne(SysUserEntity::getStatus, UserStatus.DELETED.name());
    }

    private SysUserAuthState requireManagedUser(Long userId) {
        SysUserAuthState user = sysUserMapper.authenticationState(userId);
        if (user == null || !USER_ROLE.equals(user.getRole())
                || UserStatus.DELETED.name().equals(user.getStatus())) {
            throw new BizException(ResultCode.NOT_FOUND, "user not found");
        }
        return user;
    }

    private void ensureUpdated(int updated, Long userId) {
        if (updated != 1) {
            throw new BizException(ResultCode.NOT_FOUND, "user not found: " + userId);
        }
    }

    private UserStatus parseStatus(String value) {
        try {
            return UserStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BizException(ResultCode.BAD_REQUEST, "unsupported user status");
        }
    }

    private UserVO toUserVO(SysUserEntity user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(UserStatus.DELETED.name().equals(user.getStatus())
                ? "已注销用户 #" + user.getId()
                : user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setDeletedAt(user.getDeletedAt());
        return vo;
    }

    @Data
    public static class UserVO {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String role;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime deletedAt;
    }
}
