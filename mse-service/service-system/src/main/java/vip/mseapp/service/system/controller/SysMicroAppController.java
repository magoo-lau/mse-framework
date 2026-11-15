package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.Result;
import vip.mseapp.web.log.OperLog;
import vip.mseapp.service.system.catalog.CatalogChangeRecorder;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysMicroApp;
import vip.mseapp.service.system.service.SysMicroAppService;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Comparator;
import java.util.List;

/**
 * 微前端子应用注册管理（平台级全局数据，平台端维护）。
 *
 * <p>仅维护应用地址类配置（标识/入口/可见端等），页面菜单在「菜单管理」以 type=3 子应用页面登记；
 * name 唯一（/sub/{name} 路由前缀）。租户端可用性由菜单推导（套餐勾任一页面菜单即可用）。
 * 子应用列表不参与 Sa-Token 鉴权数据源缓存，无需失效权限缓存。
 * 目录真源在会话库（平台会话=平台库），单库直读；编辑经批次扇出租户库。
 */
@Validated
@RestController
@RequestMapping("/system/micro-app")
public class SysMicroAppController {

    private final SysMicroAppService microAppService;
    private final ObjectProvider<CatalogChangeRecorder> recorderProvider;

    public SysMicroAppController(SysMicroAppService microAppService,
                                 ObjectProvider<CatalogChangeRecorder> recorderProvider) {
        this.microAppService = microAppService;
        this.recorderProvider = recorderProvider;
    }

    @SaCheckPermission(value = "system:micro-app:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/list")
    public Result<List<SysMicroApp>> list() {
        return Result.ok(microAppService.list().stream()
                .sorted(Comparator.comparing(SysMicroApp::getSort)
                        .thenComparing(SysMicroApp::getId)).toList());
    }

    @OperLog(module = "system:micro-app", type = "新增")
    @SaCheckPermission(value = "system:micro-app:create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated MicroAppSaveRequest request) {
        SysMicroApp app = request.toEntity();
        microAppService.create(app);
        recordUpsert(app.getId());
        return Result.ok(app.getId());
    }

    @OperLog(module = "system:micro-app", type = "修改")
    @SaCheckPermission(value = "system:micro-app:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated MicroAppSaveRequest request) {
        microAppService.update(request.toEntity());
        recordUpsert(request.id());
        return Result.ok();
    }

    @OperLog(module = "system:micro-app", type = "启停")
    @SaCheckPermission(value = "system:micro-app:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam boolean enable) {
        SysMicroApp app = microAppService.getById(id);
        if (app == null) {
            return Result.fail(404, "子应用不存在");
        }
        app.setStatus(enable ? 0 : 1);
        microAppService.updateById(app);
        recordUpsert(id);
        return Result.ok();
    }

    @OperLog(module = "system:micro-app", type = "删除")
    @SaCheckPermission(value = "system:micro-app:delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        microAppService.removeById(id);
        CatalogChangeRecorder recorder = recorderProvider.getIfAvailable();
        if (recorder != null) {
            recorder.recordMicroAppDelete(id);
        }
        return Result.ok();
    }

    /** 保存后回读全列快照落目录变更批次（平台会话路由平台库，回读即真源行） */
    private void recordUpsert(Long id) {
        CatalogChangeRecorder recorder = recorderProvider.getIfAvailable();
        if (recorder != null) {
            recorder.recordUpsert(microAppService.getById(id));
        }
    }

    public record MicroAppSaveRequest(
            Long id,
            @NotBlank(message = "标识不能为空") String name,
            @NotBlank(message = "显示名不能为空") String title,
            String devEntry,
            @NotBlank(message = "生产入口不能为空") String prodEntry,
            @NotNull(message = "可见端不能为空") Integer side,
            /** 客户端 1 PC 2 移动端（缺省 PC，存量零迁移） */
            Integer client,
            Integer sort,
            Integer status,
            String remark) {

        SysMicroApp toEntity() {
            SysMicroApp app = new SysMicroApp();
            app.setId(id);
            app.setName(name);
            app.setTitle(title);
            app.setDevEntry(devEntry);
            app.setProdEntry(prodEntry);
            app.setSide(side);
            app.setClient(client == null ? SysMenu.CLIENT_PC : client);
            app.setSort(sort == null ? 0 : sort);
            app.setStatus(status == null ? 0 : status);
            app.setRemark(remark);
            return app;
        }
    }
}
