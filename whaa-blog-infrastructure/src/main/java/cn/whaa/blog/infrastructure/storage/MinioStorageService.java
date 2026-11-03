package cn.whaa.blog.infrastructure.storage;

import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.infrastructure.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
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
    private static final long MAX_CHAT_FILE_SIZE = 20L * 1024 * 1024;
    private static final Map<String, String> CHAT_MIME_BY_EXTENSION = Map.ofEntries(
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".png", "image/png"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry(".xls", "application/vnd.ms-excel"),
            Map.entry(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry(".ppt", "application/vnd.ms-powerpoint"),
            Map.entry(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry(".txt", "text/plain"),
            Map.entry(".csv", "text/csv"),
            Map.entry(".zip", "application/zip")
    );
    private static final Set<String> OLE_EXTENSIONS = Set.of(".doc", ".xls", ".ppt");
    private static final Set<String> ZIP_EXTENSIONS = Set.of(".docx", ".xlsx", ".pptx", ".zip");

    private final MinioProperties properties;

    private MinioClient client;

    @PostConstruct
    public void init() {
        client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        try {
            ensureBucket(properties.getBucket());
            ensureBucket(properties.getChatBucket());
        } catch (Exception e) {
            log.warn("MinIO init failed, uploads may be unavailable: {}", e.getMessage());
        }
    }

    private void ensureBucket(String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
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

    public ChatUploadResult uploadChatAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "file is empty");
        }
        if (file.getSize() > MAX_CHAT_FILE_SIZE) {
            throw new BizException(ResultCode.BAD_REQUEST, "each attachment must be 20 MB or smaller");
        }

        String originalName = safeOriginalName(file.getOriginalFilename());
        String extension = extensionOf(originalName);
        String mimeType = CHAT_MIME_BY_EXTENSION.get(extension);
        if (mimeType == null || !hasExpectedSignature(file, extension)) {
            throw new BizException(ResultCode.BAD_REQUEST, "unsupported or invalid file type");
        }

        String objectKey = "chat/" + UUID.randomUUID() + extension;
        try (InputStream stream = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.getChatBucket())
                    .object(objectKey)
                    .stream(stream, file.getSize(), -1)
                    .contentType(mimeType)
                    .build());
        } catch (Exception e) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "attachment upload failed");
        }
        return new ChatUploadResult(objectKey, mimeType, file.getSize(), originalName);
    }

    public GetObjectResponse openChatAttachment(String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(properties.getChatBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new BizException(ResultCode.NOT_FOUND, "attachment not found");
        }
    }

    public void deleteChatAttachment(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getChatBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "attachment cleanup failed");
        }
    }

    private String safeOriginalName(String name) {
        String normalized = name == null ? "file" : name.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_").trim();
        if (normalized.isBlank()) normalized = "file";
        if (normalized.length() > 255) {
            throw new BizException(ResultCode.BAD_REQUEST, "filename must be 255 characters or fewer");
        }
        return normalized;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private boolean hasExpectedSignature(MultipartFile file, String extension) {
        try (InputStream stream = file.getInputStream()) {
            byte[] header = stream.readNBytes(16);
            if (extension.equals(".jpg") || extension.equals(".jpeg")) {
                return header.length >= 3 && unsigned(header[0]) == 0xff
                        && unsigned(header[1]) == 0xd8 && unsigned(header[2]) == 0xff;
            }
            if (extension.equals(".png")) {
                return startsWith(header, new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
            }
            if (extension.equals(".gif")) {
                String signature = new String(header, 0, Math.min(header.length, 6), StandardCharsets.US_ASCII);
                return signature.equals("GIF87a") || signature.equals("GIF89a");
            }
            if (extension.equals(".webp")) {
                return header.length >= 12 && asciiAt(header, 0, "RIFF") && asciiAt(header, 8, "WEBP");
            }
            if (extension.equals(".pdf")) {
                return startsWith(header, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            }
            if (OLE_EXTENSIONS.contains(extension)) {
                return startsWith(header, new byte[]{(byte) 0xd0, (byte) 0xcf, 0x11, (byte) 0xe0,
                        (byte) 0xa1, (byte) 0xb1, 0x1a, (byte) 0xe1});
            }
            if (ZIP_EXTENSIONS.contains(extension)) {
                return header.length >= 4 && header[0] == 'P' && header[1] == 'K'
                        && header[2] == 3 && header[3] == 4;
            }
            if (extension.equals(".txt") || extension.equals(".csv")) {
                for (byte value : header) {
                    if (value == 0) return false;
                }
                return header.length > 0;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) return false;
        }
        return true;
    }

    private boolean asciiAt(byte[] value, int offset, String expected) {
        if (value.length < offset + expected.length()) return false;
        for (int i = 0; i < expected.length(); i++) {
            if (value[offset + i] != (byte) expected.charAt(i)) return false;
        }
        return true;
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }

    private String extractExt(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.'));
    }

    public record UploadResult(String objectKey, String url, String mimeType, long sizeBytes, String originalName) {
    }

    public record ChatUploadResult(String objectKey, String mimeType, long sizeBytes, String originalName) {
    }
}
