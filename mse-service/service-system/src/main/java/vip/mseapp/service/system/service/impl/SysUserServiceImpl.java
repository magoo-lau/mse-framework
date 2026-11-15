package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.security.crypto.MsePasswords;
import vip.mseapp.service.system.entity.SysUser;
import vip.mseapp.service.system.entity.SysUserPost;
import vip.mseapp.service.system.entity.SysUserRole;
import vip.mseapp.service.system.mapper.SysUserMapper;
import vip.mseapp.service.system.mapper.SysUserPostMapper;
import vip.mseapp.service.system.mapper.SysUserRoleMapper;
import vip.mseapp.redis.PermissionCache;
import vip.mseapp.service.system.service.SysConfigService;
import vip.mseapp.service.system.service.SysUserService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SysUserServiceImpl extends CrudRepository<SysUserMapper, SysUser> implements SysUserService {

    /** 导入缺省密码的参数键（sys_config） */
    private static final String INIT_PASSWORD_CONFIG_KEY = "sys.user.initPassword";

    private final SysUserRoleMapper userRoleMapper;
    private final SysUserPostMapper userPostMapper;
    private final PermissionCache permissionCache;
    private final SysConfigService configService;

    /** 配额守卫（0.1.0 权益模型；缺席时不设限） */
    private final org.springframework.beans.factory.ObjectProvider<vip.mseapp.service.system.security.QuotaGuard> quotaGuardProvider;

    public SysUserServiceImpl(SysUserRoleMapper userRoleMapper, SysUserPostMapper userPostMapper,
                              PermissionCache permissionCache, SysConfigService configService,
                                             org.springframework.beans.factory.ObjectProvider<vip.mseapp.service.system.security.QuotaGuard> quotaGuardProvider) {
        this.userRoleMapper = userRoleMapper;
        this.userPostMapper = userPostMapper;
        this.permissionCache = permissionCache;
        this.configService = configService;
        this.quotaGuardProvider = quotaGuardProvider;
    }

    @Override
    public PageResult<SysUser> pageUsers(long pageNum, long pageSize, String username, Integer status, Long deptId) {
        // 走 @DataPermission 标注的自定义查询：除租户隔离外还叠加当前用户的数据范围（部门/本人）
        Page<SysUser> page = baseMapper.selectPageUsers(new Page<>(pageNum, pageSize), username, status, deptId);
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    public PageResult<SysUser> pageUsersPlatform(long pageNum, long pageSize, String username, Integer status) {
        // 平台端仅平台域用户：平台 token 已把租户上下文置为 0，租户行级拦截自动限定 tenant_id=0
        Page<SysUser> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysUser>()
                        .like(StringUtils.hasText(username), SysUser::getUsername, username)
                        .eq(status != null, SysUser::getStatus, status)
                        .orderByDesc(SysUser::getId));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    public SysUser getByUsername(Long tenantId, String username) {
        // 显式携带租户条件：独立库路由下租户插件不改写 sys_user，仅靠 runAs 会跨租户误命中同名账号
        return TenantContext.runAs(tenantId, () -> getOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getTenantId, tenantId)));
    }

    @Override
    public List<SysUser> listSuperAdmins(Long tenantId) {
        // 显式携带租户条件（同 getByUsername 语义）：路由与插件任一失效都不会跨租户泄露账号
        return TenantContext.runAs(tenantId, () -> list(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getSuperAdmin, Boolean.TRUE)
                .eq(SysUser::getTenantId, tenantId)
                .orderByAsc(SysUser::getId)));
    }

    /**
     * 单条 UPDATE 原子提交，不加 @Transactional：写库世界由入参 tenantId 经 runAs 语句级路由决定，
     * 若在方法入口（平台会话/无上下文）先开事务，TenantRouteTransactionManager 会把连接钉死在
     * 平台库/主库，事务内 runAs 切不动已绑连接，UPDATE 错库 0 行命中（报误导性"租户管理员不存在"）。
     */
    @Override
    public void resetSuperAdminPassword(Long tenantId, String username, String passwordHash) {
        // 按租户 + 用户名定位（平台一次性写入，不回读租户库）：显式租户 + 超管条件，
        // 越权账号或普通用户一律 0 行命中并抛错（不会误升级普通用户）
        boolean updated = Boolean.TRUE.equals(TenantContext.runAs(tenantId, () -> lambdaUpdate()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getSuperAdmin, Boolean.TRUE)
                .eq(SysUser::getTenantId, tenantId)
                .set(SysUser::getPassword, passwordHash)
                .update()));
        if (!updated) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "租户管理员不存在：" + username);
        }
    }

    /** 同 resetSuperAdminPassword：单条 UPDATE 语句级路由，不加 @Transactional（入口开事务会钉错库） */
    @Override
    public void resetSuperAdminTotp(Long tenantId, String username) {
        boolean updated = Boolean.TRUE.equals(TenantContext.runAs(tenantId, () -> lambdaUpdate()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getSuperAdmin, Boolean.TRUE)
                .eq(SysUser::getTenantId, tenantId)
                .set(SysUser::getTotpSecret, null)
                .update()));
        if (!updated) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "租户管理员不存在：" + username);
        }
    }

    /**
     * 单条 UPDATE 语句级路由，不加 @Transactional（入口开事务会钉错库，同 resetSuperAdminPassword）。
     * 显式携带租户条件（同 getByUsername 语义）：路由与插件任一失效都不会误写其他租户账号。
     */
    @Override
    public void updateTotpSecret(Long tenantId, Long userId, String secret) {
        boolean updated = Boolean.TRUE.equals(TenantContext.runAs(tenantId, () -> lambdaUpdate()
                .eq(SysUser::getId, userId)
                .eq(SysUser::getTenantId, tenantId)
                .set(SysUser::getTotpSecret, secret)
                .update()));
        if (!updated) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "用户不存在：" + userId);
        }
    }

    @Override
    public Long createUser(SysUser user) {
        checkUsernameUnique(user, user.getId());
        // 0.1.0 配额准入：quota.max_users（软配额；平台世界不设限）
        vip.mseapp.service.system.security.QuotaGuard guard = quotaGuardProvider.getIfAvailable();
        if (guard != null) {
            Long scope = user.getTenantId() != null ? user.getTenantId() : vip.mseapp.core.context.TenantContext.getTenantId();
            if (scope != null && scope != 0L) {
                guard.checkMaxUsers(scope, lambdaQuery().eq(SysUser::getTenantId, scope).count());
            }
        }
        user.setId(null);
        save(user);
        return user.getId();
    }

    @Override
    public void updateUser(SysUser user) {
        checkUsernameUnique(user, user.getId());
        SysUser db = getById(user.getId());
        if (db == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        // 登录名与租户归属不允许经此入口变更
        user.setUsername(null);
        user.setTenantId(null);
        user.setPassword(null);
        updateById(user);
    }

    @Override
    public void resetPassword(Long userId, String passwordHash) {
        SysUser update = new SysUser();
        update.setId(userId);
        update.setPassword(passwordHash);
        updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(Long userId, String avatarUrl) {
        SysUser update = new SysUser();
        update.setId(userId);
        update.setAvatar(avatarUrl);
        updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        removeById(userId);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        return userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (!CollectionUtils.isEmpty(roleIds)) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                userRoleMapper.insert(userRole);
            }
        }
        // 用户权限即刻生效（踢出租户权限缓存）
        permissionCache.evictTenant(TenantContext.getRequiredTenantId());
    }

    @Override
    public List<Long> getUserPostIds(Long userId) {
        return userPostMapper.selectList(
                        new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId))
                .stream().map(SysUserPost::getPostId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPosts(Long userId, List<Long> postIds) {
        userPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, userId));
        if (CollectionUtils.isEmpty(postIds)) {
            return;
        }
        for (Long postId : postIds) {
            SysUserPost userPost = new SysUserPost();
            userPost.setUserId(userId);
            userPost.setPostId(postId);
            userPostMapper.insert(userPost);
        }
    }

    @Override
    public List<SysUser> listUsersForExport(String username, Integer status) {
        return baseMapper.selectListUsers(username, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserImportResult importUsers(List<UserImportRow> rows, boolean updateSupport) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int success = 0;
        // 同名存在性判定限定在当前租户域（与租户插件的独立库跳过语义解耦）
        Long scope = TenantContext.getTenantId();
        for (int i = 0; i < rows.size(); i++) {
            // 行号从 2 起（1 为表头），与下载模板中的展示一致
            int line = i + 2;
            UserImportRow row = rows.get(i);
            if (!StringUtils.hasText(row.username())) {
                errors.add("第 " + line + " 行：登录名称为空，已跳过");
                continue;
            }
            if (!seen.add(row.username())) {
                errors.add("第 " + line + " 行：登录名称 " + row.username() + " 在文件内重复，已跳过");
                continue;
            }
            SysUser db = getOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, row.username())
                    .eq(scope != null, SysUser::getTenantId, scope));
            if (db != null) {
                if (!updateSupport) {
                    errors.add("第 " + line + " 行：用户 " + row.username() + " 已存在（未勾选更新）");
                    continue;
                }
                SysUser update = new SysUser();
                update.setId(db.getId());
                update.setNickname(row.nickname());
                update.setMobile(row.mobile());
                update.setEmail(row.email());
                update.setDeptId(row.deptId());
                update.setStatus(row.status() == null ? db.getStatus() : row.status());
                updateById(update);
                success++;
                continue;
            }
            SysUser user = new SysUser();
            user.setUsername(row.username());
            user.setNickname(StringUtils.hasText(row.nickname()) ? row.nickname() : row.username());
            user.setMobile(row.mobile());
            user.setEmail(row.email());
            user.setDeptId(row.deptId());
            user.setSuperAdmin(Boolean.FALSE);
            user.setStatus(row.status() == null ? 0 : row.status());
            String password = StringUtils.hasText(row.password()) ? row.password()
                    : configService.getConfigValueByKey(INIT_PASSWORD_CONFIG_KEY);
            user.setPassword(MsePasswords.encode(StringUtils.hasText(password) ? password : "123456"));
            save(user);
            success++;
        }
        return new UserImportResult(success, errors.size(), errors);
    }

    /**
     * 用户名唯一性校验（租户内唯一）：显式携带租户条件，不依赖租户插件的改写——
     * 独立库路由下插件跳过 sys_user，缺失该条件会退化为对主库的全局查重，误伤新租户开通。
     */
    private void checkUsernameUnique(SysUser user, Long excludeId) {
        Long scope = user.getTenantId() != null ? user.getTenantId() : TenantContext.getTenantId();
        Long count = lambdaQuery()
                .eq(SysUser::getUsername, user.getUsername())
                .eq(scope != null, SysUser::getTenantId, scope)
                .ne(excludeId != null, SysUser::getId, excludeId)
                .count();
        if (count != null && count > 0) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_USER_USERNAME_DUPLICATE);
        }
    }
}
