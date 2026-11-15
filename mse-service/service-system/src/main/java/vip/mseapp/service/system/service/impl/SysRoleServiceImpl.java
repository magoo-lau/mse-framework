package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.UserContext;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysPermission;
import vip.mseapp.service.system.entity.SysRole;
import vip.mseapp.service.system.entity.SysRoleDept;
import vip.mseapp.service.system.entity.SysRoleMenu;
import vip.mseapp.service.system.entity.SysRolePermission;
import vip.mseapp.service.system.entity.SysUserRole;
import vip.mseapp.service.system.mapper.SysRoleDeptMapper;
import vip.mseapp.service.system.mapper.SysRoleMapper;
import vip.mseapp.service.system.mapper.SysRoleMenuMapper;
import vip.mseapp.service.system.mapper.SysRolePermissionMapper;
import vip.mseapp.service.system.mapper.SysUserRoleMapper;
import vip.mseapp.redis.PermissionCache;
import vip.mseapp.service.system.service.SysMenuService;
import vip.mseapp.service.system.service.SysRoleService;
import vip.mseapp.service.system.service.TenantScopeService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SysRoleServiceImpl extends CrudRepository<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuService menuService;
    private final TenantScopeService tenantScopeService;
    private final PermissionCache permissionCache;

    public SysRoleServiceImpl(SysRoleMenuMapper roleMenuMapper, SysRolePermissionMapper rolePermissionMapper,
                              SysRoleDeptMapper roleDeptMapper, SysUserRoleMapper userRoleMapper,
                              SysMenuService menuService, TenantScopeService tenantScopeService,
                              PermissionCache permissionCache) {
        this.roleMenuMapper = roleMenuMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.roleDeptMapper = roleDeptMapper;
        this.userRoleMapper = userRoleMapper;
        this.menuService = menuService;
        this.tenantScopeService = tenantScopeService;
        this.permissionCache = permissionCache;
    }

    @Override
    public PageResult<SysRole> pageRoles(long pageNum, long pageSize, String name, Integer status) {
        Page<SysRole> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysRole>()
                        .like(StringUtils.hasText(name), SysRole::getName, name)
                        .eq(status != null, SysRole::getStatus, status)
                        .orderByAsc(SysRole::getSort));
        return PageResult.of(page.getTotal(), pageNum, pageSize, page.getRecords());
    }

    @Override
    public List<SysRole> listRoles() {
        return list(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getSort));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(SysRole role, List<Long> menuIds, List<Long> permissionIds, List<Long> deptIds) {
        checkCodeUnique(role.getCode(), null);
        checkGrantsInScope(menuIds, permissionIds);
        role.setId(null);
        if (role.getDataScope() == null) {
            role.setDataScope(SysRole.SCOPE_ALL);
        }
        save(role);
        saveGrants(role.getId(), menuIds, permissionIds, deptIds);
        permissionCache.evictTenant(TenantContext.getRequiredTenantId());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(SysRole role, List<Long> menuIds, List<Long> permissionIds, List<Long> deptIds) {
        checkCodeUnique(role.getCode(), role.getId());
        checkGrantsInScope(menuIds, permissionIds);
        if (role.getDataScope() == null) {
            role.setDataScope(SysRole.SCOPE_ALL);
        }
        updateById(role);
        // 授权集合 null = 保持不变（只改基础字段的调用方不误清授权）；非 null 含空数组 = 全量替换
        if (menuIds != null) {
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, role.getId()));
        }
        if (permissionIds != null) {
            rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, role.getId()));
        }
        if (deptIds != null || role.getDataScope() != SysRole.SCOPE_CUSTOM) {
            // 数据范围不再指向自定义时，残留的自定义部门集合一并清理
            roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, role.getId()));
        }
        saveGrants(role.getId(), menuIds, permissionIds, deptIds);
        permissionCache.evictTenant(TenantContext.getRequiredTenantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId) {
        Long bound = userRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        if (bound != null && bound > 0) {
            throw new ServiceException(1203, "角色已分配给用户，无法删除");
        }
        removeById(roleId);
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId));
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        permissionCache.evictTenant(TenantContext.getRequiredTenantId());
    }

    @Override
    public RoleGrants getRoleGrants(Long roleId) {
        List<Long> menuIds = roleMenuMapper.selectList(
                        new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, roleId))
                .stream().map(SysRolePermission::getPermissionId).toList();
        List<Long> deptIds = roleDeptMapper.selectList(
                        new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId))
                .stream().map(SysRoleDept::getDeptId).toList();
        return new RoleGrants(menuIds, permissionIds, deptIds);
    }

    @Override
    public AssignOptions getAssignOptions(int client) {
        boolean platform = Boolean.TRUE.equals(UserContext.isPlatform());
        int scope = platform ? SysMenu.SCOPE_PLATFORM : SysMenu.SCOPE_TENANT;
        Long tenantId = TenantContext.getRequiredTenantId();
        // 授权选项树与 checkGrantsInScope 同域：租户端只列出套餐域内菜单（平台端菜单/套餐外菜单不再出现），
        // 避免「树里看得到、保存被 SYSTEM_GRANT_OUT_OF_SCOPE 拒绝」的体验割裂；
        // 菜单树按 client 过滤（PC/移动端入口分离），权限点不按端过滤（能力双端共享，
        // 挂靠 PC 菜单的权限点在移动页签落入「通用权限」分组仍可勾选）
        Set<Long> domainMenuIds = platform
                ? tenantScopeService.getPlatformMenuIds()
                : tenantScopeService.getTenantMenuIds(tenantId);
        Set<SysPermission> permissions = platform
                ? tenantScopeService.getPlatformPermissions()
                : tenantScopeService.getTenantPermissions(tenantId);
        List<SysPermission> sorted = permissions.stream()
                .sorted(Comparator.comparing(SysPermission::getMenuId,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SysPermission::getSort))
                .collect(Collectors.toList());
        return new AssignOptions(menuService.getMenuTreeInDomain(scope, client, domainMenuIds), sorted);
    }

    /**
     * 域校验：平台端可授平台域；租户端可授套餐域（历史越权数据在鉴权侧另有防御性交集）。
     */
    private void checkGrantsInScope(List<Long> menuIds, List<Long> permissionIds) {
        boolean platform = Boolean.TRUE.equals(UserContext.isPlatform());
        Long tenantId = TenantContext.getRequiredTenantId();
        Set<Long> domainMenuIds = platform
                ? tenantScopeService.getPlatformMenuIds()
                : tenantScopeService.getTenantMenuIds(tenantId);
        Set<Long> domainPermissionIds = (platform
                ? tenantScopeService.getPlatformPermissions()
                : tenantScopeService.getTenantPermissions(tenantId))
                .stream().map(SysPermission::getId).collect(Collectors.toSet());
        if (menuIds != null && !domainMenuIds.containsAll(menuIds)) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_GRANT_OUT_OF_SCOPE);
        }
        if (permissionIds != null && !domainPermissionIds.containsAll(permissionIds)) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_GRANT_OUT_OF_SCOPE);
        }
    }

    private void saveGrants(Long roleId, List<Long> menuIds, List<Long> permissionIds, List<Long> deptIds) {
        if (!CollectionUtils.isEmpty(menuIds)) {
            for (Long menuId : new ArrayList<>(menuIds)) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenuMapper.insert(roleMenu);
            }
        }
        if (!CollectionUtils.isEmpty(permissionIds)) {
            for (Long permissionId : permissionIds) {
                SysRolePermission rolePermission = new SysRolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permissionId);
                rolePermissionMapper.insert(rolePermission);
            }
        }
        // 自定义部门集合仅在 data_scope=2 时有意义
        SysRole role = getById(roleId);
        if (role != null && role.getDataScope() == SysRole.SCOPE_CUSTOM && !CollectionUtils.isEmpty(deptIds)) {
            for (Long deptId : deptIds) {
                SysRoleDept roleDept = new SysRoleDept();
                roleDept.setRoleId(roleId);
                roleDept.setDeptId(deptId);
                roleDeptMapper.insert(roleDept);
            }
        }
    }

    private void checkCodeUnique(String code, Long excludeId) {
        Long count = lambdaQuery()
                .eq(SysRole::getCode, code)
                .ne(excludeId != null, SysRole::getId, excludeId)
                .count();
        if (count != null && count > 0) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_ROLE_CODE_DUPLICATE);
        }
    }
}
