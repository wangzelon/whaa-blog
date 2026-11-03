package cn.whaa.blog.api.job;

import cn.whaa.blog.application.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatAttachmentCleanupJob {

    private final ChatService chatService;

    @Scheduled(initialDelay = 60_000L, fixedDelay = 600_000L)
    public void removeExpiredUploads() {
        chatService.cleanupExpiredPendingAttachments();
    }
}
