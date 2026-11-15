-- mse-target: platform
-- =====================================================================
-- 租户数据源加「结构同步状态」三列（管理面「租户数据源」页批量同步改异步后台执行）
--   sync_status：NULL 未发起 / 0 同步中 / 1 成功 / 2 失败（单库同步与批量同步共用，同 init_status 先例）
--   sync_message：最近一次同步结果摘要（成功=迁移+结构比对统计；失败=截断后的错误信息）
--   sync_time：最近一次同步动作时间（发起/完成均刷新）
-- 批量同步逐库执行、库多时同步等待必撞前端超时——改「登记批次即返回 + 逐库状态落行 +
-- 批次进度 Redis hash（GET /tenant/datasource/batch-sync/status 轮询）」。
-- ALTER 无条件幂等写法（MySQL 不支持 ADD COLUMN IF NOT EXISTS），单次执行由
-- mse_platform_schema_history 账本保证；半途失败重放前需先手工核对列状态。
-- =====================================================================
ALTER TABLE sys_tenant_datasource
    ADD COLUMN sync_status TINYINT NULL COMMENT '结构同步任务状态：NULL 未发起 0 同步中 1 成功 2 失败' AFTER init_message,
    ADD COLUMN sync_message VARCHAR(500) NULL COMMENT '最近一次结构同步结果摘要（成功统计/失败原因）' AFTER sync_status,
    ADD COLUMN sync_time DATETIME NULL COMMENT '最近一次结构同步动作时间' AFTER sync_message;
