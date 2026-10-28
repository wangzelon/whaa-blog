package cn.whaa.blog.application.service;

import cn.whaa.blog.domain.enums.PostStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostSaveCommand {
    private Long id;
    private String title;
    private String slug;
    private String description;
    private String excerpt;
    private String contentMd;
    private PostStatus status;
    private Long categoryId;
    private List<Long> tagIds;
    private String coverUrl;
    private Boolean pinned;
    private Boolean allowComment;
    private Boolean hideTime;
    private LocalDateTime publishedAt;
}
