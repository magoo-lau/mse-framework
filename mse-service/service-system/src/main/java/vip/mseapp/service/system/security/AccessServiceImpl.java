package vip.mseapp.service.system.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysMicroApp;
import vip.mseapp.service.system.entity.SysPermission;
import vip.mseapp.service.system.mapper.SysMenuMapper;
import vip.mseapp.service.system.mapper.SysPermissionMapper;
import vip.mseapp.service.system.service.SysMenuService;
import vip.mseapp.service.system.service.SysMicroAppService;
import vip.mseapp.service.system.service.SysPermissionService;
import vip.mseapp.service.system.service.TenantScopeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AccessServiceImpl implements AccessService {

    private final SysMenuMapper menuMapper;
    private final SysMenuService menuService;
    private final SysPermissionService permissionService;
    private final SysPermissionMapper permissionMapper;
    private final TenantScopeService tenantScopeService;
    private final SysMicroAppService microAppService;

    public AccessServiceImpl(SysMenuMapper menuMapper, SysMenuService menuService,
                             SysPermissionService permissionService, SysPermissionMapper permissionMapper,
                             TenantScopeService tenantScopeService, SysMicroAppService microAppService) {
        this.menuMapper = menuMapper;
        this.menuService = menuService;
        this.permissionService = permissionService;
        this.permissionMapper = permissionMapper;
        this.tenantScopeService = tenantScopeService;
        this.microAppService = microAppService;
    }

    @Override
    public List<SysMenuService.MenuTreeNode> getMenusForUser(boolean platform, Long tenantId, Long userId, boolean superAdmin, int client) {
        Set<Long> domainMenuIds = platform
                ? tenantScopeService.getPlatformMenuIds()
                : tenantScopeService.getTenantMenuIds(tenantId);
        int scope = platform ? SysMenu.SCOPE_PLATFORM : SysMenu.SCOPE_TENANT;
        List<SysMenu> menus;
        if (superAdmin) {
            // 超管 = 域内全量：平台域即全部平台菜单；租户域仍受套餐裁剪（而非全部租户菜单）。
            // 域内直读会话库（平台会话=平台库目录真源，租户会话=租户库自持副本）
            menus = menuService.listByScopeMerged(scope, client).stream()
                    .filter(menu -> domainMenuIds.contains(menu.getId()))
                    .toList();
        } else {
            // 普通用户 = 角色菜单 ∩ 域（防御历史越权绑定数据）；双端菜单两端均可见；client 条件在 SQL 层过滤
            menus = menuMapper.selectMenusByUserId(userId, client).stream()
                    .filter(menu -> domainMenuIds.contains(menu.getId()))
                    .filter(menu -> menu.getMenuScope() != null
                            && (menu.getMenuScope() == scope || menu.getMenuScope() == SysMenu.SCOPE_BOTH))
                    .toList();
        }
        // 目录不外露：空目录剪枝 + 祖先链补齐后再组树
        return menuService.buildTree(pruneDirTree(new ArrayList<>(menus), scope));
    }

    /**
     * 用户菜单树目录整形：只保留「非目录节点 + 其祖先链上的目录」。
     * 只授权了目录而其下无可可见子菜单时，空目录不再渲染（普通用户没权限就不该看到目录）；
     * 反向（授了子菜单但漏授父目录）也补齐祖先链，避免组树时整支子树因缺父被丢——
     * 祖先不在已授权集合内时从会话库按需取（限启用 + 同可见域，缺失即无法补齐的坏数据照旧丢弃）。
     */
    private List<SysMenu> pruneDirTree(List<SysMenu> menus, int scope) {
        Map<Long, SysMenu> byId = new HashMap<>();
        for (SysMenu menu : menus) {
            byId.put(menu.getId(), menu);
        }
        Set<Long> keep = new HashSet<>();
        for (SysMenu menu : menus) {
            if (menu.getType() == null || menu.getType() != SysMenu.TYPE_DIR) {
                keep.add(menu.getId());
            }
        }
        for (boolean grown = true; grown; ) {
            grown = false;
            for (SysMenu menu : menus) {
                if (!keep.contains(menu.getId())) {
                    continue;
                }
                SysMenu parent = menu.getParentId() == null ? null : byId.get(menu.getParentId());
                if (parent != null && !keep.contains(parent.getId())) {
                    keep.add(parent.getId());
                    grown = true;
                }
            }
            // 祖先不在已授权集合内：查库补齐缺失层（限启用 + 同可见域），直到无新增
            List<Long> missing = menus.stream()
                    .filter(menu -> keep.contains(menu.getId())
                            && menu.getParentId() != null && !byId.containsKey(menu.getParentId()))
                    .map(SysMenu::getParentId).distinct().toList();
            if (!missing.isEmpty()) {
                List<SysMenu> ancestors = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .in(SysMenu::getId, missing)
                        .eq(SysMenu::getStatus, 0)
                        .in(SysMenu::getMenuScope, scope, SysMenu.SCOPE_BOTH));
                for (SysMenu ancestor : ancestors) {
                    if (!byId.containsKey(ancestor.getId())) {
                        byId.put(ancestor.getId(), ancestor);
                        menus.add(ancestor);
                        keep.add(ancestor.getId());
                        grown = true;
                    }
                }
            }
        }
        return menus.stream().filter(menu -> keep.contains(menu.getId())).toList();
    }

    @Override
    public Set<String> getPermissionCodesForUser(boolean platform, Long tenantId, Long userId, boolean superAdmin) {
        Set<SysPermission> domainPermissions = platform
                ? tenantScopeService.getPlatformPermissions()
                : tenantScopeService.getTenantPermissions(tenantId);
        Set<String> domainCodes = domainPermissions.stream()
                .map(SysPermission::getCode).collect(Collectors.toSet());
        if (superAdmin) {
            return domainCodes;
        }
        Set<String> result = permissionService.getPermissionCodesByUserId(userId).stream()
                .filter(domainCodes::contains)
                .collect(Collectors.toSet());
        // 通用权限（未挂菜单，如个人资料）自动授予全部登录用户
        result.addAll(permissionMapper.selectUniversalCodes());
        return result;
    }

    @Override
    public Set<String> getRoleCodesForUser(Long userId) {
        return menuService.getRoleCodesByUserId(userId);
    }

    @Override
    public List<MicroAppVO> getMicroAppsForUser(boolean platform, Long tenantId, int client) {
        int tenantSide = platform ? SysMicroApp.SIDE_PLATFORM : SysMicroApp.SIDE_TENANT;
        // 全局登记过页面菜单（type=3）的子应用集合：这类应用的入口收敛在菜单树（套餐/角色双重裁剪），
        // 前端不得再以「未登记页面菜单」名义兜底渲染单项，否则未授权用户也会看到入口（点击即 403）。
        // 页面菜单按 client 过滤——PC 菜单不算移动应用的页面（反之亦然），防跨端 hasMenus 误判
        Set<String> appsWithMenus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getType, SysMenu.TYPE_MICRO)
                        .eq(SysMenu::getClient, client)
                        .eq(SysMenu::getStatus, 0)).stream()
                .map(SysMenu::getMicroName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // 租户端应用可用性由菜单推导：租户菜单集（套餐裁剪后）里有该应用任一页面（type=3）即可用；
        // 未登记任何页面菜单的应用（零配置/兜底形态）对所有租户可用。
        // 平台端不受套餐裁剪，仅需 side 过滤（domainMenuIds=null 表示不做菜单推导）
        Set<Long> domainMenuIds = platform ? null : tenantScopeService.getTenantMenuIds(tenantId);
        Set<String> visibleMicroNames = null;
        if (domainMenuIds != null) {
            List<SysMenu> microMenus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .eq(SysMenu::getType, SysMenu.TYPE_MICRO)
                    .eq(SysMenu::getClient, client)
                    .eq(SysMenu::getStatus, 0));
            visibleMicroNames = microMenus.stream()
                    .filter(menu -> domainMenuIds.contains(menu.getId()))
                    .map(SysMenu::getMicroName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        // 子应用注册随会话库（平台会话=平台库目录真源，租户会话=租户库自持副本）直读
        List<SysMicroApp> enabledApps = microAppService.listEnabled();
        List<MicroAppVO> result = new ArrayList<>();
        for (SysMicroApp app : enabledApps) {
            // 可见端过滤：side=双端 恒可见，否则须匹配登录端
            if (app.getSide() != SysMicroApp.SIDE_BOTH && app.getSide() != tenantSide) {
                continue;
            }
            // 客户端过滤：client=PC 恒可见（存量零迁移），否则须匹配请求端
            Integer appClient = app.getClient() == null ? SysMenu.CLIENT_PC : app.getClient();
            if (appClient != SysMenu.CLIENT_PC && appClient != client) {
                continue;
            }
            // 租户端：登记了页面菜单的应用，须菜单集里至少一个页面可见，否则整体不可用
            if (visibleMicroNames != null && appsWithMenus.contains(app.getName())
                    && !visibleMicroNames.contains(app.getName())) {
                continue;
            }
            result.add(new MicroAppVO(app.getId(), app.getName(), app.getTitle(),
                    app.getDevEntry(), app.getProdEntry(), app.getSide(), appClient,
                    appsWithMenus.contains(app.getName())));
        }
        return result;
    }

    @Override
    public boolean hasRegisteredApp() {
        // 与 getMicroAppsForUser 同口径：会话库任一启用行即视为已登记
        return !microAppService.listEnabled().isEmpty();
    }
}
