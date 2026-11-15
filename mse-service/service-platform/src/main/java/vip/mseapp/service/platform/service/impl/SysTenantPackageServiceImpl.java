package vip.mseapp.service.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.redis.MsePermCacheKeys;
import vip.mseapp.service.platform.entity.SysTenantPackage;
import vip.mseapp.service.platform.mapper.SysTenantPackageMapper;
import vip.mseapp.service.platform.service.SysTenantPackageService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

// 控制面服务（sys_tenant_package 仅平台库存在）：类级 NOT_SUPPORTED 防错库堤坝，勿删
//（getPackageMenuIds 在调用方租户事务内读套餐即中招；范式详见 SysTenantRouteServiceImpl 类注释）。
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class SysTenantPackageServiceImpl extends CrudRepository<SysTenantPackageMapper, SysTenantPackage>
        implements SysTenantPackageService {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 0.1.0 权益模型：套餐变更影响引用租户的权益（menuIds/套餐项）——失效 + 事件 */
    private final org.springframework.beans.factory.ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider;

    public SysTenantPackageServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                       org.springframework.beans.factory.ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider) {
        this.entitlementProvider = entitlementProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<SysTenantPackage> listPackages() {
        return list(new LambdaQueryWrapper<SysTenantPackage>().orderByDesc(SysTenantPackage::getId));
    }

    @Override
    public Long createPackage(SysTenantPackage tenantPackage) {
        validateMenuIds(tenantPackage.getMenuIds());
        tenantPackage.setId(null);
        save(tenantPackage);
        evictGlobal();
        return tenantPackage.getId();
    }

    @Override
    public void updatePackage(SysTenantPackage tenantPackage) {
        validateMenuIds(tenantPackage.getMenuIds());
        updateById(tenantPackage);
        evictGlobal();
    }

    @Override
    public void deletePackage(Long id) {
        removeById(id);
        evictGlobal();
    }

    /**
     * 套餐定义变更 → 全局版本自增，所有租户用户的权限缓存随 TTL 内失效。
     * 套餐菜单内容（是否租户域、是否仍存在）由 service-system 的权限域交集兜底。
     */
    private void evictGlobal() {
        vip.mseapp.service.platform.service.EntitlementService entitlement = entitlementProvider.getIfAvailable();
        if (entitlement != null) {
            entitlement.evictGlobal();
        }
        redisTemplate.opsForValue().increment(MsePermCacheKeys.globalVer());
    }

    private void validateMenuIds(String menuIds) {
        if (!StringUtils.hasText(menuIds)) {
            return;
        }
        for (String id : menuIds.split(",")) {
            String trimmed = id.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                throw new ServiceException(1230, "套餐菜单编号格式不正确: " + trimmed);
            }
        }
    }
}
