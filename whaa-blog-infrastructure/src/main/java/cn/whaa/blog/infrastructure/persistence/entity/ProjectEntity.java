package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project")
public class ProjectEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String description;
    private String url;
    private String icon;
    private String categoryCode;
    private Integer sortOrder;
    private Boolean visible;
    private LocalDateTime createdAt;
}
