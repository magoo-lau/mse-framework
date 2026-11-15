package vip.mseapp.service.system.rpc;

import vip.mseapp.api.system.RemoteRoleService;
import vip.mseapp.api.system.dto.RoleDTO;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.service.system.entity.SysRole;
import vip.mseapp.service.system.service.SysRoleService;

import java.util.List;

/**
 * 角色服务契约实现：只提供查询与基础创建（不挂菜单/权限点授权），
 * 授权矩阵仍由管理员在管理端维护，防止机器凭证越权改权限。
 */
@RpcService(RemoteRoleService.class)
public class RemoteRoleServiceImpl implements RemoteRoleService {

    private final SysRoleService roleService;

    public RemoteRoleServiceImpl(SysRoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public List<RoleDTO> listRoles() {
        return roleService.listRoles().stream().map(RemoteRoleServiceImpl::toDto).toList();
    }

    @Override
    public Long createRole(RoleDTO role) {
        if (role.getDataScope() != null && (role.getDataScope() < 1 || role.getDataScope() > 5)) {
            throw new ServiceException(400, "dataScope 取值须为 1-5");
        }
        SysRole entity = new SysRole();
        entity.setName(role.getName());
        entity.setCode(role.getCode());
        entity.setSort(role.getSort() == null ? 0 : role.getSort());
        entity.setDataScope(role.getDataScope() == null ? SysRole.SCOPE_SELF : role.getDataScope());
        entity.setStatus(0);
        return roleService.createRole(entity, List.of(), List.of(), List.of());
    }

    private static RoleDTO toDto(SysRole role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setCode(role.getCode());
        dto.setSort(role.getSort());
        dto.setDataScope(role.getDataScope());
        dto.setStatus(role.getStatus());
        return dto;
    }
}
