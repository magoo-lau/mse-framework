-- mse-target: base
-- mse-base-scope: platform

-- =====================================================================
-- 框架通知中枢：Webhook 渠道配置表平台库副本 + monitor 业务来源退役清理（人大金仓，platform 通道）
-- 内容：sys_notify_webhook 平台库建表（与 V20261020_01 租户通道份同构，两库两份维护）
--       + 清理 monitor 退出中枢的存量登记（v3.1 恢复独立直发）：
--         · sys_notify_channel 的 webhook 登记行（mse-monitor 提供，内置渠道接管，物理删防墓碑撞键）
--         · sys_notify_source 的 monitor 来源行（业务来源下拉不再出现）
--         · sys_notify_route 平台域种子路由 (monitor,*)（V20261019_05 播种，告警不再走中枢）
-- 幂等：建表 IF NOT EXISTS + DELETE 天然幂等，可重复执行
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
    deleted        SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT ux_notify_webhook UNIQUE (tenant_id, deleted)
);
COMMENT ON TABLE sys_notify_webhook IS 'Webhook 渠道配置（内置群发渠道，租户级，每世界一行）';
COMMENT ON COLUMN sys_notify_webhook.tenant_id IS '租户编号（每世界一行）';
COMMENT ON COLUMN sys_notify_webhook.webhook_url IS '群机器人 webhook 地址（钉钉/企微/飞书；清空=停用渠道）';
COMMENT ON COLUMN sys_notify_webhook.webhook_format IS '报文格式（dingtalk/wecom/feishu）';
COMMENT ON COLUMN sys_notify_webhook.deleted IS '逻辑删除（0 未删除 1 已删除）';

DELETE FROM sys_notify_channel WHERE channel_code = 'webhook';
DELETE FROM sys_notify_source WHERE biz_code = 'monitor';
DELETE FROM sys_notify_route WHERE tenant_id = 0 AND biz_code = 'monitor';
