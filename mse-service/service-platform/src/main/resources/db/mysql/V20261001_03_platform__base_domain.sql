-- mse-target: platform
-- =====================================================================
-- 平台库·基座域（身份表 + 全局表）结构与平台种子
-- 归属：平台库（出厂 mse_platform）——service-platform 启动经 PLATFORM 通道自动应用（类 flyway，
--       账本 mse_platform_schema_history）；平台域数据只进平台库，不经 base 通道重放进租户库
-- 结构同步：身份表段与 service-system 的租户身份脚本（V20261001_03_system__tenant_identity.sql，
--       租户库身份表）两份同构、各自维护，结构变更两处同步
-- 结构要点：sys_menu/sys_micro_app 带 client 列（1 PC 2 移动端）；sys_dept 带 leader_user_id；
--       sys_home_template 两端独立（无 scope_type 列，平台单行 tenant_id=0，uk_home_tenant）
-- 种子幂等：全部按主键 NOT EXISTS 判重
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

-- ---------------------------- 门户首页模板（全局表：两端独立，平台单行） ----------------------------
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

-- ---------------------------- 部门（租户表，树形） ----------------------------
CREATE TABLE IF NOT EXISTS sys_dept
(
    id              BIGINT       NOT NULL COMMENT '编号',
    tenant_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    parent_id       BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门编号（0=根）',
    ancestors       VARCHAR(500) NOT NULL DEFAULT '' COMMENT '祖级列表（逗号分隔，如 0,1000；用于本部门及以下展开）',
    name            VARCHAR(60)  NOT NULL COMMENT '部门名称',
    sort            INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0 启用 1 停用',
    leader_user_id  BIGINT       NULL COMMENT '部门负责人（sys_user.id，空=未设置；IM 部门群群主依据）',
    create_by       BIGINT       NULL COMMENT '创建者',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       BIGINT       NULL COMMENT '更新者',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_parent (tenant_id, parent_id)
) ENGINE = InnoDB COMMENT ='部门';

-- ---------------------------- 用户（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_user
(
    id          BIGINT       NOT NULL COMMENT '编号',
    tenant_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    dept_id     BIGINT       NULL COMMENT '部门编号（数据权限锚点）',
    username    VARCHAR(30)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt',
    nickname    VARCHAR(30)  NULL COMMENT '用户昵称',
    mobile      VARCHAR(20)  NULL COMMENT '手机号码',
    email       VARCHAR(60)  NULL COMMENT '邮箱地址',
    avatar      VARCHAR(255) NULL COMMENT '头像地址',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 启用 1 停用',
    super_admin TINYINT      NOT NULL DEFAULT 0 COMMENT '租户管理员标志（权限=套餐域内全量；平台端=全量 *）',
    totp_secret VARCHAR(64)  NULL COMMENT 'TOTP 双因子密钥（Base32；空=未绑定）',
    create_by   BIGINT       NULL COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_username (tenant_id, username),
    KEY idx_tenant_dept (tenant_id, dept_id)
) ENGINE = InnoDB COMMENT ='用户';

-- ---------------------------- 角色（租户表；tenant_id=0 为平台角色） ----------------------------
CREATE TABLE IF NOT EXISTS sys_role
(
    id          BIGINT       NOT NULL COMMENT '编号',
    tenant_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    name        VARCHAR(60)  NOT NULL COMMENT '角色名称',
    code        VARCHAR(60)  NOT NULL COMMENT '角色编码',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    data_scope  TINYINT      NOT NULL DEFAULT 1 COMMENT '数据范围 1 全部 2 自定义 3 本部门 4 本部门及以下 5 仅本人',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 启用 1 停用',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_by   BIGINT       NULL COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_code (tenant_id, code)
) ENGINE = InnoDB COMMENT ='角色';

-- ---------------------------- 用户-角色（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_role
(
    id          BIGINT   NOT NULL COMMENT '编号',
    tenant_id   BIGINT   NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    user_id     BIGINT   NOT NULL COMMENT '用户编号',
    role_id     BIGINT   NOT NULL COMMENT '角色编号',
    create_by   BIGINT   NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT   NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_user (tenant_id, user_id),
    KEY idx_tenant_role (tenant_id, role_id)
) ENGINE = InnoDB COMMENT ='用户角色关联';

-- ---------------------------- 角色-菜单（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_menu
(
    id          BIGINT   NOT NULL COMMENT '编号',
    tenant_id   BIGINT   NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    role_id     BIGINT   NOT NULL COMMENT '角色编号',
    menu_id     BIGINT   NOT NULL COMMENT '菜单编号',
    create_by   BIGINT   NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT   NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_role (tenant_id, role_id)
) ENGINE = InnoDB COMMENT ='角色菜单关联';

-- ---------------------------- 角色-权限点（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission
(
    id             BIGINT   NOT NULL COMMENT '编号',
    tenant_id      BIGINT   NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    role_id        BIGINT   NOT NULL COMMENT '角色编号',
    permission_id  BIGINT   NOT NULL COMMENT '权限点编号',
    create_by      BIGINT   NULL COMMENT '创建者',
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by      BIGINT   NULL COMMENT '更新者',
    update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted        TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_role (tenant_id, role_id),
    KEY idx_tenant_permission (tenant_id, permission_id)
) ENGINE = InnoDB COMMENT ='角色权限点关联';

-- ---------------------------- 角色-部门（租户表；data_scope=2 自定义时的部门集合） ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_dept
(
    id          BIGINT   NOT NULL COMMENT '编号',
    tenant_id   BIGINT   NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    role_id     BIGINT   NOT NULL COMMENT '角色编号',
    dept_id     BIGINT   NOT NULL COMMENT '部门编号',
    create_by   BIGINT   NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT   NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_role (tenant_id, role_id)
) ENGINE = InnoDB COMMENT ='角色自定义数据部门';

-- ---------------------------- 岗位（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_post
(
    id          BIGINT       NOT NULL COMMENT '编号',
    tenant_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    post_code   VARCHAR(60)  NOT NULL COMMENT '岗位编码（租户内唯一，代码层校验）',
    post_name   VARCHAR(60)  NOT NULL COMMENT '岗位名称',
    post_sort   INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 停用',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_by   BIGINT       NULL COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_code (tenant_id, post_code)
) ENGINE = InnoDB COMMENT ='岗位';

-- ---------------------------- 用户-岗位（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_post
(
    id          BIGINT   NOT NULL COMMENT '编号',
    tenant_id   BIGINT   NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    user_id     BIGINT   NOT NULL COMMENT '用户编号',
    post_id     BIGINT   NOT NULL COMMENT '岗位编号',
    create_by   BIGINT   NULL COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT   NULL COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_user (tenant_id, user_id),
    KEY idx_tenant_post (tenant_id, post_id)
) ENGINE = InnoDB COMMENT ='用户岗位关联';

-- ---------------------------- 字典类型（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_dict_type
(
    id          BIGINT       NOT NULL COMMENT '编号',
    tenant_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    dict_name   VARCHAR(60)  NOT NULL COMMENT '字典名称',
    dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型（租户内唯一，代码层校验）',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 停用',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_by   BIGINT       NULL COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_type (tenant_id, dict_type)
) ENGINE = InnoDB COMMENT ='字典类型';

-- ---------------------------- 字典数据（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_dict_data
(
    id          BIGINT       NOT NULL COMMENT '编号',
    tenant_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型',
    dict_label  VARCHAR(60)  NOT NULL COMMENT '数据标签',
    dict_value  VARCHAR(100) NOT NULL COMMENT '数据键值',
    dict_sort   INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    list_class  VARCHAR(30)  NULL COMMENT '标签样式（default/primary/success/info/warning/danger）',
    is_default  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 0 否 1 是',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 正常 1 停用',
    remark      VARCHAR(255) NULL COMMENT '备注',
    create_by   BIGINT       NULL COMMENT '创建者',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by   BIGINT       NULL COMMENT '更新者',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_type (tenant_id, dict_type)
) ENGINE = InnoDB COMMENT ='字典数据';

-- ---------------------------- 参数配置（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_config
(
    id           BIGINT       NOT NULL COMMENT '编号',
    tenant_id    BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    config_name  VARCHAR(60)  NOT NULL COMMENT '参数名称',
    config_key   VARCHAR(100) NOT NULL COMMENT '参数键名（租户内唯一，代码层校验）',
    config_value VARCHAR(500) NOT NULL COMMENT '参数键值',
    config_type  TINYINT      NOT NULL DEFAULT 1 COMMENT '0 系统内置 1 自定义',
    remark       VARCHAR(255) NULL COMMENT '备注',
    create_by    BIGINT       NULL COMMENT '创建者',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by    BIGINT       NULL COMMENT '更新者',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_key (tenant_id, config_key)
) ENGINE = InnoDB COMMENT ='参数配置';

-- ---------------------------- 站内消息（租户表，接收人个人数据） ----------------------------
CREATE TABLE IF NOT EXISTS sys_message
(
    id          BIGINT       NOT NULL COMMENT '编号',
    type        VARCHAR(20)  NOT NULL COMMENT '消息类型（业务自定义，如 TODO/CC/RESULT/REMIND）',
    receiver_id BIGINT       NOT NULL COMMENT '接收人',
    title       VARCHAR(255) NULL COMMENT '消息标题',
    content     VARCHAR(500) NULL COMMENT '消息内容',
    biz_id      VARCHAR(64)  NULL COMMENT '业务关联标识（如流程实例编号）',
    read_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未读 1 已读',
    tenant_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_tenant_receiver (tenant_id, receiver_id, read_flag)
) ENGINE = InnoDB COMMENT ='站内消息';

-- ---------------------------- 操作日志（租户表，仅插入） ----------------------------
CREATE TABLE IF NOT EXISTS sys_oper_log
(
    id           BIGINT        NOT NULL COMMENT '编号',
    tenant_id    BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台）',
    module       VARCHAR(30)   NOT NULL COMMENT '业务模块（如 system:user）',
    oper_type    VARCHAR(20)   NULL COMMENT '操作类型（新增/修改/删除/导入/导出等）',
    method       VARCHAR(200)  NULL COMMENT '方法名（类.方法）',
    request_method VARCHAR(10) NULL COMMENT '请求方式（GET/POST/PUT/DELETE）',
    oper_url     VARCHAR(255)  NULL COMMENT '请求 URL',
    oper_param   VARCHAR(2000) NULL COMMENT '请求参数（JSON，超长截断）',
    oper_user_id BIGINT        NULL COMMENT '操作人员编号',
    oper_username VARCHAR(30)  NULL COMMENT '操作人员账号',
    oper_ip      VARCHAR(60)   NULL COMMENT '操作 IP',
    cost_ms      INT           NULL COMMENT '耗时（毫秒）',
    status       TINYINT       NOT NULL DEFAULT 0 COMMENT '0 成功 1 失败',
    error_msg    VARCHAR(2000) NULL COMMENT '错误消息（超长截断）',
    oper_time    DATETIME      NULL COMMENT '操作时间',
    deleted      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_tenant_time (tenant_id, oper_time)
) ENGINE = InnoDB COMMENT ='操作日志';

-- ---------------------------- 登录日志（租户表，仅插入） ----------------------------
CREATE TABLE IF NOT EXISTS sys_logininfor
(
    id          BIGINT       NOT NULL COMMENT '编号',
    tenant_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号（0=平台；租户不存在时的失败记录也落 0）',
    username    VARCHAR(30)  NOT NULL COMMENT '登录账号',
    ip          VARCHAR(60)  NULL COMMENT '登录 IP',
    browser     VARCHAR(60)  NULL COMMENT '浏览器',
    os          VARCHAR(60)  NULL COMMENT '操作系统',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '0 成功 1 失败',
    msg         VARCHAR(255) NULL COMMENT '提示消息',
    login_time  DATETIME     NULL COMMENT '登录时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_tenant_time (tenant_id, login_time)
) ENGINE = InnoDB COMMENT ='登录日志';

-- ---------------------------- Seata AT 回滚日志（全局表，M4 使用） ----------------------------
CREATE TABLE IF NOT EXISTS undo_log
(
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '上下文',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚信息',
    log_status    INT          NOT NULL COMMENT '0 正常 1 全局已完成',
    log_created   DATETIME(6)  NOT NULL COMMENT '创建时间',
    log_modified  DATETIME(6)  NOT NULL COMMENT '修改时间',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB COMMENT ='Seata AT 模式回滚日志';


-- =====================================================================
-- 平台种子（幂等：按主键 NOT EXISTS 判重）
--   平台端：platform/admin123（超管 *）；platform_op/op123456（平台运营角色，仅租户/套餐管理）
--   租户：不预置示例租户，经平台端「租户管理」自行开通；需要演示数据可手动执行
--         docker/mysql/demo/02_demo_tenant.sql（示例租户 demo：admin/tenant123、user/user123）
-- =====================================================================

-- 部门（tenant_id=0 平台自身部门树）
INSERT INTO sys_dept (id, tenant_id, parent_id, ancestors, name, sort, status, create_time, update_time)
SELECT t.id, t.tenant_id, t.parent_id, t.ancestors, t.name, t.sort, t.status, NOW(), NOW()
FROM (SELECT 900 AS id, 0 AS tenant_id, 0 AS parent_id, '0' AS ancestors, '平台总部' AS name, 1 AS sort, 0 AS status FROM DUAL) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dept x WHERE x.id = t.id);

-- 菜单（全局，平台维护；type 1 目录 2 菜单 3 子应用页面；menu_scope 1 平台端 2 租户端 3 双端）
INSERT INTO sys_menu (id, parent_id, name, type, menu_scope, path, component, icon, sort, visible, status, create_time, update_time)
SELECT t.id, t.parent_id, t.name, t.type, t.menu_scope, t.path, t.component, t.icon, t.sort, t.visible, t.status, NOW(), NOW()
FROM (
    SELECT 200 AS id, 0 AS parent_id, '平台管理' AS name, 1 AS type, 1 AS menu_scope, '/platform' AS path, NULL AS component, 'Monitor' AS icon, 20 AS sort, 0 AS visible, 0 AS status FROM DUAL
    UNION ALL SELECT 210, 200, '租户管理', 2, 1, 'tenant', 'Tenant', 'OfficeBuilding', 1, 0, 0 FROM DUAL
    UNION ALL SELECT 220, 200, '套餐管理', 2, 1, 'tenant-package', 'platform/Package', 'Goods', 2, 0, 0 FROM DUAL
    UNION ALL SELECT 230, 200, '菜单管理', 2, 1, 'menu', 'platform/Menu', 'Menu', 3, 0, 0 FROM DUAL
    UNION ALL SELECT 240, 200, '权限点管理', 2, 1, 'permission', 'platform/Permission', 'Key', 4, 0, 0 FROM DUAL
    UNION ALL SELECT 250, 200, '平台用户', 2, 1, 'user', 'User', 'User', 5, 0, 0 FROM DUAL
    UNION ALL SELECT 260, 200, '平台角色', 2, 1, 'role', 'system/Role', 'Avatar', 6, 0, 0 FROM DUAL
    UNION ALL SELECT 270, 200, '平台部门', 2, 1, 'dept', 'system/Dept', 'Grid', 7, 0, 0 FROM DUAL
    UNION ALL SELECT 280, 200, '域名个性化', 2, 1, 'domain-config', 'platform/DomainConfig', 'Picture', 8, 0, 0 FROM DUAL
    UNION ALL SELECT 290, 200, '子应用管理', 2, 1, 'micro-app', 'platform/MicroApp', 'Menu', 9, 0, 0 FROM DUAL
    UNION ALL SELECT 300, 200, '首页配置', 2, 1, 'home-layout', 'system/HomeLayout', 'Operation', 10, 0, 0 FROM DUAL
    UNION ALL SELECT 310, 200, '参数设置', 2, 1, 'config', 'system/Config', 'Tools', 11, 0, 0 FROM DUAL
    UNION ALL SELECT 320, 200, '在线用户', 2, 1, 'online', 'system/OnlineUser', 'Connection', 12, 0, 0 FROM DUAL
    UNION ALL SELECT 330, 200, '租户数据源', 2, 1, 'tenant-datasource', 'platform/TenantDatasource', 'Coin', 13, 0, 0 FROM DUAL
    UNION ALL SELECT 331, 200, '隔离策略', 2, 1, 'tenant-isolation', 'platform/TenantIsolation', 'SetUp', 14, 0, 0 FROM DUAL
    UNION ALL SELECT 340, 200, '登录安全设置', 2, 1, 'login-security', 'system/LoginSecurity', 'Lock', 15, 0, 0 FROM DUAL
    UNION ALL SELECT 1, 0, '系统管理', 1, 2, '/system', NULL, 'Setting', 10, 0, 0 FROM DUAL
    UNION ALL SELECT 100, 1, '用户管理', 2, 2, 'user', 'User', 'User', 1, 0, 0 FROM DUAL
    UNION ALL SELECT 110, 1, '角色管理', 2, 2, 'role', 'system/Role', 'Avatar', 2, 0, 0 FROM DUAL
    UNION ALL SELECT 120, 1, '部门管理', 2, 2, 'dept', 'system/Dept', 'Grid', 3, 0, 0 FROM DUAL
    UNION ALL SELECT 130, 1, '岗位管理', 2, 2, 'post', 'system/Post', 'Postcard', 4, 0, 0 FROM DUAL
    UNION ALL SELECT 140, 1, '字典管理', 2, 2, 'dict', 'system/Dict', 'Collection', 5, 0, 0 FROM DUAL
    UNION ALL SELECT 150, 1, '参数设置', 2, 2, 'config', 'system/Config', 'Tools', 6, 0, 0 FROM DUAL
    UNION ALL SELECT 160, 1, '日志管理', 1, 2, 'log', NULL, 'Document', 7, 0, 0 FROM DUAL
    UNION ALL SELECT 161, 160, '操作日志', 2, 2, 'operlog', 'system/log/OperLog', 'Memo', 1, 0, 0 FROM DUAL
    UNION ALL SELECT 162, 160, '登录日志', 2, 2, 'logininfor', 'system/log/Logininfor', 'Tickets', 2, 0, 0 FROM DUAL
    UNION ALL SELECT 170, 1, '首页配置', 2, 2, 'home-layout', 'system/HomeLayout', 'Operation', 8, 0, 0 FROM DUAL
    UNION ALL SELECT 180, 1, '在线用户', 2, 2, 'online', 'system/OnlineUser', 'Connection', 9, 0, 0 FROM DUAL
    UNION ALL SELECT 190, 1, '登录安全设置', 2, 2, 'login-security', 'system/LoginSecurity', 'Lock', 10, 0, 0 FROM DUAL
    UNION ALL SELECT 195, 1, '任务调度', 2, 2, 'schedule', 'system/Schedule', 'Timer', 12, 0, 0 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = t.id);

-- 权限点（全局，平台维护；menu_id 挂靠菜单用于授权分组与套餐裁剪；NULL=通用权限不受套餐裁剪）
INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time)
SELECT t.id, t.menu_id, t.module, t.name, t.code, t.sort, 0, NOW(), NOW()
FROM (
    SELECT 1001 AS id, 100 AS menu_id, 'system' AS module, '用户查询' AS name, 'system:user:list' AS code, 1 AS sort FROM DUAL
    UNION ALL SELECT 1002, 100, 'system', '用户详情', 'system:user:query', 2 FROM DUAL
    UNION ALL SELECT 1003, 100, 'system', '用户新增', 'system:user:create', 3 FROM DUAL
    UNION ALL SELECT 1004, 100, 'system', '用户修改', 'system:user:update', 4 FROM DUAL
    UNION ALL SELECT 1005, 100, 'system', '用户删除', 'system:user:delete', 5 FROM DUAL
    UNION ALL SELECT 1006, 100, 'system', '重置密码', 'system:user:reset-password', 6 FROM DUAL
    UNION ALL SELECT 1007, 100, 'system', '分配角色', 'system:user:assign-role', 7 FROM DUAL
    UNION ALL SELECT 1008, 100, 'system', '用户导入', 'system:user:import', 8 FROM DUAL
    UNION ALL SELECT 1009, 100, 'system', '用户导出', 'system:user:export', 9 FROM DUAL
    UNION ALL SELECT 1010, 100, 'system', '重置双因子', 'system:user:reset-totp', 10 FROM DUAL
    UNION ALL SELECT 1101, 110, 'system', '角色查询', 'system:role:list', 1 FROM DUAL
    UNION ALL SELECT 1102, 110, 'system', '角色详情', 'system:role:query', 2 FROM DUAL
    UNION ALL SELECT 1103, 110, 'system', '角色新增', 'system:role:create', 3 FROM DUAL
    UNION ALL SELECT 1104, 110, 'system', '角色修改', 'system:role:update', 4 FROM DUAL
    UNION ALL SELECT 1105, 110, 'system', '角色删除', 'system:role:delete', 5 FROM DUAL
    UNION ALL SELECT 1106, 110, 'system', '角色授权', 'system:role:assign', 6 FROM DUAL
    UNION ALL SELECT 1201, 120, 'system', '部门查询', 'system:dept:list', 1 FROM DUAL
    UNION ALL SELECT 1202, 120, 'system', '部门新增', 'system:dept:create', 2 FROM DUAL
    UNION ALL SELECT 1203, 120, 'system', '部门修改', 'system:dept:update', 3 FROM DUAL
    UNION ALL SELECT 1204, 120, 'system', '部门删除', 'system:dept:delete', 4 FROM DUAL
    UNION ALL SELECT 1301, 130, 'system', '岗位查询', 'system:post:list', 1 FROM DUAL
    UNION ALL SELECT 1302, 130, 'system', '岗位详情', 'system:post:query', 2 FROM DUAL
    UNION ALL SELECT 1303, 130, 'system', '岗位新增', 'system:post:create', 3 FROM DUAL
    UNION ALL SELECT 1304, 130, 'system', '岗位修改', 'system:post:update', 4 FROM DUAL
    UNION ALL SELECT 1305, 130, 'system', '岗位删除', 'system:post:delete', 5 FROM DUAL
    UNION ALL SELECT 1401, 140, 'system', '字典查询', 'system:dict:list', 1 FROM DUAL
    UNION ALL SELECT 1402, 140, 'system', '字典新增', 'system:dict:create', 2 FROM DUAL
    UNION ALL SELECT 1403, 140, 'system', '字典修改', 'system:dict:update', 3 FROM DUAL
    UNION ALL SELECT 1404, 140, 'system', '字典删除', 'system:dict:delete', 4 FROM DUAL
    UNION ALL SELECT 1501, 150, 'system', '参数查询', 'system:config:list', 1 FROM DUAL
    UNION ALL SELECT 1502, 150, 'system', '参数新增', 'system:config:create', 2 FROM DUAL
    UNION ALL SELECT 1503, 150, 'system', '参数修改', 'system:config:update', 3 FROM DUAL
    UNION ALL SELECT 1504, 150, 'system', '参数删除', 'system:config:delete', 4 FROM DUAL
    UNION ALL SELECT 1601, 161, 'system', '操作日志查询', 'system:operlog:list', 1 FROM DUAL
    UNION ALL SELECT 1602, 161, 'system', '操作日志删除', 'system:operlog:delete', 2 FROM DUAL
    UNION ALL SELECT 1611, 162, 'system', '登录日志查询', 'system:logininfor:list', 1 FROM DUAL
    UNION ALL SELECT 1612, 162, 'system', '登录日志删除', 'system:logininfor:delete', 2 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = t.id);

-- 平台域权限点（租户/套餐/菜单/权限点/平台用户/角色/部门/域名/子应用管理）
INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time)
SELECT t.id, t.menu_id, t.module, t.name, t.code, t.sort, 0, NOW(), NOW()
FROM (
    SELECT 2101 AS id, 210 AS menu_id, 'tenant' AS module, '租户查询' AS name, 'tenant:tenant:list' AS code, 1 AS sort FROM DUAL
    UNION ALL SELECT 2102, 210, 'tenant', '租户详情', 'tenant:tenant:query', 2 FROM DUAL
    UNION ALL SELECT 2103, 210, 'tenant', '租户新增', 'tenant:tenant:create', 3 FROM DUAL
    UNION ALL SELECT 2104, 210, 'tenant', '租户修改', 'tenant:tenant:update', 4 FROM DUAL
    UNION ALL SELECT 2105, 210, 'tenant', '租户删除', 'tenant:tenant:delete', 5 FROM DUAL
    UNION ALL SELECT 2106, 210, 'tenant', '启停租户', 'tenant:tenant:change-status', 6 FROM DUAL
    UNION ALL SELECT 2107, 210, 'tenant', '重置管理员密码', 'tenant:tenant:reset-password', 7 FROM DUAL
    UNION ALL SELECT 2201, 220, 'tenant', '套餐查询', 'tenant:package:list', 1 FROM DUAL
    UNION ALL SELECT 2202, 220, 'tenant', '套餐新增', 'tenant:package:create', 2 FROM DUAL
    UNION ALL SELECT 2203, 220, 'tenant', '套餐修改', 'tenant:package:update', 3 FROM DUAL
    UNION ALL SELECT 2204, 220, 'tenant', '套餐删除', 'tenant:package:delete', 4 FROM DUAL
    UNION ALL SELECT 2301, 230, 'system', '菜单查询', 'system:menu:list', 1 FROM DUAL
    UNION ALL SELECT 2302, 230, 'system', '菜单新增', 'system:menu:create', 2 FROM DUAL
    UNION ALL SELECT 2303, 230, 'system', '菜单修改', 'system:menu:update', 3 FROM DUAL
    UNION ALL SELECT 2304, 230, 'system', '菜单删除', 'system:menu:delete', 4 FROM DUAL
    UNION ALL SELECT 2401, 240, 'system', '权限点查询', 'system:permission:list', 1 FROM DUAL
    UNION ALL SELECT 2402, 240, 'system', '权限点新增', 'system:permission:create', 2 FROM DUAL
    UNION ALL SELECT 2403, 240, 'system', '权限点修改', 'system:permission:update', 3 FROM DUAL
    UNION ALL SELECT 2404, 240, 'system', '权限点删除', 'system:permission:delete', 4 FROM DUAL
    UNION ALL SELECT 2501, 250, 'system', '平台用户查询', 'system:user:platform-list', 1 FROM DUAL
    UNION ALL SELECT 2502, 250, 'system', '平台用户新增', 'system:user:platform-create', 2 FROM DUAL
    UNION ALL SELECT 2503, 250, 'system', '平台用户修改', 'system:user:platform-update', 3 FROM DUAL
    UNION ALL SELECT 2504, 250, 'system', '平台用户删除', 'system:user:platform-delete', 4 FROM DUAL
    UNION ALL SELECT 2505, 250, 'system', '平台用户重置双因子', 'system:user:platform-reset-totp', 5 FROM DUAL
    UNION ALL SELECT 2601, 260, 'system', '平台角色查询', 'system:role:platform-list', 1 FROM DUAL
    UNION ALL SELECT 2602, 260, 'system', '平台角色详情', 'system:role:platform-query', 2 FROM DUAL
    UNION ALL SELECT 2603, 260, 'system', '平台角色新增', 'system:role:platform-create', 3 FROM DUAL
    UNION ALL SELECT 2604, 260, 'system', '平台角色修改', 'system:role:platform-update', 4 FROM DUAL
    UNION ALL SELECT 2605, 260, 'system', '平台角色授权', 'system:role:platform-assign', 5 FROM DUAL
    UNION ALL SELECT 2701, 270, 'system', '平台部门查询', 'system:dept:platform-list', 1 FROM DUAL
    UNION ALL SELECT 2702, 270, 'system', '平台部门新增', 'system:dept:platform-create', 2 FROM DUAL
    UNION ALL SELECT 2703, 270, 'system', '平台部门修改', 'system:dept:platform-update', 3 FROM DUAL
    UNION ALL SELECT 2704, 270, 'system', '平台部门删除', 'system:dept:platform-delete', 4 FROM DUAL
    UNION ALL SELECT 2801, 280, 'tenant', '个性化查询', 'tenant:domain:list', 1 FROM DUAL
    UNION ALL SELECT 2802, 280, 'tenant', '个性化详情', 'tenant:domain:query', 2 FROM DUAL
    UNION ALL SELECT 2803, 280, 'tenant', '个性化新增', 'tenant:domain:create', 3 FROM DUAL
    UNION ALL SELECT 2804, 280, 'tenant', '个性化修改', 'tenant:domain:update', 4 FROM DUAL
    UNION ALL SELECT 2805, 280, 'tenant', '个性化删除', 'tenant:domain:delete', 5 FROM DUAL
    UNION ALL SELECT 2911, 290, 'system', '子应用查询', 'system:micro-app:list', 1 FROM DUAL
    UNION ALL SELECT 2912, 290, 'system', '子应用新增', 'system:micro-app:create', 2 FROM DUAL
    UNION ALL SELECT 2913, 290, 'system', '子应用修改', 'system:micro-app:update', 3 FROM DUAL
    UNION ALL SELECT 2914, 290, 'system', '子应用删除', 'system:micro-app:delete', 4 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = t.id);

-- 双端复用页面权限点（首页/参数/在线用户/登录安全；platform-* 后缀区分两端）+ 数据源/隔离策略（平台域）
INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time)
SELECT t.id, t.menu_id, t.module, t.name, t.code, t.sort, 0, NOW(), NOW()
FROM (
    SELECT 3001 AS id, 300 AS menu_id, 'system' AS module, '首页模板查询' AS name, 'system:home-template:platform-list' AS code, 1 AS sort FROM DUAL
    UNION ALL SELECT 3002, 300, 'system', '首页模板保存', 'system:home-template:platform-save', 2 FROM DUAL
    UNION ALL SELECT 3003, 300, 'system', '首页模板恢复默认', 'system:home-template:platform-reset', 3 FROM DUAL
    UNION ALL SELECT 1701, 170, 'system', '首页模板查询', 'system:home-template:list', 1 FROM DUAL
    UNION ALL SELECT 1702, 170, 'system', '首页模板保存', 'system:home-template:save', 2 FROM DUAL
    UNION ALL SELECT 1703, 170, 'system', '首页模板恢复默认', 'system:home-template:reset', 3 FROM DUAL
    UNION ALL SELECT 3004, 310, 'system', '参数查询', 'system:config:platform-list', 1 FROM DUAL
    UNION ALL SELECT 3005, 310, 'system', '参数新增', 'system:config:platform-create', 2 FROM DUAL
    UNION ALL SELECT 3006, 310, 'system', '参数修改', 'system:config:platform-update', 3 FROM DUAL
    UNION ALL SELECT 3007, 310, 'system', '参数删除', 'system:config:platform-delete', 4 FROM DUAL
    UNION ALL SELECT 2901, NULL, 'system', '个人信息查询', 'system:profile:query', 1 FROM DUAL
    UNION ALL SELECT 2902, NULL, 'system', '个人信息修改', 'system:profile:update', 2 FROM DUAL
    UNION ALL SELECT 1801, 180, 'system', '在线用户查询', 'system:online:list', 1 FROM DUAL
    UNION ALL SELECT 1802, 180, 'system', '强制下线', 'system:online:kick', 2 FROM DUAL
    UNION ALL SELECT 3201, 320, 'system', '在线用户查询', 'system:online:platform-list', 1 FROM DUAL
    UNION ALL SELECT 3202, 320, 'system', '强制下线', 'system:online:platform-kick', 2 FROM DUAL
    UNION ALL SELECT 1901, 190, 'system', '登录安全查询', 'system:loginsecurity:query', 1 FROM DUAL
    UNION ALL SELECT 1902, 190, 'system', '登录安全保存', 'system:loginsecurity:update', 2 FROM DUAL
    UNION ALL SELECT 3401, 340, 'system', '登录安全查询', 'system:loginsecurity:platform-query', 1 FROM DUAL
    UNION ALL SELECT 3402, 340, 'system', '登录安全保存', 'system:loginsecurity:platform-update', 2 FROM DUAL
    UNION ALL SELECT 3301, 330, 'tenant', '数据源查询', 'tenant:ds:list', 1 FROM DUAL
    UNION ALL SELECT 3302, 330, 'tenant', '数据源详情', 'tenant:ds:query', 2 FROM DUAL
    UNION ALL SELECT 3303, 330, 'tenant', '数据源新增', 'tenant:ds:create', 3 FROM DUAL
    UNION ALL SELECT 3304, 330, 'tenant', '数据源修改', 'tenant:ds:update', 4 FROM DUAL
    UNION ALL SELECT 3305, 330, 'tenant', '数据源删除', 'tenant:ds:delete', 5 FROM DUAL
    UNION ALL SELECT 3306, 330, 'tenant', '迁移脚本应用', 'tenant:ds:migrate', 6 FROM DUAL
    UNION ALL SELECT 3307, 330, 'tenant', '结构比对同步', 'tenant:ds:sync', 7 FROM DUAL
    UNION ALL SELECT 3308, 330, 'tenant', '临时SQL执行', 'tenant:ds:execute', 8 FROM DUAL
    UNION ALL SELECT 3311, 331, 'tenant', '策略查询', 'tenant:route:list', 1 FROM DUAL
    UNION ALL SELECT 3312, 331, 'tenant', '策略详情', 'tenant:route:query', 2 FROM DUAL
    UNION ALL SELECT 3313, 331, 'tenant', '策略保存', 'tenant:route:save', 3 FROM DUAL
    UNION ALL SELECT 3314, 331, 'tenant', '策略删除', 'tenant:route:delete', 4 FROM DUAL
    UNION ALL SELECT 3315, 331, 'tenant', '生成分表', 'tenant:route:generate', 5 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = t.id);

-- 角色（tenant_id=0 为平台角色；data_scope 1 全部 2 自定义 3 本部门 4 本部门及以下 5 仅本人）
INSERT INTO sys_role (id, tenant_id, name, code, sort, data_scope, status, remark, create_time, update_time)
SELECT t.id, t.tenant_id, t.name, t.code, t.sort, t.data_scope, t.status, t.remark, NOW(), NOW()
FROM (SELECT 10 AS id, 0 AS tenant_id, '平台运营' AS name, 'platform_op' AS code, 1 AS sort, 1 AS data_scope, 0 AS status, '租户/套餐管理，不含菜单与权限点维护' AS remark FROM DUAL) t
WHERE NOT EXISTS (SELECT 1 FROM sys_role x WHERE x.id = t.id);

-- 用户（tenant_id=0 平台域；platform/admin123 超管 *、platform_op/op123456 平台运营）
INSERT INTO sys_user (id, tenant_id, dept_id, username, password, nickname, status, super_admin, create_time, update_time)
SELECT t.id, t.tenant_id, t.dept_id, t.username, t.password, t.nickname, t.status, t.super_admin, NOW(), NOW()
FROM (
    SELECT 1 AS id, 0 AS tenant_id, 900 AS dept_id, 'platform' AS username, '$2a$10$hvzCJfttG/KRR4rGXdYQOuQsx2jeFjI3uUX34jx3dhylud93VrHwG' AS password, '平台管理员' AS nickname, 0 AS status, 1 AS super_admin FROM DUAL
    UNION ALL SELECT 3, 0, 900, 'platform_op', '$2a$10$Vv30Sfciur6s/Mw/sD8uF.zjDxAevdl0jhx6Tm5EC/eN1TCFuDGtG', '平台运营', 0, 0 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_user x WHERE x.id = t.id);

-- 用户-角色
INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, create_time, update_time)
SELECT t.id, t.tenant_id, t.user_id, t.role_id, NOW(), NOW()
FROM (SELECT 1 AS id, 0 AS tenant_id, 3 AS user_id, 10 AS role_id FROM DUAL) t
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role x WHERE x.id = t.id);

-- 平台运营角色：可见菜单与权限点（含菜单查询 system:menu:list，供套餐范围展示与勾选；不含菜单管理/权限点管理/平台用户角色部门）
INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_time, update_time)
SELECT t.id, t.tenant_id, t.role_id, t.menu_id, NOW(), NOW()
FROM (
    SELECT 101 AS id, 0 AS tenant_id, 10 AS role_id, 200 AS menu_id FROM DUAL
    UNION ALL SELECT 102, 0, 10, 210 FROM DUAL
    UNION ALL SELECT 103, 0, 10, 220 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu x WHERE x.id = t.id);

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time)
SELECT t.id, t.tenant_id, t.role_id, t.permission_id, NOW(), NOW()
FROM (
    SELECT 101 AS id, 0 AS tenant_id, 10 AS role_id, 2101 AS permission_id FROM DUAL
    UNION ALL SELECT 102, 0, 10, 2102 FROM DUAL
    UNION ALL SELECT 103, 0, 10, 2103 FROM DUAL
    UNION ALL SELECT 104, 0, 10, 2104 FROM DUAL
    UNION ALL SELECT 105, 0, 10, 2105 FROM DUAL
    UNION ALL SELECT 106, 0, 10, 2106 FROM DUAL
    UNION ALL SELECT 107, 0, 10, 2201 FROM DUAL
    UNION ALL SELECT 108, 0, 10, 2202 FROM DUAL
    UNION ALL SELECT 109, 0, 10, 2203 FROM DUAL
    UNION ALL SELECT 110, 0, 10, 2204 FROM DUAL
    UNION ALL SELECT 111, 0, 10, 2301 FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_role_permission x WHERE x.id = t.id);

-- 字典类型/数据（tenant_id=0 平台域：平台侧页面在平台域解析字典；sys_oper_module 为操作日志模块翻译）
INSERT INTO sys_dict_type (id, tenant_id, dict_name, dict_type, status, remark, create_time, update_time)
SELECT t.id, t.tenant_id, t.dict_name, t.dict_type, t.status, t.remark, NOW(), NOW()
FROM (
    SELECT 11 AS id, 0 AS tenant_id, '系统状态' AS dict_name, 'sys_normal_disable' AS dict_type, 0 AS status, '通用状态（正常/停用）' AS remark FROM DUAL
    UNION ALL SELECT 12, 0, '操作日志系统模块', 'sys_oper_module', 0, '@OperLog(module) 英文代码 → 中文名（操作日志「系统模块」列翻译显示，未登记代码回退原值）' FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.id = t.id);

INSERT INTO sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, dict_sort, list_class, is_default, status, remark, create_time, update_time)
SELECT t.id, t.tenant_id, t.dict_type, t.dict_label, t.dict_value, t.dict_sort, t.list_class, t.is_default, t.status, t.remark, NOW(), NOW()
FROM (
    SELECT 21 AS id, 0 AS tenant_id, 'sys_normal_disable' AS dict_type, '正常' AS dict_label, '0' AS dict_value, 1 AS dict_sort, 'success' AS list_class, 1 AS is_default, 0 AS status, NULL AS remark FROM DUAL
    UNION ALL SELECT 22, 0, 'sys_normal_disable', '停用', '1', 2, 'danger', 0, 0, NULL FROM DUAL
    UNION ALL SELECT 23, 0, 'sys_oper_module', '用户管理', 'system:user', 1, NULL, 0, 0, NULL FROM DUAL
    UNION ALL SELECT 24, 0, 'sys_oper_module', '角色管理', 'system:role', 2, NULL, 0, 0, NULL FROM DUAL
    UNION ALL SELECT 25, 0, 'sys_oper_module', '部门管理', 'system:dept', 3, NULL, 0, 0, NULL FROM DUAL
    UNION ALL SELECT 26, 0, 'sys_oper_module', '岗位管理', 'system:post', 4, NULL, 0, 0, NULL FROM DUAL
    UNION ALL SELECT 27, 0, 'sys_oper_module', '字典管理', 'system:dict', 5, NULL, 0, 0, NULL FROM DUAL
    UNION ALL SELECT 28, 0, 'sys_oper_module', '参数设置', 'system:config', 6, NULL, 0, 0, NULL FROM DUAL
    UNION ALL SELECT 29, 0, 'sys_oper_module', '菜单管理', 'system:menu', 7, NULL, 0, 0, NULL FROM DUAL
    UNION ALL SELECT 30, 0, 'sys_oper_module', '权限点管理', 'system:permission', 8, NULL, 0, 0, NULL FROM DUAL
    UNION ALL SELECT 31, 0, 'sys_oper_module', '子应用管理', 'system:micro-app', 9, NULL, 0, 0, NULL FROM DUAL
    UNION ALL SELECT 32, 0, 'sys_oper_module', '在线用户', 'system:online', 10, NULL, 0, 0, NULL FROM DUAL
    UNION ALL SELECT 33, 0, 'sys_oper_module', '首页配置', 'system:home-template', 11, NULL, 0, 0, NULL FROM DUAL
    UNION ALL SELECT 34, 0, 'sys_oper_module', '租户管理', 'tenant:domain', 12, NULL, 0, 0, NULL FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.id = t.id);

-- 参数配置（界面名称/登录策略/多页签策略内置参数；config_type=0 内置不可删除，仅可改值）
INSERT INTO sys_config (id, tenant_id, config_name, config_key, config_value, config_type, remark, create_time, update_time)
SELECT t.id, t.tenant_id, t.config_name, t.config_key, t.config_value, t.config_type, t.remark, NOW(), NOW()
FROM (
    SELECT 4 AS id, 0 AS tenant_id, '管理平台界面名称' AS config_name, 'sys.platform.name' AS config_key, 'ai-mse 管理平台' AS config_value, 0 AS config_type, '管理平台首页左上角显示名称（系统内置，可改值不可删除）' AS remark FROM DUAL
    UNION ALL SELECT 5, 0, '登录策略-允许多端登录', 'sys.login.multiDevice', 'true', 0, 'false=单点登录（登录顶掉该账号全部旧会话）；租户可在自己参数里覆盖平台默认' FROM DUAL
    UNION ALL SELECT 6, 0, '登录策略-同端互斥', 'sys.login.sameDeviceExclusive', 'false', 0, 'true=同一设备类型仅保留最新会话（允许多端并存时生效）' FROM DUAL
    UNION ALL SELECT 7, 0, '多页签策略', 'sys.ui.tagsPolicy', 'optional', 0, 'optional=用户自选；fixed-on=强制开启；fixed-off=强制关闭（用户「主题设置」多页签开关的租户级管控，租户可在自己参数里覆盖平台默认）' FROM DUAL
    UNION ALL SELECT 10, 0, '登录安全-图形验证码模式', 'sys.login.captchaMode', 'risk', 0, 'none=关闭；risk=风控触发（同账号/同IP连续失败达阈值自动要求验证码，窗口10分钟）；always=每次登录；租户可在自己参数里覆盖平台默认' FROM DUAL
    UNION ALL SELECT 11, 0, '登录安全-验证码触发-同账号失败阈值', 'sys.login.captchaUserFailThreshold', '3', 0, '同账号连续登录失败达到该次数自动要求验证码（1~100）' FROM DUAL
    UNION ALL SELECT 12, 0, '登录安全-验证码触发-同IP失败阈值', 'sys.login.captchaIpFailThreshold', '10', 0, '同一IP连续登录失败达到该次数自动要求验证码（1~100）' FROM DUAL
    UNION ALL SELECT 13, 0, '登录安全-双因子认证模式', 'sys.login.totpMode', 'off', 0, 'off=关闭；optional=已绑定TOTP的用户登录需动态码（邮箱验证码可作降级）；required=强制全员（未绑定用户登录时先扫码绑定）' FROM DUAL
    UNION ALL SELECT 14, 0, '登录安全-邮箱动态码通道', 'sys.login.emailOtp', 'false', 0, 'true=开启双因子阶段的邮箱验证码降级通道（还需部署侧 spring.mail SMTP 配置且用户已填邮箱）' FROM DUAL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_config x WHERE x.id = t.id);

-- 门户首页模板（平台全局默认：欢迎+快捷入口；可选块内置 OA 待办/公众号概览/数据中台概览示例；主题背景库）
INSERT INTO sys_home_template (id, tenant_id, name, layout_json, status, remark, create_time, update_time)
SELECT t.id, t.tenant_id, t.name, t.layout_json, t.status, t.remark, NOW(), NOW()
FROM (SELECT 1 AS id, 0 AS tenant_id, '平台默认首页' AS name, '{"layout":[{"id":"w-welcome","type":"welcome","title":"欢迎","span":24}],"catalog":[{"id":"c-oa-todo","type":"list","title":"OA 待办","span":12,"permission":"oa:process:todo","api":"/oa/process/todo","params":{"pageNum":1,"pageSize":6},"fieldMap":{"title":"title","sub":"modelName","time":"startTime"},"link":"/sub/oa/detail/task/{taskId}?businessKey={businessKey}","refreshSeconds":60},{"id":"c-mp-stats","type":"stat","title":"公众号概览","span":12,"permission":"mp:stats:list","api":"/mp/stats/summary","metrics":[{"field":"accountTotal","label":"公众号"},{"field":"fanSubscribed","label":"订阅粉丝"},{"field":"messageToday","label":"今日消息"}]},{"id":"c-data-overview","type":"stat","title":"数据中台概览","span":12,"permission":"data:overview:list","api":"/data/overview","metrics":[{"field":"datasourceCount","label":"数据源"},{"field":"odsTableCount","label":"ODS 表"},{"field":"publishedApiCount","label":"已发布 API"},{"field":"todayApiCalls","label":"今日调用"}]}],"themes":[{"id":"t-light","name":"简约浅灰","bg":{"type":"color","value":"#f5f7fa"}},{"id":"t-blue","name":"晨曦蓝渐变","bg":{"type":"gradient","value":"linear-gradient(135deg,#dceeff 0%,#f5f7fa 55%,#e3f4e9 100%)"}},{"id":"t-dark","name":"静谧深蓝","bg":{"type":"color","value":"#1c2b3a"}}],"defaultThemeId":"t-light"}' AS layout_json, 0 AS status, '内置默认模板，租户可在首页配置中覆盖' AS remark FROM DUAL) t
WHERE NOT EXISTS (SELECT 1 FROM sys_home_template x WHERE x.id = t.id);
