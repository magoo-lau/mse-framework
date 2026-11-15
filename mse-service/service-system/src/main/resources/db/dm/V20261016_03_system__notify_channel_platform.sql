-- mse-target: platform
-- mse-base-scope: platform
-- =====================================================================
-- 框架通知中枢：渠道登记目录表（达梦 DM8；登记源 tenant_id=0 行经 runIgnore 写入，
-- master 与 base+platform 双通道覆盖落点——两份内容同构）
-- 幂等：建表 IF NOT EXISTS，可重复执行
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_notify_channel
(
    id               BIGINT       NOT NULL,
    tenant_id        BIGINT       NOT NULL DEFAULT 0,
    channel_code     VARCHAR(32)  NOT NULL,
    channel_name     VARCHAR(64)  NOT NULL,
    description      VARCHAR(255) NULL,
    auth_required    TINYINT      NOT NULL DEFAULT 0,
    entitlement_code VARCHAR(64)  NULL,
    provider_group   VARCHAR(64)  NOT NULL,
    last_push_time   TIMESTAMP    NULL,
    create_by        BIGINT       NULL,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT       NULL,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT ux_notify_channel_code UNIQUE (channel_code, deleted)
);
COMMENT ON TABLE sys_notify_channel IS '通知渠道登记目录（提供方自注册全局目录）';
COMMENT ON COLUMN sys_notify_channel.channel_code IS '渠道编码（全局唯一；inbox 为框架内置）';
COMMENT ON COLUMN sys_notify_channel.channel_name IS '展示名';
COMMENT ON COLUMN sys_notify_channel.description IS '渠道说明';
COMMENT ON COLUMN sys_notify_channel.auth_required IS '是否需租户授权（1=按权益开关开通后可用）';
COMMENT ON COLUMN sys_notify_channel.entitlement_code IS '权益开关编码（auth_required=1 时有效，feature.* 项）';
COMMENT ON COLUMN sys_notify_channel.provider_group IS '提供方服务名（dubbo group；本地 Bean 为 local）';
COMMENT ON COLUMN sys_notify_channel.last_push_time IS '最近登记变更时间';
COMMENT ON COLUMN sys_notify_channel.tenant_id IS '租户编号（登记源恒 0）';
COMMENT ON COLUMN sys_notify_channel.deleted IS '逻辑删除（0 未删除 1 已删除）';
