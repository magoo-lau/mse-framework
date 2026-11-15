package vip.mseapp.service.platform.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.platform.entity.SysTenant;
import vip.mseapp.service.platform.service.SysTenantService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户管理（平台运营端；sys_tenant 为全局表，操作不受租户插件改写）。
 */
@Validated
@RestController
@RequestMapping("/tenant")
public class SysTenantController {

    private final SysTenantService tenantService;

    private final org.springframework.beans.factory.ObjectProvider<vip.mseapp.mq.TenantEventSender> eventSenderProvider;

    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public SysTenantController(SysTenantService tenantService,
                               org.springframework.beans.factory.ObjectProvider<vip.mseapp.mq.TenantEventSender> eventSenderProvider,
                               org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.tenantService = tenantService;
        this.eventSenderProvider = eventSenderProvider;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 手动触发租户数据升级：向数据面广播 tenant.data-upgrade（tenantIds 空=全部租户），
     * 各数据面服务据此补跑自己的 pending 每租户脚本（账本 (script, tenant) 去重、幂等）。
     */
    @SaCheckPermission(value = "tenant:tenant:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/schema/upgrade-tenant-data")
    public Result<Integer> upgradeTenantData(@RequestBody(required = false) List<Long> tenantIds) {
        List<Long> ids = (tenantIds == null || tenantIds.isEmpty())
                ? tenantService.list().stream().map(SysTenant::getId).toList() : tenantIds;
        ids.forEach(id -> {
            vip.mseapp.mq.TenantEvent event = vip.mseapp.mq.TenantEvent.of(id, vip.mseapp.mq.TenantEvent.Type.DATA_UPGRADE, id);
            vip.mseapp.mq.TenantEventSender sender = eventSenderProvider.getIfAvailable();
            if (sender != null) {
                sender.sendTenant(vip.mseapp.mq.TenantEvent.TOPIC, id, vip.mseapp.mq.TenantEvent.Type.DATA_UPGRADE, id);
            } else {
                eventPublisher.publishEvent(event);
            }
        });
        return Result.ok(ids.size());
    }

    @SaCheckPermission(value = "tenant:tenant:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/page")
    public Result<PageResult<SysTenant>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(tenantService.pageTenants(pageNum, pageSize, name, status));
    }

    @SaCheckPermission(value = "tenant:tenant:query", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/{id}")
    public Result<SysTenant> get(@PathVariable Long id) {
        return Result.ok(tenantService.getById(id));
    }

    @SaCheckPermission(value = "tenant:tenant:create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated TenantCreateRequest request) {
        SysTenant tenant = new SysTenant();
        tenant.setCode(request.code());
        tenant.setName(request.name());
        tenant.setPackageId(request.packageId());
        tenant.setDsKey(request.dsKey());
        tenant.setDomain(request.domain());
        tenant.setContactUserName(request.contactUserName());
        tenant.setContactPhone(request.contactPhone());
        tenant.setAdminEmail(request.adminEmail());
        tenant.setExpireTime(request.expireTime());
        tenant.setRemark(request.remark());
        return Result.ok(tenantService.createTenant(tenant, request.adminUsername(), request.adminPassword()));
    }

    /** 重试开通（开通失败/中断的租户）：后台重跑开通任务，状态经列表轮询刷新；密码不落库需重新录入 */
    @SaCheckPermission(value = "tenant:tenant:create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/{id}/provision")
    public Result<Void> retryProvision(@PathVariable Long id,
                                       @RequestBody @Validated TenantProvisionRetryRequest request) {
        tenantService.retryProvision(id, request.adminUsername(), request.adminPassword());
        return Result.ok();
    }

    @SaCheckPermission(value = "tenant:tenant:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping
    public Result<Void> update(@RequestBody SysTenant tenant) {
        tenantService.updateTenant(tenant);
        return Result.ok();
    }

    @SaCheckPermission(value = "tenant:tenant:change-status", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        tenantService.changeStatus(id, status);
        return Result.ok();
    }

    /**
     * 租户管理员账号（重置弹窗数据源；平台库开通冗余，不再回读租户库——平台租户互不可读）。
     * 返回仅含登记账号一行（id=null，username=开通登记账号）。
     */
    @SaCheckPermission(value = "tenant:tenant:query", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/{id}/admins")
    public Result<List<AdminInfo>> admins(@PathVariable Long id) {
        SysTenant tenant = tenantService.getById(id);
        if (tenant == null) {
            return Result.ok(List.of());
        }
        return Result.ok(tenant.getAdminUsername() == null ? List.of()
                : List.of(new AdminInfo(tenant.getAdminUsername(), tenant.getAdminEmail())));
    }

    /** 重置租户管理员密码（管理员忘记密码时由平台运营重置） */
    @SaCheckPermission(value = "tenant:tenant:reset-password", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/{id}/admin-password")
    public Result<Void> resetAdminPassword(@PathVariable Long id,
                                           @RequestBody @Validated AdminPasswordRequest request) {
        tenantService.resetAdminPassword(id, request.username(), request.password());
        return Result.ok();
    }

    /** 重置租户管理员 TOTP 双因子（管理员丢失验证器被锁死时的恢复手段；与重置密码同权限点） */
    @SaCheckPermission(value = "tenant:tenant:reset-password", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/{id}/admin-totp-reset")
    public Result<Void> resetAdminTotp(@PathVariable Long id, @RequestBody @Validated AdminTotpResetRequest request) {
        tenantService.resetAdminTotp(id, request.username());
        return Result.ok();
    }

    /** 绑定域名格式：完整主机名（至少含一个点，如 demo.example.com）；空 = 不绑定 */
    private static final String DOMAIN_PATTERN =
            "(?i)^$|^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$";

    public record AdminPasswordRequest(
            @NotBlank(message = "管理员账号不能为空") String username,
            @NotBlank(message = "新密码不能为空") @Size(min = 6, max = 32, message = "新密码长度须在 6~32 位之间") String password) {
    }

    public record AdminTotpResetRequest(
            @NotBlank(message = "管理员账号不能为空") String username) {
    }

    /** 管理员账号信息（平台库开通冗余；email 供凭证投递） */
    public record AdminInfo(String username, String email) {
    }

    public record TenantProvisionRetryRequest(
            @NotBlank(message = "管理员账号不能为空") @Size(max = 30) String adminUsername,
            @NotBlank(message = "管理员密码不能为空") @Size(min = 6, max = 32, message = "密码长度须为 6-32 位") String adminPassword) {
    }

    public record TenantCreateRequest(
            @NotBlank(message = "租户编码不能为空") @Size(max = 30) String code,
            @NotBlank(message = "租户名不能为空") String name,
            Long packageId,
            String dsKey,
            @Size(max = 128) @Pattern(regexp = DOMAIN_PATTERN, message = "绑定域名格式不正确（完整主机名，如 demo.example.com）") String domain,
            String contactUserName,
            String contactPhone,
            LocalDateTime expireTime,
            String remark,
            @NotBlank(message = "管理员用户名不能为空") @Size(max = 30) String adminUsername,
            @NotBlank(message = "管理员密码不能为空") @Size(min = 6, max = 32) String adminPassword,
            @Email(message = "管理员邮箱格式不正确") String adminEmail) {
    }
}
