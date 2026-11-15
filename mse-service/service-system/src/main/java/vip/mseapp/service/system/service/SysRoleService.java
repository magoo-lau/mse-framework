package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysPermission;
import vip.mseapp.service.system.entity.SysRole;

import java.util.List;

public interface SysRoleService extends IRepository<SysRole> {

    PageResult<SysRole> pageRoles(long pageNum, long pageSize, String name, Integer status);

    List<SysRole> listRoles();

    /**
     * 创建角色并绑定菜单/权限点/自定义部门集合。
     * 授权内容按当前端域校验：平台端 ⊆ 平台域；租户端 ⊆ 租户套餐域。
     */
    Long createRole(SysRole role, List<Long> menuIds, List<Long> permissionIds, List<Long> deptIds);

    /**
     * 更新角色基础字段。授权集合语义：null = 保持已有授权不变；非 null（含空数组）= 全量替换。
     * 授权内容按当前端域校验：平台端 ⊆ 平台域；租户端 ⊆ 租户套餐域。
     */
    void updateRole(SysRole role, List<Long> menuIds, List<Long> permissionIds, List<Long> deptIds);

    /** 已分配给用户的角色不可删除 */
    void deleteRole(Long roleId);

    record RoleGrants(List<Long> menuIds, List<Long> permissionIds, List<Long> deptIds) {
    }

    RoleGrants getRoleGrants(Long roleId);

    /** 当前端可授权选项：域内菜单树 + 域内权限点（授权界面按菜单分组展示权限点） */
    /** 授权选项树（menus 按 client 过滤——PC/移动端入口分离；permissions 为域内全量，能力双端共享） */
    AssignOptions getAssignOptions(int client);

    record AssignOptions(List<SysMenuService.MenuTreeNode> menus, List<SysPermission> permissions) {
    }
}
