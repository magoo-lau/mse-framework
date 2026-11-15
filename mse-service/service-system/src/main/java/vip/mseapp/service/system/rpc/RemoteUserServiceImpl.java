package vip.mseapp.service.system.rpc;

import org.springframework.util.StringUtils;
import vip.mseapp.api.system.RemoteUserService;
import vip.mseapp.api.system.dto.UserDTO;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysUser;
import vip.mseapp.service.system.service.SysUserService;

import java.util.List;

/**
 * 用户服务契约实现。
 *
 * <p>本实现同时服务两种调用形态：@RpcReference 注入（local 直调 / dubbo 远程）与 HTTP 链路内部调用。
 * 所有方法均显式携带租户参数并在内部 runAs，保证无请求上下文（MQ/定时任务）时语义正确。
 */
@RpcService(RemoteUserService.class)
public class RemoteUserServiceImpl implements RemoteUserService {

    private final SysUserService userService;

    public RemoteUserServiceImpl(SysUserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDTO getUserByUsername(Long tenantId, String username) {
        SysUser user = userService.getByUsername(tenantId, username);
        return toDto(user);
    }

    @Override
    public UserDTO getUserById(Long userId) {
        SysUser user = userService.getById(userId);
        return toDto(user);
    }

    @Override
    public List<UserDTO> listTenantAdmins(Long tenantId) {
        return userService.listSuperAdmins(tenantId).stream().map(RemoteUserServiceImpl::toDto).toList();
    }

    @Override
    public void resetTenantAdminPassword(Long tenantId, String username, String passwordHash) {
        userService.resetSuperAdminPassword(tenantId, username, passwordHash);
    }

    @Override
    public UserDTO createTenantAdmin(Long tenantId, String username, String passwordHash, String nickname) {
        SysUser user = new SysUser();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setPassword(passwordHash);
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        user.setStatus(0);
        user.setSuperAdmin(Boolean.TRUE);
        return TenantContext.runAs(tenantId, () -> {
            Long id = userService.createUser(user);
            user.setId(id);
            return toDto(user);
        });
    }

    @Override
    public PageResult<UserDTO> pageUsers(long pageNum, long pageSize, String username, Integer status, Long deptId) {
        PageResult<SysUser> page = userService.pageUsers(pageNum, pageSize, username, status, deptId);
        return PageResult.of(page.total(), page.pageNum(), page.pageSize(),
                page.records().stream().map(RemoteUserServiceImpl::toDto).toList());
    }

    @Override
    public UserDTO createUser(UserDTO user) {
        SysUser entity = new SysUser();
        entity.setUsername(user.getUsername());
        entity.setPassword(user.getPassword());
        entity.setNickname(user.getNickname());
        entity.setMobile(user.getMobile());
        entity.setEmail(user.getEmail());
        entity.setDeptId(user.getDeptId());
        entity.setStatus(user.getStatus() == null ? 0 : user.getStatus());
        // 契约创建链路一律普通用户：超级管理员只能由平台/租户管理员在人机会话里授予
        entity.setSuperAdmin(Boolean.FALSE);
        Long id = userService.createUser(entity);
        entity.setId(id);
        return toDto(entity);
    }

    @Override
    public void updateUser(UserDTO user) {
        SysUser entity = new SysUser();
        entity.setId(user.getId());
        entity.setNickname(user.getNickname());
        entity.setMobile(user.getMobile());
        entity.setEmail(user.getEmail());
        entity.setStatus(user.getStatus());
        entity.setDeptId(user.getDeptId());
        userService.updateUser(entity);
    }

    @Override
    public void resetPassword(Long userId, String passwordHash) {
        userService.resetPassword(userId, passwordHash);
    }

    @Override
    public void deleteUser(Long userId) {
        userService.deleteUser(userId);
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        return userService.getUserRoleIds(userId);
    }

    @Override
    public void assignRoles(Long userId, List<Long> roleIds) {
        userService.assignRoles(userId, roleIds == null ? List.of() : roleIds);
    }

    private static UserDTO toDto(SysUser user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setTenantId(user.getTenantId());
        dto.setUsername(user.getUsername());
        dto.setPassword(user.getPassword());
        dto.setNickname(user.getNickname());
        dto.setMobile(user.getMobile());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setDeptId(user.getDeptId());
        dto.setStatus(user.getStatus());
        dto.setSuperAdmin(user.getSuperAdmin());
        dto.setTotpBound(StringUtils.hasText(user.getTotpSecret()));
        return dto;
    }
}
