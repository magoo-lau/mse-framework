package vip.mseapp.service.system.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.mseapp.api.system.RemoteOrgService;
import vip.mseapp.api.system.dto.OrgDeptDTO;
import vip.mseapp.api.system.dto.OrgRoleDTO;
import vip.mseapp.api.system.dto.OrgUserDTO;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysDept;
import vip.mseapp.service.system.entity.SysRole;
import vip.mseapp.service.system.entity.SysUser;
import vip.mseapp.service.system.entity.SysUserRole;
import vip.mseapp.service.system.mapper.SysDeptMapper;
import vip.mseapp.service.system.mapper.SysRoleMapper;
import vip.mseapp.service.system.mapper.SysUserMapper;
import vip.mseapp.service.system.mapper.SysUserRoleMapper;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 组织架构查询契约实现（只读）。
 *
 * <p>租户隔离由调用方上下文 + 基座租户插件保证（Dubbo attachment 透传租户）。
 * 查询均为 Lambda 条件（不经 LIMIT 语法），三方言数据库通用。
 */
@RpcService(RemoteOrgService.class)
public class RemoteOrgServiceImpl implements RemoteOrgService {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    public RemoteOrgServiceImpl(SysUserMapper userMapper, SysDeptMapper deptMapper,
                                SysRoleMapper roleMapper, SysUserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public OrgUserDTO getUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user == null ? null : toOrgUser(user);
    }

    @Override
    public List<OrgUserDTO> listUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(userIds).stream().map(RemoteOrgServiceImpl::toOrgUser).toList();
    }

    @Override
    public List<OrgUserDTO> listUsersByDept(Long deptId, boolean includeChildren) {
        Set<Long> deptIds = new HashSet<>();
        deptIds.add(deptId);
        if (includeChildren) {
            List<SysDept> all = deptMapper.selectList(new LambdaQueryWrapper<>());
            boolean grown = true;
            while (grown) {
                grown = false;
                for (SysDept dept : all) {
                    if (dept.getParentId() != null && deptIds.contains(dept.getParentId())
                            && deptIds.add(dept.getId())) {
                        grown = true;
                    }
                }
            }
        }
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getDeptId, deptIds))
                .stream().map(RemoteOrgServiceImpl::toOrgUser).toList();
    }

    @Override
    public List<OrgUserDTO> listUsersByRole(Long roleId) {
        List<Long> userIds = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, roleId))
                .stream().map(SysUserRole::getUserId).toList();
        return listUsersByIds(userIds);
    }

    @Override
    public List<OrgDeptDTO> listAllDepts() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort))
                .stream()
                .map(dept -> new OrgDeptDTO(dept.getId(), dept.getParentId(), dept.getName(),
                        dept.getAncestors(), dept.getLeaderUserId()))
                .toList();
    }

    @Override
    public PageResult<OrgUserDTO> pageUsers(long pageNum, long pageSize, String keyword, Long deptId) {
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysUser>()
                        .and(keyword != null && !keyword.isBlank(),
                                w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword))
                        .eq(deptId != null, SysUser::getDeptId, deptId)
                        .orderByAsc(SysUser::getId));
        List<OrgUserDTO> records = page.getRecords().stream().map(RemoteOrgServiceImpl::toOrgUser).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, records);
    }

    @Override
    public List<OrgRoleDTO> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId))
                .stream()
                .map(role -> new OrgRoleDTO(role.getId(), role.getName()))
                .toList();
    }

    @Override
    public Long getTenantAdminUserId() {
        // 取首行走分页插件按方言翻译（不用 LIMIT 1，达梦/金仓等信创库不保证语法兼容）
        List<SysUser> records = userMapper.selectPage(Page.of(1, 1, false),
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getSuperAdmin, true)
                                .orderByAsc(SysUser::getId))
                .getRecords();
        return records.isEmpty() ? null : records.get(0).getId();
    }

    private static OrgUserDTO toOrgUser(SysUser user) {
        return new OrgUserDTO(user.getId(), user.getUsername(), user.getNickname(),
                user.getDeptId(), user.getMobile(), user.getAvatar(), user.getStatus());
    }
}
