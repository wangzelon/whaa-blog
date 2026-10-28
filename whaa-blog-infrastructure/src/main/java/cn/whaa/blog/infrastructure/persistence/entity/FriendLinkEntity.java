package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_link")
public class FriendLinkEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String blogName;
    private String url;
    private String avatar;
    private String description;
    private String color;
    private String email;
    private Integer sortOrder;
    private Boolean visible;
    private LocalDateTime createdAt;
}
