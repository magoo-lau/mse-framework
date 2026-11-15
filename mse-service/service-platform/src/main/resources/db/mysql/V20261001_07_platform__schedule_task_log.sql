-- mse-target: platform
-- =====================================================================
-- 内置任务执行日志（MySQL）：调度器进程内采集（环形缓冲 1000 条），starter-bridge
--   状态同步器增量上行（幂等键 log_key = bootId:taskId:runIndex，重启重报去重）；
--   每任务保留最近 200 条（追加后剪枝：10s 级任务约 33 分钟历史、5 分钟任务约 16 小时）
-- 从 _06 拆出：_06 已被存量库应用（checksum 冻结不再改），日志表按仓库纪律走新版本脚本
-- 幂等：IF NOT EXISTS；账本 mse_platform_schema_history
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_schedule_task_log
(
    id            BIGINT        NOT NULL COMMENT '编号',
    log_key       VARCHAR(80)   NOT NULL COMMENT '幂等键（bootId:taskId:runIndex）',
    task_key      VARCHAR(60)   NOT NULL COMMENT '任务编号',
    owner_service VARCHAR(60)   NULL COMMENT '执行实例服务名',
    start_time    DATETIME      NOT NULL COMMENT '执行开始时间',
    duration_ms   BIGINT        NOT NULL DEFAULT 0 COMMENT '执行耗时（毫秒）',
    result        VARCHAR(20)   NOT NULL COMMENT 'SUCCESS/FAILED',
    error_msg     VARCHAR(1000) NULL COMMENT '失败原因摘要',
    create_by     BIGINT        NULL COMMENT '创建者',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    update_by     BIGINT        NULL COMMENT '更新者',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_log_key (log_key),
    KEY idx_task_time (task_key, start_time)
) ENGINE = InnoDB COMMENT ='内置任务执行日志';
