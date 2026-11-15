-- mse-target: base
-- mse-base-scope: platform
SET NAMES utf8mb4;
-- =====================================================================
-- 框架通知中枢：Webhook 渠道配置表平台库副本 + monitor 业务来源退役清理（MySQL，platform 通道）
-- 内容：sys_notify_webhook 平台库建表（与 V20261020_01 租户通道份同构，两库两份维护）
--       + 清理 monitor 退出中枢的存量登记（v3.1 恢复独立直发）：
--         · sys_notify_channel 的 webhook 登记行（mse-monitor 提供，内置渠道接管，物理删防墓碑撞键）
--         · sys_notify_source 的 monitor 来源行（业务来源下拉不再出现）
--         · sys_notify_route 平台域种子路由 (monitor,*)（V20261019_05 播种，告警不再走中枢）
-- 幂等：建表 IF NOT EXISTS + DELETE 天然幂等，可重复执行
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_notify_webhook
(
    id             BIGINT       NOT NULL COMMENT '主键ID',
    tenant_id      BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（每世界一行）',
    webhook_url    VARCHAR(512) NULL COMMENT '群机器人 webhook 地址（钉钉/企微/飞书；清空=停用渠道）',
    webhook_format VARCHAR(16)  NOT NULL DEFAULT 'dingtalk' COMMENT '报文格式（dingtalk/wecom/feishu）',
    create_by      BIGINT       NULL COMMENT '创建者',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by      BIGINT       NULL COMMENT '更新者',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除（0 未删除 1 已删除）',
    PRIMARY KEY (id),
    UNIQUE KEY ux_notify_webhook (tenant_id, deleted)
) ENGINE = InnoDB COMMENT ='Webhook 渠道配置（内置群发渠道，租户级，每世界一行）';

DELETE FROM sys_notify_channel WHERE channel_code = 'webhook';
DELETE FROM sys_notify_source WHERE biz_code = 'monitor';
DELETE FROM sys_notify_route WHERE tenant_id = 0 AND biz_code = 'monitor';
