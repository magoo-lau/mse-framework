package vip.mseapp.service.platform.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.service.platform.entity.SysTenantPackage;

import java.util.List;

/**
 * 租户套餐管理：定义变更会使全部租户用户的权限缓存失效（全局版本自增）。
 */
public interface SysTenantPackageService extends IRepository<SysTenantPackage> {

    List<SysTenantPackage> listPackages();

    /** menuIds 格式校验（逗号分隔数字；菜单存在性/域合法性由权限域交集兜底） */
    Long createPackage(SysTenantPackage tenantPackage);

    void updatePackage(SysTenantPackage tenantPackage);

    void deletePackage(Long id);
}
