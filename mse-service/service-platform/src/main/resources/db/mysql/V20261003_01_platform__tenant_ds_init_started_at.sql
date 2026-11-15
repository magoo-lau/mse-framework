-- mse-target: platform
-- =====================================================================
-- 租户数据源加「初始化发起时间」列：异步初始化任务在进程重启/崩溃时随执行器一并丢弃
-- （shutdownNow 丢排队任务），行状态会永久停留「初始化中」（init_status=0）——启动对账
-- 恢复器（TenantDsInitRecoveryRunner）按本列判定超时仍进行中的任务并自动重新提交
-- （幂等：CREATE DATABASE IF NOT EXISTS + 账本续跑）；阈值 > 执行期锁 TTL（15min），
-- 阈值内视为他实例正常执行中不抢。ALTER 无条件幂等写法，单次执行由
-- mse_platform_schema_history 账本保证。
-- =====================================================================
ALTER TABLE sys_tenant_datasource
    ADD COLUMN init_started_at DATETIME NULL COMMENT '最近一次置初始化中的时间（启动对账恢复超时任务）' AFTER init_message;
