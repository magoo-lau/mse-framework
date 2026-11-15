package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysPermission;
import vip.mseapp.service.system.entity.SysRolePermission;
import vip.mseapp.service.system.mapper.SysMenuMapper;
import vip.mseapp.service.system.mapper.SysPermissionMapper;
import vip.mseapp.service.system.mapper.SysRolePermissionMapper;
import vip.mseapp.redis.PermissionCache;
import vip.mseapp.service.system.catalog.CatalogChangeRecorder;
import vip.mseapp.service.system.service.SysPermissionService;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SysPermissionServiceImpl extends CrudRepository<SysPermissionMapper, SysPermission>
        implements SysPermissionService {

    private final SysMenuMapper menuMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PermissionCache permissionCache;
    private final org.springframework.beans.factory.ObjectProvider<CatalogChangeRecorder> recorderProvider;

    public SysPermissionServiceImpl(SysMenuMapper menuMapper,
                                    SysRolePermissionMapper rolePermissionMapper,
                                    PermissionCache permissionCache,
                                    org.springframework.beans.factory.ObjectProvider<CatalogChangeRecorder> recorderProvider) {
        this.menuMapper = menuMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.permissionCache = permissionCache;
        this.recorderProvider = recorderProvider;
    }

    @Override
    public List<SysPermission> listAll() {
        // 目录真源在会话库（平台会话=平台库），单库直读
        return list(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, 0)
                .orderByAsc(SysPermission::getSort));
    }

    @Override
    public Long createPermission(SysPermission permission) {
        checkCodeUnique(permission.getCode(), null);
        checkMenuExists(permission.getMenuId());
        permission.setId(null);
        save(permission);
        permissionCache.evictGlobal();
        recordUpsert(permission.getId());
        return permission.getId();
    }

    @Override
    public void updatePermission(SysPermission permission) {
        checkCodeUnique(permission.getCode(), permission.getId());
        checkMenuExists(permission.getMenuId());
        updateById(permission);
        permissionCache.evictGlobal();
        recordUpsert(permission.getId());
    }

    @Override
    public void deletePermission(Long id) {
        Long bound = rolePermissionMapper.selectCount(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getPermissionId, id));
        if (bound != null && bound > 0) {
            throw new ServiceException(1210, "权限点已分配给角色，无法删除");
        }
        removeById(id);
        permissionCache.evictGlobal();
        CatalogChangeRecorder recorder = recorderProvider.getIfAvailable();
        if (recorder != null) {
            recorder.recordPermissionDelete(id);
        }
    }

    /** 保存后回读全列快照落目录变更批次（扇出租户库） */
    private void recordUpsert(Long id) {
        CatalogChangeRecorder recorder = recorderProvider.getIfAvailable();
        if (recorder != null) {
            recorder.recordUpsert(getById(id));
        }
    }

    @Override
    public Set<String> getPermissionCodesByUserId(Long userId) {
        return new HashSet<>(baseMapper.selectCodesByUserId(userId));
    }

    private void checkCodeUnique(String code, Long excludeId) {
        Long count = lambdaQuery()
                .eq(SysPermission::getCode, code)
                .ne(excludeId != null, SysPermission::getId, excludeId)
                .count();
        if (count != null && count > 0) {
            throw new ServiceException(GlobalErrorCode.SYSTEM_PERMISSION_CODE_DUPLICATE);
        }
    }

    private void checkMenuExists(Long menuId) {
        if (menuId == null) {
            return;
        }
        if (menuMapper.selectById(menuId) == null) {
            throw new ServiceException(1211, "挂靠菜单不存在");
        }
    }
}
