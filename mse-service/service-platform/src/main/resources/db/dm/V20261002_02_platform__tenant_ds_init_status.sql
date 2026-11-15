-- mse-target: platform
-- =====================================================================
-- 租户数据源加「初始化任务状态」两列（管理面「租户数据源」页初始化改异步后台执行，达梦 DM8）
--   init_status：NULL 未发起 / 0 初始化中 / 1 成功 / 2 失败（同 sys_tenant.provision_status 先例）
--   init_message：最近一次初始化结果摘要（成功统计/失败原因）
-- ALTER 无条件写法（账本 mse_platform_schema_history 保证单次执行）。
-- =====================================================================
ALTER TABLE sys_tenant_datasource ADD COLUMN init_status TINYINT NULL;
ALTER TABLE sys_tenant_datasource ADD COLUMN init_message VARCHAR(500) NULL;
COMMENT ON COLUMN sys_tenant_datasource.init_status IS '初始化任务状态：NULL 未发起 0 初始化中 1 成功 2 失败';
COMMENT ON COLUMN sys_tenant_datasource.init_message IS '最近一次初始化结果摘要（成功统计/失败原因）';
