package cn.whaa.blog.application.dto;

import lombok.Data;

@Data
public class CategoryVO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long postCount;
}
