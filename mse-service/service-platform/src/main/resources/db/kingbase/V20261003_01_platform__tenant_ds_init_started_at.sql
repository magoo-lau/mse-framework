-- mse-target: platform
-- =====================================================================
-- 租户数据源加「初始化发起时间」列（人大金仓）：异步初始化任务在进程重启/崩溃时随执行器
-- 一并丢弃，行状态会永久停留「初始化中」——启动对账恢复器按本列判定超时任务并自动重提。
-- ALTER 无条件写法（账本 mse_platform_schema_history 保证单次执行）。
-- =====================================================================
ALTER TABLE sys_tenant_datasource ADD COLUMN init_started_at TIMESTAMP NULL;
COMMENT ON COLUMN sys_tenant_datasource.init_started_at IS '最近一次置初始化中的时间（启动对账恢复超时任务）';
