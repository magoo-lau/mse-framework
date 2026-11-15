-- mse-target: tenant

-- =====================================================================
-- 框架通知中枢：通知路由配置表（达梦 DM8；与 sys_message 同 TENANT 通道——租户库/共享库脚本直建）
-- 内容：sys_notify_route（业务动作 → 投递渠道，租户隔离的框架级配置；
--       解析链 (biz,type) 精确 → (biz,*) → (*,*) → 兜底 [inbox]）
-- 配套：V20261019_04（系统管理「通知渠道设置」页面菜单/权限点）
-- 幂等：建表 IF NOT EXISTS，可重复执行
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_notify_route
(
    id          BIGINT       NOT NULL,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    biz_code    VARCHAR(32)  NOT NULL DEFAULT '*',
    msg_type    VARCHAR(32)  NOT NULL DEFAULT '*',
    channels    VARCHAR(128) NULL,
    status      TINYINT      NOT NULL DEFAULT 0,
    remark      VARCHAR(255) NULL,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT ux_notify_route UNIQUE (tenant_id, biz_code, msg_type, deleted)
);
COMMENT ON TABLE sys_notify_route IS '通知路由配置（业务动作到投递渠道，租户隔离）';
COMMENT ON COLUMN sys_notify_route.biz_code IS '业务来源码（已登记来源或 * 通配）';
COMMENT ON COLUMN sys_notify_route.msg_type IS '消息类型（来源内动作语义或 * 通配）';
COMMENT ON COLUMN sys_notify_route.channels IS '投递渠道 CSV（渠道编码逗号分隔，如 inbox,im）';
COMMENT ON COLUMN sys_notify_route.status IS '状态（0 启用 1 停用；停用不参与解析）';
COMMENT ON COLUMN sys_notify_route.remark IS '备注';
COMMENT ON COLUMN sys_notify_route.tenant_id IS '租户编号';
COMMENT ON COLUMN sys_notify_route.deleted IS '逻辑删除（0 未删除 1 已删除）';
