-- mse-target: base
-- mse-base-scope: platform
SET NAMES utf8mb4;
-- =====================================================================
-- 框架通知中枢：通知路由配置表平台库副本 + monitor 平台域默认路由（MySQL；
-- sys_message 平台库份同款 platform 通道——平台世界（tenant 0）告警等通知的路由解析落平台库）
-- 内容：sys_notify_route 平台库建表（与 V20261019_03 租户通道份同构，两库两份维护）
--       + 平台域默认路由 (monitor,*) → inbox,webhook（monitor 平台端告警升级行为等价）
-- 幂等：建表 IF NOT EXISTS + 种子 NOT EXISTS 判重，可重复执行
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

-- 平台域默认路由（monitor 平台端告警：站内 + Webhook，与迁移前 mon_setting 开关行为对齐）
INSERT INTO sys_notify_route (id, tenant_id, biz_code, msg_type, channels, status, remark, create_time, update_time)
SELECT 172001, 0, 'monitor', '*', 'inbox,webhook', 0, '监控告警默认路由（平台域，种子）', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_notify_route r WHERE r.tenant_id = 0 AND r.biz_code = 'monitor' AND r.deleted = 0);
