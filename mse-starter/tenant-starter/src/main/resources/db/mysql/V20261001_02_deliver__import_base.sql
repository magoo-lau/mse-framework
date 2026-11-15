-- mse-target: base
-- mse-base-scope: tenant
-- =====================================================================
-- 离线交付·导入页菜单与权限点（租户域 base 副本，随 tenant-starter jar 走）
--   仅私有部署环境经 StandaloneBootstrap BASE 通道落租户库；平台侧无 tenant-starter 不受影响。
--   可见性：租户域菜单参与套餐/license 裁剪——如需在私有环境侧边栏显示「部署包导入」，
--   在平台「套餐管理」勾选本菜单（license menuIds 携带后即显示）；未勾选时可经
--   角色/路由直达 /deliver-import（超管导入权限不依赖 license 域）。
-- 幂等：NOT EXISTS 判重；账本 mse_deliver_schema_history
-- =====================================================================
INSERT INTO sys_menu (id, parent_id, name, type, menu_scope, path, component, icon, sort, visible, status, create_time, update_time)
SELECT t.id, t.parent_id, t.name, t.type, t.menu_scope, t.path, t.component, t.icon, t.sort, t.visible, t.status, NOW(), NOW()
FROM (SELECT 515 AS id, 1 AS parent_id, '部署包导入' AS name, 2 AS type, 2 AS menu_scope, 'deliver-import' AS path,
             'deliver/Import' AS component, 'Box' AS icon, 11 AS sort, 0 AS visible, 0 AS status FROM DUAL) t
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = t.id);

INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time)
SELECT t.id, t.menu_id, t.module, t.name, t.code, t.sort, 0, NOW(), NOW()
FROM (SELECT 4501 AS id, 515 AS menu_id, 'deliver' AS module, '部署包导入' AS name,
             'deliver:package:import' AS code, 1 AS sort FROM DUAL) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = t.id);
