package vip.mseapp.api.system;

import vip.mseapp.api.system.dto.UserDTO;
import vip.mseapp.core.result.PageResult;

import java.util.List;

/**
 * 用户服务契约（service-system 实现）。
 *
 * <p>消费方（认证链路、业务微服务、开放平台）经 @RpcReference 注入：单体本地直调，
 * 微服务经 Dubbo 调用 mse-system。租户隔离语义：方法内隐式依赖调用方租户上下文
 * （HTTP 链路由 token 同步注入，Dubbo 链路由 attachment 透传），服务端经租户插件隔离。
 */
public interface RemoteUserService {

    /** 按租户 + 用户名查询（登录链路使用，含密码散列） */
    UserDTO getUserByUsername(Long tenantId, String username);

    /** 按编号查询 */
    UserDTO getUserById(Long userId);

    /** 租户管理员列表（平台端「重置管理员密码」链路使用；super_admin=1，显式租户语义） */
    List<UserDTO> listTenantAdmins(Long tenantId);

    /**
     * 重置租户管理员密码（平台端链路使用，一次性开通写入）：按租户 + 用户名定位（平台库只存
     * 管理员账号，不回读租户库），显式租户 + 超管条件，目标不存在或非管理员时抛错；
     * 密码为 BCrypt 口令散列（调用方完成散列）。
     */
    void resetTenantAdminPassword(Long tenantId, String username, String passwordHash);

    /**
     * 创建租户管理员（租户开通链路使用）。
     *
     * @param tenantId     租户编号
     * @param username     登录名
     * @param passwordHash BCrypt 口令散列（调用方完成散列）
     * @param nickname     昵称
     * @return 创建后的用户
     */
    UserDTO createTenantAdmin(Long tenantId, String username, String passwordHash, String nickname);

    /** 当前租户分页查询（deptId 传部门编号时含其子部门；机器凭证链路无用户上下文，不叠加数据范围） */
    PageResult<UserDTO> pageUsers(long pageNum, long pageSize, String username, Integer status, Long deptId);

    /**
     * 创建普通用户（开放链路使用）：superAdmin 一律强制 FALSE，
     * 超级管理员只能由平台/租户管理员在人机会话里授予。
     *
     * @param user username/password(BCrypt 散列)/nickname/mobile/email/deptId，status 缺省启用
     * @return 创建后的用户
     */
    UserDTO createUser(UserDTO user);

    /** 更新用户基础信息（仅 id/nickname/mobile/email/status/deptId 参与更新） */
    void updateUser(UserDTO user);

    /** 重置密码 */
    void resetPassword(Long userId, String passwordHash);

    /** 删除用户及其角色关联 */
    void deleteUser(Long userId);

    /** 查询用户角色编号集合 */
    List<Long> getUserRoleIds(Long userId);

    /** 覆盖式分配用户角色（传空集合 = 清空角色） */
    void assignRoles(Long userId, List<Long> roleIds);
}
