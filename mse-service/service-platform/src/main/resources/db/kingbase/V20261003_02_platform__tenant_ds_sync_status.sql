-- mse-target: platform
-- =====================================================================
-- 租户数据源加「结构同步状态」三列（批量同步异步化，人大金仓；背景见 mysql 版注释）
-- =====================================================================
ALTER TABLE sys_tenant_datasource ADD COLUMN sync_status SMALLINT NULL;
ALTER TABLE sys_tenant_datasource ADD COLUMN sync_message VARCHAR(500) NULL;
ALTER TABLE sys_tenant_datasource ADD COLUMN sync_time TIMESTAMP NULL;
COMMENT ON COLUMN sys_tenant_datasource.sync_status IS '结构同步任务状态：NULL 未发起 0 同步中 1 成功 2 失败';
COMMENT ON COLUMN sys_tenant_datasource.sync_message IS '最近一次结构同步结果摘要（成功统计/失败原因）';
COMMENT ON COLUMN sys_tenant_datasource.sync_time IS '最近一次结构同步动作时间';
