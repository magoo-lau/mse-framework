-- mse-target: master
-- =====================================================================
-- 离线交付·导入审计表（租户库）：standalone 私有环境「部署包导入」的阶段/结果审计
--   脚本随 tenant-starter jar 走——仅私有部署环境建表（平台侧无 tenant-starter，天然隔离），
--   结构由 StandaloneBootstrap 的 MASTER 通道启动直建；导入经原生 JDBC 写入（不经租户插件）
-- 幂等：IF NOT EXISTS；账本 mse_deliver_schema_history
-- =====================================================================
CREATE TABLE IF NOT EXISTS deliver_import_record
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '编号',
    file_name      VARCHAR(255) NOT NULL COMMENT '部署包文件名',
    package_type   VARCHAR(10)  NULL COMMENT 'FULL 全量 / UPGRADE 升级',
    tenant_code    VARCHAR(30)  NULL COMMENT '包内租户编码',
    license_id     VARCHAR(64)  NULL COMMENT '随包授权凭证编号',
    verify_result  VARCHAR(500) NULL COMMENT '验签/校验结果摘要',
    plan_json      MEDIUMTEXT   NULL COMMENT '导入计划快照（表清单/版本比对/缺口报告）',
    phase          VARCHAR(20)  NOT NULL DEFAULT 'VERIFY' COMMENT '阶段：VERIFY/PLAN/APPLY/DONE/FAILED',
    applied_tables INT          NOT NULL DEFAULT 0 COMMENT '已装载表数',
    applied_rows   BIGINT       NOT NULL DEFAULT 0 COMMENT '已装载数据行数',
    error_msg      VARCHAR(2000) NULL COMMENT '失败原因（截断）',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_phase (phase)
) ENGINE = InnoDB COMMENT ='离线交付部署包导入记录';
