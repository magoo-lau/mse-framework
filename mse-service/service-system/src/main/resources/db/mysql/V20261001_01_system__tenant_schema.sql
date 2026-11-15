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
    id         BIGINT       NOT NULL COMMENT '编号',
    name       VARCHAR(30)  NOT NULL COMMENT '子应用唯一标识（决定主应用路由前缀 /sub/{name}，须与子应用构建 --base=/sub-apps/{name}/ 一致）',
    title      VARCHAR(60)  NOT NULL COMMENT '显示名（未配置页面菜单时作为侧边栏单项标题）',
    dev_entry  VARCHAR(200) NULL COMMENT '开发环境入口（子应用 dev server，如 //localhost:3400）',
    prod_entry VARCHAR(200) NOT NULL COMMENT '生产环境入口（子应用静态资源部署路径，如 /sub-apps/oa/）',
    side       TINYINT      NOT NULL DEFAULT 1 COMMENT '可见端 1 租户端 2 平台端 3 双端',
    client     TINYINT      NOT NULL DEFAULT 1 COMMENT '客户端 1 PC 2 移动端',
    sort       INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    status     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 启用 1 停用',
    remark     VARCHAR(255) NULL COMMENT '备注',
    create_by  BIGINT       NULL COMMENT '创建者',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by  BIGINT       NULL COMMENT '更新者',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB COMMENT ='微前端子应用注册表';

-- ---------------------------- 门户首页模板（全局表：两端独立，每租户一行） ----------------------------
CREATE TABLE IF NOT EXISTS sys_home_template
(
    id          BIGINT       NOT NULL COMMENT '编号',
    tenant_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（每租户一行；平台库份即平台单行 tenant_id=0）',
    name        VARCHAR(60)  NOT NULL COMMENT '模板名',
    layout_json TEXT         NULL COMMENT '布局 JSON：{"layout":[...],"catalog":[可选块...],"themes":[...],"defaultThemeId":"..."}',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 启用 1 停用（停用视为无配置，级联回退）',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_by   BIGINT       NULL COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_home_tenant (tenant_id)
) ENGINE = InnoDB COMMENT ='门户首页模板';

-- ---------------------------- 用户首页个性化（全局表：owner + user 定位唯一一行） ----------------------------
CREATE TABLE IF NOT EXISTS sys_home_user_setting
(
    id          BIGINT       NOT NULL COMMENT '编号',
    owner_type  TINYINT      NOT NULL DEFAULT 1 COMMENT '归属 1 租户用户 2 平台用户',
    tenant_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（owner=1 为本租户；owner=2 恒为 0）',
    user_id     BIGINT       NOT NULL COMMENT '用户编号',
    layout_json TEXT         NULL COMMENT '个性化 JSON：{"layout":[...],"themeId":"..."}',
    create_by   BIGINT       NULL COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_home_owner (owner_type, tenant_id, user_id)
) ENGINE = InnoDB COMMENT ='用户门户首页个性化设置';

-- ---------------------------- 菜单（全局表，平台维护；纯导航职责） ----------------------------
CREATE TABLE IF NOT EXISTS sys_menu
(
    id          BIGINT       NOT NULL COMMENT '编号',
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单编号（0=根）',
    name        VARCHAR(60)  NOT NULL COMMENT '菜单名称',
    type        TINYINT      NOT NULL COMMENT '1 目录 2 菜单 3 子应用页面',
    menu_scope  TINYINT      NOT NULL DEFAULT 2 COMMENT '可见域 1 平台端 2 租户端 3 双端',
    client      TINYINT      NOT NULL DEFAULT 1 COMMENT '客户端 1 PC 2 移动端',
    path        VARCHAR(200) NULL COMMENT '路由地址（type=3 为子应用内部路由，支持 :param 动态段）',
    micro_name  VARCHAR(30)  NULL COMMENT '所属子应用标识（type=3 专用，关联 sys_micro_app.name）',
    component   VARCHAR(200) NULL COMMENT '组件路径（相对前端 views，不带扩展名）',
    icon        VARCHAR(60)  NULL COMMENT '菜单图标',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    visible     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 显示 1 隐藏',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 启用 1 停用',
    create_by   BIGINT       NULL COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT ='导航菜单';

-- ---------------------------- 权限点（全局表，平台维护） ----------------------------
CREATE TABLE IF NOT EXISTS sys_permission
(
    id          BIGINT       NOT NULL COMMENT '编号',
    menu_id     BIGINT       NULL COMMENT '挂靠菜单（授权界面分组 + 套餐裁剪推导依据；NULL=通用权限，不受套餐裁剪）',
    module      VARCHAR(30)  NOT NULL COMMENT '所属模块（system/tenant/demo）',
    name        VARCHAR(60)  NOT NULL COMMENT '权限名',
    code        VARCHAR(100) NOT NULL COMMENT '权限标识（module:resource:action）',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 启用 1 停用',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_by   BIGINT       NULL COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_menu (menu_id)
) ENGINE = InnoDB COMMENT ='权限点';
