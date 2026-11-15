package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.service.system.entity.SysPermission;

import java.util.List;
import java.util.Set;

/**
 * 权限点管理（平台端维护全局定义；定义变更会使全部用户的权限缓存失效）。
 */
public interface SysPermissionService extends IRepository<SysPermission> {

    List<SysPermission> listAll();

    Long createPermission(SysPermission permission);

    void updatePermission(SysPermission permission);

    void deletePermission(Long id);

    /** 用户绑定的权限码集合（原始值，未做域裁剪；需租户上下文） */
    Set<String> getPermissionCodesByUserId(Long userId);
}
