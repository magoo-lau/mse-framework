-- mse-target: platform
-- =====================================================================
-- 调度器设置（「调度器」设置页持久层，平台库全局键值表）
--   当前承载租户并发闸门上限：gates.<key>=<limit>（key=TenantTaskGates.key()，
--   如 gates.data / gates.oa-webhook / gates.mes-push）。保存后写透 Redis 闸门
--   参数键 mse:schedule:gates，各实例监控线程控制节拍差量应用（5s 级收敛）；
--   平台侧控制缓存去抖重建一并重写该键（对账自愈）；键缺失/清空=各闸门回落
--   构造默认（yaml 静态值）。
-- 幂等：IF NOT EXISTS + 唯一键判重；账本 mse_platform_schema_history
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_schedule_setting
(
    id            BIGINT        NOT NULL COMMENT '编号',
    setting_key   VARCHAR(60)   NOT NULL COMMENT '设置键（全局唯一；闸门参数 gates.<key>）',
    setting_value VARCHAR(500)  NULL COMMENT '设置值（闸门=每租户并发上限）',
    remark        VARCHAR(255)  NULL COMMENT '说明',
    create_by     BIGINT        NULL COMMENT '创建者',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by     BIGINT        NULL COMMENT '更新者',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_setting_key (setting_key)
) ENGINE = InnoDB COMMENT ='调度器设置（任务调度·调度器页签）';
