package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("album_photo")
public class AlbumPhotoEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long albumId;
    private String url;
    private String caption;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
