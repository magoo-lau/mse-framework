package vip.mseapp.api.system;

import vip.mseapp.api.system.dto.BrandDTO;

/**
 * 租户品牌个性化查询契约（service-system 实现）：登录页按域名解析出租户后，
 * 经此读取该租户自助配置的品牌（sys_tenant_branding，租户世界数据）。
 * 实现内部 runAs 指定租户建上下文（路由到租户绑定库），认证链路无租户上下文也安全。
 */
public interface RemoteBrandingService {

    /**
     * 按租户读取品牌个性化配置。
     *
     * @param tenantId 租户编号（须 &gt; 0；0=平台世界走 sys_domain_config，不经本契约）
     * @return 品牌配置；租户未配置过返回 null（登录页回退默认样式）
     */
    BrandDTO getByTenantId(Long tenantId);

    /**
     * 触发租户域品牌缓存全量重建（幂等，多实例分布式锁防重做）。
     * 读取侧发现租户域 hash 缺失（Redis 被清）时调用，恢复「租户绑定域名恒优先」的解析口径。
     */
    void rebuildDomainCache();
}
