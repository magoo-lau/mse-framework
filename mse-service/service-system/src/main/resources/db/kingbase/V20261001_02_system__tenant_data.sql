-- mse-target: base
-- =====================================================================
-- 租户库·租户域种子（租户端菜单/权限点 + 字典/参数/首页默认模板 + 操作日志模块字典）
-- 归属：租户域（共享主库 + 各独立租户库自持副本）——随车队重放 / 懒初始化 BASE 通道自动应用
-- 只含租户域：平台账号/平台角色/平台部门/平台菜单/平台权限点一律不进本脚本（平台域归平台库，
--   由 service-platform 的 V20261001_03_platform__base_domain.sql 维护）
-- tenant_id=0 的字典/参数/首页模板行 = 租户域默认模板语义（租户侧解析回退默认值），非平台数据侵入
-- id 号段：见 zcode-plugin/plugins/mse-dev/references/alloc-tables.md
-- 幂等：按主键 NOT EXISTS 判重
-- =====================================================================

-- 租户域菜单（menu_scope=2、client=1 缺省；与平台库平台域菜单 200 段分离）
INSERT INTO sys_menu (id, parent_id, name, type, menu_scope, path, component, icon, sort, visible, status, create_time, update_time)
SELECT t.id, t.parent_id, t.name, t.type, t.menu_scope, t.path, t.component, t.icon, t.sort, t.visible, t.status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 1 AS id, 0 AS parent_id, '系统管理' AS name, 1 AS type, 2 AS menu_scope, '/system' AS path, NULL AS component, 'Setting' AS icon, 10 AS sort, 0 AS visible, 0 AS status
    UNION ALL SELECT 100, 1, '用户管理', 2, 2, 'user', 'User', 'User', 1, 0, 0
    UNION ALL SELECT 110, 1, '角色管理', 2, 2, 'role', 'system/Role', 'Avatar', 2, 0, 0
    UNION ALL SELECT 120, 1, '部门管理', 2, 2, 'dept', 'system/Dept', 'Grid', 3, 0, 0
    UNION ALL SELECT 130, 1, '岗位管理', 2, 2, 'post', 'system/Post', 'Postcard', 4, 0, 0
    UNION ALL SELECT 140, 1, '字典管理', 2, 2, 'dict', 'system/Dict', 'Collection', 5, 0, 0
    UNION ALL SELECT 150, 1, '参数设置', 2, 2, 'config', 'system/Config', 'Tools', 6, 0, 0
    UNION ALL SELECT 160, 1, '日志管理', 1, 2, 'log', NULL, 'Document', 7, 0, 0
    UNION ALL SELECT 161, 160, '操作日志', 2, 2, 'operlog', 'system/log/OperLog', 'Memo', 1, 0, 0
    UNION ALL SELECT 162, 160, '登录日志', 2, 2, 'logininfor', 'system/log/Logininfor', 'Tickets', 2, 0, 0
    UNION ALL SELECT 170, 1, '首页配置', 2, 2, 'home-layout', 'system/HomeLayout', 'Operation', 8, 0, 0
    UNION ALL SELECT 171, 1, '品牌个性化', 2, 2, 'tenant-branding', 'system/TenantBranding', 'Brush', 11, 0, 0
    UNION ALL SELECT 180, 1, '在线用户', 2, 2, 'online', 'system/OnlineUser', 'Connection', 9, 0, 0
    UNION ALL SELECT 190, 1, '登录安全设置', 2, 2, 'login-security', 'system/LoginSecurity', 'Lock', 10, 0, 0
    UNION ALL SELECT 195, 1, '任务调度', 2, 2, 'schedule', 'system/Schedule', 'Timer', 12, 0, 0
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.id = t.id);

-- 租户域权限点（用户/角色/部门/岗位/字典/参数/日志/首页/品牌/在线用户/登录安全 + 通用个人资料）
INSERT INTO sys_permission (id, menu_id, module, name, code, sort, status, create_time, update_time)
SELECT t.id, t.menu_id, t.module, t.name, t.code, t.sort, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 1001 AS id, 100 AS menu_id, 'system' AS module, '用户查询' AS name, 'system:user:list' AS code, 1 AS sort
    UNION ALL SELECT 1002, 100, 'system', '用户详情', 'system:user:query', 2
    UNION ALL SELECT 1003, 100, 'system', '用户新增', 'system:user:create', 3
    UNION ALL SELECT 1004, 100, 'system', '用户修改', 'system:user:update', 4
    UNION ALL SELECT 1005, 100, 'system', '用户删除', 'system:user:delete', 5
    UNION ALL SELECT 1006, 100, 'system', '重置密码', 'system:user:reset-password', 6
    UNION ALL SELECT 1007, 100, 'system', '分配角色', 'system:user:assign-role', 7
    UNION ALL SELECT 1008, 100, 'system', '用户导入', 'system:user:import', 8
    UNION ALL SELECT 1009, 100, 'system', '用户导出', 'system:user:export', 9
    UNION ALL SELECT 1010, 100, 'system', '重置双因子', 'system:user:reset-totp', 10
    UNION ALL SELECT 1101, 110, 'system', '角色查询', 'system:role:list', 1
    UNION ALL SELECT 1102, 110, 'system', '角色详情', 'system:role:query', 2
    UNION ALL SELECT 1103, 110, 'system', '角色新增', 'system:role:create', 3
    UNION ALL SELECT 1104, 110, 'system', '角色修改', 'system:role:update', 4
    UNION ALL SELECT 1105, 110, 'system', '角色删除', 'system:role:delete', 5
    UNION ALL SELECT 1106, 110, 'system', '角色授权', 'system:role:assign', 6
    UNION ALL SELECT 1201, 120, 'system', '部门查询', 'system:dept:list', 1
    UNION ALL SELECT 1202, 120, 'system', '部门新增', 'system:dept:create', 2
    UNION ALL SELECT 1203, 120, 'system', '部门修改', 'system:dept:update', 3
    UNION ALL SELECT 1204, 120, 'system', '部门删除', 'system:dept:delete', 4
    UNION ALL SELECT 1301, 130, 'system', '岗位查询', 'system:post:list', 1
    UNION ALL SELECT 1302, 130, 'system', '岗位详情', 'system:post:query', 2
    UNION ALL SELECT 1303, 130, 'system', '岗位新增', 'system:post:create', 3
    UNION ALL SELECT 1304, 130, 'system', '岗位修改', 'system:post:update', 4
    UNION ALL SELECT 1305, 130, 'system', '岗位删除', 'system:post:delete', 5
    UNION ALL SELECT 1401, 140, 'system', '字典查询', 'system:dict:list', 1
    UNION ALL SELECT 1402, 140, 'system', '字典新增', 'system:dict:create', 2
    UNION ALL SELECT 1403, 140, 'system', '字典修改', 'system:dict:update', 3
    UNION ALL SELECT 1404, 140, 'system', '字典删除', 'system:dict:delete', 4
    UNION ALL SELECT 1501, 150, 'system', '参数查询', 'system:config:list', 1
    UNION ALL SELECT 1502, 150, 'system', '参数新增', 'system:config:create', 2
    UNION ALL SELECT 1503, 150, 'system', '参数修改', 'system:config:update', 3
    UNION ALL SELECT 1504, 150, 'system', '参数删除', 'system:config:delete', 4
    UNION ALL SELECT 1601, 161, 'system', '操作日志查询', 'system:operlog:list', 1
    UNION ALL SELECT 1602, 161, 'system', '操作日志删除', 'system:operlog:delete', 2
    UNION ALL SELECT 1611, 162, 'system', '登录日志查询', 'system:logininfor:list', 1
    UNION ALL SELECT 1612, 162, 'system', '登录日志删除', 'system:logininfor:delete', 2
    UNION ALL SELECT 1701, 170, 'system', '首页模板查询', 'system:home-template:list', 1
    UNION ALL SELECT 1702, 170, 'system', '首页模板保存', 'system:home-template:save', 2
    UNION ALL SELECT 1703, 170, 'system', '首页模板恢复默认', 'system:home-template:reset', 3
    UNION ALL SELECT 1801, 180, 'system', '在线用户查询', 'system:online:list', 1
    UNION ALL SELECT 1802, 180, 'system', '强制下线', 'system:online:kick', 2
    UNION ALL SELECT 1901, 190, 'system', '登录安全查询', 'system:loginsecurity:query', 1
    UNION ALL SELECT 1902, 190, 'system', '登录安全保存', 'system:loginsecurity:update', 2
    UNION ALL SELECT 1911, 195, 'system', '任务查询', 'schedule:task:list', 1
    UNION ALL SELECT 1912, 195, 'system', '任务启停', 'schedule:task:toggle', 2
    UNION ALL SELECT 1913, 195, 'system', '任务立即触发', 'schedule:task:trigger', 3
    UNION ALL SELECT 1914, 195, 'system', '任务编辑', 'schedule:task:edit', 4
    UNION ALL SELECT 2001, 171, 'system', '品牌查询', 'system:branding:query', 1
    UNION ALL SELECT 2002, 171, 'system', '品牌保存', 'system:branding:update', 2
    UNION ALL SELECT 2901, NULL, 'system', '个人信息查询', 'system:profile:query', 1
    UNION ALL SELECT 2902, NULL, 'system', '个人信息修改', 'system:profile:update', 2
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = t.id OR p.code = t.code);

-- 字典类型/数据（tenant_id=0 默认模板：租户侧字典解析回退默认值；sys_oper_module 为操作日志模块翻译）
INSERT INTO sys_dict_type (id, tenant_id, dict_name, dict_type, status, remark, create_time, update_time)
SELECT t.id, t.tenant_id, t.dict_name, t.dict_type, t.status, t.remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 11 AS id, 0 AS tenant_id, '系统状态' AS dict_name, 'sys_normal_disable' AS dict_type, 0 AS status, '通用状态（正常/停用）' AS remark
    UNION ALL SELECT 12, 0, '操作日志系统模块', 'sys_oper_module', 0, '@OperLog(module) 英文代码 → 中文名（操作日志「系统模块」列翻译显示，未登记代码回退原值）'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_type x WHERE x.id = t.id);

INSERT INTO sys_dict_data (id, tenant_id, dict_type, dict_label, dict_value, dict_sort, list_class, is_default, status, remark, create_time, update_time)
SELECT t.id, t.tenant_id, t.dict_type, t.dict_label, t.dict_value, t.dict_sort, t.list_class, t.is_default, t.status, t.remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 21 AS id, 0 AS tenant_id, 'sys_normal_disable' AS dict_type, '正常' AS dict_label, '0' AS dict_value, 1 AS dict_sort, 'success' AS list_class, 1 AS is_default, 0 AS status, NULL AS remark
    UNION ALL SELECT 22, 0, 'sys_normal_disable', '停用', '1', 2, 'danger', 0, 0, NULL
    UNION ALL SELECT 23, 0, 'sys_oper_module', '用户管理', 'system:user', 1, NULL, 0, 0, NULL
    UNION ALL SELECT 24, 0, 'sys_oper_module', '角色管理', 'system:role', 2, NULL, 0, 0, NULL
    UNION ALL SELECT 25, 0, 'sys_oper_module', '部门管理', 'system:dept', 3, NULL, 0, 0, NULL
    UNION ALL SELECT 26, 0, 'sys_oper_module', '岗位管理', 'system:post', 4, NULL, 0, 0, NULL
    UNION ALL SELECT 27, 0, 'sys_oper_module', '字典管理', 'system:dict', 5, NULL, 0, 0, NULL
    UNION ALL SELECT 28, 0, 'sys_oper_module', '参数设置', 'system:config', 6, NULL, 0, 0, NULL
    UNION ALL SELECT 29, 0, 'sys_oper_module', '菜单管理', 'system:menu', 7, NULL, 0, 0, NULL
    UNION ALL SELECT 30, 0, 'sys_oper_module', '权限点管理', 'system:permission', 8, NULL, 0, 0, NULL
    UNION ALL SELECT 31, 0, 'sys_oper_module', '子应用管理', 'system:micro-app', 9, NULL, 0, 0, NULL
    UNION ALL SELECT 32, 0, 'sys_oper_module', '在线用户', 'system:online', 10, NULL, 0, 0, NULL
    UNION ALL SELECT 33, 0, 'sys_oper_module', '首页配置', 'system:home-template', 11, NULL, 0, 0, NULL
    UNION ALL SELECT 34, 0, 'sys_oper_module', '租户管理', 'tenant:domain', 12, NULL, 0, 0, NULL
    UNION ALL SELECT 35, 0, 'sys_oper_module', '品牌个性化', 'system:branding', 13, NULL, 0, 0, NULL
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data x WHERE x.id = t.id);

-- 参数配置（tenant_id=0 默认模板：界面名称/登录策略/多页签策略，租户可在自己参数里覆盖）
INSERT INTO sys_config (id, tenant_id, config_name, config_key, config_value, config_type, remark, create_time, update_time)
SELECT t.id, t.tenant_id, t.config_name, t.config_key, t.config_value, t.config_type, t.remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 4 AS id, 0 AS tenant_id, '管理平台界面名称' AS config_name, 'sys.platform.name' AS config_key, 'ai-mse 管理平台' AS config_value, 0 AS config_type, '管理平台首页左上角显示名称（系统内置，可改值不可删除）' AS remark
    UNION ALL SELECT 5, 0, '登录策略-允许多端登录', 'sys.login.multiDevice', 'true', 0, 'false=单点登录（登录顶掉该账号全部旧会话）；租户可在自己参数里覆盖默认值'
    UNION ALL SELECT 6, 0, '登录策略-同端互斥', 'sys.login.sameDeviceExclusive', 'false', 0, 'true=同一设备类型仅保留最新会话（允许多端并存时生效）'
    UNION ALL SELECT 7, 0, '多页签策略', 'sys.ui.tagsPolicy', 'optional', 0, 'optional=用户自选；fixed-on=强制开启；fixed-off=强制关闭（用户「主题设置」多页签开关的租户级管控，租户可在自己参数里覆盖默认值）'
    UNION ALL SELECT 10, 0, '登录安全-图形验证码模式', 'sys.login.captchaMode', 'risk', 0, 'none=关闭；risk=风控触发（同账号/同IP连续失败达阈值自动要求验证码，窗口10分钟）；always=每次登录；租户可在自己参数里覆盖默认值'
    UNION ALL SELECT 11, 0, '登录安全-验证码触发-同账号失败阈值', 'sys.login.captchaUserFailThreshold', '3', 0, '同账号连续登录失败达到该次数自动要求验证码（1~100）'
    UNION ALL SELECT 12, 0, '登录安全-验证码触发-同IP失败阈值', 'sys.login.captchaIpFailThreshold', '10', 0, '同一IP连续登录失败达到该次数自动要求验证码（1~100）'
    UNION ALL SELECT 13, 0, '登录安全-双因子认证模式', 'sys.login.totpMode', 'off', 0, 'off=关闭；optional=已绑定TOTP的用户登录需动态码（邮箱验证码可作降级）；required=强制全员（未绑定用户登录时先扫码绑定）'
    UNION ALL SELECT 14, 0, '登录安全-邮箱动态码通道', 'sys.login.emailOtp', 'false', 0, 'true=开启双因子阶段的邮箱验证码降级通道（还需部署侧 spring.mail SMTP 配置且用户已填邮箱）'
) t
WHERE NOT EXISTS (SELECT 1 FROM sys_config x WHERE x.id = t.id);

-- 门户首页默认模板（租户域默认行，租户可在首页配置中覆盖）
INSERT INTO sys_home_template (id, tenant_id, name, layout_json, status, remark, create_time, update_time)
SELECT t.id, t.tenant_id, t.name, t.layout_json, t.status, t.remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT 1 AS id, 0 AS tenant_id, '默认首页' AS name, '{"layout":[{"id":"w-welcome","type":"welcome","title":"欢迎","span":24}],"catalog":[{"id":"c-oa-todo","type":"list","title":"OA 待办","span":12,"permission":"oa:process:todo","api":"/oa/process/todo","params":{"pageNum":1,"pageSize":6},"fieldMap":{"title":"title","sub":"modelName","time":"startTime"},"link":"/sub/oa/detail/task/{taskId}?businessKey={businessKey}","refreshSeconds":60},{"id":"c-mp-stats","type":"stat","title":"公众号概览","span":12,"permission":"mp:stats:list","api":"/mp/stats/summary","metrics":[{"field":"accountTotal","label":"公众号"},{"field":"fanSubscribed","label":"订阅粉丝"},{"field":"messageToday","label":"今日消息"}]},{"id":"c-data-overview","type":"stat","title":"数据中台概览","span":12,"permission":"data:overview:list","api":"/data/overview","metrics":[{"field":"datasourceCount","label":"数据源"},{"field":"odsTableCount","label":"ODS 表"},{"field":"publishedApiCount","label":"已发布 API"},{"field":"todayApiCalls","label":"今日调用"}]}],"themes":[{"id":"t-light","name":"简约浅灰","bg":{"type":"color","value":"#f5f7fa"}},{"id":"t-blue","name":"晨曦蓝渐变","bg":{"type":"gradient","value":"linear-gradient(135deg,#dceeff 0%,#f5f7fa 55%,#e3f4e9 100%)"}},{"id":"t-dark","name":"静谧深蓝","bg":{"type":"color","value":"#1c2b3a"}}],"defaultThemeId":"t-light"}' AS layout_json, 0 AS status, '内置默认模板，租户可在首页配置中覆盖' AS remark) t
WHERE NOT EXISTS (SELECT 1 FROM sys_home_template x WHERE x.id = t.id);

COMMIT;
