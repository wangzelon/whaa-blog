package cn.whaa.blog.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "whaa.minio")
public class MinioProperties {
    private boolean enabled = true;
    private String endpoint = "http://localhost:9000";
    private String accessKey;
    private String secretKey;
    private String bucket = "whaa-blog";
    private String publicUrl = "http://localhost:9000/whaa-blog";
}
