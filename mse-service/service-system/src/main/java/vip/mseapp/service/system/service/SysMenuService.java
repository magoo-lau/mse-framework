package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.service.system.entity.SysMenu;

import java.util.List;
import java.util.Set;

public interface SysMenuService extends IRepository<SysMenu> {

    /** 全量菜单树（平台端维护界面，含平台域与租户域、双 client——维护界面按行编辑 client 字段） */
    List<MenuTreeNode> getMenuTree();

    /** 按可见域 + 客户端过滤的菜单树（client 取 SysMenu.CLIENT_*） */
    List<MenuTreeNode> getMenuTreeByScope(int menuScope, int client);

    /** 按可见域 + 客户端过滤的菜单列表（平台会话 = 基座库 ∪ 共享主库并集；登录态/权限域等非管理界面读路径须用本方法，租户域菜单自持于租户库） */
    List<SysMenu> listByScopeMerged(int menuScope, int client);

    /** 可见域 + 客户端 + 域菜单集合三重过滤的菜单树（授权选项树与保存校验同域，防止看得到却存不了） */
    List<MenuTreeNode> getMenuTreeInDomain(int menuScope, int client, Set<Long> domainMenuIds);

    /** 菜单列表构建为树（供授权/路由等场景复用） */
    List<MenuTreeNode> buildTree(List<SysMenu> menus);

    /** 保存前校验：类型/可见域枚举，type=3 子应用页面的 micro_name 存在性与 path 格式 */
    void validate(SysMenu menu);

    /** 用户角色编码集合（需租户上下文） */
    Set<String> getRoleCodesByUserId(Long userId);

    record MenuTreeNode(SysMenu menu, List<MenuTreeNode> children) {
    }
}
