-- mse-target: master
SET NAMES utf8mb4;
-- =====================================================================
-- 框架通知中枢：渠道登记目录表（MySQL，master 通道——业务主库+各租户独立库；
-- 登记源 tenant_id=0 行经 runIgnore 写入，与 V20261016_03 平台库副本双通道覆盖落点）
-- 内容：sys_notify_channel（渠道提供方自注册全局目录，dubbo 推送幂等 upsert，
--       同码不同提供组跳过防覆盖；inbox 为框架内置无登记行）
-- 幂等：建表 IF NOT EXISTS，可重复执行
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_notify_channel
(
    id               BIGINT       NOT NULL COMMENT '主键ID',
    tenant_id        BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（登记源恒 0）',
    channel_code     VARCHAR(32)  NOT NULL COMMENT '渠道编码（全局唯一；inbox 为框架内置）',
    channel_name     VARCHAR(64)  NOT NULL COMMENT '展示名',
    description      VARCHAR(255) NULL COMMENT '渠道说明',
    auth_required    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否需租户授权（1=按权益开关开通后可用）',
    entitlement_code VARCHAR(64)  NULL COMMENT '权益开关编码（auth_required=1 时有效，feature.* 项）',
    provider_group   VARCHAR(64)  NOT NULL COMMENT '提供方服务名（dubbo group；本地 Bean 为 local）',
    last_push_time   DATETIME     NULL COMMENT '最近登记变更时间',
    create_by        BIGINT       NULL COMMENT '创建者',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by        BIGINT       NULL COMMENT '更新者',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除（0 未删除 1 已删除）',
    PRIMARY KEY (id),
    UNIQUE KEY ux_notify_channel_code (channel_code, deleted)
) ENGINE = InnoDB COMMENT ='通知渠道登记目录（提供方自注册全局目录）';
