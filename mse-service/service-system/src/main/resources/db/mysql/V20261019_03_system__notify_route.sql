-- mse-target: tenant
SET NAMES utf8mb4;
-- =====================================================================
-- 框架通知中枢：通知路由配置表（MySQL；与 sys_message 同 TENANT 通道——租户库/共享库脚本直建）
-- 内容：sys_notify_route（业务动作 → 投递渠道，租户隔离的框架级配置；
--       解析链 (biz,type) 精确 → (biz,*) → (*,*) → 兜底 [inbox]）
-- 自业务层上收（v3）：业务方不再自持路由表，发送不携带渠道时由中枢按本表解析
-- 配套：V20261019_04（系统管理「通知渠道设置」页面菜单/权限点）
-- 幂等：建表 IF NOT EXISTS，可重复执行
-- =====================================================================

CREATE TABLE IF NOT EXISTS sys_notify_route
(
    id          BIGINT      NOT NULL COMMENT '主键ID',
    tenant_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '租户编号',
    biz_code    VARCHAR(32) NOT NULL DEFAULT '*' COMMENT '业务来源码（已登记来源或 * 通配）',
    msg_type    VARCHAR(32) NOT NULL DEFAULT '*' COMMENT '消息类型（来源内动作语义或 * 通配）',
    channels    VARCHAR(128) NULL COMMENT '投递渠道 CSV（渠道编码逗号分隔，如 inbox,im）',
    status      TINYINT     NOT NULL DEFAULT 0 COMMENT '状态（0 启用 1 停用；停用不参与解析）',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_by   BIGINT      NULL COMMENT '创建者',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT      NULL COMMENT '更新者',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除（0 未删除 1 已删除）',
    PRIMARY KEY (id),
    UNIQUE KEY ux_notify_route (tenant_id, biz_code, msg_type, deleted)
) ENGINE = InnoDB COMMENT ='通知路由配置（业务动作到投递渠道，租户隔离）';
