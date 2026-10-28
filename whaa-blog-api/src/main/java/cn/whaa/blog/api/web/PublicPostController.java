package cn.whaa.blog.api.web;

import cn.whaa.blog.application.dto.PageResult;
import cn.whaa.blog.application.dto.PostDetailVO;
import cn.whaa.blog.application.dto.PostSummaryVO;
import cn.whaa.blog.application.service.DashboardService;
import cn.whaa.blog.application.service.PostService;
import cn.whaa.blog.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicPostController {

    private final PostService postService;
    private final DashboardService dashboardService;

    @GetMapping("/posts")
    public Result<PageResult<PostSummaryVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            HttpServletRequest request) {
        trackPv(request);
        return Result.ok(postService.listPublished(page, size, categoryId, tagId));
    }

    @GetMapping("/posts/{slug}")
    public Result<PostDetailVO> detail(@PathVariable String slug, HttpServletRequest request) {
        trackPv(request);
        return Result.ok(postService.getPublishedBySlug(slug));
    }

    @GetMapping("/posts/archive")
    public Result<List<?>> archive() {
        return Result.ok(postService.listArchive());
    }

    @GetMapping("/posts/search")
    public Result<List<PostSummaryVO>> search(@RequestParam String q) {
        return Result.ok(postService.search(q));
    }

    private void trackPv(HttpServletRequest request) {
        String visitor = request.getHeader("X-Visitor-Id");
        if (visitor == null) {
            visitor = request.getRemoteAddr();
        }
        dashboardService.trackPageView(visitor);
    }
}
