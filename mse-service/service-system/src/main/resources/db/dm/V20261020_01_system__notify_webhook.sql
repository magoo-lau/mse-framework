-- mse-target: tenant

-- =====================================================================
-- 框架通知中枢：Webhook 渠道租户级配置表（达梦 DM8；与 sys_notify_route 同 TENANT 通道——
-- 租户库/共享库脚本直建；平台库副本见 V20261020_02）
-- 内容：sys_notify_webhook（内置 Webhook 群发渠道的地址与报文格式，每世界一行，
--       本租户全部勾选 webhook 的路由共用；地址清空即渠道不可用）
-- 背景：webhook 渠道自 mse-monitor 业务来源收编为框架内置（v3.1），配置随渠道上收
-- 幂等：建表 IF NOT EXISTS，可重复执行
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_notify_webhook
(
    id             BIGINT       NOT NULL,
    tenant_id      BIGINT       NOT NULL DEFAULT 0,
    webhook_url    VARCHAR(512) NULL,
    webhook_format VARCHAR(16)  NOT NULL DEFAULT 'dingtalk',
    create_by      BIGINT       NULL,
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT       NULL,
    update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT ux_notify_webhook UNIQUE (tenant_id, deleted)
);
COMMENT ON TABLE sys_notify_webhook IS 'Webhook 渠道配置（内置群发渠道，租户级，每世界一行）';
COMMENT ON COLUMN sys_notify_webhook.tenant_id IS '租户编号（每世界一行）';
COMMENT ON COLUMN sys_notify_webhook.webhook_url IS '群机器人 webhook 地址（钉钉/企微/飞书；清空=停用渠道）';
COMMENT ON COLUMN sys_notify_webhook.webhook_format IS '报文格式（dingtalk/wecom/feishu）';
COMMENT ON COLUMN sys_notify_webhook.deleted IS '逻辑删除（0 未删除 1 已删除）';
