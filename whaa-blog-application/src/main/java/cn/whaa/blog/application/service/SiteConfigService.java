package cn.whaa.blog.application.service;

import cn.whaa.blog.infrastructure.persistence.entity.SiteConfigEntity;
import cn.whaa.blog.infrastructure.persistence.entity.SiteTimelineEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.SiteConfigMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.SiteTimelineMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private static final Set<String> PUBLIC_KEYS = Set.of("site", "sidebar", "social", "theme", "comment", "links");

    private final SiteConfigMapper siteConfigMapper;
    private final SiteTimelineMapper siteTimelineMapper;
    private final ObjectMapper objectMapper;

    public Map<String, JsonNode> getPublicConfig() {
        Map<String, JsonNode> map = new LinkedHashMap<>();
        for (String key : PUBLIC_KEYS) {
            SiteConfigEntity entity = siteConfigMapper.selectById(key);
            if (entity != null) {
                try {
                    map.put(key, objectMapper.readTree(entity.getConfigValue()));
                } catch (Exception ignored) {
                }
            }
        }
        return map;
    }

    public Map<String, JsonNode> getAllConfig() {
        Map<String, JsonNode> map = new LinkedHashMap<>();
        siteConfigMapper.selectList(null).forEach(entity -> {
            try {
                map.put(entity.getConfigKey(), objectMapper.readTree(entity.getConfigValue()));
            } catch (Exception ignored) {
            }
        });
        return map;
    }

    public void saveConfig(String key, JsonNode value) {
        SiteConfigEntity entity = siteConfigMapper.selectById(key);
        if (entity == null) {
            entity = new SiteConfigEntity();
            entity.setConfigKey(key);
        }
        entity.setConfigValue(value.toString());
        entity.setUpdatedAt(LocalDateTime.now());
        if (siteConfigMapper.selectById(key) == null) {
            siteConfigMapper.insert(entity);
        } else {
            siteConfigMapper.updateById(entity);
        }
    }

    public List<SiteTimelineEntity> listTimeline() {
        return siteTimelineMapper.selectList(new LambdaQueryWrapper<SiteTimelineEntity>()
                .orderByDesc(SiteTimelineEntity::getEventDate)
                .orderByDesc(SiteTimelineEntity::getSortOrder));
    }

    public Long saveTimeline(SiteTimelineEntity entity) {
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
            siteTimelineMapper.insert(entity);
        } else {
            siteTimelineMapper.updateById(entity);
        }
        return entity.getId();
    }

    public void deleteTimeline(Long id) {
        siteTimelineMapper.deleteById(id);
    }
}
