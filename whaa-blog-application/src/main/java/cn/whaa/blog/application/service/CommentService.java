package cn.whaa.blog.application.service;

import cn.whaa.blog.common.exception.BizException;
import cn.whaa.blog.common.result.ResultCode;
import cn.whaa.blog.domain.enums.CommentStatus;
import cn.whaa.blog.infrastructure.cache.CacheService;
import cn.whaa.blog.infrastructure.persistence.entity.CommentEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.CommentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final CacheService cacheService;

    public List<CommentVO> listApproved(String targetType, Long targetId) {
        List<CommentEntity> list = commentMapper.selectList(new LambdaQueryWrapper<CommentEntity>()
                .eq(CommentEntity::getTargetType, targetType)
                .eq(CommentEntity::getTargetId, targetId)
                .eq(CommentEntity::getStatus, CommentStatus.APPROVED.name())
                .orderByAsc(CommentEntity::getCreatedAt));
        return buildTree(list);
    }

    public void create(CommentCreateCommand cmd, String ip, String userAgent) {
        if (!cacheService.rateLimit("comment:ip:" + hash(ip), 10, Duration.ofMinutes(10))) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, "comment too frequent");
        }
        if (!StringUtils.hasText(cmd.getNickname()) || !StringUtils.hasText(cmd.getContent())) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        CommentEntity entity = new CommentEntity();
        entity.setTargetType(cmd.getTargetType());
        entity.setTargetId(cmd.getTargetId());
        entity.setParentId(cmd.getParentId());
        entity.setNickname(cmd.getNickname().trim());
        entity.setEmail(cmd.getEmail());
        entity.setWebsite(cmd.getWebsite());
        entity.setContent(cmd.getContent().trim());
        entity.setStatus(CommentStatus.PENDING.name());
        entity.setIpHash(hash(ip));
        entity.setUserAgent(userAgent);
        entity.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(entity);
    }

    public List<CommentEntity> listPending() {
        return commentMapper.selectList(new LambdaQueryWrapper<CommentEntity>()
                .eq(CommentEntity::getStatus, CommentStatus.PENDING.name())
                .orderByDesc(CommentEntity::getCreatedAt));
    }

    public void moderate(Long id, CommentStatus status) {
        CommentEntity entity = commentMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        entity.setStatus(status.name());
        commentMapper.updateById(entity);
    }

    public void delete(Long id) {
        commentMapper.deleteById(id);
    }

    private List<CommentVO> buildTree(List<CommentEntity> list) {
        Map<Long, CommentVO> map = new LinkedHashMap<>();
        for (CommentEntity entity : list) {
            CommentVO vo = toVo(entity);
            vo.setReplies(new ArrayList<>());
            map.put(entity.getId(), vo);
        }
        List<CommentVO> roots = new ArrayList<>();
        for (CommentEntity entity : list) {
            CommentVO vo = map.get(entity.getId());
            if (entity.getParentId() == null) {
                roots.add(vo);
            } else {
                CommentVO parent = map.get(entity.getParentId());
                if (parent != null) {
                    parent.getReplies().add(vo);
                } else {
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    private CommentVO toVo(CommentEntity entity) {
        CommentVO vo = new CommentVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setNickname(entity.getNickname());
        vo.setWebsite(entity.getWebsite());
        vo.setContent(entity.getContent());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((input == null ? "" : input).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Data
    public static class CommentCreateCommand {
        private String targetType;
        private Long targetId;
        private Long parentId;
        private String nickname;
        private String email;
        private String website;
        private String content;
    }

    @Data
    public static class CommentVO {
        private Long id;
        private Long parentId;
        private String nickname;
        private String website;
        private String content;
        private LocalDateTime createdAt;
        private List<CommentVO> replies;
    }
}
