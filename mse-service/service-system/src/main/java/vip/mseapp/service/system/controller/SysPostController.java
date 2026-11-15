package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.system.entity.SysPost;
import vip.mseapp.service.system.service.SysPostService;
import vip.mseapp.web.log.OperLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 岗位管理（仅租户端）：/system/post/**。
 */
@Validated
@RestController
@RequestMapping("/system/post")
public class SysPostController {

    private final SysPostService postService;

    public SysPostController(SysPostService postService) {
        this.postService = postService;
    }

    @SaCheckPermission(value = "system:post:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/page")
    public Result<PageResult<SysPost>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String postCode,
                                            @RequestParam(required = false) String postName,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(postService.pagePosts(pageNum, pageSize, postCode, postName, status));
    }

    @SaCheckPermission(value = "system:post:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/list-enabled")
    public Result<List<SysPost>> listEnabled() {
        return Result.ok(postService.listEnabled());
    }

    @SaCheckPermission(value = "system:post:query", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/{id}")
    public Result<SysPost> get(@PathVariable Long id) {
        return Result.ok(postService.getById(id));
    }

    @OperLog(module = "system:post", type = "新增")
    @SaCheckPermission(value = "system:post:create", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated PostSaveRequest request) {
        return Result.ok(postService.createPost(toEntity(request, new SysPost())));
    }

    @OperLog(module = "system:post", type = "修改")
    @SaCheckPermission(value = "system:post:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated PostUpdateRequest request) {
        SysPost post = toEntity(request.save(), new SysPost());
        post.setId(request.id());
        postService.updatePost(post);
        return Result.ok();
    }

    @OperLog(module = "system:post", type = "删除")
    @SaCheckPermission(value = "system:post:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.ok();
    }

    private SysPost toEntity(PostSaveRequest request, SysPost post) {
        post.setPostCode(request.postCode());
        post.setPostName(request.postName());
        post.setPostSort(request.postSort());
        post.setStatus(request.status() == null ? 0 : request.status());
        post.setRemark(request.remark());
        return post;
    }

    // -------------------- 请求体 --------------------

    public record PostSaveRequest(
            @NotBlank(message = "岗位编码不能为空") String postCode,
            @NotBlank(message = "岗位名称不能为空") String postName,
            Integer postSort,
            Integer status,
            String remark) {
    }

    public record PostUpdateRequest(
            @NotNull(message = "岗位编号不能为空") Long id,
            @NotBlank(message = "岗位编码不能为空") String postCode,
            @NotBlank(message = "岗位名称不能为空") String postName,
            Integer postSort,
            Integer status,
            String remark) {

        PostSaveRequest save() {
            return new PostSaveRequest(postCode(), postName(), postSort(), status(), remark());
        }
    }
}
