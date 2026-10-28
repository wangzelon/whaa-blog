package cn.whaa.blog.api.web;

import cn.whaa.blog.application.dto.CategoryVO;
import cn.whaa.blog.application.dto.TagVO;
import cn.whaa.blog.application.service.*;
import cn.whaa.blog.common.result.Result;
import cn.whaa.blog.infrastructure.persistence.entity.FriendLinkEntity;
import cn.whaa.blog.infrastructure.persistence.entity.ProjectEntity;
import cn.whaa.blog.infrastructure.persistence.entity.SiteTimelineEntity;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicMetaController {

    private final CategoryService categoryService;
    private final TagService tagService;
    private final PageService pageService;
    private final FriendLinkService friendLinkService;
    private final ProjectService projectService;
    private final AlbumService albumService;
    private final SiteConfigService siteConfigService;
    private final CommentService commentService;

    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.ok(categoryService.listPublic());
    }

    @GetMapping("/categories/{slug}")
    public Result<CategoryVO> category(@PathVariable String slug) {
        return Result.ok(categoryService.getBySlug(slug));
    }

    @GetMapping("/tags")
    public Result<List<TagVO>> tags(@RequestParam(defaultValue = "false") boolean cloud) {
        return Result.ok(cloud ? tagService.wordCloud() : tagService.listPublic());
    }

    @GetMapping("/tags/{slug}")
    public Result<TagVO> tag(@PathVariable String slug) {
        return Result.ok(tagService.getBySlug(slug));
    }

    @GetMapping("/pages/{slug}")
    public Result<PageService.PageVO> page(@PathVariable String slug) {
        return Result.ok(pageService.getBySlug(slug));
    }

    @GetMapping("/projects")
    public Result<List<ProjectEntity>> projects(@RequestParam(required = false) String category) {
        return Result.ok(projectService.listPublic(category));
    }

    @GetMapping("/links")
    public Result<List<FriendLinkEntity>> links(@RequestParam(required = false) Boolean random) {
        return Result.ok(friendLinkService.listPublic(Boolean.TRUE.equals(random)));
    }

    @GetMapping("/albums")
    public Result<?> albums() {
        return Result.ok(albumService.listPublic());
    }

    @GetMapping("/albums/{slug}")
    public Result<AlbumService.AlbumDetailVO> album(@PathVariable String slug) {
        return Result.ok(albumService.getBySlug(slug));
    }

    @GetMapping("/timeline/site")
    public Result<List<SiteTimelineEntity>> timeline() {
        return Result.ok(siteConfigService.listTimeline());
    }

    @GetMapping("/site/config")
    public Result<Map<String, JsonNode>> siteConfig() {
        return Result.ok(siteConfigService.getPublicConfig());
    }

    @GetMapping("/comments")
    public Result<List<CommentService.CommentVO>> comments(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        return Result.ok(commentService.listApproved(targetType, targetId));
    }

    @PostMapping("/comments")
    public Result<Void> createComment(@RequestBody CommentService.CommentCreateCommand cmd,
                                      jakarta.servlet.http.HttpServletRequest request) {
        commentService.create(cmd, request.getRemoteAddr(), request.getHeader("User-Agent"));
        return Result.ok();
    }
}
