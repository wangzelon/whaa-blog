package cn.whaa.blog.application.service;

import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.common.util.SlugUtils;
import cn.whaa.blog.infrastructure.persistence.entity.AlbumEntity;
import cn.whaa.blog.infrastructure.persistence.entity.AlbumPhotoEntity;
import cn.whaa.blog.infrastructure.persistence.entity.MediaEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.AlbumMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.AlbumPhotoMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.MediaMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumMapper albumMapper;
    private final AlbumPhotoMapper albumPhotoMapper;
    private final MediaMapper mediaMapper;

    public List<AlbumEntity> listPublic() {
        return albumMapper.selectList(new LambdaQueryWrapper<AlbumEntity>()
                .eq(AlbumEntity::getVisible, true)
                .orderByDesc(AlbumEntity::getCreatedAt));
    }

    public List<AlbumEntity> listAdmin() {
        return albumMapper.selectList(new LambdaQueryWrapper<AlbumEntity>()
                .orderByDesc(AlbumEntity::getCreatedAt));
    }

    public AlbumDetailVO getBySlug(String slug) {
        AlbumEntity album = albumMapper.selectOne(new LambdaQueryWrapper<AlbumEntity>().eq(AlbumEntity::getSlug, slug).eq(AlbumEntity::getVisible, true));
        if (album == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return toDetail(album);
    }

    public AlbumDetailVO getAdminById(Long id) {
        AlbumEntity album = albumMapper.selectById(id);
        if (album == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return toDetail(album);
    }

    private AlbumDetailVO toDetail(AlbumEntity album) {
        AlbumDetailVO vo = new AlbumDetailVO();
        vo.setAlbum(album);
        List<AlbumPhotoEntity> photos = albumPhotoMapper.selectList(new LambdaQueryWrapper<AlbumPhotoEntity>()
                .eq(AlbumPhotoEntity::getAlbumId, album.getId())
                .orderByAsc(AlbumPhotoEntity::getSortOrder));
        if (!photos.isEmpty()) {
            List<String> urls = photos.stream()
                    .map(AlbumPhotoEntity::getUrl)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .toList();
            if (!urls.isEmpty()) {
                Map<String, MediaEntity> mediaByUrl = new HashMap<>();
                mediaMapper.selectList(new LambdaQueryWrapper<MediaEntity>().in(MediaEntity::getUrl, urls))
                        .forEach(media -> {
                            mediaByUrl.putIfAbsent(media.getUrl(), media);
                        });
                photos.forEach(photo -> {
                    MediaEntity media = mediaByUrl.get(photo.getUrl());
                    if (media != null) {
                        photo.setOriginalName(safeFileName(media.getOriginalName()));
                        photo.setMimeType(media.getMimeType());
                    }
                });
            }
        }
        vo.setPhotos(photos);
        return vo;
    }

    private String safeFileName(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String normalized = name.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_")
                .trim();
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    @Transactional
    public Long save(AlbumSaveCommand cmd) {
        AlbumEntity album;
        if (cmd.getId() != null) {
            album = albumMapper.selectById(cmd.getId());
            if (album == null) {
                throw new BizException(ResultCode.NOT_FOUND);
            }
        } else {
            album = new AlbumEntity();
            album.setCreatedAt(LocalDateTime.now());
        }
        album.setTitle(cmd.getTitle());
        album.setSlug(StringUtils.hasText(cmd.getSlug()) ? cmd.getSlug() : SlugUtils.toSlug(cmd.getTitle()));
        album.setDescription(cmd.getDescription());
        album.setCoverUrl(cmd.getCoverUrl());
        album.setVisible(cmd.getVisible() == null || cmd.getVisible());
        if (album.getId() == null) {
            albumMapper.insert(album);
        } else {
            albumMapper.updateById(album);
            albumPhotoMapper.delete(new LambdaQueryWrapper<AlbumPhotoEntity>().eq(AlbumPhotoEntity::getAlbumId, album.getId()));
        }
        if (cmd.getPhotos() != null) {
            int order = 0;
            for (AlbumPhotoSave photo : cmd.getPhotos()) {
                AlbumPhotoEntity entity = new AlbumPhotoEntity();
                entity.setAlbumId(album.getId());
                entity.setUrl(photo.getUrl());
                entity.setCaption(photo.getCaption());
                entity.setSortOrder(order++);
                entity.setCreatedAt(LocalDateTime.now());
                albumPhotoMapper.insert(entity);
            }
        }
        return album.getId();
    }

    @Transactional
    public void delete(Long id) {
        albumPhotoMapper.delete(new LambdaQueryWrapper<AlbumPhotoEntity>().eq(AlbumPhotoEntity::getAlbumId, id));
        albumMapper.deleteById(id);
    }

    @Data
    public static class AlbumDetailVO {
        private AlbumEntity album;
        private List<AlbumPhotoEntity> photos;
    }

    @Data
    public static class AlbumSaveCommand {
        private Long id;
        private String title;
        private String slug;
        private String description;
        private String coverUrl;
        private Boolean visible;
        private List<AlbumPhotoSave> photos;
    }

    @Data
    public static class AlbumPhotoSave {
        private String url;
        private String caption;
    }
}
