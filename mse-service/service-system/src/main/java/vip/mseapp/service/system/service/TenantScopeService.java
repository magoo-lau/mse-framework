package vip.mseapp.service.system.service;

import vip.mseapp.service.system.entity.SysPermission;

import java.util.Set;

/**
 * 权限域计算：平台域 / 租户域的可见菜单与权限点集合。
 *
 * <p>租户域 = 租户套餐圈定的菜单及其挂靠的权限点（+ 通用权限）；
 * 平台域 = 全部平台菜单（menu_scope=1）及其挂靠的权限点（+ 通用权限）。
 * 角色授权校验与鉴权数据源的裁剪均以此为边界。
 */
public interface TenantScopeService {

    /** 平台域菜单编号集合（menu_scope=1 全部启用菜单） */
    Set<Long> getPlatformMenuIds();

    /** 租户域菜单编号集合（套餐圈定；未配套餐 = 全部租户域菜单，宽松默认防误锁） */
    Set<Long> getTenantMenuIds(Long tenantId);

    /** 平台域权限点（挂靠平台菜单 + 通用权限） */
    Set<SysPermission> getPlatformPermissions();

    /** 租户域权限点（挂靠套餐菜单 + 通用权限） */
    Set<SysPermission> getTenantPermissions(Long tenantId);

    /**
     * 租户权益值（0.1.0 权益模型：quota 与 feature 前缀项 编码→值）：在线=EntitlementCache 版本缓存
     * 回源契约；standalone=license features。配额守卫（QuotaGuard）与业务开关消费入口。
     */
    java.util.Map<String, String> getEntitlementValues(Long tenantId);
}
