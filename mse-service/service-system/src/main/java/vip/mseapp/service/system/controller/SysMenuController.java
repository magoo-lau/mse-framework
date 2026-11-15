package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.Result;
import vip.mseapp.web.log.OperLog;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.redis.PermissionCache;
import vip.mseapp.service.system.catalog.CatalogChangeRecorder;
import vip.mseapp.service.system.service.SysMenuService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.ObjectProvider;
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
 * 菜单管理（平台级全局数据，平台端维护；纯导航职责，权限点见 /system/permission）。
 *
 * <p>type=3 子应用页面：micro_name 关联 sys_micro_app、path 为子应用内部路由，
 * 由基座侧边栏解析为 /sub/{name}{path}；子应用应用地址类配置见 /system/micro-app。
 * 编辑经 {@link CatalogChangeRecorder} 落批次扇出租户库（目录真源=平台库）。
 */
@Validated
@RestController
@RequestMapping("/system/menu")
public class SysMenuController {

    private final SysMenuService menuService;
    private final PermissionCache permissionCache;
    private final ObjectProvider<CatalogChangeRecorder> recorderProvider;

    public SysMenuController(SysMenuService menuService, PermissionCache permissionCache,
                             ObjectProvider<CatalogChangeRecorder> recorderProvider) {
        this.menuService = menuService;
        this.permissionCache = permissionCache;
        this.recorderProvider = recorderProvider;
    }

    @SaCheckPermission(value = "system:menu:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/tree")
    public Result<List<SysMenuService.MenuTreeNode>> tree() {
        return Result.ok(menuService.getMenuTree());
    }

    @OperLog(module = "system:menu", type = "新增")
    @SaCheckPermission(value = "system:menu:create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated MenuSaveRequest request) {
        SysMenu menu = request.toEntity();
        menu.setId(null);
        menuService.validate(menu);
        menuService.save(menu);
        permissionCache.evictGlobal();
        recordUpsert(menu.getId());
        return Result.ok(menu.getId());
    }

    @OperLog(module = "system:menu", type = "修改")
    @SaCheckPermission(value = "system:menu:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated MenuSaveRequest request) {
        SysMenu menu = request.toEntity();
        menuService.validate(menu);
        menuService.updateById(menu);
        permissionCache.evictGlobal();
        recordUpsert(menu.getId());
        return Result.ok();
    }

    @OperLog(module = "system:menu", type = "删除")
    @SaCheckPermission(value = "system:menu:delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.removeById(id);
        permissionCache.evictGlobal();
        recordDelete(id);
        return Result.ok();
    }

    /** 保存后回读全列快照落批次（平台会话路由平台库，回读即真源行） */
    private void recordUpsert(Long id) {
        CatalogChangeRecorder recorder = recorderProvider.getIfAvailable();
        if (recorder != null) {
            recorder.recordUpsert(menuService.getById(id));
        }
    }

    private void recordDelete(Long id) {
        CatalogChangeRecorder recorder = recorderProvider.getIfAvailable();
        if (recorder != null) {
            recorder.recordMenuDelete(id);
        }
    }

    public record MenuSaveRequest(
            Long id,
            Long parentId,
            @NotBlank(message = "菜单名不能为空") String name,
            @NotNull(message = "类型不能为空") Integer type,
            /** 可见域 1 平台端 2 租户端 3 双端 */
            @NotNull(message = "可见域不能为空") Integer menuScope,
            /** 客户端 1 PC 2 移动端（缺省 PC，存量零迁移） */
            Integer client,
            String path,
            /** type=3 子应用页面专用：所属子应用标识（sys_micro_app.name） */
            String microName,
            String component,
            String icon,
            Integer sort,
            Integer visible,
            Integer status) {

        SysMenu toEntity() {
            SysMenu menu = new SysMenu();
            menu.setId(id);
            menu.setParentId(parentId == null ? 0L : parentId);
            menu.setName(name);
            menu.setType(type);
            menu.setMenuScope(menuScope);
            menu.setClient(client == null ? SysMenu.CLIENT_PC : client);
            menu.setPath(path);
            menu.setMicroName(microName);
            menu.setComponent(component);
            menu.setIcon(icon);
            menu.setSort(sort == null ? 0 : sort);
            menu.setVisible(visible == null ? 0 : visible);
            menu.setStatus(status == null ? 0 : status);
            return menu;
        }
    }
}
