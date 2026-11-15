-- mse-target: base

-- =====================================================================
-- 框架通知中枢：通知渠道设置页面菜单 + 权限点（达梦 DM8，base 通道）
-- 号段：菜单 172（系统管理根 id=1 下）、权限点 1721/1722（172 组 ×10+n 惯例）
-- 幂等：全段 NOT EXISTS 判重，可重复执行
-- =====================================================================

INSERT INTO sys_menu (id, parent_id, name, type, menu_scope, path, component, icon, sort, visible, status, create_time, update_time)
SELECT t.id, t.parent_id, t.name, t.type, t.menu_scope, t.path, t.component, t.icon, t.sort, t.visible, t.status, NOW(), NOW()
FROM (
    SELECT 172 AS id, 1 AS parent_id, '通知渠道设置' AS name, 2 AS type, 2 AS menu_scope,
           'notify-route' AS path, 'system/NotifyRoute' AS component, 'Bell' AS icon, 13 AS sort, 0 AS visible, 0 AS status FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = t.id);

INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time)
SELECT t.id, t.menu_id, t.module, t.name, t.code, t.sort, t.status, NOW(), NOW()
FROM (
    SELECT 1721 AS id, 172 AS menu_id, 'system' AS module, '通知渠道设置查询' AS name,
           'system:notify:route:list' AS code, 1 AS sort, 0 AS status FROM DUAL
    UNION ALL SELECT 1722, 172, 'system', '通知渠道设置保存', 'system:notify:route:save', 2, 0 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = t.id);
