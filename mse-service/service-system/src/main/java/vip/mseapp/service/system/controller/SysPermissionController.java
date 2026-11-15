package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.Result;
import vip.mseapp.web.log.OperLog;
import vip.mseapp.service.system.entity.SysPermission;
import vip.mseapp.service.system.service.SysPermissionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
 * 权限点管理（平台级全局数据，平台端维护；定义即 API/按钮权限标识）。
 */
@Validated
@RestController
@RequestMapping("/system/permission")
public class SysPermissionController {

    private final SysPermissionService permissionService;

    public SysPermissionController(SysPermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @SaCheckPermission(value = "system:permission:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/list")
    public Result<List<SysPermission>> list() {
        return Result.ok(permissionService.listAll());
    }

    @OperLog(module = "system:permission", type = "新增")
    @SaCheckPermission(value = "system:permission:create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated PermissionSaveRequest request) {
        SysPermission permission = request.toEntity();
        return Result.ok(permissionService.createPermission(permission));
    }

    @OperLog(module = "system:permission", type = "修改")
    @SaCheckPermission(value = "system:permission:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated PermissionSaveRequest request) {
        permissionService.updatePermission(request.toEntity());
        return Result.ok();
    }

    @OperLog(module = "system:permission", type = "删除")
    @SaCheckPermission(value = "system:permission:delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.ok();
    }

    public record PermissionSaveRequest(
            Long id,
            /** 挂靠菜单（NULL=通用权限，不受套餐裁剪） */
            Long menuId,
            @NotBlank(message = "所属模块不能为空") @Size(max = 30) String module,
            @NotBlank(message = "权限名不能为空") String name,
            @NotBlank(message = "权限标识不能为空") @Size(max = 100) String code,
            Integer sort,
            Integer status,
            String remark) {

        SysPermission toEntity() {
            SysPermission permission = new SysPermission();
            permission.setId(id);
            permission.setMenuId(menuId);
            permission.setModule(module);
            permission.setName(name);
            permission.setCode(code);
            permission.setSort(sort == null ? 0 : sort);
            permission.setStatus(status == null ? 0 : status);
            permission.setRemark(remark);
            return permission;
        }
    }
}
