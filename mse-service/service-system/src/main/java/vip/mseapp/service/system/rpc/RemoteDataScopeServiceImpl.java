package vip.mseapp.service.system.rpc;

import vip.mseapp.api.system.RemoteDataScopeService;
import vip.mseapp.api.system.dto.DataScopeDTO;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.service.system.datascope.DataScopeResolverImpl;
import vip.mseapp.service.system.entity.SysRole;
import vip.mseapp.service.system.entity.SysUser;
import vip.mseapp.service.system.mapper.SysDeptMapper;
import vip.mseapp.service.system.mapper.SysRoleMapper;
import vip.mseapp.service.system.mapper.SysUserMapper;
import vip.mseapp.mybatis.datascope.DataScopeParam;

import java.util.List;
import java.util.Set;

/**
 * 数据范围查询契约实现：按 userId 解析（不依赖调用线程的用户上下文），
 * 与 {@link DataScopeResolverImpl} 复用同一合并算法，语义零偏差。
 * 服务端在调用方租户上下文内执行（Dubbo attachment 透传租户），经租户插件隔离。
 */
@RpcService(RemoteDataScopeService.class)
public class RemoteDataScopeServiceImpl implements RemoteDataScopeService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;

    public RemoteDataScopeServiceImpl(SysUserMapper userMapper, SysRoleMapper roleMapper, SysDeptMapper deptMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
    }

    @Override
    public DataScopeDTO resolve(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null || Boolean.TRUE.equals(user.getSuperAdmin())) {
            return null;
        }
        List<Integer> scopes = roleMapper.selectDataScopesByUserId(userId);
        if (scopes.contains(SysRole.SCOPE_ALL)) {
            return null;
        }
        Set<Long> custom = scopes.contains(SysRole.SCOPE_CUSTOM)
                ? Set.copyOf(roleMapper.selectCustomDeptIdsByUserId(userId))
                : null;
        Set<Long> children = null;
        if (user.getDeptId() != null) {
            if (scopes.contains(SysRole.SCOPE_DEPT_AND_CHILD)) {
                children = Set.copyOf(deptMapper.selectDeptAndChildren(user.getDeptId()));
            } else if (scopes.contains(SysRole.SCOPE_DEPT)) {
                children = Set.of(user.getDeptId());
            }
        }
        return toDto(DataScopeResolverImpl.mergeScopes(userId, user.getDeptId(), custom, children));
    }

    private static DataScopeDTO toDto(DataScopeParam param) {
        DataScopeDTO dto = new DataScopeDTO();
        dto.setType(param.type().name());
        dto.setUserId(param.userId());
        dto.setDeptId(param.deptId());
        dto.setDeptIds(param.deptIds());
        return dto;
    }
}
