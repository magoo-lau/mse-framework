-- mse-target: platform
-- =====================================================================
-- 权益模型（0.1.0）：权益 = 套餐 menuIds（菜单域既有载体）+ 权益项链
--   default → 套餐(sys_package_item) → 附加项(sys_addon_item×qty) → 租户覆盖(override，后者优先)
-- 配额/开关项控制面定义、快照携带、数据面本地缓存消费（EntitlementCache 版本失效）。
-- 附：附加项/租户权益覆盖两张平台端管理页（菜单 351/352 + 权限点 3431-3434/3441-3443）。
-- 应用：自动（启动迁移/推送通道），免手工执行；幂等可重复；账本 mse_platform_schema_history
-- =====================================================================

-- ---------------------------- 权益项定义（控制面字典） ----------------------------
CREATE TABLE IF NOT EXISTS sys_entitlement_item
(
    id            BIGINT       NOT NULL,
    item_code     VARCHAR(60)  NOT NULL,
    item_type     SMALLINT      NOT NULL DEFAULT 1,
    name          VARCHAR(60)  NOT NULL,
    value_type    VARCHAR(10)  NOT NULL DEFAULT 'INT',
    default_value VARCHAR(100) NULL,
    remark        VARCHAR(255) NULL,
    status        SMALLINT      NOT NULL DEFAULT 0,
    create_by     BIGINT       NULL,
    create_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by     BIGINT       NULL,
    update_time   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_item_code UNIQUE (item_code)
);

-- ---------------------------- 套餐 × 权益项 ----------------------------
CREATE TABLE IF NOT EXISTS sys_package_item
(
    id          BIGINT       NOT NULL,
    package_id  BIGINT       NOT NULL,
    item_id     BIGINT       NOT NULL,
    item_value  VARCHAR(100) NULL,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_package_item UNIQUE (package_id, item_id)
);

-- ---------------------------- 附加项定义 ----------------------------
CREATE TABLE IF NOT EXISTS sys_addon
(
    id          BIGINT       NOT NULL,
    code        VARCHAR(60)  NOT NULL,
    name        VARCHAR(60)  NOT NULL,
    remark      VARCHAR(255) NULL,
    status      SMALLINT      NOT NULL DEFAULT 0,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_addon_code UNIQUE (code)
);

-- ---------------------------- 附加项 × 权益项 ----------------------------
CREATE TABLE IF NOT EXISTS sys_addon_item
(
    id          BIGINT       NOT NULL,
    addon_id    BIGINT       NOT NULL,
    item_id     BIGINT       NOT NULL,
    item_value  VARCHAR(100) NULL,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_addon_item UNIQUE (addon_id, item_id)
);

-- ---------------------------- 租户订购附加项 ----------------------------
CREATE TABLE IF NOT EXISTS sys_tenant_addon
(
    id          BIGINT       NOT NULL,
    tenant_id   BIGINT       NOT NULL,
    addon_id    BIGINT       NOT NULL,
    qty         INT          NOT NULL DEFAULT 1,
    status      SMALLINT      NOT NULL DEFAULT 0,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant_addon UNIQUE (tenant_id, addon_id)
);

-- ---------------------------- 租户级权益覆盖（单租户特批） ----------------------------
CREATE TABLE IF NOT EXISTS sys_tenant_entitlement_override
(
    id          BIGINT       NOT NULL,
    tenant_id   BIGINT       NOT NULL,
    item_id     BIGINT       NOT NULL,
    item_value  VARCHAR(100) NULL,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant_item UNIQUE (tenant_id, item_id)
);

-- 基础权益项种子（配额：用户数上限；租户覆盖/附加项按需在管理页配置）
INSERT INTO sys_entitlement_item (id, item_code, item_type, name, value_type, default_value, remark, status, create_time, update_time)
SELECT 1, 'quota.max_users', 1, '用户数上限', 'INT', '50', '软配额：超限创建用户被拒（并发窗口可容忍）', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_entitlement_item x WHERE x.id = 1);

-- 平台端管理页：附加项管理（351）+ 租户权益覆盖（352），挂 200 平台管理目录
INSERT INTO sys_menu (id, parent_id, name, type, menu_scope, path, component, icon, sort, visible, status, create_time, update_time)
SELECT t.id, t.parent_id, t.name, t.type, t.menu_scope, t.path, t.component, t.icon, t.sort, t.visible, t.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 351 AS id, 200 AS parent_id, '附加项管理' AS name, 2 AS type, 1 AS menu_scope, 'addon' AS path, 'platform/Addon' AS component, 'Present' AS icon, 16 AS sort, 0 AS visible, 0 AS status FROM DUAL
    UNION ALL SELECT 352, 200, '租户权益覆盖', 2, 1, 'entitlement-override', 'platform/EntitlementOverride', 'MagicStick', 17, 0, 0 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = t.id);

INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time)
SELECT t.id, t.menu_id, t.module, t.name, t.code, t.sort, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 3431 AS id, 351 AS menu_id, 'tenant' AS module, '附加项查询' AS name, 'tenant:addon:list' AS code, 1 AS sort FROM DUAL
    UNION ALL SELECT 3432, 351, 'tenant', '附加项新增', 'tenant:addon:create', 2 FROM DUAL
    UNION ALL SELECT 3433, 351, 'tenant', '附加项修改', 'tenant:addon:update', 3 FROM DUAL
    UNION ALL SELECT 3434, 351, 'tenant', '附加项删除', 'tenant:addon:delete', 4 FROM DUAL
    UNION ALL SELECT 3441, 352, 'tenant', '权益覆盖查询', 'tenant:entitlement:list', 1 FROM DUAL
    UNION ALL SELECT 3442, 352, 'tenant', '权益覆盖保存', 'tenant:entitlement:update', 2 FROM DUAL
    UNION ALL SELECT 3443, 352, 'tenant', '权益覆盖删除', 'tenant:entitlement:delete', 3 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = t.id);
