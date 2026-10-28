package cn.whaa.blog.application.service;

import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.common.util.SlugUtils;
import cn.whaa.blog.infrastructure.persistence.entity.AlbumEntity;
import cn.whaa.blog.infrastructure.persistence.entity.AlbumPhotoEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.AlbumMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.AlbumPhotoMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private final AlbumMapper albumMapper;
    private final AlbumPhotoMapper albumPhotoMapper;

    public List<AlbumEntity> listPublic() {
        return albumMapper.selectList(new LambdaQueryWrapper<AlbumEntity>()
                .eq(AlbumEntity::getVisible, true)
                .orderByDesc(AlbumEntity::getCreatedAt));
    }

    public AlbumDetailVO getBySlug(String slug) {
        AlbumEntity album = albumMapper.selectOne(new LambdaQueryWrapper<AlbumEntity>().eq(AlbumEntity::getSlug, slug).eq(AlbumEntity::getVisible, true));
        if (album == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        AlbumDetailVO vo = new AlbumDetailVO();
        vo.setAlbum(album);
        vo.setPhotos(albumPhotoMapper.selectList(new LambdaQueryWrapper<AlbumPhotoEntity>()
                .eq(AlbumPhotoEntity::getAlbumId, album.getId())
                .orderByAsc(AlbumPhotoEntity::getSortOrder)));
        return vo;
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

    public void delete(Long id) {
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
