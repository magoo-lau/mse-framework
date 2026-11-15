-- mse-target: master
SET NAMES utf8mb4;
-- =====================================================================
-- 框架通知中枢：业务来源登记目录表 + 渠道目录接收人语义列（MySQL，master 通道——
-- 业务主库+各租户独立库；登记源 tenant_id=0 行经 runIgnore 写入，
-- 与 V20261019_02 平台库副本双通道覆盖落点）
-- 内容：sys_notify_source（业务积木自注册的来源/消息类型目录，dubbo 推送幂等 upsert，
--       同码不同提供组跳过防覆盖）
--       sys_notify_channel.receiver_required（群发渠道语义：NULL/1=按接收人投递，0=Webhook 等无接收人也投递）
-- 幂等：建表 IF NOT EXISTS；加列经 information_schema 探针 + PREPARE 动态执行（MySQL 无
--       ADD COLUMN IF NOT EXISTS 语法），列已存在降级 SELECT 1——结构先行/账本缺席/
--       半途失败重放均自愈，无需重放前手工核对列状态
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_notify_source
(
    id             BIGINT        NOT NULL COMMENT '主键ID',
    tenant_id      BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号（登记源恒 0）',
    biz_code       VARCHAR(32)   NOT NULL COMMENT '业务来源码（全局唯一，如 oa/monitor）',
    biz_name       VARCHAR(64)   NOT NULL COMMENT '展示名（如「协同办公」）',
    types          VARCHAR(1000) NULL COMMENT '消息类型清单 JSON（[{code,name}]；空=未声明类型仅通配可用）',
    provider_group VARCHAR(64)   NOT NULL COMMENT '提供方服务名（dubbo group；本地 Bean 为 local）',
    last_push_time DATETIME      NULL COMMENT '最近登记变更时间',
    create_by      BIGINT        NULL COMMENT '创建者',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by      BIGINT        NULL COMMENT '更新者',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除（0 未删除 1 已删除）',
    PRIMARY KEY (id),
    UNIQUE KEY ux_notify_source_biz (biz_code, deleted)
) ENGINE = InnoDB COMMENT ='通知业务来源登记目录（业务积木自注册全局目录）';

SET @mse_notify_col = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notify_channel'
       AND COLUMN_NAME = 'receiver_required') = 0,
    'ALTER TABLE sys_notify_channel ADD COLUMN receiver_required TINYINT NULL COMMENT ''是否需个人接收人（NULL/1=按 receiverIds 投递；0=群发渠道如 Webhook，无接收人也投递）'' AFTER entitlement_code',
    'SELECT 1');
PREPARE mse_notify_col_stmt FROM @mse_notify_col;
EXECUTE mse_notify_col_stmt;
DEALLOCATE PREPARE mse_notify_col_stmt;
