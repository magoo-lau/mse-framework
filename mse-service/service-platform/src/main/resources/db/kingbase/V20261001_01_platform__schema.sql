-- mse-target: platform
-- =====================================================================
-- 平台控制面库结构（platform-service，7 平台表）
-- 应用：自动（启动迁移/推送通道），免手工执行；幂等可重复
-- 结构要点：sys_tenant 带 admin_email、ds_key 未绑定=待开通状态（登录/路由明确拒绝）；
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_tenant
(
    id                BIGINT       NOT NULL,
    code              VARCHAR(30)  NOT NULL,
    name              VARCHAR(60)  NOT NULL,
    package_id        BIGINT       NULL,
    ds_key            VARCHAR(60)  NULL,
    domain            VARCHAR(128) NULL,
    contact_user_name VARCHAR(30)  NULL,
    contact_phone     VARCHAR(20)  NULL,
    status            SMALLINT      NOT NULL DEFAULT 0,
    admin_username    VARCHAR(30)  NULL,
    admin_email       VARCHAR(60)  NULL,
    provision_status  SMALLINT      NOT NULL DEFAULT 1,
    provision_error   VARCHAR(500) NULL,
    expire_time       TIMESTAMP    NULL,
    remark            VARCHAR(255) NULL,
    create_by         BIGINT       NULL,
    create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by         BIGINT       NULL,
    update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_tenant_code UNIQUE (code),
    CONSTRAINT uk_tenant_domain UNIQUE (domain)
);
COMMENT ON TABLE sys_tenant IS '租户';
COMMENT ON COLUMN sys_tenant.id IS '租户编号';
COMMENT ON COLUMN sys_tenant.code IS '租户编码（全局唯一）';
COMMENT ON COLUMN sys_tenant.name IS '租户名';
COMMENT ON COLUMN sys_tenant.package_id IS '套餐编号';
COMMENT ON COLUMN sys_tenant.ds_key IS '独立库数据源 key（未绑定=待开通状态，登录/路由明确拒绝；绑定缺省共享库后恢复）';
COMMENT ON COLUMN sys_tenant.domain IS '绑定域名（完整主机名，小写，全局唯一；NULL=未绑定）';
COMMENT ON COLUMN sys_tenant.contact_user_name IS '联系人';
COMMENT ON COLUMN sys_tenant.contact_phone IS '联系电话';
COMMENT ON COLUMN sys_tenant.status IS '0 启用 1 停用';
COMMENT ON COLUMN sys_tenant.admin_username IS '租户管理员账号（开通时登记，重试开通回显；密码明文不落库）';
COMMENT ON COLUMN sys_tenant.admin_email IS '管理员邮箱（重置密码/TOTP 的一次性凭证投递地址，开通时登记）';
COMMENT ON COLUMN sys_tenant.provision_status IS '开通状态：0 初始化中 1 已开通 2 开通失败';
COMMENT ON COLUMN sys_tenant.provision_error IS '开通失败原因';
COMMENT ON COLUMN sys_tenant.expire_time IS '过期时间';
COMMENT ON COLUMN sys_tenant.deleted IS '逻辑删除';
COMMENT ON COLUMN sys_tenant.remark IS '备注';
COMMENT ON COLUMN sys_tenant.create_by IS '创建者';
COMMENT ON COLUMN sys_tenant.create_time IS '创建时间';
COMMENT ON COLUMN sys_tenant.update_by IS '更新者';
COMMENT ON COLUMN sys_tenant.update_time IS '更新时间';

-- ---------------------------- 租户套餐（全局表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_tenant_package
(
    id          BIGINT        NOT NULL,
    name        VARCHAR(60)   NOT NULL,
    menu_ids    VARCHAR(2048) NULL,
    remark      VARCHAR(255)  NULL,
    status      SMALLINT       NOT NULL DEFAULT 0,
    create_by   BIGINT        NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT        NULL,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_tenant_package IS '租户套餐';
COMMENT ON COLUMN sys_tenant_package.name IS '套餐名';
COMMENT ON COLUMN sys_tenant_package.menu_ids IS '可用菜单编号集合（逗号分隔；同时圈定挂靠菜单的权限点，并推导子应用可用性：含任一 type=3 页面即可用）';
COMMENT ON COLUMN sys_tenant_package.id IS '编号';
COMMENT ON COLUMN sys_tenant_package.remark IS '备注';
COMMENT ON COLUMN sys_tenant_package.status IS '0 启用 1 停用';
COMMENT ON COLUMN sys_tenant_package.create_by IS '创建者';
COMMENT ON COLUMN sys_tenant_package.create_time IS '创建时间';
COMMENT ON COLUMN sys_tenant_package.update_by IS '更新者';
COMMENT ON COLUMN sys_tenant_package.update_time IS '更新时间';
COMMENT ON COLUMN sys_tenant_package.deleted IS '逻辑删除';


-- ---------------------------- 租户数据源（全局表，平台维护；界面化维护的租户库，绑定决定共享/独立） ----------------------------
CREATE TABLE IF NOT EXISTS sys_tenant_datasource
(
    id          BIGINT        NOT NULL,
    ds_key      VARCHAR(60)   NOT NULL,
    ds_mode     VARCHAR(20)   NOT NULL DEFAULT 'dedicated',
    service_name VARCHAR(30)  NOT NULL DEFAULT '*',
    db_type     VARCHAR(20)   NOT NULL DEFAULT 'mysql',
    host        VARCHAR(128)  NOT NULL,
    port        INT           NOT NULL,
    db_name     VARCHAR(64)   NOT NULL,
    params      VARCHAR(500)  NULL,
    username    VARCHAR(64)   NOT NULL,
    password    VARCHAR(200)  NOT NULL,
    status      SMALLINT       NOT NULL DEFAULT 0,
    remark      VARCHAR(255)  NULL,
    create_by   BIGINT        NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT        NULL,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_tds_key UNIQUE (ds_key)
);
COMMENT ON TABLE sys_tenant_datasource IS '租户数据源（界面化维护的租户库：共享库/独立库）';
COMMENT ON COLUMN sys_tenant_datasource.id IS '编号';
COMMENT ON COLUMN sys_tenant_datasource.ds_key IS '数据源 key（sys_tenant.ds_key 绑定引用；启动/变更时注册进 dynamic-datasource）';
COMMENT ON COLUMN sys_tenant_datasource.ds_mode IS '库模式 shared 共享库（多租户绑定，tenant_id 行级隔离）/ dedicated 独立库（单租户独占）';
COMMENT ON COLUMN sys_tenant_datasource.service_name IS '所属服务（spring.application.name，如 mse-oa；*=通用）';
COMMENT ON COLUMN sys_tenant_datasource.db_type IS '数据库类型 mysql/dm/kingbase';
COMMENT ON COLUMN sys_tenant_datasource.host IS '主机地址（IP 或域名）';
COMMENT ON COLUMN sys_tenant_datasource.port IS '端口（MySQL 3306 / 达梦 5236 / 金仓 54321）';
COMMENT ON COLUMN sys_tenant_datasource.db_name IS '库名（MySQL/金仓为 database；达梦为 schema，习惯大写）';
COMMENT ON COLUMN sys_tenant_datasource.params IS '额外 JDBC 参数（追加到连接串，如 useSSL=false）';
COMMENT ON COLUMN sys_tenant_datasource.username IS '连接账号';
COMMENT ON COLUMN sys_tenant_datasource.password IS '连接密码（AES 加密存储，密钥 mse.tenant.dynamic-ds.encrypt-key）';
COMMENT ON COLUMN sys_tenant_datasource.status IS '0 启用 1 停用';
COMMENT ON COLUMN sys_tenant_datasource.deleted IS '逻辑删除';


-- ---------------------------- 租户×服务隔离配置（全局表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_tenant_service_config
(
    id             BIGINT        NOT NULL,
    tenant_id      BIGINT        NOT NULL,
    service_name   VARCHAR(30)   NOT NULL,
    isolation_mode VARCHAR(30)   NOT NULL,
    ds_key         VARCHAR(60)   NULL,
    table_suffix   VARCHAR(32)   NULL,
    tenant_column  VARCHAR(64)   NULL,
    remark         VARCHAR(255)  NULL,
    create_by      BIGINT        NULL,
    create_time    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT        NULL,
    update_time    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        SMALLINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_tsc_tenant_service UNIQUE (tenant_id, service_name)
);
COMMENT ON TABLE sys_tenant_service_config IS '租户×服务隔离配置';
COMMENT ON COLUMN sys_tenant_service_config.isolation_mode IS '隔离模式 INDEPENDENT_DB 独立库 / SHARED_TABLE_SPLIT 共享库分表 / SHARED_FIELD 共享库字段隔离';
COMMENT ON COLUMN sys_tenant_service_config.ds_key IS '路由数据源 key（空=不路由，落本服务业务主库）';
COMMENT ON COLUMN sys_tenant_service_config.table_suffix IS '服务级分表后缀（SHARED_TABLE_SPLIT 用，如 _0001）';
COMMENT ON COLUMN sys_tenant_service_config.tenant_column IS '字段隔离列名（空=tenant_id）';

-- ---------------------------- 租户×服务×表级隔离策略（全局表，覆盖服务级默认） ----------------------------
CREATE TABLE IF NOT EXISTS sys_tenant_service_table_policy
(
    id           BIGINT        NOT NULL,
    tenant_id    BIGINT        NOT NULL,
    service_name VARCHAR(30)   NOT NULL,
    table_name   VARCHAR(64)   NOT NULL,
    table_mode   VARCHAR(20)   NOT NULL,
    table_suffix VARCHAR(32)   NULL,
    create_by    BIGINT        NULL,
    create_time  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT        NULL,
    update_time  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_tspt_tenant_service_table UNIQUE (tenant_id, service_name, table_name)
);
COMMENT ON TABLE sys_tenant_service_table_policy IS '租户×服务表级隔离策略';
COMMENT ON COLUMN sys_tenant_service_table_policy.table_mode IS '表级模式 SPLIT 分表 / FIELD 字段隔离 / GLOBAL 全局共享';


-- ---------------------------- 域名个性化配置（全局表，平台维护） ----------------------------
CREATE TABLE IF NOT EXISTS sys_domain_config
(
    id               BIGINT       NOT NULL,
    domain           VARCHAR(128) NOT NULL,
    site_title       VARCHAR(60)  NULL,
    site_logo        VARCHAR(500) NULL,
    login_background VARCHAR(500) NULL,
    background_color VARCHAR(200) NULL,
    copyright        VARCHAR(200) NULL,
    remark           VARCHAR(255) NULL,
    status           SMALLINT      NOT NULL DEFAULT 0,
    create_by        BIGINT       NULL,
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by        BIGINT       NULL,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_domain_config_domain UNIQUE (domain)
);
COMMENT ON TABLE sys_domain_config IS '域名个性化配置';
COMMENT ON COLUMN sys_domain_config.id IS '编号';
COMMENT ON COLUMN sys_domain_config.domain IS '域名（完整主机名，小写，全局唯一）';
COMMENT ON COLUMN sys_domain_config.site_title IS '登录页标题（NULL=前端默认标题）';
COMMENT ON COLUMN sys_domain_config.site_logo IS 'Logo 图片地址（http(s):// 或 / 开头路径或 data:image）';
COMMENT ON COLUMN sys_domain_config.login_background IS '登录背景图地址（NULL=背景色/默认渐变）';
COMMENT ON COLUMN sys_domain_config.background_color IS '背景色或渐变（CSS background 值；背景图存在时垫底）';
COMMENT ON COLUMN sys_domain_config.copyright IS '版权文案（登录页底部）';
COMMENT ON COLUMN sys_domain_config.status IS '0 启用 1 停用（停用后登录页回退默认样式）';
COMMENT ON COLUMN sys_domain_config.deleted IS '逻辑删除';
COMMENT ON COLUMN sys_domain_config.remark IS '备注';
COMMENT ON COLUMN sys_domain_config.create_by IS '创建者';
COMMENT ON COLUMN sys_domain_config.create_time IS '创建时间';
COMMENT ON COLUMN sys_domain_config.update_by IS '更新者';
COMMENT ON COLUMN sys_domain_config.update_time IS '更新时间';


-- ---------------------------- 定义目录变更批次（控制面表，平台编辑→租户库扇出的持久队列） ----------------------------
CREATE TABLE IF NOT EXISTS sys_catalog_change
(
    id           BIGINT       NOT NULL,
    target_table VARCHAR(30)  NOT NULL,
    row_id       BIGINT       NOT NULL,
    op           SMALLINT     NOT NULL DEFAULT 0,
    sql_text     TEXT         NOT NULL,
    status       SMALLINT     NOT NULL DEFAULT 0,
    create_by    BIGINT       NULL,
    create_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT       NULL,
    update_time  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      SMALLINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_catalog_change IS '定义目录变更批次（0=upsert 1=逻辑删除；append-only——已扇出批次不改 sql_text）';
COMMENT ON COLUMN sys_catalog_change.id IS '编号（雪花 id，时间有序=扇出顺序）';
COMMENT ON COLUMN sys_catalog_change.target_table IS '目标表：sys_menu|sys_permission|sys_micro_app';
COMMENT ON COLUMN sys_catalog_change.row_id IS '目标行主键';
COMMENT ON COLUMN sys_catalog_change.op IS '0=upsert 1=逻辑删除';
COMMENT ON COLUMN sys_catalog_change.sql_text IS '渲染好的幂等变更 SQL（ANSI，可多语句）';
COMMENT ON COLUMN sys_catalog_change.status IS '0=待扇出 1=已扇出（监控用；正确性靠各库 mse_catalog_schema_history 账本）';
COMMENT ON COLUMN sys_catalog_change.deleted IS '逻辑删除';
