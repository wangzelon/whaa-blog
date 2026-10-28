package cn.whaa.blog.infrastructure.persistence.projection;

import lombok.Data;

@Data
public class ChatUserRow {
    private Long id;
    private String username;
    private String nickname;
    private String role;
}
