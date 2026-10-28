package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("page")
public class PageEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String slug;
    private String title;
    private String pageType;
    private String icon;
    private String contentMd;
    private String contentHtml;
    private Boolean allowComment;
    private LocalDateTime updatedAt;
}
