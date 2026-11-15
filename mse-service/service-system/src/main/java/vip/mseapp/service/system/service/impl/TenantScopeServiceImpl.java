package vip.mseapp.service.system.service.impl;

import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysPermission;
import vip.mseapp.service.system.mapper.SysMenuMapper;
import vip.mseapp.service.system.mapper.SysPermissionMapper;
import vip.mseapp.service.system.service.TenantScopeService;
import vip.mseapp.service.system.util.MenuScopes;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 租户权限域默认实现：平台域=平台库目录真源（会话库直读），租户域=套餐 ∩ 租户库自持副本。
 *
 * <p><b>平台域读取</b>：目录真源已上收平台库（base 脚本经推送链摄入 + 平台端编辑经批次扇出），
 * 平台会话随拦截器路由平台库直读全集，不再经 runIgnore 并集查共享主库。平台域结果带 5s 进程内
 * 缓存——一次 /platform/me 或鉴权回源中菜单域会被 buildProfile/权限域/Sa-Token 数据源重复计算
 * 2~3 次，短 TTL 收敛为单次查询（对齐路由解析的 L1 先例）；平台端编辑菜单最长 5s 自然可见，
 * 权限码另有 PermissionCache 版本失效（30min TTL 兜底）管收敛。
 */
public class TenantScopeServiceImpl implements TenantScopeService {

    /** 平台域短 TTL 进程内缓存（毫秒） */
    private static final long PLATFORM_CACHE_TTL_MS = 5_000L;

    /** 套餐数据归 service-tenant 持有，经 RPC 契约查询（单体下本地直调） */
    @RpcReference
    private RemoteTenantService remoteTenantService;

    private final SysMenuMapper menuMapper;
    private final SysPermissionMapper permissionMapper;

    /** 权益缓存（0.1.0：版本失效；无 Redis 部署直连契约） */
    private final org.springframework.beans.factory.ObjectProvider<vip.mseapp.redis.EntitlementCache> entitlementCacheProvider;

    private volatile CacheEntry<Set<Long>> platformMenuIdsCache;

    private volatile CacheEntry<Set<SysPermission>> platformPermissionsCache;

    private record CacheEntry<T>(T value, long expireAt) {
    }

    public TenantScopeServiceImpl(SysMenuMapper menuMapper, SysPermissionMapper permissionMapper,
                                   org.springframework.beans.factory.ObjectProvider<vip.mseapp.redis.EntitlementCache> entitlementCacheProvider) {
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
        this.entitlementCacheProvider = entitlementCacheProvider;
    }

    @Override
    public Set<Long> getPlatformMenuIds() {
        long now = System.currentTimeMillis();
        CacheEntry<Set<Long>> cached = platformMenuIdsCache;
        if (cached != null && cached.expireAt() > now) {
            return cached.value();
        }
        Set<Long> ids = new HashSet<>();
        for (SysMenu menu : menuMapper.selectByScope(SysMenu.SCOPE_PLATFORM)) {
            ids.add(menu.getId());
        }
        Set<Long> snapshot = Collections.unmodifiableSet(ids);
        platformMenuIdsCache = new CacheEntry<>(snapshot, now + PLATFORM_CACHE_TTL_MS);
        return snapshot;
    }

    @Override
    public Set<Long> getTenantMenuIds(Long tenantId) {
        List<SysMenu> scopeMenus = menuMapper.selectByScope(SysMenu.SCOPE_TENANT);
        List<Long> packageMenuIds = entitlement(tenantId).getMenuIds();
        // 与真实存在的租户域菜单取交集：套餐中已删除/越权写入的平台菜单编号自然失效
        if (packageMenuIds == null) {
            // 未配套餐：宽松默认为全部租户域菜单（避免运营疏漏导致整租户锁死）
            return MenuScopes.ids(scopeMenus);
        }
        Set<Long> result = new HashSet<>(packageMenuIds);
        result.retainAll(MenuScopes.ids(scopeMenus));
        // 祖先链闭合：套餐只圈叶子（历史坏数据/界面收割缺陷）时自愈补齐目录，否则 buildTree 无根可组
        return MenuScopes.withAncestorDirs(result, scopeMenus);
    }

    @Override
    public Set<SysPermission> getPlatformPermissions() {
        long now = System.currentTimeMillis();
        CacheEntry<Set<SysPermission>> cached = platformPermissionsCache;
        if (cached != null && cached.expireAt() > now) {
            return cached.value();
        }
        Set<SysPermission> permissions = new HashSet<>(permissionMapper.selectByMenuIds(getPlatformMenuIds()));
        Set<SysPermission> snapshot = Collections.unmodifiableSet(permissions);
        platformPermissionsCache = new CacheEntry<>(snapshot, now + PLATFORM_CACHE_TTL_MS);
        return snapshot;
    }

    @Override
    public Set<SysPermission> getTenantPermissions(Long tenantId) {
        return new HashSet<>(permissionMapper.selectByMenuIds(getTenantMenuIds(tenantId)));
    }

    @Override
    public java.util.Map<String, String> getEntitlementValues(Long tenantId) {
        vip.mseapp.api.tenant.dto.EntitlementDTO entitlement = entitlement(tenantId);
        java.util.Map<String, String> values = new java.util.LinkedHashMap<>(entitlement.getQuotas());
        values.putAll(entitlement.getFeatures());
        return values;
    }

    /** 权益快照：EntitlementCache 版本缓存回源契约（无 Redis 直连；运行期不再逐次 RPC） */
    private vip.mseapp.api.tenant.dto.EntitlementDTO entitlement(Long tenantId) {
        vip.mseapp.redis.EntitlementCache cache = entitlementCacheProvider.getIfAvailable();
        vip.mseapp.api.tenant.dto.EntitlementDTO loaded = remoteTenantService.getTenantEntitlement(tenantId);
        if (loaded == null) {
            loaded = new vip.mseapp.api.tenant.dto.EntitlementDTO();
        }
        if (cache == null) {
            return loaded;
        }
        final vip.mseapp.api.tenant.dto.EntitlementDTO snapshot = loaded;
        return cache.getOrLoad(tenantId == null ? 0L : tenantId, vip.mseapp.api.tenant.dto.EntitlementDTO.class,
                () -> snapshot);
    }
}
