-- mse-target: platform
-- =====================================================================
-- 租户数据源加「初始化任务状态」两列（管理面「租户数据源」页初始化改异步后台执行）
--   init_status：NULL 未发起 / 0 初始化中 / 1 成功 / 2 失败（同 sys_tenant.provision_status 先例）
--   init_message：最近一次初始化结果摘要（成功=本次应用脚本统计；失败=截断后的错误信息）
-- 初始化（建库 + TENANT/MASTER/BASE 三通道全量脚本直建）耗时随业务积木增长且不可控，
-- 同步执行必撞前端超时——改「登记任务即返回 + 状态落行 + 管理端轮询」。
-- ALTER 无条件幂等写法（MySQL 不支持 ADD COLUMN IF NOT EXISTS），单次执行由
-- mse_platform_schema_history 账本保证；半途失败重放前需先手工核对列状态。
-- =====================================================================
ALTER TABLE sys_tenant_datasource
    ADD COLUMN init_status TINYINT NULL COMMENT '初始化任务状态：NULL 未发起 0 初始化中 1 成功 2 失败' AFTER status,
    ADD COLUMN init_message VARCHAR(500) NULL COMMENT '最近一次初始化结果摘要（成功统计/失败原因）' AFTER init_status;
