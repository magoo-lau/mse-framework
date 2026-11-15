package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysUser;

import java.util.List;

/**
 * 用户服务（租户内操作由租户插件自动隔离；跨租户方法内部显式 runAs/runIgnore）。
 */
public interface SysUserService extends IRepository<SysUser> {

    /** 当前租户分页查询（deptId 传部门编号时含其子部门，数据范围仍然叠加生效） */
    PageResult<SysUser> pageUsers(long pageNum, long pageSize, String username, Integer status, Long deptId);

    /** 平台端分页查询（仅平台域 tenant_id=0 用户，机制与租户内查询一致） */
    PageResult<SysUser> pageUsersPlatform(long pageNum, long pageSize, String username, Integer status);

    /** 按租户 + 用户名查询（显式租户语义，内部 runAs） */
    SysUser getByUsername(Long tenantId, String username);

    /** 租户管理员列表（super_admin=1，显式租户语义；平台端重置密码链路使用） */
    List<SysUser> listSuperAdmins(Long tenantId);

    /** 重置租户管理员密码（显式租户 + 超管条件，目标不存在或非管理员时抛错；密码 BCrypt 由调用方处理） */
    void resetSuperAdminPassword(Long tenantId, String username, String passwordHash);

    /** 重置租户管理员 TOTP 双因子（显式租户 + 超管条件，目标不存在或非管理员时抛错；secret 传 null 即解绑） */
    void resetSuperAdminTotp(Long tenantId, String username);

    /** 更新用户 TOTP 密钥（显式租户语义，登录双因子链路与管理端重置共用；secret 传 null 即解绑） */
    void updateTotpSecret(Long tenantId, Long userId, String secret);

    /** 创建当前租户用户（username 租户内唯一校验，密码 BCrypt 由调用方处理） */
    Long createUser(SysUser user);

    /** 更新当前租户用户基础信息 */
    void updateUser(SysUser user);

    /** 重置密码 */
    void resetPassword(Long userId, String passwordHash);

    /** 更新当前登录者头像地址（个人中心上传） */
    void updateAvatar(Long userId, String avatarUrl);

    /** 删除用户及其角色关联 */
    void deleteUser(Long userId);

    /** 查询用户角色编号集合 */
    List<Long> getUserRoleIds(Long userId);

    /** 覆盖式分配用户角色 */
    void assignRoles(Long userId, List<Long> roleIds);

    /** 查询用户岗位编号集合 */
    List<Long> getUserPostIds(Long userId);

    /** 覆盖式分配用户岗位 */
    void assignPosts(Long userId, List<Long> postIds);

    /** 导出用全量查询（数据范围与分页查询一致） */
    List<SysUser> listUsersForExport(String username, Integer status);

    /**
     * Excel 导入（当前租户域）。
     *
     * <p>按用户名幂等：已存在且 updateSupport=true 时更新基础字段，否则记为失败行；
     * 新用户密码取行内密码，缺省用参数 sys.user.initPassword。
     */
    UserImportResult importUsers(List<UserImportRow> rows, boolean updateSupport);

    /** 导入行（控制器从 Excel 行转换） */
    record UserImportRow(String username, String nickname, String mobile, String email,
                         Long deptId, Integer status, String password) {
    }

    /** 导入结果：逐行错误原因（行号从 2 起，含表头行） */
    record UserImportResult(int successCount, int failCount, List<String> errors) {
    }
}
