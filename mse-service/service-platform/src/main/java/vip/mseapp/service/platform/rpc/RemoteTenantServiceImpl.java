package vip.mseapp.service.platform.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import vip.mseapp.api.system.RemoteBrandingService;
import vip.mseapp.api.system.dto.BrandDTO;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.api.tenant.dto.DomainConfigDTO;
import vip.mseapp.api.tenant.dto.TablePolicyDTO;
import vip.mseapp.api.tenant.dto.TenantDTO;
import vip.mseapp.api.tenant.dto.TenantDatasourceDTO;
import vip.mseapp.api.tenant.dto.TenantRouteDTO;
import vip.mseapp.api.tenant.dto.TenantSnapshotDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.redis.DomainBrandingCache;
import vip.mseapp.redis.DomainBrandingEntry;
import vip.mseapp.redis.TenantCache;
import vip.mseapp.service.platform.entity.SysDomainConfig;
import vip.mseapp.service.platform.entity.SysTenant;
import vip.mseapp.service.platform.entity.SysTenantDatasource;
import vip.mseapp.service.platform.entity.SysTenantPackage;
import vip.mseapp.service.platform.runner.DomainBrandingCacheRunner;
import vip.mseapp.service.platform.service.SysDomainConfigService;
import vip.mseapp.service.platform.service.SysTenantDatasourceService;
import vip.mseapp.service.platform.service.SysTenantPackageService;
import vip.mseapp.service.platform.service.SysTenantRouteService;
import vip.mseapp.service.platform.service.SysTenantService;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@RpcService(RemoteTenantService.class)
public class RemoteTenantServiceImpl implements RemoteTenantService {

    private static final Logger log = LoggerFactory.getLogger(RemoteTenantServiceImpl.class);

    private final SysTenantService tenantService;
    private final SysTenantPackageService packageService;
    private final SysDomainConfigService domainConfigService;
    private final SysTenantRouteService routeService;
    private final SysTenantDatasourceService datasourceService;
    private final ObjectProvider<DomainBrandingCache> cacheProvider;
    /** 租户行 Redis 缓存（个人资料/在线用户/AI 平台等每请求路径；写侧经路由 evict INCR 版本失效） */
    private final ObjectProvider<TenantCache> tenantCacheProvider;

    /** 0.1.0 权益模型：快照/契约取值（ObjectProvider 防装配环） */
    private final ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider;

    /** 租户自助品牌查询（service-system 契约，单体本地直调 / 微服务 dubbo 远调） */
    @RpcReference
    private RemoteBrandingService remoteBrandingService;

    public RemoteTenantServiceImpl(SysTenantService tenantService, SysTenantPackageService packageService,
                                   SysDomainConfigService domainConfigService, SysTenantRouteService routeService,
                                   SysTenantDatasourceService datasourceService,
                                   ObjectProvider<DomainBrandingCache> cacheProvider,
                                   ObjectProvider<TenantCache> tenantCacheProvider,
                                   ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider) {
        this.tenantService = tenantService;
        this.packageService = packageService;
        this.domainConfigService = domainConfigService;
        this.routeService = routeService;
        this.datasourceService = datasourceService;
        this.cacheProvider = cacheProvider;
        this.tenantCacheProvider = tenantCacheProvider;
        this.entitlementProvider = entitlementProvider;
    }

    @Override
    public TenantDTO getTenantById(Long tenantId) {
        if (tenantId == null) {
            return toDto(tenantService.getById(null));
        }
        TenantCache tenantCache = tenantCacheProvider.getIfAvailable();
        if (tenantCache == null) {
            return toDto(tenantService.getById(tenantId));
        }
        return tenantCache.getTenantInfo(tenantId, TenantDTO.class, () -> toDto(tenantService.getById(tenantId)));
    }

    @Override
    public TenantDTO getTenantByCode(String tenantCode) {
        SysTenant tenant = tenantService.lambdaQuery()
                .eq(SysTenant::getCode, tenantCode)
                .one();
        return toDto(tenant);
    }

    @Override
    public TenantDTO getTenantByDomain(String domain) {
        return toDto(findTenantByDomain(domain));
    }

    @Override
    public TenantDTO validateTenant(Long tenantId) {
        return toDto(tenantService.validateTenant(tenantId));
    }

    /**
     * 登录页品牌解析（缓存优先）：先查租户域缓存（域名绑定租户 → 租户自助品牌 + 租户三要素，
     * service-system 启动注册/变更写穿），再查平台域缓存（平台自有 sys_domain_config），
     * 双未命中回源 DB 并回填缓存（Redis 被清后的自愈通道）。租户绑定域名恒优先于平台配置。
     */
    @Override
    public DomainConfigDTO getDomainConfig(String domain) {
        String normalized = domain == null ? null : domain.trim().toLowerCase();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        DomainBrandingCache cache = cacheProvider.getIfAvailable();
        if (cache != null) {
            try {
                DomainBrandingEntry tenantEntry = cache.getTenantDomain(normalized);
                if (tenantEntry != null) {
                    return toConfigDto(normalized, tenantEntry);
                }
                DomainBrandingEntry platformEntry = cache.getPlatformDomain(normalized);
                if (platformEntry != null) {
                    return toConfigDto(normalized, platformEntry);
                }
            } catch (Exception ex) {
                // Redis 异常降级走 DB 回源，不阻断登录页
                log.warn("[mse-branding] 域名缓存读取失败，回源 DB: {}", ex.getMessage());
            }
        }
        // ---- DB 兜底（含缓存回填）----
        // 租户域 hash 缺失（Redis 被清等）：触发 service-system 全量重建后重读，保住「租户绑定恒优先」口径
        if (cache != null && !cache.hasTenantHash()) {
            try {
                remoteBrandingService.rebuildDomainCache();
            } catch (Exception ex) {
                log.warn("[mse-branding] 租户域缓存重建触发失败: {}", ex.getMessage());
            }
            try {
                DomainBrandingEntry retry = cache.getTenantDomain(normalized);
                if (retry != null) {
                    return toConfigDto(normalized, retry);
                }
            } catch (Exception ex) {
                log.warn("[mse-branding] 租户域缓存重读失败: {}", ex.getMessage());
            }
        }
        SysTenant tenant = findTenantByDomain(normalized);
        if (tenant != null) {
            BrandDTO brand = remoteBrandingService.getByTenantId(tenant.getId());
            DomainConfigDTO dto = toConfigDto(normalized, tenant, brand);
            if (cache != null) {
                backfillTenantEntry(cache, normalized, tenant, brand);
            }
            return dto;
        }
        SysDomainConfig config = domainConfigService.getEnabledByDomain(normalized);
        if (config == null) {
            return null;
        }
        if (cache != null) {
            try {
                cache.putPlatformDomain(normalized, DomainBrandingCacheRunner.toEntry(config));
            } catch (Exception ex) {
                log.warn("[mse-branding] 平台域缓存回填失败: {}", ex.getMessage());
            }
        }
        return toConfigDto(normalized, config);
    }

    /** 租户绑定条目回填（品牌为 null 也发布条目：命中租户与品牌生效解耦，登录页免填编码仍成立） */
    private void backfillTenantEntry(DomainBrandingCache cache, String domain, SysTenant tenant, BrandDTO brand) {
        try {
            DomainBrandingEntry entry = new DomainBrandingEntry();
            entry.setTenantId(tenant.getId());
            entry.setTenantCode(tenant.getCode());
            entry.setTenantName(tenant.getName());
            if (brand != null) {
                entry.setSiteTitle(brand.getSiteTitle());
                entry.setSiteLogo(brand.getSiteLogo());
                entry.setLoginBackground(brand.getLoginBackground());
                entry.setBackgroundColor(brand.getBackgroundColor());
                entry.setCopyright(brand.getCopyright());
            }
            cache.putTenantDomain(domain, entry);
        } catch (Exception ex) {
            log.warn("[mse-branding] 租户域缓存回填失败: {}", ex.getMessage());
        }
    }

    private DomainConfigDTO toConfigDto(String domain, DomainBrandingEntry entry) {
        DomainConfigDTO dto = new DomainConfigDTO();
        dto.setDomain(domain);
        dto.setTenantId(entry.getTenantId());
        dto.setTenantCode(entry.getTenantCode());
        dto.setTenantName(entry.getTenantName());
        dto.setSiteTitle(entry.getSiteTitle());
        dto.setSiteLogo(entry.getSiteLogo());
        dto.setLoginBackground(entry.getLoginBackground());
        dto.setBackgroundColor(entry.getBackgroundColor());
        dto.setCopyright(entry.getCopyright());
        return dto;
    }

    private DomainConfigDTO toConfigDto(String domain, SysTenant tenant, BrandDTO brand) {
        DomainConfigDTO dto = new DomainConfigDTO();
        dto.setDomain(domain);
        dto.setTenantId(tenant.getId());
        dto.setTenantCode(tenant.getCode());
        dto.setTenantName(tenant.getName());
        if (brand != null) {
            dto.setSiteTitle(brand.getSiteTitle());
            dto.setSiteLogo(brand.getSiteLogo());
            dto.setLoginBackground(brand.getLoginBackground());
            dto.setBackgroundColor(brand.getBackgroundColor());
            dto.setCopyright(brand.getCopyright());
        }
        return dto;
    }

    private DomainConfigDTO toConfigDto(String domain, SysDomainConfig config) {
        DomainConfigDTO dto = new DomainConfigDTO();
        dto.setDomain(domain);
        dto.setSiteTitle(config.getSiteTitle());
        dto.setSiteLogo(config.getSiteLogo());
        dto.setLoginBackground(config.getLoginBackground());
        dto.setBackgroundColor(config.getBackgroundColor());
        dto.setCopyright(config.getCopyright());
        return dto;
    }

    private SysTenant findTenantByDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return null;
        }
        return tenantService.lambdaQuery()
                .eq(SysTenant::getDomain, domain.trim().toLowerCase())
                .one();
    }

    @Override
    public List<Long> getPackageMenuIds(Long tenantId) {
        SysTenant tenant = tenantService.getById(tenantId);
        if (tenant == null || tenant.getPackageId() == null) {
            // 未配套餐：调用方按宽松默认处理（不裁剪）
            return null;
        }
        SysTenantPackage tenantPackage = packageService.getById(tenant.getPackageId());
        if (tenantPackage == null || !StringUtils.hasText(tenantPackage.getMenuIds())) {
            return List.of();
        }
        List<Long> menuIds = new ArrayList<>();
        for (String id : tenantPackage.getMenuIds().split(",")) {
            if (StringUtils.hasText(id)) {
                menuIds.add(Long.parseLong(id.trim()));
            }
        }
        return menuIds;
    }

    @Override
    public List<TenantDTO> listTenants() {
        return tenantService.lambdaQuery()
                .orderByAsc(SysTenant::getId)
                .list()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public TenantRouteDTO getTenantRoute(Long tenantId, String serviceName) {
        TenantRoute route = routeService.resolveRoute(tenantId, serviceName);
        TenantRouteDTO dto = new TenantRouteDTO();
        dto.setTenantId(route.getTenantId());
        dto.setServiceName(route.getServiceName());
        dto.setMode(route.getMode().name());
        dto.setDsKey(route.getDsKey());
        dto.setTableSuffix(route.getTableSuffix());
        dto.setTenantColumn(route.getTenantColumn());
        dto.setTablePolicies(route.getTablePolicies().entrySet().stream()
                .map(entry -> {
                    TablePolicyDTO policy = new TablePolicyDTO();
                    policy.setTableName(entry.getKey());
                    policy.setMode(entry.getValue().getMode().name());
                    policy.setTableSuffix(entry.getValue().getTableSuffix());
                    return policy;
                })
                .toList());
        return dto;
    }

    @Override
    public TenantSnapshotDTO getTenantSnapshot(Long tenantId) {
        SysTenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            return null;
        }
        TenantSnapshotDTO snapshot = new TenantSnapshotDTO();
        snapshot.setTenantId(tenant.getId());
        snapshot.setCode(tenant.getCode());
        snapshot.setName(tenant.getName());
        snapshot.setDomain(tenant.getDomain());
        snapshot.setStatus(tenant.getStatus());
        snapshot.setDsKey(tenant.getDsKey());
        snapshot.setPackageMenuIds(getPackageMenuIds(tenantId));
        snapshot.setRoutes(routeService.listRoutes(tenantId));
        // 0.1.0 权益模型：快照携带配额/开关（数据面经 TenantSnapshotStore/EntitlementCache 本地消费）
        vip.mseapp.service.platform.service.EntitlementService entitlement = entitlementProvider.getIfAvailable();
        if (entitlement != null) {
            vip.mseapp.api.tenant.dto.EntitlementDTO computed = entitlement.compute(tenantId);
            snapshot.setQuotas(computed.getQuotas());
            snapshot.setFeatures(computed.getFeatures());
        }
        // 快照版本与控制面失效通道同源（TenantCache 租户版本号，控制面 evict 即 INCR）：
        // 数据面命中内存快照后节流比对版本号即可秒级感知路由/策略变更，不等事件到达与 TTL 过期；
        // 无 Redis（如 standalone 离线形态）回落时间戳（数据面探测不可用时跳过校验，TTL 兜底）
        Long snapshotVersion = null;
        TenantCache tenantCache = tenantCacheProvider.getIfAvailable();
        if (tenantCache != null) {
            snapshotVersion = tenantCache.readTenantVersion(tenantId);
        }
        snapshot.setSnapshotVersion(snapshotVersion != null ? snapshotVersion : System.currentTimeMillis());
        return snapshot;
    }

    @Override
    public vip.mseapp.api.tenant.dto.EntitlementDTO getTenantEntitlement(Long tenantId) {
        vip.mseapp.service.platform.service.EntitlementService entitlement = entitlementProvider.getIfAvailable();
        return entitlement == null ? new vip.mseapp.api.tenant.dto.EntitlementDTO()
                : entitlement.compute(tenantId);
    }

    @Override
    public List<TenantDatasourceDTO> listTenantDatasources(String serviceName) {
        return datasourceService.lambdaQuery()
                .and(wrapper -> wrapper.eq(SysTenantDatasource::getServiceName, serviceName)
                        .or().eq(SysTenantDatasource::getServiceName, "*"))
                .orderByDesc(SysTenantDatasource::getId)
                .list()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public TenantDatasourceDTO getTenantDatasource(String dsKey) {
        if (!StringUtils.hasText(dsKey)) {
            return null;
        }
        return toDto(datasourceService.lambdaQuery()
                .eq(SysTenantDatasource::getDsKey, dsKey.trim())
                .one());
    }

    private TenantDatasourceDTO toDto(SysTenantDatasource ds) {
        if (ds == null) {
            return null;
        }
        TenantDatasourceDTO dto = new TenantDatasourceDTO();
        dto.setId(ds.getId());
        dto.setDsKey(ds.getDsKey());
        dto.setDsMode(ds.getDsMode());
        dto.setServiceName(ds.getServiceName());
        dto.setDbType(ds.getDbType());
        dto.setHost(ds.getHost());
        dto.setPort(ds.getPort());
        dto.setDbName(ds.getDbName());
        dto.setParams(ds.getParams());
        dto.setUsername(ds.getUsername());
        // 密文传输，调用方以共享 encrypt-key 解密
        dto.setPassword(ds.getPassword());
        dto.setStatus(ds.getStatus());
        return dto;
    }

    private TenantDTO toDto(SysTenant tenant) {
        if (tenant == null) {
            return null;
        }
        TenantDTO dto = new TenantDTO();
        dto.setId(tenant.getId());
        dto.setCode(tenant.getCode());
        dto.setName(tenant.getName());
        dto.setPackageId(tenant.getPackageId());
        dto.setDsKey(tenant.getDsKey());
        dto.setDomain(tenant.getDomain());
        dto.setStatus(tenant.getStatus());
        dto.setExpireTime(tenant.getExpireTime());
        return dto;
    }
}
