package cn.whaa.blog.application.service;

import cn.whaa.blog.application.dto.PageResult;
import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.infrastructure.persistence.entity.MediaEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.MediaMapper;
import cn.whaa.blog.infrastructure.storage.MinioStorageService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MediaService {

    private static final long MAX_IMAGE_SIZE_BYTES = 20L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE_BYTES = 500L * 1024 * 1024;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml",
            "video/mp4", "video/webm"
    );
    private static final Map<String, String> MIME_TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".png", "image/png"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".webm", "video/webm")
    );

    private final ObjectProvider<MinioStorageService> minioStorageService;
    private final MediaMapper mediaMapper;

    public MediaEntity upload(MultipartFile file, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "file is empty");
        }
        String mimeType = resolveMimeType(file);
        boolean video = mimeType.startsWith("video/");
        long maxSize = video ? MAX_VIDEO_SIZE_BYTES : MAX_IMAGE_SIZE_BYTES;
        if (file.getSize() > maxSize) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    video ? "video files must be 500 MB or smaller" : "image files must be 20 MB or smaller");
        }

        MinioStorageService storage = minioStorageService.getIfAvailable();
        if (storage == null) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "object storage unavailable");
        }
        MinioStorageService.UploadResult result = storage.upload(file, mimeType);
        MediaEntity entity = new MediaEntity();
        entity.setObjectKey(result.objectKey());
        entity.setUrl(result.url());
        entity.setOriginalName(result.originalName());
        entity.setMimeType(result.mimeType());
        entity.setSizeBytes(result.sizeBytes());
        entity.setUploaderId(uploaderId);
        entity.setCreatedAt(LocalDateTime.now());
        mediaMapper.insert(entity);
        return entity;
    }

    private String resolveMimeType(MultipartFile file) {
        String extension = extensionOf(file.getOriginalFilename());
        String mimeType = file.getContentType();
        if (!StringUtils.hasText(mimeType) || "application/octet-stream".equalsIgnoreCase(mimeType)) {
            mimeType = MIME_TYPES_BY_EXTENSION.get(extension);
        }
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BizException(ResultCode.BAD_REQUEST, "unsupported media type; use an image, MP4, or WebM video");
        }
        if (mimeType.startsWith("video/") && !mimeType.equals(MIME_TYPES_BY_EXTENSION.get(extension))) {
            throw new BizException(ResultCode.BAD_REQUEST, "video extension and content type do not match");
        }
        return mimeType;
    }

    private String extensionOf(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        String normalized = name.replace('\\', '/');
        int dot = normalized.lastIndexOf('.');
        return dot < 0 ? "" : normalized.substring(dot).toLowerCase(java.util.Locale.ROOT);
    }

    public PageResult<MediaEntity> list(long page, long size) {
        Page<MediaEntity> result = mediaMapper.selectPage(new Page<>(page, size), null);
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }
}
