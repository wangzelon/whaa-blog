package cn.whaa.blog.api.web.admin;

import cn.whaa.blog.application.dto.PageResult;
import cn.whaa.blog.application.dto.PostDetailVO;
import cn.whaa.blog.application.dto.PostSummaryVO;
import cn.whaa.blog.application.service.PostSaveCommand;
import cn.whaa.blog.application.service.PostService;
import cn.whaa.blog.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostService postService;

    @GetMapping
    public Result<PageResult<PostSummaryVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String status) {
        return Result.ok(postService.listAdmin(page, size, status));
    }

    @GetMapping("/{id}")
    public Result<PostDetailVO> detail(@PathVariable Long id) {
        return Result.ok(postService.getAdminById(id));
    }

    @PostMapping
    public Result<Long> save(@RequestBody PostSaveCommand cmd, Authentication authentication) {
        Long authorId = Long.parseLong(authentication.getName());
        return Result.ok(postService.saveAdmin(cmd, authorId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.deleteAdmin(id);
        return Result.ok();
    }
}
