package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag")
public class TagEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String slug;
    private LocalDateTime createdAt;
}
