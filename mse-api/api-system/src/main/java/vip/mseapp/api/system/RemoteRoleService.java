package vip.mseapp.api.system;

import vip.mseapp.api.system.dto.RoleDTO;

import java.util.List;

/**
 * 角色服务契约（service-system 实现）：角色查询与基础创建。
 *
 * <p>开放链路只提供查询与基础创建（不挂菜单/权限点授权），授权矩阵仍由管理员
 * 在管理端维护，防止机器凭证越权改权限。
 */
public interface RemoteRoleService {

    /** 当前域全量角色 */
    List<RoleDTO> listRoles();

    /**
     * 创建角色（不绑定任何菜单/权限点/数据范围部门）。
     *
     * @param role      name/code 必填；sort 缺省 0；dataScope 缺省 5（仅本人），取值 1-5；status 缺省启用
     * @return 角色编号
     */
    Long createRole(RoleDTO role);
}
