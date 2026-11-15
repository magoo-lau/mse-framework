-- mse-target: base
-- =====================================================================
-- 租户库·基座全局表结构（sys_micro_app / sys_home_template / sys_home_user_setting / sys_menu / sys_permission）
-- 归属：租户域（缺省共享主库 mse_tenant + 各独立租户库自持副本）——随车队重放 / 懒初始化 BASE 通道
--       自动应用（类 flyway，账本 mse_system_schema_history，共享库只初始化一次）
-- 两份各自维护：平台库的对应结构在 service-platform 的 V20261001_03_platform__base_domain.sql
--       （platform 通道）；身份表（sys_user/sys_role/sys_dept 等）由 tenant 通道脚本
--       V20261001_03_system__tenant_identity.sql 直建——本脚本不含身份表结构与任何平台域种子
-- 结构要点：sys_menu/sys_micro_app 带 client 列（1 PC 2 移动端，菜单按端分离、权限点双端共享）；
--       sys_home_template 两端独立（库即 scope，无 scope_type 列，每租户一行 uk_home_tenant）
-- 幂等：IF NOT EXISTS；重复应用无害
-- =====================================================================
CREATE TABLE IF NOT EXISTS sys_micro_app
(
    id         BIGINT       NOT NULL,
    name       VARCHAR(30)  NOT NULL,
    title      VARCHAR(60)  NOT NULL,
    dev_entry  VARCHAR(200) NULL,
    prod_entry VARCHAR(200) NOT NULL,
    side       SMALLINT      NOT NULL DEFAULT 1,
    client     SMALLINT      NOT NULL DEFAULT 1,
    sort       INT          NOT NULL DEFAULT 0,
    status     SMALLINT      NOT NULL DEFAULT 0,
    remark     VARCHAR(255) NULL,
    create_by  BIGINT       NULL,
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by  BIGINT       NULL,
    update_time TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_micro_app_name UNIQUE (name)
);
COMMENT ON TABLE sys_micro_app IS '微前端子应用注册表';
COMMENT ON COLUMN sys_micro_app.name IS '子应用唯一标识（决定主应用路由前缀 /sub/{name}，须与子应用构建 --base=/sub-apps/{name}/ 一致）';
COMMENT ON COLUMN sys_micro_app.title IS '显示名（未配置页面菜单时作为侧边栏单项标题）';
COMMENT ON COLUMN sys_micro_app.dev_entry IS '开发环境入口（子应用 dev server，如 //localhost:3400）';
COMMENT ON COLUMN sys_micro_app.prod_entry IS '生产环境入口（子应用静态资源部署路径，如 /sub-apps/oa/）';
COMMENT ON COLUMN sys_micro_app.side IS '可见端 1 租户端 2 平台端 3 双端';
COMMENT ON COLUMN sys_micro_app.client IS '客户端 1 PC 2 移动端';
COMMENT ON COLUMN sys_micro_app.status IS '0 启用 1 停用';
COMMENT ON COLUMN sys_micro_app.sort IS '排序号';
COMMENT ON COLUMN sys_micro_app.remark IS '备注';
COMMENT ON COLUMN sys_micro_app.create_by IS '创建者';
COMMENT ON COLUMN sys_micro_app.create_time IS '创建时间';
COMMENT ON COLUMN sys_micro_app.update_by IS '更新者';
COMMENT ON COLUMN sys_micro_app.update_time IS '更新时间';
COMMENT ON COLUMN sys_micro_app.id IS '编号';
COMMENT ON COLUMN sys_micro_app.deleted IS '逻辑删除';

-- ---------------------------- 门户首页模板（全局表：两端独立，每租户一行） ----------------------------
CREATE TABLE IF NOT EXISTS sys_home_template
(
    id          BIGINT       NOT NULL,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(60)  NOT NULL,
    layout_json TEXT         NULL,
    status      SMALLINT      NOT NULL DEFAULT 0,
    remark      VARCHAR(255) NULL,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_home_tenant UNIQUE (tenant_id)
);
COMMENT ON TABLE sys_home_template IS '门户首页模板';
COMMENT ON COLUMN sys_home_template.tenant_id IS '租户编号（每租户一行；平台库份即平台单行 tenant_id=0）';
COMMENT ON COLUMN sys_home_template.name IS '模板名';
COMMENT ON COLUMN sys_home_template.layout_json IS '布局 JSON：{"layout":[...],"catalog":[可选块...],"themes":[...],"defaultThemeId":"..."}';
COMMENT ON COLUMN sys_home_template.status IS '0 启用 1 停用（停用视为无配置，级联回退）';
COMMENT ON COLUMN sys_home_template.remark IS '备注';
COMMENT ON COLUMN sys_home_template.create_by IS '创建者';
COMMENT ON COLUMN sys_home_template.create_time IS '创建时间';
COMMENT ON COLUMN sys_home_template.update_by IS '更新者';
COMMENT ON COLUMN sys_home_template.update_time IS '更新时间';
COMMENT ON COLUMN sys_home_template.id IS '编号';
COMMENT ON COLUMN sys_home_template.deleted IS '逻辑删除';

-- ---------------------------- 用户首页个性化（全局表：owner + user 定位唯一一行） ----------------------------
CREATE TABLE IF NOT EXISTS sys_home_user_setting
(
    id          BIGINT       NOT NULL,
    owner_type  SMALLINT      NOT NULL DEFAULT 1,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    user_id     BIGINT       NOT NULL,
    layout_json TEXT         NULL,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_home_owner UNIQUE (owner_type, tenant_id, user_id)
);
COMMENT ON TABLE sys_home_user_setting IS '用户门户首页个性化设置';
COMMENT ON COLUMN sys_home_user_setting.owner_type IS '归属 1 租户用户 2 平台用户';
COMMENT ON COLUMN sys_home_user_setting.tenant_id IS '租户编号（owner=1 为本租户；owner=2 恒为 0）';
COMMENT ON COLUMN sys_home_user_setting.layout_json IS '个性化 JSON：{"layout":[...],"themeId":"..."}';
COMMENT ON COLUMN sys_home_user_setting.create_by IS '创建者';
COMMENT ON COLUMN sys_home_user_setting.create_time IS '创建时间';
COMMENT ON COLUMN sys_home_user_setting.update_by IS '更新者';
COMMENT ON COLUMN sys_home_user_setting.update_time IS '更新时间';
COMMENT ON COLUMN sys_home_user_setting.id IS '编号';
COMMENT ON COLUMN sys_home_user_setting.user_id IS '用户编号';
COMMENT ON COLUMN sys_home_user_setting.deleted IS '逻辑删除';

-- ---------------------------- 菜单（全局表，平台维护；纯导航职责） ----------------------------
CREATE TABLE IF NOT EXISTS sys_menu
(
    id          BIGINT       NOT NULL,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(60)  NOT NULL,
    type        SMALLINT      NOT NULL,
    menu_scope  SMALLINT      NOT NULL DEFAULT 2,
    client      SMALLINT      NOT NULL DEFAULT 1,
    path        VARCHAR(200) NULL,
    micro_name  VARCHAR(30)  NULL,
    component   VARCHAR(200) NULL,
    icon        VARCHAR(60)  NULL,
    sort        INT          NOT NULL DEFAULT 0,
    visible     SMALLINT      NOT NULL DEFAULT 0,
    status      SMALLINT      NOT NULL DEFAULT 0,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
COMMENT ON TABLE sys_menu IS '导航菜单';
COMMENT ON COLUMN sys_menu.type IS '1 目录 2 菜单 3 子应用页面';
COMMENT ON COLUMN sys_menu.menu_scope IS '可见域 1 平台端 2 租户端 3 双端';
COMMENT ON COLUMN sys_menu.client IS '客户端 1 PC 2 移动端';
COMMENT ON COLUMN sys_menu.path IS '路由地址（type=3 为子应用内部路由，支持 :param 动态段）';
COMMENT ON COLUMN sys_menu.micro_name IS '所属子应用标识（type=3 专用，关联 sys_micro_app.name）';
COMMENT ON COLUMN sys_menu.component IS '组件路径（相对前端 views，不带扩展名）';
COMMENT ON COLUMN sys_menu.visible IS '0 显示 1 隐藏';
COMMENT ON COLUMN sys_menu.id IS '编号';
COMMENT ON COLUMN sys_menu.parent_id IS '父菜单编号（0=根）';
COMMENT ON COLUMN sys_menu.name IS '菜单名称';
COMMENT ON COLUMN sys_menu.icon IS '菜单图标';
COMMENT ON COLUMN sys_menu.sort IS '排序号';
COMMENT ON COLUMN sys_menu.status IS '0 启用 1 停用';
COMMENT ON COLUMN sys_menu.create_by IS '创建者';
COMMENT ON COLUMN sys_menu.create_time IS '创建时间';
COMMENT ON COLUMN sys_menu.update_by IS '更新者';
COMMENT ON COLUMN sys_menu.update_time IS '更新时间';
COMMENT ON COLUMN sys_menu.deleted IS '逻辑删除';

-- ---------------------------- 权限点（全局表，平台维护） ----------------------------
CREATE TABLE IF NOT EXISTS sys_permission
(
    id          BIGINT        NOT NULL,
    menu_id     BIGINT        NULL,
    module      VARCHAR(30)   NOT NULL,
    name        VARCHAR(60)   NOT NULL,
    code        VARCHAR(100)  NOT NULL,
    sort        INT           NOT NULL DEFAULT 0,
    status      SMALLINT       NOT NULL DEFAULT 0,
    remark      VARCHAR(255)  NULL,
    create_by   BIGINT        NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT        NULL,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_permission_code UNIQUE (code)
);
CREATE INDEX IF NOT EXISTS idx_permission_menu ON sys_permission (menu_id);
COMMENT ON TABLE sys_permission IS '权限点';
