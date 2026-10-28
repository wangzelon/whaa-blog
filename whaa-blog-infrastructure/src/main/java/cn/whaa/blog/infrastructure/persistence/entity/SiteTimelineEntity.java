package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("site_timeline")
public class SiteTimelineEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate eventDate;
    private String title;
    private String content;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
