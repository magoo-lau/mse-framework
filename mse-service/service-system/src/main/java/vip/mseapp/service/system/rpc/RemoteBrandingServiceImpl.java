package vip.mseapp.service.system.rpc;

import org.springframework.beans.factory.ObjectProvider;
import vip.mseapp.api.system.RemoteBrandingService;
import vip.mseapp.api.system.dto.BrandDTO;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.service.system.entity.SysTenantBranding;
import vip.mseapp.service.system.service.SysTenantBrandingService;
import vip.mseapp.service.system.service.TenantBrandingCachePublisher;
/**
 * 租户品牌个性化查询契约实现：runAs 指定租户语义读取 sys_tenant_branding（租户世界数据）。
 *
 * <p>登录页解析链路（认证服务无租户上下文）：host → 域名缓存（本服务启动注册/写穿）→ 本契约
 * 兜底重建。runAs 使查询路由到该租户绑定的数据源（共享主库或独立库），租户未配置过返回 null。
 */
@RpcService(RemoteBrandingService.class)
public class RemoteBrandingServiceImpl implements RemoteBrandingService {

    private final SysTenantBrandingService brandingService;
    private final ObjectProvider<TenantBrandingCachePublisher> publisherProvider;

    public RemoteBrandingServiceImpl(SysTenantBrandingService brandingService,
                                     ObjectProvider<TenantBrandingCachePublisher> publisherProvider) {
        this.brandingService = brandingService;
        this.publisherProvider = publisherProvider;
    }

    @Override
    public BrandDTO getByTenantId(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            // 平台世界（0）品牌走 service-platform 的 sys_domain_config，不经本契约
            return null;
        }
        return TenantContext.runAs(tenantId, () -> toDto(brandingService.getByTenantId(tenantId)));
    }

    @Override
    public void rebuildDomainCache() {
        TenantBrandingCachePublisher publisher = publisherProvider.getIfAvailable();
        if (publisher != null) {
            publisher.rebuildAll();
        }
    }

    private BrandDTO toDto(SysTenantBranding branding) {
        if (branding == null) {
            return null;
        }
        BrandDTO dto = new BrandDTO();
        dto.setSiteTitle(branding.getSiteTitle());
        dto.setSiteLogo(branding.getSiteLogo());
        dto.setLoginBackground(branding.getLoginBackground());
        dto.setBackgroundColor(branding.getBackgroundColor());
        dto.setCopyright(branding.getCopyright());
        return dto;
    }
}
