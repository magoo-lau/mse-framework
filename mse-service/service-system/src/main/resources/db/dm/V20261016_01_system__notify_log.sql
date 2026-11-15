-- mse-target: tenant
-- =====================================================================
-- 框架通知中枢：投递日志表（达梦 DM8；与 sys_message 同 TENANT 通道——租户库/共享库脚本直建）
-- 内容：sys_notify_log（一次派发一行：计划渠道与逐渠道结果，租户隔离，只写不改）
-- 幂等：建表 IF NOT EXISTS，可重复执行
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_notify_log
(
    id             BIGINT       NOT NULL,
    tenant_id      BIGINT       NOT NULL DEFAULT 0,
    biz_code       VARCHAR(32)  NULL,
    msg_type       VARCHAR(32)  NULL,
    title          VARCHAR(255) NULL,
    receiver_count INT          NULL,
    channels       VARCHAR(128) NULL,
    ok_channels    VARCHAR(128) NULL,
    fail_channels  VARCHAR(128) NULL,
    error          VARCHAR(500) NULL,
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_notify_log_tenant ON sys_notify_log (tenant_id, id);
COMMENT ON TABLE sys_notify_log IS '通知投递日志（框架消息中枢派发留痕）';
COMMENT ON COLUMN sys_notify_log.biz_code IS '业务来源码（如 oa/mes）';
COMMENT ON COLUMN sys_notify_log.msg_type IS '消息类型（业务自定义动作语义）';
COMMENT ON COLUMN sys_notify_log.title IS '标题（快照）';
COMMENT ON COLUMN sys_notify_log.receiver_count IS '接收人数';
COMMENT ON COLUMN sys_notify_log.channels IS '计划渠道 CSV';
COMMENT ON COLUMN sys_notify_log.ok_channels IS '成功渠道 CSV';
COMMENT ON COLUMN sys_notify_log.fail_channels IS '失败渠道 CSV';
COMMENT ON COLUMN sys_notify_log.error IS '失败原因摘要（截断500）';
COMMENT ON COLUMN sys_notify_log.tenant_id IS '租户编号';
COMMENT ON COLUMN sys_notify_log.create_time IS '创建时间';
COMMENT ON COLUMN sys_notify_log.deleted IS '逻辑删除（0 未删除 1 已删除）';
