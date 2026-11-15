package vip.mseapp.service.system.datascope;

import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.UserContext;
import vip.mseapp.mybatis.datascope.DataScopeParam;
import vip.mseapp.mybatis.datascope.DataScopeParamResolver;
import vip.mseapp.service.system.entity.SysRole;
import vip.mseapp.service.system.entity.SysUser;
import vip.mseapp.service.system.mapper.SysDeptMapper;
import vip.mseapp.service.system.mapper.SysRoleMapper;
import vip.mseapp.service.system.mapper.SysUserMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据范围解析（common-mybatis SPI 实现）：按当前用户的角色 data_scope 合并出最宽范围。
 *
 * <p>合并规则：任一角色为「全部」→ 不过滤；否则 CUSTOM/本部门/本部门及以下 的部门集合取并集；
 * 仅剩「仅本人」时按创建人过滤。无用户上下文（系统内部调用）与超管同样不过滤。
 * 本实现依赖请求线程的租户/用户上下文，且自身查询不标注 @DataPermission，无递归。
 * 合并算法见 {@link #mergeScopes}（RPC 契约实现 RemoteDataScopeServiceImpl 按 userId 复用同一算法）。
 */
public class DataScopeResolverImpl implements DataScopeParamResolver {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;

    public DataScopeResolverImpl(SysUserMapper userMapper, SysRoleMapper roleMapper, SysDeptMapper deptMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
    }

    @Override
    public DataScopeParam resolve() {
        UserContext.UserInfo current = UserContext.get();
        if (current == null || TenantContext.getTenantId() == null) {
            return null;
        }
        SysUser user = userMapper.selectById(current.userId());
        if (user == null || Boolean.TRUE.equals(user.getSuperAdmin())) {
            return null;
        }
        List<Integer> scopes = roleMapper.selectDataScopesByUserId(current.userId());
        if (scopes.contains(SysRole.SCOPE_ALL)) {
            return null;
        }
        Set<Long> custom = null;
        Set<Long> children = null;
        for (Integer scope : scopes) {
            if (scope == null) {
                continue;
            }
            switch (scope) {
                case SysRole.SCOPE_CUSTOM -> custom = new HashSet<>(roleMapper.selectCustomDeptIdsByUserId(current.userId()));
                case SysRole.SCOPE_DEPT -> {
                    if (user.getDeptId() != null) {
                        children = union(children, Set.of(user.getDeptId()));
                    }
                }
                case SysRole.SCOPE_DEPT_AND_CHILD -> {
                    if (user.getDeptId() != null) {
                        children = union(children, new HashSet<>(deptMapper.selectDeptAndChildren(user.getDeptId())));
                    }
                }
                default -> {
                    // 仅本人：无需展开
                }
            }
        }
        return mergeScopes(current.userId(), user.getDeptId(), custom, children);
    }

    /**
     * 合并各部门类范围为最宽范围：部门并集非空 → CUSTOM，否则 → 仅本人。
     * 「任一角色为全部 → null」由调用方先行短路（本方法不感知角色编号）。
     *
     * @param customDeptIds   自定义范围勾选的部门集合（无 CUSTOM 角色时传 null）
     * @param deptAndChildren 本部门 / 本部门及以下展开的部门集合（无该类角色或无部门时传 null）
     */
    public static DataScopeParam mergeScopes(Long userId, Long deptId, Set<Long> customDeptIds, Set<Long> deptAndChildren) {
        Set<Long> union = union(customDeptIds, deptAndChildren);
        if (union != null && !union.isEmpty()) {
            return new DataScopeParam(DataScopeParam.Type.CUSTOM, userId, deptId, union);
        }
        return new DataScopeParam(DataScopeParam.Type.SELF, userId, deptId, null);
    }

    private static Set<Long> union(Set<Long> a, Set<Long> b) {
        // 双入参皆空（如角色仅「仅本人」，或用户无部门）返回空集而非 null，防调用方 isEmpty NPE
        if (a == null || a.isEmpty()) {
            return b == null ? Set.of() : b;
        }
        if (b == null || b.isEmpty()) {
            return a;
        }
        Set<Long> merged = new HashSet<>(a);
        merged.addAll(b);
        return merged;
    }
}
