package vip.mseapp.service.platform.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.Result;
import vip.mseapp.redis.PermissionCache;
import vip.mseapp.service.platform.entity.SysTenantPackage;
import vip.mseapp.service.platform.service.SysTenantPackageService;
import jakarta.validation.constraints.NotBlank;
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
 * 租户套餐管理（平台运营端）。
 */
@Validated
@RestController
@RequestMapping("/tenant-package")
public class SysTenantPackageController {

    private final SysTenantPackageService packageService;
    private final PermissionCache permissionCache;

    public SysTenantPackageController(SysTenantPackageService packageService, PermissionCache permissionCache) {
        this.packageService = packageService;
        this.permissionCache = permissionCache;
    }

    @SaCheckPermission(value = "tenant:package:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/list")
    public Result<List<SysTenantPackage>> list() {
        return Result.ok(packageService.listPackages());
    }

    @SaCheckPermission(value = "tenant:package:create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated PackageSaveRequest request) {
        return Result.ok(packageService.createPackage(request.toEntity()));
    }

    @SaCheckPermission(value = "tenant:package:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated PackageSaveRequest request) {
        packageService.updatePackage(request.toEntity());
        return Result.ok();
    }

    @SaCheckPermission(value = "tenant:package:delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        packageService.deletePackage(id);
        return Result.ok();
    }

    /**
     * 手动刷新权限缓存（自增全局版本）：权限点/菜单脚本随业务 jar 直接落库（base 通道重放）不触发失效，
     * 最长 30 分钟 TTL 内"已落库却不生效"，此端点供套餐管理页一键立即收敛。权限复用套餐修改。
     */
    @SaCheckPermission(value = "tenant:package:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        permissionCache.evictGlobal();
        return Result.ok();
    }

    public record PackageSaveRequest(
            Long id,
            @NotBlank(message = "套餐名不能为空") String name,
            String menuIds,
            String remark,
            Integer status) {

        SysTenantPackage toEntity() {
            SysTenantPackage entity = new SysTenantPackage();
            entity.setId(id);
            entity.setName(name);
            entity.setMenuIds(menuIds);
            entity.setRemark(remark);
            entity.setStatus(status == null ? 0 : status);
            return entity;
        }
    }
}
