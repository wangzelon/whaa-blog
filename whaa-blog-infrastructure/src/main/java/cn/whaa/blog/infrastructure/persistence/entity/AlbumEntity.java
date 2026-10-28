package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("album")
public class AlbumEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String slug;
    private String title;
    private String description;
    private String coverUrl;
    private Boolean visible;
    private LocalDateTime createdAt;
}
