package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.api.tenant.dto.TenantDTO;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.redis.DomainBrandingCache;
import vip.mseapp.redis.DomainBrandingEntry;
import vip.mseapp.service.system.entity.SysDomainRegistry;
import vip.mseapp.service.system.entity.SysTenantBranding;
import vip.mseapp.service.system.entity.SysTenantBrandingDomain;
import vip.mseapp.service.system.mapper.SysDomainRegistryMapper;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 租户域品牌缓存发布器（service-system 侧的 Redis 写穿/重建通道）。
 *
 * <p>租户域条目 = 域名 → {租户三要素 + 租户自助品牌}。域名来源两路合并：租户在
 * 品牌个性化页自助绑定的 sys_tenant_branding_domain 行 + 控制面遗留的 sys_tenant.domain
 * 单域名（平台租户管理侧历史数据，继续有效）。品牌未配置也发布条目（品牌字段为 null，
 * 登录页默认样式 + 租户编码可锁定），保证「域名命中租户」与「品牌生效」解耦。
 *
 * <p>触发时机：应用启动全量重建（Runner，分布式锁防多实例重做）；品牌保存/绑定/解绑
 * 写穿该租户名下条目。Redis 不可用时静默跳过——读取侧有 DB 兜底，登录不受阻。
 * 重建同时把全部域名回填平台库 sys_domain_registry（跨库判重锚点，详见绑定侧两道判重）。
 */
public class TenantBrandingCachePublisher {

    private static final Logger log = LoggerFactory.getLogger(TenantBrandingCachePublisher.class);

    private final SysTenantBrandingService brandingService;
    private final SysTenantBrandingDomainService domainService;
    private final ObjectProvider<DomainBrandingCache> cacheProvider;
    private final ObjectProvider<RemoteTenantService> remoteTenantProvider;
    private final ObjectProvider<SysDomainRegistryMapper> registryProvider;

    public TenantBrandingCachePublisher(SysTenantBrandingService brandingService,
                                        SysTenantBrandingDomainService domainService,
                                        ObjectProvider<DomainBrandingCache> cacheProvider,
                                        ObjectProvider<RemoteTenantService> remoteTenantProvider,
                                        ObjectProvider<SysDomainRegistryMapper> registryProvider) {
        this.brandingService = brandingService;
        this.domainService = domainService;
        this.cacheProvider = cacheProvider;
        this.remoteTenantProvider = remoteTenantProvider;
        this.registryProvider = registryProvider;
    }

    /** 刷新该租户名下全部域名的缓存条目（品牌保存/绑定变更后写穿） */
    public void publishTenant(long tenantId) {
        DomainBrandingCache cache = cacheProvider.getIfAvailable();
        if (cache == null || tenantId <= 0) {
            return;
        }
        RemoteTenantService remoteTenant = remoteTenantProvider.getIfAvailable();
        if (remoteTenant == null) {
            return;
        }
        TenantDTO tenant = remoteTenant.getTenantById(tenantId);
        if (tenant == null) {
            return;
        }
        Map<String, DomainBrandingEntry> entries = TenantContext.runAs(tenantId, () ->
                assembleEntries(tenant, brandingService.getByTenantId(tenantId),
                        domainService.listByTenantId(tenantId)));
        entries.forEach(cache::putTenantDomain);
    }

    /** 摘除该租户的域名条目（解绑后调用；条目已易主则不动） */
    public void evictDomain(String domain, long ownerTenantId) {
        DomainBrandingCache cache = cacheProvider.getIfAvailable();
        if (cache != null && StringUtils.hasText(domain)) {
            cache.evictTenantDomain(domain.trim().toLowerCase(), ownerTenantId);
        }
    }

    /** 启动全量重建：先清后写（多实例经分布式锁只做一份，失败不阻断启动）；顺带回填平台库域名注册表 */
    public void rebuildAll() {
        DomainBrandingCache cache = cacheProvider.getIfAvailable();
        RemoteTenantService remoteTenant = remoteTenantProvider.getIfAvailable();
        if (cache == null || remoteTenant == null) {
            log.debug("[mse-branding-cache] Redis 或租户契约不可用，租户域缓存重建跳过");
            return;
        }
        cache.clearTenant();
        Map<String, DomainBrandingEntry> entries = new LinkedHashMap<>();
        for (TenantDTO tenant : remoteTenant.listTenants()) {
            if (tenant.getId() == null || tenant.getId() <= 0) {
                continue;
            }
            try {
                Map<String, DomainBrandingEntry> tenantEntries = TenantContext.runAs(tenant.getId(), () ->
                        assembleEntries(tenant, brandingService.getByTenantId(tenant.getId()),
                                domainService.listByTenantId(tenant.getId())));
                entries.putAll(tenantEntries);
                backfillRegistry(tenant, tenantEntries.keySet());
            } catch (Exception ex) {
                // 单租户失败（如独立库暂不可达）不拖垮整体，读取侧按 key 惰性回填兜底
                log.warn("[mse-branding-cache] 租户 {} 品牌缓存重建失败: {}", tenant.getId(), ex.getMessage());
            }
        }
        cache.putAllTenant(entries);
        log.info("[mse-branding-cache] 租户域品牌缓存重建完成: 租户 {} 个、域名 {} 条",
                entries.values().stream().map(DomainBrandingEntry::getTenantId).distinct().count(), entries.size());
    }

    /** 平台库域名注册表回填（存量绑定 + 控制面遗留单域名；按属主幂等，已被他租户占用则保留现属主） */
    private void backfillRegistry(TenantDTO tenant, Set<String> domains) {
        SysDomainRegistryMapper registryMapper = registryProvider.getIfAvailable();
        if (registryMapper == null || domains.isEmpty()) {
            return;
        }
        for (String domain : domains) {
            try {
                Long owner = TenantContext.runAs(0L, () -> {
                    SysDomainRegistry row = registryMapper.selectOne(
                            new LambdaQueryWrapper<SysDomainRegistry>()
                                    .eq(SysDomainRegistry::getDomain, domain));
                    if (row == null) {
                        SysDomainRegistry insert = new SysDomainRegistry();
                        insert.setDomain(domain);
                        insert.setTenantId(tenant.getId());
                        insert.setSource(StringUtils.hasText(tenant.getDomain())
                                && tenant.getDomain().trim().equalsIgnoreCase(domain)
                                ? SysDomainRegistry.SOURCE_CONTROL_PLANE
                                : SysDomainRegistry.SOURCE_SELF_SERVICE);
                        registryMapper.insert(insert);
                        return null;
                    }
                    return row.getTenantId();
                });
                if (owner != null && !owner.equals(tenant.getId())) {
                    log.warn("[mse-branding] 域名 {} 登记属主为租户 {}，与缓存重建租户 {} 不符（以登记为准）",
                            domain, owner, tenant.getId());
                }
            } catch (Exception ex) {
                log.warn("[mse-branding] 域名 {} 注册表回填失败: {}", domain, ex.getMessage());
            }
        }
    }

    /** 条目装配：域名 = 自助绑定行 ∪ 控制面遗留单域名；品牌未配置发布空品牌条目 */
    private Map<String, DomainBrandingEntry> assembleEntries(TenantDTO tenant, SysTenantBranding branding,
                                                             List<SysTenantBrandingDomain> bindings) {
        Set<String> domains = new LinkedHashSet<>();
        if (StringUtils.hasText(tenant.getDomain())) {
            domains.add(tenant.getDomain().trim().toLowerCase());
        }
        for (SysTenantBrandingDomain binding : bindings) {
            domains.add(binding.getDomain());
        }
        Map<String, DomainBrandingEntry> entries = new LinkedHashMap<>();
        for (String domain : domains) {
            DomainBrandingEntry entry = new DomainBrandingEntry();
            entry.setTenantId(tenant.getId());
            entry.setTenantCode(tenant.getCode());
            entry.setTenantName(tenant.getName());
            if (branding != null) {
                entry.setSiteTitle(branding.getSiteTitle());
                entry.setSiteLogo(branding.getSiteLogo());
                entry.setLoginBackground(branding.getLoginBackground());
                entry.setBackgroundColor(branding.getBackgroundColor());
                entry.setCopyright(branding.getCopyright());
            }
            entries.put(domain, entry);
        }
        return entries;
    }
}
