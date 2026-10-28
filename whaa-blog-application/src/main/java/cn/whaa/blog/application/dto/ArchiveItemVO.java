package cn.whaa.blog.application.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArchiveItemVO {
    private String slug;
    private String title;
    private LocalDateTime publishedAt;
}
