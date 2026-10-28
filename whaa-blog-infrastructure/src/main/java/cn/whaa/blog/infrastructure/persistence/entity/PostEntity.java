package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post")
public class PostEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String slug;
    private String title;
    private String description;
    private String excerpt;
    private String contentMd;
    private String contentHtml;
    private String status;
    private Long categoryId;
    private Long authorId;
    private String coverUrl;
    private Integer viewCount;
    private Boolean pinned;
    private Boolean allowComment;
    private Boolean hideTime;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
