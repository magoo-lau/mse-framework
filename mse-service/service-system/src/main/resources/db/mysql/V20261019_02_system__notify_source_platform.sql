-- mse-target: base
-- mse-base-scope: platform
SET NAMES utf8mb4;
-- =====================================================================
-- 框架通知中枢：业务来源登记目录表平台库副本（MySQL；sys_domain_registry 同款 base+platform 通道）
-- 说明：登记接收经 runIgnore 写入的落库目标之一，与 V20261019_01（master 通道）同构双份；
--       同步补渠道目录平台库副本的接收人语义列
-- ALTER 无条件幂等写法，单次执行由平台库 base 分册账本保证
-- 幂等：建表 IF NOT EXISTS，可重复执行
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

ALTER TABLE sys_notify_channel
    ADD COLUMN receiver_required TINYINT NULL COMMENT '是否需个人接收人（NULL/1=按 receiverIds 投递；0=群发渠道如 Webhook，无接收人也投递）' AFTER entitlement_code;
