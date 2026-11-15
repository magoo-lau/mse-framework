-- mse-target: platform
-- =====================================================================
-- 内置任务调度登记（统一调度中心 local 模式的管理面，平台库，达梦 DM8）
--   各服务经 starter-bridge 状态同步器周期上报任务定义快照+运行态（upsert，
--   管理面字段 enabled/trigger_pending 永不被上报覆盖）；平台端「任务调度」页
--   启停/触发落本表，全域实例在一个同步周期内收敛（触发为乐观锁抢占，恰好一个实例执行）
-- 平台端菜单/权限点种子：menu_scope=1 平台端（360 任务调度，挂 200 平台管理目录，图标 Timer）
-- 幂等：IF NOT EXISTS + 按主键 NOT EXISTS 判重；账本 mse_platform_schema_history
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_schedule_task
(
    id               BIGINT        NOT NULL,
    task_key         VARCHAR(60)   NOT NULL,
    owner_service    VARCHAR(60)   NULL,
    scheduler_mode   VARCHAR(20)   NULL,
    task_name        VARCHAR(100)  NULL,
    description      VARCHAR(255)  NULL,
    task_type        VARCHAR(20)   NOT NULL,
    schedule_expr    VARCHAR(120)  NULL,
    expr_override    VARCHAR(120)  NULL,
    remark           VARCHAR(1000) NULL,
    enabled          SMALLINT      NOT NULL DEFAULT 1,
    trigger_pending  SMALLINT      NOT NULL DEFAULT 0,
    last_run_time    TIMESTAMP     NULL,
    last_duration_ms BIGINT        NOT NULL DEFAULT 0,
    last_result      VARCHAR(20)   NULL,
    last_error       VARCHAR(1000) NULL,
    run_count        BIGINT        NOT NULL DEFAULT 0,
    error_count      BIGINT        NOT NULL DEFAULT 0,
    report_time      TIMESTAMP     NULL,
    create_by        BIGINT        NULL,
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT        NULL,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_schedule_task_key UNIQUE (task_key)
);
COMMENT ON TABLE sys_schedule_task IS '内置任务调度登记（各服务调度中心上报聚合）';
COMMENT ON COLUMN sys_schedule_task.task_key IS '任务编号（全局唯一，@MseScheduled.taskId）';
COMMENT ON COLUMN sys_schedule_task.owner_service IS '最近上报服务名（同任务多实例=最近执行者）';
COMMENT ON COLUMN sys_schedule_task.scheduler_mode IS '调度模式（local/xxl-job/...）';
COMMENT ON COLUMN sys_schedule_task.task_name IS '展示名';
COMMENT ON COLUMN sys_schedule_task.description IS '描述';
COMMENT ON COLUMN sys_schedule_task.task_type IS 'FIXED_DELAY/CRON/STARTUP';
COMMENT ON COLUMN sys_schedule_task.schedule_expr IS '生效调度表达式（cron 原文 / 间隔描述；覆盖时为覆盖后的值）';
COMMENT ON COLUMN sys_schedule_task.expr_override IS '调度覆盖（纯数字=毫秒 fixedDelay，否则为 cron；空=按代码默认；管理面写）';
COMMENT ON COLUMN sys_schedule_task.remark IS '详细说明（管理端编辑，上报不覆盖）';
COMMENT ON COLUMN sys_schedule_task.enabled IS '1 启用 0 暂停（管理面写）';
COMMENT ON COLUMN sys_schedule_task.trigger_pending IS '1 待触发（管理面写，同步时抢占复位）';
COMMENT ON COLUMN sys_schedule_task.last_run_time IS '最近执行开始时间';
COMMENT ON COLUMN sys_schedule_task.last_duration_ms IS '最近执行耗时（毫秒）';
COMMENT ON COLUMN sys_schedule_task.last_result IS 'SUCCESS/FAILED（空=未执行过）';
COMMENT ON COLUMN sys_schedule_task.last_error IS '最近失败原因摘要';
COMMENT ON COLUMN sys_schedule_task.run_count IS '累计执行次数';
COMMENT ON COLUMN sys_schedule_task.error_count IS '累计失败次数';
COMMENT ON COLUMN sys_schedule_task.report_time IS '最近上报时间（判活）';

INSERT INTO sys_menu (id, parent_id, name, type, menu_scope, path, component, icon, sort, visible, status, create_time, update_time)
SELECT t.id, t.parent_id, t.name, t.type, t.menu_scope, t.path, t.component, t.icon, t.sort, t.visible, t.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT 360 AS id, 200 AS parent_id, '任务调度' AS name, 2 AS type, 1 AS menu_scope, 'schedule' AS path,
             'system/Schedule' AS component, 'Timer' AS icon, 17 AS sort, 0 AS visible, 0 AS status FROM DUAL) t
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = t.id);

INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time)
SELECT t.id, t.menu_id, t.module, t.name, t.code, t.sort, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 3421 AS id, 360 AS menu_id, 'tenant' AS module, '任务查询' AS name, 'schedule:task:list' AS code, 1 AS sort FROM DUAL
    UNION ALL SELECT 3422, 360, 'tenant', '任务启停', 'schedule:task:toggle', 2 FROM DUAL
    UNION ALL SELECT 3423, 360, 'tenant', '任务立即触发', 'schedule:task:trigger', 3
    UNION ALL SELECT 3424, 360, 'tenant', '任务编辑', 'schedule:task:edit', 4 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = t.id);

-- 历史遗留清理：schedule-state-sync 同步任务已改经 Redis 控制哈希 + 独立监控线程，
-- 不再作为调度任务存在；全新部署为 no-op，防陈库残留成为管理页僵尸条目（幂等）
DELETE FROM sys_schedule_task WHERE task_key = 'schedule-state-sync';
