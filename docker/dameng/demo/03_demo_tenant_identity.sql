-- =====================================================================
-- 可选示例租户·租户域数据（手动导入，不属于类 flyway 初始化链路；达梦 DM8）
--   内容：租户 1 的部门/岗位/角色/用户（admin/tenant123、user/user123）/授权/字典/参数演示数据
--   导入：disql 执行本脚本（先启动 mse-system：TENANT 通道已直建身份表、租户域种子已就绪）
--   验证：hosts 加 127.0.0.1 demo.mse.local 可体验域名锁定登录与登录页个性化（青绿渐变，需 02 已导入）
--   注意：脚本非幂等；信创单 schema：MSE schema 平台/租户合一，与 mysql 双库不同，导入同一 schema
-- =====================================================================
SET SCHEMA MSE;
-- 部门（租户 1 示例树）
INSERT INTO sys_dept (id, tenant_id, parent_id, ancestors, name, sort, status, create_time, update_time) VALUES
(1000, 1, 0,    '0',      '总公司',   1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1010, 1, 1000, '0,1000', '技术部',   1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1020, 1, 1000, '0,1000', '市场部',   2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 岗位（租户 1 示例：与用户挂载用）
INSERT INTO sys_post (id, tenant_id, post_code, post_name, post_sort, status, create_time, update_time) VALUES
(1, 1, 'ceo',  '董事长',   1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 'se',   '项目经理', 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 'hr',   '人力资源', 3, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, 'user', '普通员工', 4, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 角色（租户 1 普通用户）
INSERT INTO sys_role (id, tenant_id, name, code, sort, data_scope, status, remark, create_time, update_time) VALUES
(1, 1, '普通用户', 'common', 2, 4, 0, '查询为主，本部门及以下数据范围', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 用户（租户 1）
INSERT INTO sys_user (id, tenant_id, dept_id, username, password, nickname, status, super_admin, create_time, update_time) VALUES
(2, 1, 1000, 'admin', '$2a$10$OUQapwD84lrQG.44mZb.4.bd0rTlC.Tb6tn2HuRzIHJd0HbjyaCMq', '租户管理员', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, 1010, 'user',  '$2a$10$my1UFgFc.lZdqlwUhzoYj..mF51EDD3t57UUrK4Tsz7ZGS.I33wuq', '普通用户',   0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 用户-角色（租户 1）
INSERT INTO sys_user_role (id, tenant_id, user_id, role_id, create_time, update_time) VALUES
(2, 1, 4, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 普通用户角色：可见菜单与权限点（查询为主）
INSERT INTO sys_role_menu (id, tenant_id, role_id, menu_id, create_time, update_time) VALUES
(1, 1, 1, 1,   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 1, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 1, 110, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO sys_role_permission (id, tenant_id, role_id, permission_id, create_time, update_time) VALUES
(1, 1, 1, 1001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 1, 1002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, 1, 1101, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, 1, 1102, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 字典类型（租户 1 示例）
INSERT INTO sys_dict_type (id, tenant_id, dict_name, dict_type, status, remark, create_time, update_time) VALUES
(1, 1, '系统状态', 'sys_normal_disable', 0, '通用状态（正常/停用）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, '用户性别', 'sys_user_sex',       0, '用户性别列表',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, '系统是否', 'sys_yes_no',         0, '系统是否列表',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 1, '菜单状态', 'sys_show_hide',      0, '菜单显示/隐藏',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 1, '登录状态', 'sys_common_status',  0, '登录成功/失败',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 字典数据（租户 1 示例）
INSERT INTO sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, dict_sort, list_class, is_default, status, remark, create_time, update_time) VALUES
(1,  1, 'sys_normal_disable', '正常', '0', 1, 'success', 1, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2,  1, 'sys_normal_disable', '停用', '1', 2, 'danger',  0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3,  1, 'sys_user_sex',       '男',   '0', 1, 'primary', 1, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4,  1, 'sys_user_sex',       '女',   '1', 2, 'danger',  0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5,  1, 'sys_user_sex',       '未知', '2', 3, 'info',    0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6,  1, 'sys_yes_no',         '是',   'Y', 1, 'primary', 0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7,  1, 'sys_yes_no',         '否',   'N', 2, 'info',    0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8,  1, 'sys_show_hide',      '显示', '0', 1, 'success', 1, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9,  1, 'sys_show_hide',      '隐藏', '1', 2, 'info',    0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 1, 'sys_common_status',  '成功', '0', 1, 'success', 1, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 1, 'sys_common_status',  '失败', '1', 2, 'danger',  0, 0, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 参数配置（租户 1 示例；config_type=0 内置不可删除，仅可改值）
INSERT INTO sys_config (id, tenant_id, config_name, config_key, config_value, config_type, remark, create_time, update_time) VALUES
(1, 1, '用户管理-初始密码', 'sys.user.initPassword', '123456',   0, '用户导入/新增未填密码时使用', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, '主框架页-皮肤样式', 'sys.index.skinName',    'default',  1, '界面主题预留配置',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 1, '租户端界面名称',   'sys.site.name',     'ai-mse 租户工作台', 0, '租户端首页左上角显示名称（系统内置，可改值不可删除）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 1, '登录策略-允许多端登录', 'sys.login.multiDevice', 'true', 0, '示例租户覆盖：false=单点登录（登录顶掉该账号全部旧会话）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 1, '登录策略-同端互斥', 'sys.login.sameDeviceExclusive', 'false', 0, '示例租户覆盖：true=同一设备类型仅保留最新会话', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 1, '多页签策略', 'sys.ui.tagsPolicy', 'optional', 0, '示例租户覆盖：optional=用户自选；fixed-on=强制开启；fixed-off=强制关闭', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

COMMIT;
