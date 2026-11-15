-- mse-target: platform
-- =====================================================================
-- 平台控制面库结构（service-platform，7 平台表）
-- 应用：自动（启动迁移/推送通道），免手工执行；幂等可重复
-- 结构要点：sys_tenant 带 admin_email、ds_key 未绑定=待开通状态（登录/路由明确拒绝）；
--       仅 shared 模式可标记，全局至多一个）
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_tenant
(
    id               BIGINT       NOT NULL COMMENT '租户编号',
    code             VARCHAR(30)  NOT NULL COMMENT '租户编码（全局唯一）',
    name             VARCHAR(60)  NOT NULL COMMENT '租户名',
    package_id       BIGINT       NULL COMMENT '套餐编号',
    ds_key           VARCHAR(60)  NULL COMMENT '独立库数据源 key（未绑定=待开通状态，登录/路由明确拒绝；绑定缺省共享库后恢复）',
    domain           VARCHAR(128) NULL COMMENT '绑定域名（完整主机名，小写，全局唯一；NULL=未绑定）',
    contact_user_name VARCHAR(30) NULL COMMENT '联系人',
    contact_phone    VARCHAR(20)  NULL COMMENT '联系电话',
    status           TINYINT      NOT NULL DEFAULT 0 COMMENT '0 启用 1 停用',
    admin_username   VARCHAR(30)  NULL COMMENT '租户管理员账号（开通时登记，重试开通回显；密码明文不落库）',
    admin_email      VARCHAR(60)  NULL COMMENT '管理员邮箱（重置密码/TOTP 的一次性凭证投递地址，开通时登记）',
    provision_status TINYINT      NOT NULL DEFAULT 1 COMMENT '开通状态：0 初始化中 1 已开通 2 开通失败',
    provision_error  VARCHAR(500) NULL COMMENT '开通失败原因',
    expire_time      DATETIME     NULL COMMENT '过期时间',
    remark           VARCHAR(255) NULL COMMENT '备注',
    create_by        BIGINT       NULL COMMENT '创建者',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by        BIGINT       NULL COMMENT '更新者',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    UNIQUE KEY uk_domain (domain)
) ENGINE = InnoDB COMMENT ='租户';

-- ---------------------------- 租户套餐（全局表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_tenant_package
(
    id          BIGINT       NOT NULL COMMENT '编号',
    name        VARCHAR(60)  NOT NULL COMMENT '套餐名',
    menu_ids    VARCHAR(2048) NULL COMMENT '可用菜单编号集合（逗号分隔；同时圈定挂靠菜单的权限点，并推导子应用可用性：含任一 type=3 页面即可用）',
    remark      VARCHAR(255) NULL COMMENT '备注',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 启用 1 停用',
    create_by   BIGINT       NULL COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT ='租户套餐';

-- ---------------------------- 租户数据源（全局表，平台维护；界面化维护的租户库，绑定决定共享/独立） ----------------------------
CREATE TABLE IF NOT EXISTS sys_tenant_datasource
(
    id          BIGINT       NOT NULL COMMENT '编号',
    ds_key      VARCHAR(60)  NOT NULL COMMENT '数据源 key（隔离配置绑定引用；启动/变更时注册进 dynamic-datasource）',
    ds_mode     VARCHAR(20)  NOT NULL DEFAULT 'dedicated' COMMENT '库模式 shared 共享库（多租户绑定，tenant_id 行级隔离）/ dedicated 独立库（单租户独占）',
    service_name VARCHAR(30) NOT NULL DEFAULT '*' COMMENT '所属服务（spring.application.name，如 mse-oa；*=通用）',
    db_type     VARCHAR(20)  NOT NULL DEFAULT 'mysql' COMMENT '数据库类型 mysql/dm/kingbase',
    host        VARCHAR(128) NOT NULL COMMENT '主机地址（IP 或域名）',
    port        INT          NOT NULL COMMENT '端口（MySQL 3306 / 达梦 5236 / 金仓 54321）',
    db_name     VARCHAR(64)  NOT NULL COMMENT '库名（MySQL/金仓为 database；达梦为 schema，习惯大写）',
    params      VARCHAR(500) NULL COMMENT '额外 JDBC 参数（追加到连接串，如 useSSL=false）',
    username    VARCHAR(64)  NOT NULL COMMENT '连接账号',
    password    VARCHAR(200) NOT NULL COMMENT '连接密码（AES 加密存储，密钥 mse.tenant.dynamic-ds.encrypt-key）',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 启用 1 停用',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_by   BIGINT       NULL COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ds_key (ds_key)
) ENGINE = InnoDB COMMENT ='租户数据源';

-- ---------------------------- 租户×服务隔离配置（全局表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_tenant_service_config
(
    id             BIGINT       NOT NULL COMMENT '编号',
    tenant_id      BIGINT       NOT NULL COMMENT '租户编号',
    service_name   VARCHAR(30)  NOT NULL COMMENT '服务名（spring.application.name，如 mse-oa）',
    isolation_mode VARCHAR(30)  NOT NULL COMMENT '隔离模式 INDEPENDENT_DB 独立库 / SHARED_TABLE_SPLIT 共享库分表 / SHARED_FIELD 共享库字段隔离',
    ds_key         VARCHAR(60)  NULL COMMENT '路由数据源 key（空=不路由，落本服务业务主库）',
    table_suffix   VARCHAR(32)  NULL COMMENT '服务级分表后缀（SHARED_TABLE_SPLIT 用，如 _0001）',
    tenant_column  VARCHAR(64)  NULL COMMENT '字段隔离列名（空=tenant_id）',
    remark         VARCHAR(255) NULL COMMENT '备注',
    create_by      BIGINT       NULL COMMENT '创建者',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by      BIGINT       NULL COMMENT '更新者',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_service (tenant_id, service_name)
) ENGINE = InnoDB COMMENT ='租户×服务隔离配置';

-- ---------------------------- 租户×服务×表级隔离策略（全局表，覆盖服务级默认） ----------------------------
CREATE TABLE IF NOT EXISTS sys_tenant_service_table_policy
(
    id           BIGINT       NOT NULL COMMENT '编号',
    tenant_id    BIGINT       NOT NULL COMMENT '租户编号',
    service_name VARCHAR(30)  NOT NULL COMMENT '服务名',
    table_name   VARCHAR(64)  NOT NULL COMMENT '逻辑表名（小写）',
    table_mode   VARCHAR(20)  NOT NULL COMMENT '表级模式 SPLIT 分表 / FIELD 字段隔离 / GLOBAL 全局共享',
    table_suffix VARCHAR(32)  NULL COMMENT '分表后缀（空=服务级 table_suffix）',
    create_by    BIGINT       NULL COMMENT '创建者',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by    BIGINT       NULL COMMENT '更新者',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_service_table (tenant_id, service_name, table_name)
) ENGINE = InnoDB COMMENT ='租户×服务表级隔离策略';

-- ---------------------------- 域名个性化配置（全局表，平台维护） ----------------------------
CREATE TABLE IF NOT EXISTS sys_domain_config
(
    id               BIGINT       NOT NULL COMMENT '编号',
    domain           VARCHAR(128) NOT NULL COMMENT '域名（完整主机名，小写，全局唯一）',
    site_title       VARCHAR(60)  NULL COMMENT '登录页标题（NULL=前端默认标题）',
    site_logo        VARCHAR(500) NULL COMMENT 'Logo 图片地址（http(s):// 或 / 开头路径或 data:image）',
    login_background VARCHAR(500) NULL COMMENT '登录背景图地址（NULL=背景色/默认渐变）',
    background_color VARCHAR(200) NULL COMMENT '背景色或渐变（CSS background 值；背景图存在时垫底）',
    copyright        VARCHAR(200) NULL COMMENT '版权文案（登录页底部）',
    remark           VARCHAR(255) NULL COMMENT '备注',
    status           TINYINT      NOT NULL DEFAULT 0 COMMENT '0 启用 1 停用（停用后登录页回退默认样式）',
    create_by        BIGINT       NULL COMMENT '创建者',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by        BIGINT       NULL COMMENT '更新者',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_domain (domain)
) ENGINE = InnoDB COMMENT ='域名个性化配置';

-- ---------------------------- 定义目录变更批次（控制面表，平台编辑→租户库扇出的持久队列） ----------------------------
CREATE TABLE IF NOT EXISTS sys_catalog_change
(
    id           BIGINT       NOT NULL COMMENT '编号（雪花 id，时间有序=扇出顺序）',
    target_table VARCHAR(30)  NOT NULL COMMENT '目标表：sys_menu|sys_permission|sys_micro_app',
    row_id       BIGINT       NOT NULL COMMENT '目标行主键',
    op           TINYINT      NOT NULL DEFAULT 0 COMMENT '0=upsert 1=逻辑删除',
    sql_text     TEXT         NOT NULL COMMENT '渲染好的幂等变更 SQL（ANSI，可多语句；append-only——已扇出批次不改）',
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待扇出 1=已扇出（监控用；正确性靠各库 mse_catalog_schema_history 账本）',
    create_by    BIGINT       NULL COMMENT '创建者',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by    BIGINT       NULL COMMENT '更新者',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT ='定义目录变更批次';
