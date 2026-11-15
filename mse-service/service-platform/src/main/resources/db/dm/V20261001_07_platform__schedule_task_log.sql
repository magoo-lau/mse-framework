-- mse-target: platform
-- =====================================================================
-- 内置任务执行日志（达梦 DM8）：调度器进程内采集（环形缓冲 1000 条），starter-bridge
--   状态同步器增量上行（幂等键 log_key = bootId:taskId:runIndex，重启重报去重）；
--   每任务保留最近 200 条（追加后剪枝：10s 级任务约 33 分钟历史、5 分钟任务约 16 小时）
-- 从 _06 拆出：_06 已被存量库应用（checksum 冻结不再改），日志表按仓库纪律走新版本脚本
-- 幂等：IF NOT EXISTS；账本 mse_platform_schema_history
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_schedule_task_log
(
    id            BIGINT        NOT NULL,
    log_key       VARCHAR(80)   NOT NULL,
    task_key      VARCHAR(60)   NOT NULL,
    owner_service VARCHAR(60)   NULL,
    start_time    TIMESTAMP     NOT NULL,
    duration_ms   BIGINT        NOT NULL DEFAULT 0,
    result        VARCHAR(20)   NOT NULL,
    error_msg     VARCHAR(1000) NULL,
    create_by     BIGINT        NULL,
    create_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT        NULL,
    update_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_schedule_log_key UNIQUE (log_key)
);
CREATE INDEX IF NOT EXISTS idx_schedule_log_task_time ON sys_schedule_task_log (task_key, start_time);
COMMENT ON TABLE sys_schedule_task_log IS '内置任务执行日志';
COMMENT ON COLUMN sys_schedule_task_log.log_key IS '幂等键（bootId:taskId:runIndex）';
COMMENT ON COLUMN sys_schedule_task_log.task_key IS '任务编号';
COMMENT ON COLUMN sys_schedule_task_log.owner_service IS '执行实例服务名';
COMMENT ON COLUMN sys_schedule_task_log.start_time IS '执行开始时间';
COMMENT ON COLUMN sys_schedule_task_log.duration_ms IS '执行耗时（毫秒）';
COMMENT ON COLUMN sys_schedule_task_log.result IS 'SUCCESS/FAILED';
COMMENT ON COLUMN sys_schedule_task_log.error_msg IS '失败原因摘要';
