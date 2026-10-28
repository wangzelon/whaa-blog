package cn.whaa.blog.application.service;

import cn.whaa.blog.domain.enums.CommentStatus;
import cn.whaa.blog.domain.enums.PostStatus;
import cn.whaa.blog.infrastructure.cache.CacheService;
import cn.whaa.blog.infrastructure.persistence.entity.CommentEntity;
import cn.whaa.blog.infrastructure.persistence.entity.PostEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.CommentMapper;
import cn.whaa.blog.infrastructure.persistence.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final CacheService cacheService;

    public DashboardVO stats() {
        DashboardVO vo = new DashboardVO();
        vo.setPostCount(postMapper.selectCount(new LambdaQueryWrapper<PostEntity>().eq(PostEntity::getStatus, PostStatus.PUBLISHED.name())));
        vo.setDraftCount(postMapper.selectCount(new LambdaQueryWrapper<PostEntity>().eq(PostEntity::getStatus, PostStatus.DRAFT.name())));
        vo.setPendingComments(commentMapper.selectCount(new LambdaQueryWrapper<CommentEntity>()
                .eq(CommentEntity::getStatus, CommentStatus.PENDING.name())));
        vo.setPv7d(cacheService.hyperLogLogCount("stats:pv:7d"));
        return vo;
    }

    public void trackPageView(String visitorId) {
        cacheService.incrementHyperLogLog("stats:pv:7d", visitorId);
    }

    @Data
    public static class DashboardVO {
        private long postCount;
        private long draftCount;
        private long pendingComments;
        private long pv7d;
    }
}
