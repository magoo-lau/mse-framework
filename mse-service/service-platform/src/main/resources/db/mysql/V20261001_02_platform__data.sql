-- mse-target: platform
-- =====================================================================
-- 平台控制面库种子（基础套餐）
-- 应用：自动（启动迁移/推送通道），免手工执行；幂等可重复
-- =====================================================================
-- 套餐（基础套餐：系统管理；业务子应用页面菜单由各业务工程 <biz>_base.sql 提供，运营在「套餐管理」按需勾选）
INSERT INTO sys_tenant_package (id, name, menu_ids, remark, status, create_time, update_time)
SELECT 1, '基础套餐', '1,100,110,120,130,140,150,160,161,162,170,171,180,190', '系统管理（含岗位/字典/参数/日志/首页配置/在线用户）', 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant_package x WHERE x.id = 1);
