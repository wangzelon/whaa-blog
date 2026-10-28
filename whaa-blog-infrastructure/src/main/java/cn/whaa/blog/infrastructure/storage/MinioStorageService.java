package cn.whaa.blog.infrastructure.storage;

import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.infrastructure.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "whaa.minio.enabled", havingValue = "true", matchIfMissing = true)
public class MinioStorageService {

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    private final MinioProperties properties;

    private MinioClient client;

    @PostConstruct
    public void init() {
        client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
            }
        } catch (Exception e) {
            log.warn("MinIO init failed, uploads may be unavailable: {}", e.getMessage());
        }
    }

    public UploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME.contains(contentType)) {
            throw new BizException(ResultCode.BAD_REQUEST, "unsupported file type");
        }
        String ext = extractExt(file.getOriginalFilename());
        String objectKey = "uploads/" + UUID.randomUUID() + ext;
        try (InputStream stream = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(objectKey)
                    .stream(stream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "upload failed");
        }
        String url = properties.getPublicUrl().replaceAll("/$", "") + "/" + objectKey;
        return new UploadResult(objectKey, url, contentType, file.getSize(), file.getOriginalFilename());
    }

    private String extractExt(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.'));
    }

    public record UploadResult(String objectKey, String url, String mimeType, long sizeBytes, String originalName) {
    }
}
