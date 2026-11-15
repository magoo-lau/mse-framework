package vip.mseapp.service.platform.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.platform.entity.SysAddon;
import vip.mseapp.service.platform.entity.SysEntitlementItem;
import vip.mseapp.service.platform.entity.SysTenantAddon;
import vip.mseapp.service.platform.entity.SysTenantEntitlementOverride;
import vip.mseapp.service.platform.mapper.SysAddonMapper;
import vip.mseapp.service.platform.mapper.SysEntitlementItemMapper;
import vip.mseapp.service.platform.mapper.SysTenantAddonMapper;
import vip.mseapp.service.platform.mapper.SysTenantEntitlementOverrideMapper;
import vip.mseapp.service.platform.service.EntitlementService;
import vip.mseapp.web.log.OperLog;

import java.util.List;
import java.util.Map;

/**
 * 租户权益覆盖（0.1.0 权益模型，平台端）：单租户特批——租户级覆盖（绝对值最高优先）与
 * 附加项订购/退订。变更即失效该租户权益缓存 + FEATURE_CHANGED 事件（快照随事件失效）。
 */
@Validated
@RestController
@RequestMapping("/tenant/entitlement")
public class TenantEntitlementController {

    private final SysEntitlementItemMapper itemMapper;

    private final SysTenantEntitlementOverrideMapper overrideMapper;

    private final SysTenantAddonMapper tenantAddonMapper;

    private final SysAddonMapper addonMapper;

    private final EntitlementService entitlementService;

    public TenantEntitlementController(SysEntitlementItemMapper itemMapper,
                                       SysTenantEntitlementOverrideMapper overrideMapper,
                                       SysTenantAddonMapper tenantAddonMapper,
                                       SysAddonMapper addonMapper,
                                       EntitlementService entitlementService) {
        this.itemMapper = itemMapper;
        this.overrideMapper = overrideMapper;
        this.tenantAddonMapper = tenantAddonMapper;
        this.addonMapper = addonMapper;
        this.entitlementService = entitlementService;
    }

    /** 租户视图：当前生效权益（compute 全链结果）+ 覆盖明细 + 订购明细 + 可选项全集 */
    @SaCheckPermission(value = "tenant:entitlement:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/{tenantId}")
    public Result<Map<String, Object>> tenantView(@PathVariable Long tenantId) {
        return Result.ok(Map.of(
                "effective", entitlementService.compute(tenantId),
                "overrides", overrideMapper.selectList(new LambdaQueryWrapper<SysTenantEntitlementOverride>()
                        .eq(SysTenantEntitlementOverride::getTenantId, tenantId)),
                "subscriptions", tenantAddonMapper.selectList(new LambdaQueryWrapper<SysTenantAddon>()
                        .eq(SysTenantAddon::getTenantId, tenantId)),
                "entitlementItems", itemMapper.selectList(new LambdaQueryWrapper<SysEntitlementItem>()
                        .eq(SysEntitlementItem::getStatus, 0)),
                "addons", addonMapper.selectList(new LambdaQueryWrapper<SysAddon>()
                        .eq(SysAddon::getStatus, 0))));
    }

    /** 覆盖保存（upsert 单项；itemValue 空=删除覆盖回退链值） */
    @OperLog(module = "tenant:entitlement", type = "覆盖保存")
    @SaCheckPermission(value = "tenant:entitlement:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/override")
    public Result<Void> saveOverride(@RequestBody @Validated OverrideSaveRequest request) {
        if (itemMapper.selectById(request.itemId()) == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "权益项不存在");
        }
        SysTenantEntitlementOverride existing = overrideMapper.selectOne(
                new LambdaQueryWrapper<SysTenantEntitlementOverride>()
                        .eq(SysTenantEntitlementOverride::getTenantId, request.tenantId())
                        .eq(SysTenantEntitlementOverride::getItemId, request.itemId()));
        if (request.itemValue() == null || request.itemValue().isBlank()) {
            // 物理删（同退订附加项语义）：uk_tenant_item 含逻辑删除墓碑，逻辑删会令清除→重存必撞唯一键
            overrideMapper.deletePhysical(request.tenantId(), request.itemId());
        } else if (existing == null) {
            // 防御历史墓碑行（修复前的逻辑删残留）：物理清理后再插，否则撞 uk_tenant_item
            overrideMapper.deletePhysical(request.tenantId(), request.itemId());
            SysTenantEntitlementOverride override = new SysTenantEntitlementOverride();
            override.setTenantId(request.tenantId());
            override.setItemId(request.itemId());
            override.setItemValue(request.itemValue().trim());
            overrideMapper.insert(override);
        } else {
            existing.setItemValue(request.itemValue().trim());
            overrideMapper.updateById(existing);
        }
        entitlementService.evictTenant(request.tenantId());
        return Result.ok();
    }

    /** 覆盖删除（物理删：uk_tenant_item 含逻辑删除墓碑，逻辑删会令清除→重存必撞唯一键） */
    @OperLog(module = "tenant:entitlement", type = "覆盖删除")
    @SaCheckPermission(value = "tenant:entitlement:delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/override/{tenantId}/{itemId}")
    public Result<Void> deleteOverride(@PathVariable Long tenantId, @PathVariable Long itemId) {
        overrideMapper.deletePhysical(tenantId, itemId);
        entitlementService.evictTenant(tenantId);
        return Result.ok();
    }

    /** 附加项订购（qty 缺省 1；重复订购按 upsert 刷新数量） */
    @OperLog(module = "tenant:entitlement", type = "订购附加项")
    @SaCheckPermission(value = "tenant:entitlement:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/subscribe")
    public Result<Void> subscribe(@RequestBody @Validated SubscribeRequest request) {
        if (addonMapper.selectById(request.addonId()) == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "附加项不存在");
        }
        SysTenantAddon existing = tenantAddonMapper.selectOne(new LambdaQueryWrapper<SysTenantAddon>()
                .eq(SysTenantAddon::getTenantId, request.tenantId())
                .eq(SysTenantAddon::getAddonId, request.addonId()));
        if (existing == null) {
            // 防御历史墓碑行（修复前的逻辑删残留）：物理清理后再插，否则撞 uk_tenant_addon
            tenantAddonMapper.deletePhysical(request.tenantId(), request.addonId());
            SysTenantAddon subscription = new SysTenantAddon();
            subscription.setTenantId(request.tenantId());
            subscription.setAddonId(request.addonId());
            subscription.setQty(request.qty() == null || request.qty() < 1 ? 1 : request.qty());
            tenantAddonMapper.insert(subscription);
        } else {
            existing.setQty(request.qty() == null || request.qty() < 1 ? 1 : request.qty());
            existing.setStatus(0);
            tenantAddonMapper.updateById(existing);
        }
        entitlementService.evictTenant(request.tenantId());
        return Result.ok();
    }

    /** 附加项退订（物理删：uk_tenant_addon 含逻辑删除墓碑，逻辑删会令退订→重订必撞唯一键） */
    @OperLog(module = "tenant:entitlement", type = "退订附加项")
    @SaCheckPermission(value = "tenant:entitlement:delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/subscribe/{tenantId}/{addonId}")
    public Result<Void> unsubscribe(@PathVariable Long tenantId, @PathVariable Long addonId) {
        tenantAddonMapper.deletePhysical(tenantId, addonId);
        entitlementService.evictTenant(tenantId);
        return Result.ok();
    }

    public record OverrideSaveRequest(@NotNull Long tenantId, @NotNull Long itemId, String itemValue) {
    }

    public record SubscribeRequest(@NotNull Long tenantId, @NotNull Long addonId, Integer qty) {
    }
}
