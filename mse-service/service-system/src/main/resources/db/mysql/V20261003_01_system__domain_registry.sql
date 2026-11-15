-- mse-target: base
-- mse-base-scope: platform
-- 品牌域名全局注册表（平台库）：独立库租户的品牌域名行互不可见，跨租户「一个域名只归一个世界」
-- 判重以本表为全局锚点（自助绑定 sys_tenant_branding_domain 与控制面 sys_tenant.domain 两路来源）。
-- 幂等建表；存量绑定经启动缓存重建通道回填（TenantBrandingCachePublisher.rebuildAll）。
CREATE TABLE IF NOT EXISTS sys_domain_registry (
    id BIGINT NOT NULL COMMENT '主键（雪花）',
    domain VARCHAR(120) NOT NULL COMMENT '规范化域名（trim+小写）',
    tenant_id BIGINT NOT NULL COMMENT '属主租户编号（0=平台）',
    source TINYINT NOT NULL DEFAULT 2 COMMENT '来源：1=控制面 sys_tenant.domain 2=租户自助绑定',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登记时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_domain (domain)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '品牌域名全局注册表（平台库，跨库判重锚点）';
