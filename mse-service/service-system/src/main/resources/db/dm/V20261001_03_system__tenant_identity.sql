-- mse-target: tenant
-- =====================================================================
-- 基座身份表直建脚本（TENANT 通道，达梦）
-- 用途：租户库/共享库「脚本直建」——TenantDsLazyInitializer 初始化目标租户库时执行，
--       替代原「从主库克隆表结构」机制（克隆已退役）。含全部 BaseEntity 身份表 + undo_log。
-- 幂等：全段 IF NOT EXISTS，可重复执行；账本 mse_system_schema_history（target=tenant 分段）
-- 结构要点：sys_dept 带 leader_user_id（部门负责人，IM 部门群群主依据）；
--       与平台库身份表段（service-platform V20261001_03_platform__base_domain.sql）同构——结构变更两处同步
-- =====================================================================

-- ---------------------------- 部门（租户表，树形） ----------------------------
CREATE TABLE IF NOT EXISTS sys_dept
(
    id          BIGINT       NOT NULL,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    ancestors   VARCHAR(500) NOT NULL DEFAULT '',
    name        VARCHAR(60)  NOT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 0,
    leader_user_id BIGINT    NULL,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_dept_tenant_parent ON sys_dept (tenant_id, parent_id);
COMMENT ON TABLE sys_dept IS '部门';
COMMENT ON COLUMN sys_dept.tenant_id IS '租户编号（0=平台）';
COMMENT ON COLUMN sys_dept.ancestors IS '祖级列表（逗号分隔，如 0,1000；用于本部门及以下展开）';
COMMENT ON COLUMN sys_dept.id IS '编号';
COMMENT ON COLUMN sys_dept.parent_id IS '父部门编号（0=根）';
COMMENT ON COLUMN sys_dept.name IS '部门名称';
COMMENT ON COLUMN sys_dept.sort IS '排序号';
COMMENT ON COLUMN sys_dept.status IS '0 启用 1 停用';
COMMENT ON COLUMN sys_dept.leader_user_id IS '部门负责人（sys_user.id，空=未设置；IM 部门群群主依据）';
COMMENT ON COLUMN sys_dept.create_by IS '创建者';
COMMENT ON COLUMN sys_dept.create_time IS '创建时间';
COMMENT ON COLUMN sys_dept.update_by IS '更新者';
COMMENT ON COLUMN sys_dept.update_time IS '更新时间';
COMMENT ON COLUMN sys_dept.deleted IS '逻辑删除';

-- ---------------------------- 用户（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_user
(
    id          BIGINT        NOT NULL,
    tenant_id   BIGINT        NOT NULL DEFAULT 0,
    dept_id     BIGINT        NULL,
    username    VARCHAR(30)   NOT NULL,
    password    VARCHAR(100)  NOT NULL,
    nickname    VARCHAR(30)   NULL,
    mobile      VARCHAR(20)   NULL,
    email       VARCHAR(60)   NULL,
    avatar      VARCHAR(255)  NULL,
    status      TINYINT       NOT NULL DEFAULT 0,
    super_admin TINYINT       NOT NULL DEFAULT 0,
    totp_secret VARCHAR(64)   NULL,
    create_by   BIGINT        NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT        NULL,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_user_tenant_username ON sys_user (tenant_id, username);
CREATE INDEX idx_user_tenant_dept ON sys_user (tenant_id, dept_id);
COMMENT ON TABLE sys_user IS '用户';
COMMENT ON COLUMN sys_user.tenant_id IS '租户编号（0=平台）';
COMMENT ON COLUMN sys_user.dept_id IS '部门编号（数据权限锚点）';
COMMENT ON COLUMN sys_user.password IS 'BCrypt';
COMMENT ON COLUMN sys_user.status IS '0 启用 1 停用';
COMMENT ON COLUMN sys_user.super_admin IS '租户管理员标志（权限=套餐域内全量；平台端=全量 *）';
COMMENT ON COLUMN sys_user.totp_secret IS 'TOTP 双因子密钥（Base32；空=未绑定）';
COMMENT ON COLUMN sys_user.id IS '编号';
COMMENT ON COLUMN sys_user.username IS '登录账号';
COMMENT ON COLUMN sys_user.nickname IS '用户昵称';
COMMENT ON COLUMN sys_user.mobile IS '手机号码';
COMMENT ON COLUMN sys_user.email IS '邮箱地址';
COMMENT ON COLUMN sys_user.avatar IS '头像地址';
COMMENT ON COLUMN sys_user.create_by IS '创建者';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_by IS '更新者';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
COMMENT ON COLUMN sys_user.deleted IS '逻辑删除';

-- ---------------------------- 角色（租户表；tenant_id=0 为平台角色） ----------------------------
CREATE TABLE IF NOT EXISTS sys_role
(
    id          BIGINT       NOT NULL,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    name        VARCHAR(60)  NOT NULL,
    code        VARCHAR(60)  NOT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    data_scope  TINYINT      NOT NULL DEFAULT 1,
    status      TINYINT      NOT NULL DEFAULT 0,
    remark      VARCHAR(255) NULL,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_role_tenant_code ON sys_role (tenant_id, code);
COMMENT ON TABLE sys_role IS '角色';
COMMENT ON COLUMN sys_role.data_scope IS '数据范围 1 全部 2 自定义 3 本部门 4 本部门及以下 5 仅本人';
COMMENT ON COLUMN sys_role.id IS '编号';
COMMENT ON COLUMN sys_role.tenant_id IS '租户编号（0=平台）';
COMMENT ON COLUMN sys_role.name IS '角色名称';
COMMENT ON COLUMN sys_role.code IS '角色编码';
COMMENT ON COLUMN sys_role.sort IS '排序号';
COMMENT ON COLUMN sys_role.status IS '0 启用 1 停用';
COMMENT ON COLUMN sys_role.remark IS '备注';
COMMENT ON COLUMN sys_role.create_by IS '创建者';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.update_by IS '更新者';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';
COMMENT ON COLUMN sys_role.deleted IS '逻辑删除';

-- ---------------------------- 用户-角色（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_role
(
    id          BIGINT    NOT NULL,
    tenant_id   BIGINT    NOT NULL DEFAULT 0,
    user_id     BIGINT    NOT NULL,
    role_id     BIGINT    NOT NULL,
    create_by   BIGINT    NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT    NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT   NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_user_role_tenant_user ON sys_user_role (tenant_id, user_id);
CREATE INDEX idx_user_role_role ON sys_user_role (tenant_id, role_id);
COMMENT ON TABLE sys_user_role IS '用户角色关联';
COMMENT ON COLUMN sys_user_role.id IS '编号';
COMMENT ON COLUMN sys_user_role.tenant_id IS '租户编号（0=平台）';
COMMENT ON COLUMN sys_user_role.user_id IS '用户编号';
COMMENT ON COLUMN sys_user_role.role_id IS '角色编号';
COMMENT ON COLUMN sys_user_role.create_by IS '创建者';
COMMENT ON COLUMN sys_user_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_user_role.update_by IS '更新者';
COMMENT ON COLUMN sys_user_role.update_time IS '更新时间';
COMMENT ON COLUMN sys_user_role.deleted IS '逻辑删除';

-- ---------------------------- 角色-菜单（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_menu
(
    id          BIGINT    NOT NULL,
    tenant_id   BIGINT    NOT NULL DEFAULT 0,
    role_id     BIGINT    NOT NULL,
    menu_id     BIGINT    NOT NULL,
    create_by   BIGINT    NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT    NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT   NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_role_menu_role ON sys_role_menu (tenant_id, role_id);
COMMENT ON TABLE sys_role_menu IS '角色菜单关联';
COMMENT ON COLUMN sys_role_menu.id IS '编号';
COMMENT ON COLUMN sys_role_menu.tenant_id IS '租户编号（0=平台）';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色编号';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单编号';
COMMENT ON COLUMN sys_role_menu.create_by IS '创建者';
COMMENT ON COLUMN sys_role_menu.create_time IS '创建时间';
COMMENT ON COLUMN sys_role_menu.update_by IS '更新者';
COMMENT ON COLUMN sys_role_menu.update_time IS '更新时间';
COMMENT ON COLUMN sys_role_menu.deleted IS '逻辑删除';

-- ---------------------------- 角色-权限点（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission
(
    id             BIGINT    NOT NULL,
    tenant_id      BIGINT    NOT NULL DEFAULT 0,
    role_id        BIGINT    NOT NULL,
    permission_id  BIGINT    NOT NULL,
    create_by      BIGINT    NULL,
    create_time    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by      BIGINT    NULL,
    update_time    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        TINYINT   NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_role_permission_role ON sys_role_permission (tenant_id, role_id);
CREATE INDEX idx_role_permission_perm ON sys_role_permission (tenant_id, permission_id);
COMMENT ON TABLE sys_role_permission IS '角色权限点关联';
COMMENT ON COLUMN sys_role_permission.id IS '编号';
COMMENT ON COLUMN sys_role_permission.tenant_id IS '租户编号（0=平台）';
COMMENT ON COLUMN sys_role_permission.role_id IS '角色编号';
COMMENT ON COLUMN sys_role_permission.permission_id IS '权限点编号';
COMMENT ON COLUMN sys_role_permission.create_by IS '创建者';
COMMENT ON COLUMN sys_role_permission.create_time IS '创建时间';
COMMENT ON COLUMN sys_role_permission.update_by IS '更新者';
COMMENT ON COLUMN sys_role_permission.update_time IS '更新时间';
COMMENT ON COLUMN sys_role_permission.deleted IS '逻辑删除';

-- ---------------------------- 角色-部门（租户表；data_scope=2 自定义时的部门集合） ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_dept
(
    id          BIGINT    NOT NULL,
    tenant_id   BIGINT    NOT NULL DEFAULT 0,
    role_id     BIGINT    NOT NULL,
    dept_id     BIGINT    NOT NULL,
    create_by   BIGINT    NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT    NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT   NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_role_dept_role ON sys_role_dept (tenant_id, role_id);
COMMENT ON TABLE sys_role_dept IS '角色自定义数据部门';
COMMENT ON COLUMN sys_role_dept.id IS '编号';
COMMENT ON COLUMN sys_role_dept.tenant_id IS '租户编号（0=平台）';
COMMENT ON COLUMN sys_role_dept.role_id IS '角色编号';
COMMENT ON COLUMN sys_role_dept.dept_id IS '部门编号';
COMMENT ON COLUMN sys_role_dept.create_by IS '创建者';
COMMENT ON COLUMN sys_role_dept.create_time IS '创建时间';
COMMENT ON COLUMN sys_role_dept.update_by IS '更新者';
COMMENT ON COLUMN sys_role_dept.update_time IS '更新时间';
COMMENT ON COLUMN sys_role_dept.deleted IS '逻辑删除';

-- ---------------------------- 岗位（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_post
(
    id          BIGINT       NOT NULL,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    post_code   VARCHAR(60)  NOT NULL,
    post_name   VARCHAR(60)  NOT NULL,
    post_sort   INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 0,
    remark      VARCHAR(255) NULL,
    create_by   BIGINT       NULL,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT       NULL,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_post_tenant_code ON sys_post (tenant_id, post_code);
COMMENT ON TABLE sys_post IS '岗位';
COMMENT ON COLUMN sys_post.post_code IS '岗位编码（租户内唯一，代码层校验）';
COMMENT ON COLUMN sys_post.post_name IS '岗位名称';
COMMENT ON COLUMN sys_post.post_sort IS '显示顺序';
COMMENT ON COLUMN sys_post.status IS '0 正常 1 停用';
COMMENT ON COLUMN sys_post.id IS '编号';
COMMENT ON COLUMN sys_post.remark IS '备注';
COMMENT ON COLUMN sys_post.create_by IS '创建者';
COMMENT ON COLUMN sys_post.create_time IS '创建时间';
COMMENT ON COLUMN sys_post.update_by IS '更新者';
COMMENT ON COLUMN sys_post.update_time IS '更新时间';
COMMENT ON COLUMN sys_post.deleted IS '逻辑删除';
COMMENT ON COLUMN sys_post.tenant_id IS '租户编号（0=平台）';

-- ---------------------------- 用户-岗位（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_post
(
    id          BIGINT    NOT NULL,
    tenant_id   BIGINT    NOT NULL DEFAULT 0,
    user_id     BIGINT    NOT NULL,
    post_id     BIGINT    NOT NULL,
    create_by   BIGINT    NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT    NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT   NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_user_post_user ON sys_user_post (tenant_id, user_id);
CREATE INDEX idx_user_post_post ON sys_user_post (tenant_id, post_id);
COMMENT ON TABLE sys_user_post IS '用户岗位关联';
COMMENT ON COLUMN sys_user_post.id IS '编号';
COMMENT ON COLUMN sys_user_post.tenant_id IS '租户编号（0=平台）';
COMMENT ON COLUMN sys_user_post.user_id IS '用户编号';
COMMENT ON COLUMN sys_user_post.post_id IS '岗位编号';
COMMENT ON COLUMN sys_user_post.create_by IS '创建者';
COMMENT ON COLUMN sys_user_post.create_time IS '创建时间';
COMMENT ON COLUMN sys_user_post.update_by IS '更新者';
COMMENT ON COLUMN sys_user_post.update_time IS '更新时间';
COMMENT ON COLUMN sys_user_post.deleted IS '逻辑删除';

-- ---------------------------- 字典类型（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_dict_type
(
    id          BIGINT        NOT NULL,
    tenant_id   BIGINT        NOT NULL DEFAULT 0,
    dict_name   VARCHAR(60)   NOT NULL,
    dict_type   VARCHAR(100)  NOT NULL,
    status      TINYINT       NOT NULL DEFAULT 0,
    remark      VARCHAR(255)  NULL,
    create_by   BIGINT        NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT        NULL,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_dict_type_tenant_type ON sys_dict_type (tenant_id, dict_type);
COMMENT ON TABLE sys_dict_type IS '字典类型';
COMMENT ON COLUMN sys_dict_type.dict_name IS '字典名称';
COMMENT ON COLUMN sys_dict_type.dict_type IS '字典类型（租户内唯一，代码层校验）';
COMMENT ON COLUMN sys_dict_type.status IS '0 正常 1 停用';
COMMENT ON COLUMN sys_dict_type.id IS '编号';
COMMENT ON COLUMN sys_dict_type.remark IS '备注';
COMMENT ON COLUMN sys_dict_type.create_by IS '创建者';
COMMENT ON COLUMN sys_dict_type.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict_type.update_by IS '更新者';
COMMENT ON COLUMN sys_dict_type.update_time IS '更新时间';
COMMENT ON COLUMN sys_dict_type.deleted IS '逻辑删除';
COMMENT ON COLUMN sys_dict_type.tenant_id IS '租户编号（0=平台）';

-- ---------------------------- 字典数据（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_dict_data
(
    id          BIGINT        NOT NULL,
    tenant_id   BIGINT        NOT NULL DEFAULT 0,
    dict_type   VARCHAR(100)  NOT NULL,
    dict_label  VARCHAR(60)   NOT NULL,
    dict_value  VARCHAR(100)  NOT NULL,
    dict_sort   INT           NOT NULL DEFAULT 0,
    list_class  VARCHAR(30)   NULL,
    is_default  TINYINT       NOT NULL DEFAULT 0,
    status      TINYINT       NOT NULL DEFAULT 0,
    remark      VARCHAR(255)  NULL,
    create_by   BIGINT        NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by   BIGINT        NULL,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_dict_data_tenant_type ON sys_dict_data (tenant_id, dict_type);
COMMENT ON TABLE sys_dict_data IS '字典数据';
COMMENT ON COLUMN sys_dict_data.dict_label IS '数据标签';
COMMENT ON COLUMN sys_dict_data.dict_value IS '数据键值';
COMMENT ON COLUMN sys_dict_data.dict_sort IS '显示顺序';
COMMENT ON COLUMN sys_dict_data.list_class IS '标签样式（default/primary/success/info/warning/danger）';
COMMENT ON COLUMN sys_dict_data.is_default IS '是否默认 0 否 1 是';
COMMENT ON COLUMN sys_dict_data.status IS '0 正常 1 停用';
COMMENT ON COLUMN sys_dict_data.id IS '编号';
COMMENT ON COLUMN sys_dict_data.remark IS '备注';
COMMENT ON COLUMN sys_dict_data.create_by IS '创建者';
COMMENT ON COLUMN sys_dict_data.create_time IS '创建时间';
COMMENT ON COLUMN sys_dict_data.update_by IS '更新者';
COMMENT ON COLUMN sys_dict_data.update_time IS '更新时间';
COMMENT ON COLUMN sys_dict_data.deleted IS '逻辑删除';
COMMENT ON COLUMN sys_dict_data.tenant_id IS '租户编号（0=平台）';
COMMENT ON COLUMN sys_dict_data.dict_type IS '字典类型';

-- ---------------------------- 参数配置（租户表） ----------------------------
CREATE TABLE IF NOT EXISTS sys_config
(
    id           BIGINT        NOT NULL,
    tenant_id    BIGINT        NOT NULL DEFAULT 0,
    config_name  VARCHAR(60)   NOT NULL,
    config_key   VARCHAR(100)  NOT NULL,
    config_value VARCHAR(500)  NOT NULL,
    config_type  TINYINT       NOT NULL DEFAULT 1,
    remark       VARCHAR(255)  NULL,
    create_by    BIGINT        NULL,
    create_time  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by    BIGINT        NULL,
    update_time  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      TINYINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_config_tenant_key ON sys_config (tenant_id, config_key);
COMMENT ON TABLE sys_config IS '参数配置';
COMMENT ON COLUMN sys_config.config_name IS '参数名称';
COMMENT ON COLUMN sys_config.config_key IS '参数键名（租户内唯一，代码层校验）';
COMMENT ON COLUMN sys_config.config_value IS '参数键值';
COMMENT ON COLUMN sys_config.config_type IS '0 系统内置 1 自定义';
COMMENT ON COLUMN sys_config.id IS '编号';
COMMENT ON COLUMN sys_config.remark IS '备注';
COMMENT ON COLUMN sys_config.create_by IS '创建者';
COMMENT ON COLUMN sys_config.create_time IS '创建时间';
COMMENT ON COLUMN sys_config.update_by IS '更新者';
COMMENT ON COLUMN sys_config.update_time IS '更新时间';
COMMENT ON COLUMN sys_config.deleted IS '逻辑删除';
COMMENT ON COLUMN sys_config.tenant_id IS '租户编号（0=平台）';

-- ---------------------------- 站内消息（租户表，接收人个人数据） ----------------------------
CREATE TABLE IF NOT EXISTS sys_message
(
    id          BIGINT       NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    receiver_id BIGINT       NOT NULL,
    title       VARCHAR(255) NULL,
    content     VARCHAR(500) NULL,
    biz_id      VARCHAR(64)  NULL,
    read_flag   TINYINT      NOT NULL DEFAULT 0,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
CREATE INDEX idx_message_tenant_receiver ON sys_message (tenant_id, receiver_id, read_flag);
COMMENT ON TABLE sys_message IS '站内消息';
COMMENT ON COLUMN sys_message.type IS '消息类型（业务自定义，如 TODO/CC/RESULT/REMIND）';
COMMENT ON COLUMN sys_message.receiver_id IS '接收人';
COMMENT ON COLUMN sys_message.biz_id IS '业务关联标识（如流程实例编号）';
COMMENT ON COLUMN sys_message.read_flag IS '0 未读 1 已读';
COMMENT ON COLUMN sys_message.tenant_id IS '租户编号（0=平台）';
COMMENT ON COLUMN sys_message.id IS '编号';
COMMENT ON COLUMN sys_message.title IS '消息标题';
COMMENT ON COLUMN sys_message.content IS '消息内容';
COMMENT ON COLUMN sys_message.create_time IS '创建时间';
COMMENT ON COLUMN sys_message.deleted IS '逻辑删除';

-- ---------------------------- 操作日志（租户表，仅插入） ----------------------------
CREATE TABLE IF NOT EXISTS sys_oper_log
(
    id             BIGINT        NOT NULL,
    tenant_id      BIGINT        NOT NULL DEFAULT 0,
    module         VARCHAR(30)   NOT NULL,
    oper_type      VARCHAR(20)   NULL,
    method         VARCHAR(200)  NULL,
    request_method VARCHAR(10)   NULL,
    oper_url       VARCHAR(255)  NULL,
    oper_param     VARCHAR(2000) NULL,
    oper_user_id   BIGINT        NULL,
    oper_username  VARCHAR(30)   NULL,
    oper_ip        VARCHAR(60)   NULL,
    cost_ms        INT           NULL,
    status         TINYINT       NOT NULL DEFAULT 0,
    error_msg      VARCHAR(2000) NULL,
    oper_time      TIMESTAMP     NULL,
    deleted        TINYINT       NOT NULL DEFAULT 0,
    create_time    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_oper_log_tenant_time ON sys_oper_log (tenant_id, oper_time);
COMMENT ON TABLE sys_oper_log IS '操作日志';
COMMENT ON COLUMN sys_oper_log.module IS '业务模块（如 system:user）';
COMMENT ON COLUMN sys_oper_log.oper_type IS '操作类型（新增/修改/删除/导入/导出等）';
COMMENT ON COLUMN sys_oper_log.method IS '方法名（类.方法）';
COMMENT ON COLUMN sys_oper_log.request_method IS '请求方式（GET/POST/PUT/DELETE）';
COMMENT ON COLUMN sys_oper_log.oper_url IS '请求 URL';
COMMENT ON COLUMN sys_oper_log.oper_param IS '请求参数（JSON，超长截断）';
COMMENT ON COLUMN sys_oper_log.oper_user_id IS '操作人员编号';
COMMENT ON COLUMN sys_oper_log.oper_username IS '操作人员账号';
COMMENT ON COLUMN sys_oper_log.oper_ip IS '操作 IP';
COMMENT ON COLUMN sys_oper_log.cost_ms IS '耗时（毫秒）';
COMMENT ON COLUMN sys_oper_log.status IS '0 成功 1 失败';
COMMENT ON COLUMN sys_oper_log.error_msg IS '错误消息（超长截断）';
COMMENT ON COLUMN sys_oper_log.oper_time IS '操作时间';
COMMENT ON COLUMN sys_oper_log.id IS '编号';
COMMENT ON COLUMN sys_oper_log.deleted IS '逻辑删除';
COMMENT ON COLUMN sys_oper_log.create_time IS '创建时间';
COMMENT ON COLUMN sys_oper_log.tenant_id IS '租户编号（0=平台）';

-- ---------------------------- 登录日志（租户表，仅插入） ----------------------------
CREATE TABLE IF NOT EXISTS sys_logininfor
(
    id          BIGINT       NOT NULL,
    tenant_id   BIGINT       NOT NULL DEFAULT 0,
    username    VARCHAR(30)  NOT NULL,
    ip          VARCHAR(60)  NULL,
    browser     VARCHAR(60)  NULL,
    os          VARCHAR(60)  NULL,
    status      TINYINT      NOT NULL DEFAULT 0,
    msg         VARCHAR(255) NULL,
    login_time  TIMESTAMP    NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_logininfor_tenant_time ON sys_logininfor (tenant_id, login_time);
COMMENT ON TABLE sys_logininfor IS '登录日志';
COMMENT ON COLUMN sys_logininfor.username IS '登录账号';
COMMENT ON COLUMN sys_logininfor.ip IS '登录 IP';
COMMENT ON COLUMN sys_logininfor.browser IS '浏览器';
COMMENT ON COLUMN sys_logininfor.os IS '操作系统';
COMMENT ON COLUMN sys_logininfor.status IS '0 成功 1 失败';
COMMENT ON COLUMN sys_logininfor.msg IS '提示消息';
COMMENT ON COLUMN sys_logininfor.login_time IS '登录时间';
COMMENT ON COLUMN sys_logininfor.id IS '编号';
COMMENT ON COLUMN sys_logininfor.deleted IS '逻辑删除';
COMMENT ON COLUMN sys_logininfor.create_time IS '创建时间';
COMMENT ON COLUMN sys_logininfor.tenant_id IS '租户编号（0=平台；租户不存在时的失败记录也落 0）';

-- ---------------------------- Seata AT 回滚日志（全局表，M4 使用） ----------------------------
CREATE TABLE IF NOT EXISTS undo_log
(
    branch_id     BIGINT        NOT NULL,
    xid           VARCHAR(128)  NOT NULL,
    context       VARCHAR(128)  NOT NULL,
    rollback_info BLOB          NOT NULL,
    log_status    INT           NOT NULL,
    log_created   TIMESTAMP(6)  NOT NULL,
    log_modified  TIMESTAMP(6)  NOT NULL,
    CONSTRAINT ux_undo_log UNIQUE (xid, branch_id)
);
COMMENT ON TABLE undo_log IS 'Seata AT 模式回滚日志';
COMMENT ON COLUMN undo_log.branch_id IS '分支事务ID';
COMMENT ON COLUMN undo_log.xid IS '全局事务ID';
COMMENT ON COLUMN undo_log.context IS '上下文';
COMMENT ON COLUMN undo_log.rollback_info IS '回滚信息';
COMMENT ON COLUMN undo_log.log_status IS '0 正常 1 全局已完成';
COMMENT ON COLUMN undo_log.log_created IS '创建时间';
COMMENT ON COLUMN undo_log.log_modified IS '修改时间';

COMMENT ON TABLE sys_tenant_snapshot IS '租户快照（控制面下发，数据面本地缓存持久层）';
