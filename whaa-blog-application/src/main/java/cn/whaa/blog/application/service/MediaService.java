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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final ObjectProvider<MinioStorageService> minioStorageService;
    private final MediaMapper mediaMapper;

    public MediaEntity upload(MultipartFile file, Long uploaderId) {
        MinioStorageService storage = minioStorageService.getIfAvailable();
        if (storage == null) {
            throw new BizException(ResultCode.INTERNAL_ERROR, "object storage unavailable");
        }
        MinioStorageService.UploadResult result = storage.upload(file);
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

    public PageResult<MediaEntity> list(long page, long size) {
        Page<MediaEntity> result = mediaMapper.selectPage(new Page<>(page, size), null);
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }
}
