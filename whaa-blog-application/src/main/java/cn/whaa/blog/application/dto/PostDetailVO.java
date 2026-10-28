package cn.whaa.blog.application.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PostDetailVO extends PostSummaryVO {
    private String contentHtml;
    private String contentMd;
    private Integer viewCount;
    private Boolean allowComment;
    private String authorName;
}
