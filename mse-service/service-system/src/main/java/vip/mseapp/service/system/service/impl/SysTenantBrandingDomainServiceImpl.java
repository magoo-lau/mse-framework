package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.service.system.entity.SysDomainRegistry;
import vip.mseapp.service.system.entity.SysTenantBrandingDomain;
import vip.mseapp.service.system.mapper.SysDomainRegistryMapper;
import vip.mseapp.service.system.mapper.SysTenantBrandingDomainMapper;
import vip.mseapp.service.system.service.SysTenantBrandingDomainService;
import vip.mseapp.service.system.service.TenantBrandingCachePublisher;

import java.util.List;
import java.util.regex.Pattern;

public class SysTenantBrandingDomainServiceImpl
        extends CrudRepository<SysTenantBrandingDomainMapper, SysTenantBrandingDomain>
        implements SysTenantBrandingDomainService {

    /** 域名格式：完整主机名（至少含一个点），与平台 sys_domain_config 口径一致 */
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("(?i)^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$");

    private final ObjectProvider<TenantBrandingCachePublisher> publisherProvider;

    private final ObjectProvider<SysDomainRegistryMapper> registryProvider;

    public SysTenantBrandingDomainServiceImpl(ObjectProvider<TenantBrandingCachePublisher> publisherProvider,
                                              ObjectProvider<SysDomainRegistryMapper> registryProvider) {
        // 惰性解析发布器：发布器反向依赖本服务，直接注入会构成装配环
        this.publisherProvider = publisherProvider;
        this.registryProvider = registryProvider;
    }

    @Override
    public List<SysTenantBrandingDomain> listOwn() {
        return listByTenantId(TenantContext.getRequiredTenantId());
    }

    @Override
    public SysTenantBrandingDomain bindOwn(String domain) {
        String normalized = normalizeDomain(domain);
        Long tenantId = TenantContext.getRequiredTenantId();
        // 域名全局唯一（一个域名只归一个世界），两道判重：
        // ① 本库 runIgnore 跨租户查重（共享库各世界行同库互见）；
        // ② 平台库全局注册表（独立库租户行互不可见——此前仅①时独立库租户可抢占他租户域名）。
        Long existed = TenantContext.runIgnore(() -> lambdaQuery()
                .eq(SysTenantBrandingDomain::getDomain, normalized)
                .count());
        if (existed != null && existed > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "该域名已被绑定");
        }
        SysDomainRegistry registry = new SysDomainRegistry();
        registry.setDomain(normalized);
        registry.setTenantId(tenantId);
        registry.setSource(SysDomainRegistry.SOURCE_SELF_SERVICE);
        // ② 平台库登记先行（runAs(0) 落平台库，uk_domain 兜底并发；登记成功后才写本库，失败补偿摘除登记）
        SysDomainRegistryMapper registryMapper = registryProvider.getIfAvailable();
        if (registryMapper != null) {
            try {
                TenantContext.runAs(0L, () -> registryMapper.insert(registry));
            } catch (DuplicateKeyException e) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "该域名已被绑定");
            }
        }
        SysTenantBrandingDomain binding = new SysTenantBrandingDomain();
        binding.setDomain(normalized);
        try {
            save(binding);
        } catch (RuntimeException e) {
            if (registryMapper != null) {
                TenantContext.runAs(0L, () -> registryMapper.deleteByDomainAndTenant(normalized, tenantId));
            }
            throw e;
        }
        // 写穿：该租户名下全部域名（含新绑定）的品牌条目刷新进缓存；Redis 异常不回滚绑定
        try {
            TenantBrandingCachePublisher publisher = publisherProvider.getIfAvailable();
            if (publisher != null) {
                publisher.publishTenant(tenantId);
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(SysTenantBrandingDomainServiceImpl.class)
                    .warn("[mse-branding-cache] 绑定后缓存写穿失败（DB 已绑定）: {}", ex.getMessage());
        }
        return binding;
    }

    @Override
    public void unbindOwn(Long id) {
        SysTenantBrandingDomain binding = getById(id);
        if (binding == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "绑定不存在");
        }
        removeById(binding.getId());
        // 摘除平台库全局登记（runAs(0)；按属主防误摘易主域名）
        SysDomainRegistryMapper registryMapper = registryProvider.getIfAvailable();
        if (registryMapper != null) {
            try {
                TenantContext.runAs(0L, () -> registryMapper
                        .deleteByDomainAndTenant(binding.getDomain(), binding.getTenantId()));
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(SysTenantBrandingDomainServiceImpl.class)
                        .warn("[mse-branding] 解绑后全局登记摘除失败（残留由重建回填通道收敛）: {}", ex.getMessage());
            }
        }
        // 物理删除后按属主摘除缓存条目（条目已易主时不动）；Redis 异常不影响解绑结果
        try {
            TenantBrandingCachePublisher publisher = publisherProvider.getIfAvailable();
            if (publisher != null) {
                publisher.evictDomain(binding.getDomain(), binding.getTenantId());
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(SysTenantBrandingDomainServiceImpl.class)
                    .warn("[mse-branding-cache] 解绑后缓存摘除失败（DB 已解绑）: {}", ex.getMessage());
        }
    }

    @Override
    public List<SysTenantBrandingDomain> listByTenantId(Long tenantId) {
        // 显式携带 tenant 条件：独立库路由下租户插件跳过本表，裸查会读到错误世界
        return lambdaQuery()
                .eq(SysTenantBrandingDomain::getTenantId, tenantId)
                .orderByAsc(SysTenantBrandingDomain::getId)
                .list();
    }

    /** 规范化域名：trim + 小写；格式非法抛 400 */
    private String normalizeDomain(String domain) {
        String normalized = domain == null ? "" : domain.trim().toLowerCase();
        if (!StringUtils.hasText(normalized) || !DOMAIN_PATTERN.matcher(normalized).matches()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "域名格式不正确（完整主机名，如 demo.example.com）");
        }
        return normalized;
    }
}
