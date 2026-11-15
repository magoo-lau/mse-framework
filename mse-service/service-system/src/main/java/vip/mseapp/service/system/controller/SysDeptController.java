package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.Result;
import vip.mseapp.web.log.OperLog;
import vip.mseapp.service.system.service.SysDeptService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 部门管理：/system/dept/** 租户内；/system/dept/platform/** 平台端（tenant_id=0 域）。
 * 部门树为数据权限（本部门/本部门及以下/自定义）的锚点。
 */
@Validated
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    private final SysDeptService deptService;

    public SysDeptController(SysDeptService deptService) {
        this.deptService = deptService;
    }

    // -------------------- 租户内 --------------------

    @SaCheckPermission(value = "system:dept:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/tree")
    public Result<List<SysDeptService.DeptTreeNode>> tree() {
        return Result.ok(deptService.getDeptTree());
    }

    @OperLog(module = "system:dept", type = "新增")
    @SaCheckPermission(value = "system:dept:create", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated DeptSaveRequest request) {
        return Result.ok(deptService.createDept(request.parentId(), request.name(), request.sort(), request.leaderUserId()));
    }

    @OperLog(module = "system:dept", type = "修改")
    @SaCheckPermission(value = "system:dept:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated DeptUpdateRequest request) {
        deptService.updateDept(request.id(), request.parentId(), request.name(), request.sort(), request.status(), request.leaderUserId());
        return Result.ok();
    }

    @OperLog(module = "system:dept", type = "删除")
    @SaCheckPermission(value = "system:dept:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.ok();
    }

    // -------------------- 平台端（tenant_id=0 域） --------------------

    @SaCheckPermission(value = "system:dept:platform-list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/tree")
    public Result<List<SysDeptService.DeptTreeNode>> platformTree() {
        return Result.ok(deptService.getDeptTree());
    }

    @SaCheckPermission(value = "system:dept:platform-create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/platform")
    public Result<Long> platformCreate(@RequestBody @Validated DeptSaveRequest request) {
        return Result.ok(deptService.createDept(request.parentId(), request.name(), request.sort(), request.leaderUserId()));
    }

    @SaCheckPermission(value = "system:dept:platform-update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/platform")
    public Result<Void> platformUpdate(@RequestBody @Validated DeptUpdateRequest request) {
        deptService.updateDept(request.id(), request.parentId(), request.name(), request.sort(), request.status(), request.leaderUserId());
        return Result.ok();
    }

    @SaCheckPermission(value = "system:dept:platform-delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/platform/{id}")
    public Result<Void> platformDelete(@PathVariable Long id) {
        deptService.deleteDept(id);
        return Result.ok();
    }

    // -------------------- 请求体 --------------------

    /** leaderUserId：null=不改动、0=清空（update）；create 直接生效 */
    public record DeptSaveRequest(
            Long parentId,
            @NotBlank(message = "部门名不能为空") String name,
            Integer sort,
            Long leaderUserId) {
    }

    public record DeptUpdateRequest(
            @NotNull(message = "部门编号不能为空") Long id,
            Long parentId,
            @NotBlank(message = "部门名不能为空") String name,
            Integer sort,
            Integer status,
            Long leaderUserId) {
    }
}
