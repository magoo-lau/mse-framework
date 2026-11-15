package vip.mseapp.service.system.security;

import vip.mseapp.core.license.LicensePayload;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysPermission;
import vip.mseapp.service.system.mapper.SysMenuMapper;
import vip.mseapp.service.system.mapper.SysPermissionMapper;
import vip.mseapp.service.system.service.TenantScopeService;
import vip.mseapp.service.system.util.MenuScopes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 授权凭证驱动的租户权限域（standalone 私有部署模式，替代「实时 RPC 平台取套餐」）：
 * {@code getTenantMenuIds = 凭证 menuIds ∩ 本地租户域真实菜单}。
 *
 * <p>防伪关键：菜单/权限/角色授权（{@code checkGrantsInScope}）全部经本接口取域——
 * 凭证签名由平台私钥保证不可伪造，本地 sys_menu/sys_role_menu 被篡改也越不过凭证域。
 * 平台域直读本库（standalone 单库目录自持，无需并集）。
 */
public class LicenseTenantScopeService implements TenantScopeService {

    private final SysMenuMapper menuMapper;
    private final SysPermissionMapper permissionMapper;
    private final LicenseHolder licenseHolder;

    public LicenseTenantScopeService(SysMenuMapper menuMapper, SysPermissionMapper permissionMapper,
                                     LicenseHolder licenseHolder) {
        this.menuMapper = menuMapper;
        this.permissionMapper = permissionMapper;
        this.licenseHolder = licenseHolder;
    }

    @Override
    public Set<Long> getPlatformMenuIds() {
        Set<Long> ids = new HashSet<>();
        for (SysMenu menu : menuMapper.selectByScope(SysMenu.SCOPE_PLATFORM)) {
            ids.add(menu.getId());
        }
        return ids;
    }

    @Override
    public Set<Long> getTenantMenuIds(Long tenantId) {
        // 域真源 = 平台签发的凭证 menuIds（防伪造）；与本地租户域菜单取交集兜底已删除菜单
        LicensePayload payload = licenseHolder.payload();
        Set<Long> licensed = payload == null ? new HashSet<>() : new HashSet<>(payload.menuIds());
        List<SysMenu> scopeMenus = menuMapper.selectByScope(SysMenu.SCOPE_TENANT);
        licensed.retainAll(MenuScopes.ids(scopeMenus));
        // 祖先链闭合：凭证只圈叶子时自愈补齐目录（仅补真实存在的目录行，不放大凭证叶子能力）
        return MenuScopes.withAncestorDirs(licensed, scopeMenus);
    }

    @Override
    public Set<SysPermission> getPlatformPermissions() {
        return new HashSet<>(permissionMapper.selectByMenuIds(getPlatformMenuIds()));
    }

    @Override
    public Set<SysPermission> getTenantPermissions(Long tenantId) {
        return new HashSet<>(permissionMapper.selectByMenuIds(getTenantMenuIds(tenantId)));
    }

    @Override
    public java.util.Map<String, String> getEntitlementValues(Long tenantId) {
        // standalone：权益=license features（签发时从平台权益链灌入，离线闭环）
        LicensePayload payload = licenseHolder.payload();
        return payload == null || payload.features() == null
                ? java.util.Map.of() : new java.util.LinkedHashMap<>(payload.features());
    }
}
