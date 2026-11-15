-- mse-target: tenant
SET NAMES utf8mb4;
-- =====================================================================
-- 框架通知中枢：Webhook 渠道租户级配置表（MySQL；与 sys_notify_route 同 TENANT 通道——
-- 租户库/共享库脚本直建；平台库副本见 V20261020_02）
-- 内容：sys_notify_webhook（内置 Webhook 群发渠道的地址与报文格式，每世界一行，
--       本租户全部勾选 webhook 的路由共用；地址清空即渠道不可用）
-- 背景：webhook 渠道自 mse-monitor 业务来源收编为框架内置（v3.1），配置随渠道上收
-- 幂等：建表 IF NOT EXISTS，可重复执行
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
