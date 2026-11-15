package vip.mseapp.service.system.security;

import vip.mseapp.service.system.service.SysMenuService;

import java.util.List;
import java.util.Set;

/**
 * 用户访问视图统一计算（鉴权数据源 / 个人中心共用）：
 * 双端（平台/租户）× 超管（域内全量）/ 普通用户（角色绑定 ∩ 域）。
 */
public interface AccessService {

    /** 用户可见菜单树（构建前端动态路由；client = SysMenu.CLIENT_*，菜单入口按 PC/移动端分离） */
    List<SysMenuService.MenuTreeNode> getMenusForUser(boolean platform, Long tenantId, Long userId, boolean superAdmin, int client);

    /** 用户权限码集合（superAdmin = 域内全部权限码，而非通配 *，保证套餐裁剪对管理员同样生效） */
    Set<String> getPermissionCodesForUser(boolean platform, Long tenantId, Long userId, boolean superAdmin);

    /** 用户角色编码集合（需租户上下文） */
    Set<String> getRoleCodesForUser(Long userId);

    /**
     * 登录端可见的微前端子应用列表（/me 下发，仅应用注册信息；页面菜单走 /me 的 menus 树，
     * 由「菜单管理」type=3 子应用页面下发）。
     *
     * <p>过滤规则：仅启用；按登录端可见端（side）与客户端（client，PC 基座/移动基站各取所向）过滤；
     * 租户端应用可用性由菜单推导——租户菜单集里有该应用任一页面菜单即可用，未登记页面菜单的应用对所有租户可用。
     */
    List<MicroAppVO> getMicroAppsForUser(boolean platform, Long tenantId, int client);

    /**
     * 是否登记过任何启用子应用（sys_micro_app 存在启用行，与登录端/套餐无关）。
     * 随 /me 与 microApps 一并下发：前端据此区分 microApps 空列表的双语义——
     * 已登记（true）= 裁剪后当前账号一个都不可见，须显式清空（内置兜底表不得绕过授权）；
     * 从未登记（false）= 零配置起步，保留前端内置兜底表。
     */
    boolean hasRegisteredApp();

    /** 微前端子应用视图（wujie 加载入口 + 可见端 + 客户端 + 是否登记过页面菜单） */
    record MicroAppVO(Long id, String name, String title, String devEntry, String prodEntry, Integer side,
                      Integer client, boolean hasMenus) {
    }
}
