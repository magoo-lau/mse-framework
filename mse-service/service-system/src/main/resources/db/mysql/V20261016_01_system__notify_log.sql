-- mse-target: tenant
SET NAMES utf8mb4;
-- =====================================================================
-- 框架通知中枢：投递日志表（MySQL；与 sys_message 同 TENANT 通道——租户库/共享库脚本直建）
-- 内容：sys_notify_log（一次派发一行：计划渠道与逐渠道结果，租户隔离，只写不改）
-- 配套：V20261016_02/03（渠道登记目录表 master+平台库双份）、V20261016_04（投递日志页面菜单/权限点）
-- 幂等：建表 IF NOT EXISTS，可重复执行
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_notify_log
(
    id             BIGINT       NOT NULL COMMENT '主键ID',
    tenant_id      BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
    biz_code       VARCHAR(32)  NULL COMMENT '业务来源码（如 oa/mes）',
    msg_type       VARCHAR(32)  NULL COMMENT '消息类型（业务自定义动作语义）',
    title          VARCHAR(255) NULL COMMENT '标题（快照）',
    receiver_count INT          NULL COMMENT '接收人数',
    channels       VARCHAR(128) NULL COMMENT '计划渠道 CSV',
    ok_channels    VARCHAR(128) NULL COMMENT '成功渠道 CSV',
    fail_channels  VARCHAR(128) NULL COMMENT '失败渠道 CSV',
    error          VARCHAR(500) NULL COMMENT '失败原因摘要（截断500）',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除（0 未删除 1 已删除）',
    PRIMARY KEY (id),
    KEY idx_notify_log_tenant (tenant_id, id)
) ENGINE = InnoDB COMMENT ='通知投递日志（框架消息中枢派发留痕）';
