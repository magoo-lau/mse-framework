-- mse-target: base
-- mse-base-scope: tenant
-- =====================================================================
-- 离线交付·导入页菜单/权限点名称统一（三段交付管道术语）：「部署包导入」→「部署数据包导入」
--   （上传对象 = 最终交付包内 delivery/deploy-package.zip 数据载荷，见 docs/tech/standalone.md §四）
-- 幂等：按 id 条件 UPDATE（已是新名时零行生效）；账本 mse_deliver_schema_history
-- =====================================================================
UPDATE sys_menu SET name = '部署数据包导入' WHERE id = 515 AND name = '部署包导入';
UPDATE sys_permission SET name = '部署数据包导入' WHERE id = 4501 AND name = '部署包导入';
