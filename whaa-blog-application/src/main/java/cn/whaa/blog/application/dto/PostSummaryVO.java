package cn.whaa.blog.application.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostSummaryVO {
    private Long id;
    private String slug;
    private String title;
    private String description;
    private String excerpt;
    private String coverUrl;
    private Boolean pinned;
    private Boolean hideTime;
    private LocalDateTime publishedAt;
    private CategoryVO category;
    private List<TagVO> tags;
}
