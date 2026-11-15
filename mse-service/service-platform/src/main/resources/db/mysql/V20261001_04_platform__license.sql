-- mse-target: platform
-- =====================================================================
-- 授权凭证签发记录表（平台库）：平台「导出授权凭证」的审计台账
-- 字段与载荷对应：LicensePayload（licenseId/tenantCode/menuIds/有效期），token 本体不落库；
-- 实体 SysTenantLicense extends PlatformBaseEntity，含全套审计列（create_by/update_by/update_time/deleted）
-- 幂等：IF NOT EXISTS；账本 mse_platform_schema_history
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_tenant_license
(
    id           BIGINT       NOT NULL COMMENT '编号',
    license_id   VARCHAR(64)  NOT NULL COMMENT '凭证编号',
    tenant_id    BIGINT       NOT NULL COMMENT '租户编号',
    tenant_code  VARCHAR(30)  NOT NULL COMMENT '租户编码（防串用）',
    package_name VARCHAR(60)  NULL COMMENT '套餐名',
    menu_ids     TEXT         NULL COMMENT '授权菜单编号（逗号分隔，审计用）',
    issued_at    DATETIME     NOT NULL COMMENT '签发时间',
    expire_at    DATETIME     NOT NULL COMMENT '过期时间',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0 有效 1 撤销',
    create_by    BIGINT       NULL COMMENT '创建者',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by    BIGINT       NULL COMMENT '更新者',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_license_id (license_id),
    KEY idx_tenant (tenant_id)
) ENGINE = InnoDB COMMENT ='租户授权凭证签发记录';
