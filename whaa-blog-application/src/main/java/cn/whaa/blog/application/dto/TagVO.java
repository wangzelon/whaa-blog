package cn.whaa.blog.application.dto;

import lombok.Data;

@Data
public class TagVO {
    private Long id;
    private String name;
    private String slug;
    private Long weight;
}
