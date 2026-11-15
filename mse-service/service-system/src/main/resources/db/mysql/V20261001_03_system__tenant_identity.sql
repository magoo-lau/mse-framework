-- mse-target: tenant
-- =====================================================================
-- 基座身份表直建脚本（TENANT 通道，MySQL）
-- 用途：租户库/共享库「脚本直建」——TenantDsLazyInitializer 初始化目标租户库时执行。
--       含全部 BaseEntity 身份表 + undo_log。
-- 幂等：全段 IF NOT EXISTS，可重复执行；账本 mse_system_schema_history（target=tenant 分段）
-- 结构要点：sys_dept 带 leader_user_id（部门负责人，IM 部门群群主依据）；
--       与平台库身份表段（service-platform V20261001_03_platform__base_domain.sql）同构——结构变更两处同步
-- =====================================================================

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

