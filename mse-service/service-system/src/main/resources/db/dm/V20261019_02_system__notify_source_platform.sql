-- mse-target: base
-- mse-base-scope: platform

-- =====================================================================
-- 框架通知中枢：业务来源登记目录表平台库副本（达梦 DM8；sys_domain_registry 同款 base+platform 通道）
-- 说明：登记接收经 runIgnore 写入的落库目标之一，与 V20261019_01（master 通道）同构双份；
--       同步补渠道目录平台库副本的接收人语义列
-- ALTER 无条件幂等写法，单次执行由平台库 base 分册账本保证
-- 幂等：建表 IF NOT EXISTS，可重复执行
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_notify_source
(
    id             BIGINT        NOT NULL,
    tenant_id      BIGINT        NOT NULL DEFAULT 0,
    biz_code       VARCHAR(32)   NOT NULL,
    biz_name       VARCHAR(64)   NOT NULL,
    types          VARCHAR(1000) NULL,
    provider_group VARCHAR(64)   NOT NULL,
    last_push_time TIMESTAMP     NULL,
    create_by      BIGINT        NULL,
    create_time    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT        NULL,
    update_time    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT ux_notify_source_biz UNIQUE (biz_code, deleted)
);
COMMENT ON TABLE sys_notify_source IS '通知业务来源登记目录（业务积木自注册全局目录）';
COMMENT ON COLUMN sys_notify_source.biz_code IS '业务来源码（全局唯一，如 oa/monitor）';
COMMENT ON COLUMN sys_notify_source.biz_name IS '展示名（如「协同办公」）';
COMMENT ON COLUMN sys_notify_source.types IS '消息类型清单 JSON（[{code,name}]；空=未声明类型仅通配可用）';
COMMENT ON COLUMN sys_notify_source.provider_group IS '提供方服务名（dubbo group；本地 Bean 为 local）';
COMMENT ON COLUMN sys_notify_source.last_push_time IS '最近登记变更时间';
COMMENT ON COLUMN sys_notify_source.tenant_id IS '租户编号（登记源恒 0）';
COMMENT ON COLUMN sys_notify_source.deleted IS '逻辑删除（0 未删除 1 已删除）';

ALTER TABLE sys_notify_channel ADD COLUMN receiver_required TINYINT NULL;
COMMENT ON COLUMN sys_notify_channel.receiver_required IS '是否需个人接收人（NULL/1=按 receiverIds 投递；0=群发渠道如 Webhook，无接收人也投递）';
