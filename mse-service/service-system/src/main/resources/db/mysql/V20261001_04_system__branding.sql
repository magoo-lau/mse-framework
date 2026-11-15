-- mse-target: master
-- =====================================================================
-- 租户品牌个性化 + 租户品牌域名绑定（service-system，MySQL）
-- 目标库：业务主库 + 各租户独立库（master 通道随服务启动自动应用；租户库经初始化/懒初始化就绪）
-- 配套种子：页面菜单 171 与权限点 2001-2002 在 V20261001_02_system__tenant_data.sql（base 通道）
--
-- 品牌双轨：本表为租户自助品牌；平台自有登录页品牌仍走平台库 sys_domain_config（service-platform），
--           登录页解析顺序 = 域名绑定租户的本表行 → 平台 sys_domain_config 行 → 默认样式
-- 域名绑定：租户自助域名（sys_tenant_branding_domain，domain 全局唯一——共享主库由索引硬保证，
--           独立库形态各库自持、跨库同名以先注册者为准）与控制面遗留的 sys_tenant.domain
--           都归入登录页「租户域」缓存，命中即呈现租户自助品牌并免填租户编码
-- 幂等：CREATE TABLE IF NOT EXISTS
-- =====================================================================

-- ---------------- 租户品牌个性化 ----------------
CREATE TABLE IF NOT EXISTS sys_tenant_branding
(
    id                BIGINT       NOT NULL COMMENT '主键ID（雪花）',
    tenant_id         BIGINT       NOT NULL COMMENT '租户编号（0=平台世界）',
    site_title        VARCHAR(60)  NULL COMMENT '登录页标题（空=前端默认标题）',
    site_logo         VARCHAR(500) NULL COMMENT 'Logo 图片地址（http(s):// 或 / 开头路径；空=不显示）',
    login_background  VARCHAR(500) NULL COMMENT '登录背景图地址（空=背景色/默认渐变）',
    background_color  VARCHAR(200) NULL COMMENT '背景色或渐变（CSS background 值；背景图存在时垫底）',
    copyright         VARCHAR(200) NULL COMMENT '版权文案（登录页底部；空=不显示）',
    create_by         BIGINT       NULL COMMENT '创建者',
    create_time       DATETIME     NULL COMMENT '创建时间',
    update_by         BIGINT       NULL COMMENT '更新者',
    update_time       DATETIME     NULL COMMENT '更新时间',
    deleted           TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除（0 未删除 1 已删除）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant (tenant_id)
) ENGINE = InnoDB COMMENT ='租户品牌个性化（租户自助定制登录页样式，每租户一行）';

-- ---------------- 租户品牌域名绑定 ----------------
CREATE TABLE IF NOT EXISTS sys_tenant_branding_domain
(
    id          BIGINT       NOT NULL COMMENT '主键ID（雪花）',
    tenant_id   BIGINT       NOT NULL COMMENT '租户编号（绑定归属）',
    domain      VARCHAR(128) NOT NULL COMMENT '域名（完整主机名，小写，全局唯一）',
    create_time DATETIME     NULL COMMENT '创建时间',
    update_time DATETIME     NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_domain (domain)
) ENGINE = InnoDB COMMENT ='租户品牌域名绑定（租户自助绑定站点域名，登录页按域名命中本租户品牌）';
