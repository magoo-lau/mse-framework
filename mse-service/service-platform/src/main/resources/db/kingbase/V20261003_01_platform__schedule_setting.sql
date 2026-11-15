-- mse-target: platform
-- =====================================================================
-- 调度器设置（「调度器」设置页持久层，平台库全局键值表，人大金仓）
--   当前承载租户并发闸门上限：gates.<key>=<limit>。保存后写透 Redis 闸门参数键
--   mse:schedule:gates，实例监控线程控制节拍差量应用；重建对账自愈。
-- 幂等：IF NOT EXISTS + 唯一键判重；账本 mse_platform_schema_history
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_schedule_setting
(
    id            BIGINT        NOT NULL,
    setting_key   VARCHAR(60)   NOT NULL,
    setting_value VARCHAR(500)  NULL,
    remark        VARCHAR(255)  NULL,
    create_by     BIGINT        NULL,
    create_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT        NULL,
    update_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_schedule_setting_key UNIQUE (setting_key)
);
COMMENT ON TABLE sys_schedule_setting IS '调度器设置（任务调度·调度器页签）';
