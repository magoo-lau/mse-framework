-- mse-target: platform
-- =====================================================================
-- 离线交付（租户私有部署支撑）：运行时套件登记 + 部署包导出记录（平台库，达梦 DM8）
--   平台侧一键导出「签名最终部署包」（license 签发 + 租户数据 JSONL + 部署物料/镜像
--   按套件与勾选装配，应用形态 镜像|jar 二选一），异步任务化（记录行即任务：status/progress/stage）
--   配套 build-tenant-kit.sh|.ps1 产出的运行时套件在本表登记
-- 平台端菜单/权限点种子：menu_scope=1 平台端（350 离线交付，挂 200 平台管理目录，图标 Van）
-- 幂等：IF NOT EXISTS + 按主键 NOT EXISTS 判重；账本 mse_platform_schema_history
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_deliver_kit
(
    id           BIGINT       NOT NULL,
    kit_name     VARCHAR(60)  NOT NULL,
    kit_version  VARCHAR(30)  NOT NULL,
    form         VARCHAR(20)  NOT NULL DEFAULT 'standalone',
    dir_path     VARCHAR(500) NOT NULL,
    manifest_json CLOB        NULL,
    module_list  VARCHAR(500) NULL,
    status       TINYINT      NOT NULL DEFAULT 0,
    remark       VARCHAR(255) NULL,
    create_by    BIGINT       NULL,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT       NULL,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_kit_version UNIQUE (kit_name, kit_version)
);
COMMENT ON TABLE sys_deliver_kit IS '离线交付运行时套件登记';
COMMENT ON COLUMN sys_deliver_kit.kit_name IS '套件名';
COMMENT ON COLUMN sys_deliver_kit.kit_version IS '套件版本';
COMMENT ON COLUMN sys_deliver_kit.form IS '目标形态（standalone=租户私有单体）';
COMMENT ON COLUMN sys_deliver_kit.dir_path IS '套件目录（build-tenant-kit 产出，服务端可读绝对路径）';
COMMENT ON COLUMN sys_deliver_kit.manifest_json IS 'kit-manifest.json 内容（firstParty/thirdParty 分区清单）';
COMMENT ON COLUMN sys_deliver_kit.module_list IS '包含业务积木清单（逗号分隔，登记用）';
COMMENT ON COLUMN sys_deliver_kit.status IS '0 启用 1 停用';

CREATE TABLE IF NOT EXISTS sys_deliver_package
(
    id            BIGINT       NOT NULL,
    tenant_id     BIGINT       NOT NULL,
    tenant_code   VARCHAR(30)  NOT NULL,
    package_type  VARCHAR(10)  NOT NULL,
    license_id    VARCHAR(64)  NULL,
    kit_id        BIGINT       NULL,
    app_mode      VARCHAR(10)  NOT NULL DEFAULT 'IMAGE',
    include_images TINYINT     NOT NULL DEFAULT 0,
    shared_mode   TINYINT      NOT NULL DEFAULT 0,
    status        TINYINT      NOT NULL DEFAULT 1,
    progress      INT          NOT NULL DEFAULT 100,
    stage         VARCHAR(100) NULL,
    error_msg     VARCHAR(2000) NULL,
    table_count   INT          NOT NULL DEFAULT 0,
    row_count     BIGINT       NOT NULL DEFAULT 0,
    file_name     VARCHAR(255) NOT NULL,
    file_path     VARCHAR(500) NOT NULL,
    file_size     BIGINT       NOT NULL DEFAULT 0,
    file_sha256   VARCHAR(64)  NOT NULL,
    manifest_json CLOB         NULL,
    remark        VARCHAR(255) NULL,
    create_by     BIGINT       NULL,
    create_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT       NULL,
    update_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_deliver_package_tenant ON sys_deliver_package (tenant_id);
COMMENT ON TABLE sys_deliver_package IS '离线交付部署包导出记录（记录行即异步打包任务）';
COMMENT ON COLUMN sys_deliver_package.tenant_id IS '租户编号';
COMMENT ON COLUMN sys_deliver_package.tenant_code IS '租户编码';
COMMENT ON COLUMN sys_deliver_package.package_type IS 'FULL 全量部署包 / UPGRADE 升级包';
COMMENT ON COLUMN sys_deliver_package.license_id IS '随包授权凭证编号';
COMMENT ON COLUMN sys_deliver_package.kit_id IS '关联运行时套件（deploy/software/runtime 物料来源）';
COMMENT ON COLUMN sys_deliver_package.app_mode IS '应用交付形态：IMAGE 应用镜像 / JAR fat-jar（二选一）';
COMMENT ON COLUMN sys_deliver_package.include_images IS '1=包含中间件镜像 tar（thirdParty，GB 级）';
COMMENT ON COLUMN sys_deliver_package.shared_mode IS '1=共享库租户（按 tenant_id 过滤导出）';
COMMENT ON COLUMN sys_deliver_package.status IS '任务状态：0 打包中 1 完成 2 失败';
COMMENT ON COLUMN sys_deliver_package.progress IS '打包进度 0-100';
COMMENT ON COLUMN sys_deliver_package.stage IS '当前阶段描述';
COMMENT ON COLUMN sys_deliver_package.error_msg IS '失败原因';
COMMENT ON COLUMN sys_deliver_package.table_count IS '导出表数';
COMMENT ON COLUMN sys_deliver_package.row_count IS '导出总行数';
COMMENT ON COLUMN sys_deliver_package.file_name IS '包文件名（归档目录内）';
COMMENT ON COLUMN sys_deliver_package.file_path IS '归档路径';
COMMENT ON COLUMN sys_deliver_package.file_size IS '包大小（字节）';
COMMENT ON COLUMN sys_deliver_package.file_sha256 IS '整包 SHA-256';
COMMENT ON COLUMN sys_deliver_package.manifest_json IS 'manifest.json 快照（含逐文件校验与版本基线）';

-- 平台端菜单（全局表，平台库副本；menu_scope 1 平台端）+ 权限点（34xx 段：3401-3402 登录安全已用，3411 起）
INSERT INTO sys_menu (id, parent_id, name, type, menu_scope, path, component, icon, sort, visible, status, create_time, update_time)
SELECT t.id, t.parent_id, t.name, t.type, t.menu_scope, t.path, t.component, t.icon, t.sort, t.visible, t.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT 350 AS id, 200 AS parent_id, '离线交付' AS name, 2 AS type, 1 AS menu_scope, 'deliver' AS path,
             'platform/DeliverExport' AS component, 'Van' AS icon, 16 AS sort, 0 AS visible, 0 AS status FROM DUAL) t
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = t.id);

INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time)
SELECT t.id, t.menu_id, t.module, t.name, t.code, t.sort, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 3411 AS id, 350 AS menu_id, 'tenant' AS module, '交付记录查询' AS name, 'deliver:package:list' AS code, 1 AS sort FROM DUAL
    UNION ALL SELECT 3412, 350, 'tenant', '部署包导出', 'deliver:package:export', 2 FROM DUAL
    UNION ALL SELECT 3413, 350, 'tenant', '部署包下载', 'deliver:package:download', 3 FROM DUAL
    UNION ALL SELECT 3414, 350, 'tenant', '套件查询', 'deliver:kit:list', 4 FROM DUAL
    UNION ALL SELECT 3415, 350, 'tenant', '套件登记维护', 'deliver:kit:manage', 5 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = t.id);
