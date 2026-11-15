-- mse-target: base
-- =====================================================================
-- 框架通知中枢：投递日志页面菜单 + 权限点（人大金仓，base 通道）
-- 号段：菜单 163（160 日志管理目录下）、权限点 1621（160 组 ×10+n 惯例）
-- 幂等：全段 NOT EXISTS 判重，可重复执行
-- =====================================================================

INSERT INTO sys_menu (id, parent_id, name, type, menu_scope, path, component, icon, sort, visible, status, create_time, update_time)
SELECT t.id, t.parent_id, t.name, t.type, t.menu_scope, t.path, t.component, t.icon, t.sort, t.visible, t.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 163 AS id, 160 AS parent_id, '通知投递日志' AS name, 2 AS type, 2 AS menu_scope,
           'notifylog' AS path, 'system/log/NotifyLog' AS component, 'Bell' AS icon, 3 AS sort, 0 AS visible, 0 AS status
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = t.id);

INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time)
SELECT t.id, t.menu_id, t.module, t.name, t.code, t.sort, t.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 1621 AS id, 163 AS menu_id, 'system' AS module, '通知投递日志查询' AS name,
           'system:notify:log' AS code, 1 AS sort, 0 AS status
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = t.id);
