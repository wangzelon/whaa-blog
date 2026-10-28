package cn.whaa.blog.api.web.admin;

import cn.whaa.blog.application.dto.PageResult;
import cn.whaa.blog.application.service.*;
import cn.whaa.blog.common.result.Result;
import cn.whaa.blog.domain.enums.CommentStatus;
import cn.whaa.blog.infrastructure.persistence.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCrudController {

    private final CategoryService categoryService;
    private final TagService tagService;
    private final PageService pageService;
    private final FriendLinkService friendLinkService;
    private final ProjectService projectService;
    private final AlbumService albumService;
    private final SiteConfigService siteConfigService;
    private final CommentService commentService;
    private final DashboardService dashboardService;
    private final MediaService mediaService;

    @GetMapping("/dashboard")
    public Result<DashboardService.DashboardVO> dashboard() {
        return Result.ok(dashboardService.stats());
    }

    @GetMapping("/categories")
    public Result<List<?>> categories() {
        return Result.ok(categoryService.listPublic());
    }

    @PostMapping("/categories")
    public Result<Long> saveCategory(@RequestBody CategoryEntity entity) {
        return Result.ok(categoryService.save(entity));
    }

    @DeleteMapping("/categories/{id}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.ok();
    }

    @GetMapping("/tags")
    public Result<List<?>> tags() {
        return Result.ok(tagService.listPublic());
    }

    @PostMapping("/tags")
    public Result<Long> saveTag(@RequestBody TagEntity entity) {
        return Result.ok(tagService.save(entity));
    }

    @DeleteMapping("/tags/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        tagService.delete(id);
        return Result.ok();
    }

    @GetMapping("/pages")
    public Result<List<PageService.PageVO>> pages() {
        return Result.ok(pageService.listAll());
    }

    @PostMapping("/pages")
    public Result<Long> savePage(@RequestBody PageService.PageSaveCommand cmd) {
        return Result.ok(pageService.save(cmd));
    }

    @DeleteMapping("/pages/{id}")
    public Result<Void> deletePage(@PathVariable Long id) {
        pageService.delete(id);
        return Result.ok();
    }

    @GetMapping("/links")
    public Result<List<FriendLinkEntity>> links() {
        return Result.ok(friendLinkService.listAll());
    }

    @PostMapping("/links")
    public Result<Long> saveLink(@RequestBody FriendLinkEntity entity) {
        return Result.ok(friendLinkService.save(entity));
    }

    @DeleteMapping("/links/{id}")
    public Result<Void> deleteLink(@PathVariable Long id) {
        friendLinkService.delete(id);
        return Result.ok();
    }

    @GetMapping("/projects")
    public Result<List<ProjectEntity>> projects() {
        return Result.ok(projectService.listAll());
    }

    @PostMapping("/projects")
    public Result<Long> saveProject(@RequestBody ProjectEntity entity) {
        return Result.ok(projectService.save(entity));
    }

    @DeleteMapping("/projects/{id}")
    public Result<Void> deleteProject(@PathVariable Long id) {
        projectService.delete(id);
        return Result.ok();
    }

    @PostMapping("/albums")
    public Result<Long> saveAlbum(@RequestBody AlbumService.AlbumSaveCommand cmd) {
        return Result.ok(albumService.save(cmd));
    }

    @DeleteMapping("/albums/{id}")
    public Result<Void> deleteAlbum(@PathVariable Long id) {
        albumService.delete(id);
        return Result.ok();
    }

    @GetMapping("/timeline")
    public Result<List<SiteTimelineEntity>> timeline() {
        return Result.ok(siteConfigService.listTimeline());
    }

    @PostMapping("/timeline")
    public Result<Long> saveTimeline(@RequestBody SiteTimelineEntity entity) {
        return Result.ok(siteConfigService.saveTimeline(entity));
    }

    @DeleteMapping("/timeline/{id}")
    public Result<Void> deleteTimeline(@PathVariable Long id) {
        siteConfigService.deleteTimeline(id);
        return Result.ok();
    }

    @GetMapping("/site/config")
    public Result<Map<String, JsonNode>> config() {
        return Result.ok(siteConfigService.getAllConfig());
    }

    @PutMapping("/site/config/{key}")
    public Result<Void> saveConfig(@PathVariable String key, @RequestBody JsonNode value) {
        siteConfigService.saveConfig(key, value);
        return Result.ok();
    }

    @GetMapping("/comments/pending")
    public Result<List<CommentEntity>> pendingComments() {
        return Result.ok(commentService.listPending());
    }

    @PostMapping("/comments/{id}/status")
    public Result<Void> moderate(@PathVariable Long id, @RequestParam CommentStatus status) {
        commentService.moderate(id, status);
        return Result.ok();
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok();
    }

    @PostMapping("/media/upload")
    public Result<MediaEntity> upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        Long uploaderId = Long.parseLong(authentication.getName());
        return Result.ok(mediaService.upload(file, uploaderId));
    }

    @GetMapping("/media")
    public Result<PageResult<MediaEntity>> media(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(mediaService.list(page, size));
    }
}
