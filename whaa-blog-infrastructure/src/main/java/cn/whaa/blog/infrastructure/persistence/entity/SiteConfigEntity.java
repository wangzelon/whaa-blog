package cn.whaa.blog.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("site_config")
public class SiteConfigEntity {
    @TableId
    private String configKey;
    private String configValue;
    private LocalDateTime updatedAt;
}
