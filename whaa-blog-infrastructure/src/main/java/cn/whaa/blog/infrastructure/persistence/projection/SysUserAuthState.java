package cn.whaa.blog.infrastructure.persistence.projection;

import lombok.Data;

@Data
public class SysUserAuthState {
    private String role;
    private String status;
    private Long tokenVersion;
}
