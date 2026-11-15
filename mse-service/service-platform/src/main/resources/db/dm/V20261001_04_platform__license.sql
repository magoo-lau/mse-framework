-- mse-target: platform
-- =====================================================================
-- 授权凭证签发记录表（达梦 DM8）：平台「导出授权凭证」的审计台账
-- 字段与载荷对应：LicensePayload（licenseId/tenantCode/menuIds/有效期），token 本体不落库；
-- 实体 SysTenantLicense 含全套审计列（create_by/update_by/update_time/deleted）
-- 幂等：IF NOT EXISTS；账本 mse_platform_schema_history
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_tenant_license
(
    id           BIGINT       NOT NULL,
    license_id   VARCHAR(64)  NOT NULL,
    tenant_id    BIGINT       NOT NULL,
    tenant_code  VARCHAR(30)  NOT NULL,
    package_name VARCHAR(60)  NULL,
    menu_ids     TEXT         NULL,
    issued_at    DATETIME     NOT NULL,
    expire_at    DATETIME     NOT NULL,
    status       TINYINT      NOT NULL DEFAULT 0,
    create_by    BIGINT       NULL,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT       NULL,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    CONSTRAINT pk_tenant_license PRIMARY KEY (id),
    CONSTRAINT uk_tenant_license_id UNIQUE (license_id)
);
CREATE INDEX idx_license_tenant ON sys_tenant_license (tenant_id);
COMMENT ON TABLE sys_tenant_license IS '租户授权凭证签发记录';
COMMENT ON COLUMN sys_tenant_license.license_id IS '凭证编号';
COMMENT ON COLUMN sys_tenant_license.tenant_id IS '租户编号';
COMMENT ON COLUMN sys_tenant_license.tenant_code IS '租户编码（防串用）';
COMMENT ON COLUMN sys_tenant_license.package_name IS '套餐名';
COMMENT ON COLUMN sys_tenant_license.menu_ids IS '授权菜单编号（逗号分隔，审计用）';
COMMENT ON COLUMN sys_tenant_license.issued_at IS '签发时间';
COMMENT ON COLUMN sys_tenant_license.expire_at IS '过期时间';
COMMENT ON COLUMN sys_tenant_license.status IS '0 有效 1 撤销';
COMMENT ON COLUMN sys_tenant_license.create_by IS '创建者';
COMMENT ON COLUMN sys_tenant_license.create_time IS '创建时间';
COMMENT ON COLUMN sys_tenant_license.update_by IS '更新者';
COMMENT ON COLUMN sys_tenant_license.update_time IS '更新时间';
COMMENT ON COLUMN sys_tenant_license.deleted IS '逻辑删除';
