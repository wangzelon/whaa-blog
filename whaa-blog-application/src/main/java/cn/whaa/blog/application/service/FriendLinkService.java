package cn.whaa.blog.application.service;

import cn.whaa.blog.infrastructure.persistence.entity.FriendLinkEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.FriendLinkMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.SiteConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendLinkService {

    private final FriendLinkMapper friendLinkMapper;
    private final SiteConfigMapper siteConfigMapper;
    private final ObjectMapper objectMapper;

    public List<FriendLinkEntity> listPublic(boolean randomOverride) {
        boolean random = randomOverride || isRandomEnabled();
        if (random) {
            return friendLinkMapper.selectRandomVisible();
        }
        return friendLinkMapper.selectOrderedVisible();
    }

    public List<FriendLinkEntity> listAll() {
        return friendLinkMapper.selectList(new LambdaQueryWrapper<FriendLinkEntity>().orderByAsc(FriendLinkEntity::getSortOrder));
    }

    public Long save(FriendLinkEntity entity) {
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
            friendLinkMapper.insert(entity);
        } else {
            friendLinkMapper.updateById(entity);
        }
        return entity.getId();
    }

    public void delete(Long id) {
        friendLinkMapper.deleteById(id);
    }

    private boolean isRandomEnabled() {
        var config = siteConfigMapper.selectById("links");
        if (config == null) {
            return false;
        }
        try {
            JsonNode node = objectMapper.readTree(config.getConfigValue());
            return node.path("random").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }
}
