-- mse-target: master
-- =====================================================================
-- 租户品牌个性化 + 租户品牌域名绑定（service-system，人大金仓）
-- 目标库：业务主库 + 各租户独立库（master 通道随服务启动自动应用；租户库经初始化/懒初始化就绪）
-- 配套种子：页面菜单 171 与权限点 2001-2002 在 V20261001_02_system__tenant_data.sql（base 通道）
--
-- 品牌双轨：本表为租户自助品牌；平台自有登录页品牌仍走平台库 sys_domain_config（service-platform），
--           登录页解析顺序 = 域名绑定租户的本表行 → 平台 sys_domain_config 行 → 默认样式
-- 域名绑定：租户自助域名（sys_tenant_branding_domain，domain 全局唯一——无逻辑删除列，物理删除不占唯一键）
--           与控制面遗留的 sys_tenant.domain 都归入登录页「租户域」缓存，命中即呈现租户自助品牌并免填租户编码
-- 幂等：IF NOT EXISTS
-- =====================================================================

-- ---------------- 租户品牌个性化 ----------------
CREATE TABLE IF NOT EXISTS sys_tenant_branding
(
    id                BIGINT       NOT NULL,
    tenant_id         BIGINT       NOT NULL,
    site_title        VARCHAR(60)  NULL,
    site_logo         VARCHAR(500) NULL,
    login_background  VARCHAR(500) NULL,
    background_color  VARCHAR(200) NULL,
    copyright         VARCHAR(200) NULL,
    create_by         BIGINT       NULL,
    create_time       TIMESTAMP    NULL,
    update_by         BIGINT       NULL,
    update_time       TIMESTAMP    NULL,
    deleted           SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant UNIQUE (tenant_id)
);
COMMENT ON TABLE sys_tenant_branding IS '租户品牌个性化（租户自助定制登录页样式，每租户一行）';
COMMENT ON COLUMN sys_tenant_branding.tenant_id IS '租户编号（0=平台世界）';
COMMENT ON COLUMN sys_tenant_branding.site_title IS '登录页标题（空=前端默认标题）';
COMMENT ON COLUMN sys_tenant_branding.site_logo IS 'Logo 图片地址（http(s):// 或 / 开头路径；空=不显示）';
COMMENT ON COLUMN sys_tenant_branding.login_background IS '登录背景图地址（空=背景色/默认渐变）';
COMMENT ON COLUMN sys_tenant_branding.background_color IS '背景色或渐变（CSS background 值；背景图存在时垫底）';
COMMENT ON COLUMN sys_tenant_branding.copyright IS '版权文案（登录页底部；空=不显示）';
COMMENT ON COLUMN sys_tenant_branding.create_by IS '创建者';
COMMENT ON COLUMN sys_tenant_branding.update_by IS '更新者';
COMMENT ON COLUMN sys_tenant_branding.deleted IS '逻辑删除（0 未删除 1 已删除）';

-- ---------------- 租户品牌域名绑定 ----------------
CREATE TABLE IF NOT EXISTS sys_tenant_branding_domain
(
    id          BIGINT       NOT NULL,
    tenant_id   BIGINT       NOT NULL,
    domain      VARCHAR(128) NOT NULL,
    create_time TIMESTAMP    NULL,
    update_time TIMESTAMP    NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_domain UNIQUE (domain)
);
COMMENT ON TABLE sys_tenant_branding_domain IS '租户品牌域名绑定（租户自助绑定站点域名，登录页按域名命中本租户品牌）';
COMMENT ON COLUMN sys_tenant_branding_domain.tenant_id IS '租户编号（绑定归属）';
COMMENT ON COLUMN sys_tenant_branding_domain.domain IS '域名（完整主机名，小写，全局唯一）';
