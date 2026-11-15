package vip.mseapp.service.platform.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
import vip.mseapp.service.platform.entity.SysTenantServiceConfig;
import vip.mseapp.service.platform.entity.SysTenantServiceTablePolicy;
import vip.mseapp.service.platform.service.SysTenantRouteService;

import java.util.List;

/**
 * 租户×服务隔离配置（平台运营端；多模式：独立库 / 共享库分表 / 共享库字段隔离 + 表级策略）。
 *
 * <p>解析顺序：本表配置 → sys_tenant.ds_key 全局默认（独立库语义）→ 无配置 = SHARED_FIELD。
 */
@Validated
@RestController
@RequestMapping("/tenant/service-config")
public class SysTenantRouteController {

    private final SysTenantRouteService routeService;

    public SysTenantRouteController(SysTenantRouteService routeService) {
        this.routeService = routeService;
    }

    @SaCheckPermission(value = "tenant:route:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/list")
    public Result<List<SysTenantServiceConfig>> list(@RequestParam Long tenantId) {
        return Result.ok(routeService.listByTenant(tenantId));
    }

    /**
     * 已知服务名清单（隔离策略与租户数据源两个管理页的「服务名」下拉共用）：
     * 模块身份注解收集 + yaml service-mappings + 已有配置并集；任一页面权限即可读。
     */
    @SaCheckPermission(value = {"tenant:route:list", "tenant:ds:list"}, mode = SaMode.OR,
            type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/service-names")
    public Result<List<String>> serviceNames() {
        return Result.ok(routeService.listServiceNames());
    }

    @SaCheckPermission(value = "tenant:route:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/policies")
    public Result<List<SysTenantServiceTablePolicy>> policies(@RequestParam Long tenantId,
                                                              @RequestParam String serviceName) {
        return Result.ok(routeService.listPolicies(tenantId, serviceName));
    }

    @SaCheckPermission(value = "tenant:route:save", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping
    public Result<Void> save(@RequestBody @Validated ConfigRequest request) {
        routeService.saveConfig(request.toEntity());
        return Result.ok();
    }

    @SaCheckPermission(value = "tenant:route:delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        routeService.deleteConfig(id);
        return Result.ok();
    }

    @SaCheckPermission(value = "tenant:route:save", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/policy")
    public Result<Void> savePolicy(@RequestBody @Validated PolicyRequest request) {
        routeService.savePolicy(request.toEntity());
        return Result.ok();
    }

    @SaCheckPermission(value = "tenant:route:delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/policy/{id}")
    public Result<Void> deletePolicy(@PathVariable Long id) {
        routeService.deletePolicy(id);
        return Result.ok();
    }

    /** 生成分表：对 SPLIT 表在目标库克隆加后缀的表结构（幂等，仅补缺失） */
    @SaCheckPermission(value = "tenant:route:generate", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/{tenantId}/generate-split-tables")
    public Result<String> generateSplitTables(@PathVariable Long tenantId, @RequestParam String serviceName) {
        return Result.ok(routeService.generateSplitTables(tenantId, serviceName));
    }

    public record ConfigRequest(
            Long id,
            @NotNull(message = "tenantId 不能为空") Long tenantId,
            @NotBlank(message = "服务名不能为空") @Size(max = 30) String serviceName,
            @NotBlank(message = "隔离模式不能为空") @Pattern(regexp = "INDEPENDENT_DB|SHARED_TABLE_SPLIT|SHARED_FIELD",
                    message = "隔离模式非法（INDEPENDENT_DB / SHARED_TABLE_SPLIT / SHARED_FIELD）") String isolationMode,
            String dsKey,
            @Size(max = 32) @Pattern(regexp = "^$|^_?[A-Za-z0-9]+$", message = "分表后缀仅允许字母/数字（可带前导下划线）") String tableSuffix,
            @Size(max = 64) String tenantColumn,
            @Size(max = 255) String remark) {

        SysTenantServiceConfig toEntity() {
            SysTenantServiceConfig config = new SysTenantServiceConfig();
            config.setId(id);
            config.setTenantId(tenantId);
            config.setServiceName(serviceName);
            config.setIsolationMode(isolationMode);
            config.setDsKey(dsKey);
            config.setTableSuffix(tableSuffix);
            config.setTenantColumn(tenantColumn);
            config.setRemark(remark);
            return config;
        }
    }

    public record PolicyRequest(
            Long id,
            @NotNull(message = "tenantId 不能为空") Long tenantId,
            @NotBlank(message = "服务名不能为空") @Size(max = 30) String serviceName,
            @NotBlank(message = "表名不能为空") @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9_$]+$",
                    message = "表名仅允许字母/数字/下划线/$") String tableName,
            @NotBlank(message = "表级模式不能为空") @Pattern(regexp = "SPLIT|FIELD|GLOBAL",
                    message = "表级模式非法（SPLIT / FIELD / GLOBAL）") String mode,
            @Size(max = 32) @Pattern(regexp = "^$|^_?[A-Za-z0-9]+$", message = "分表后缀仅允许字母/数字（可带前导下划线）") String tableSuffix) {

        SysTenantServiceTablePolicy toEntity() {
            SysTenantServiceTablePolicy policy = new SysTenantServiceTablePolicy();
            policy.setId(id);
            policy.setTenantId(tenantId);
            policy.setServiceName(serviceName);
            policy.setTableName(tableName);
            policy.setTableMode(mode);
            policy.setTableSuffix(tableSuffix);
            return policy;
        }
    }
}
