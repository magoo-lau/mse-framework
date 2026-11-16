# 多租户：路由、两库划分与权益

## 一、一切皆租户：四世界路由

`tenant_id` 即世界编号。路由判定全在基座拦截器（starter-web/starter-mybatis），业务代码零感知：

| 世界 | 判定 | 落库 |
|---|---|---|
| 平台（0） | 上下文 tenantId=0 | 平台库（出厂 mse_platform，免注入） |
| 独立库租户 | 租户数据源登记 | 专属库（免 tenant_id 注入） |
| 共享租户 | 缺省 | 绑定共享池 + tenant_id 行级注入 |
| 共享分表 | 分表登记 | 表名后缀（免注入） |

- 共享池自声明：`mse.tenant.shared-main.*`（每服务自 declare，键缺省=本服务应用名即服务私有、显式 `key:*`=通用）；亦可为 dsMode=shared 新建共享库并在「隔离策略」按服务绑定。0.1.0 起 primary=platform、无静态默认租户库，租户库全部动态注册。
- 平台库的业务表结构由各业务积木 `-- mse-target: base -- mse-base-scope: platform` 脚本建结构+基线种子。

## 二、业务端访问纪律（红线）

- 业务端**裸调或显式 `runAs` 锚定世界**；租户路径禁用 `runIgnore`。
- `runIgnore` 仅两类合法场景：平台登记源（平台库 0 行，兼作每租户播种来源）、跨租户全局视图/全局扫描。无显式 push 时被改道到本服务内置共享库（不会落平台库——后台跨库扫描首个世界是平台库时必须显式 `DynamicDataSourceContextHolder.push(平台库key)`）。
- **事务红线**：`@Transactional` 开启前必须已处于目标世界（doBegin 钉死物理连接）；事务内 `runAs` 无效。写操作一律 `@Transactional(rollbackFor = Exception.class)`；跨服务/跨库全局入口叠加 `@GlobalTransactional`（直接注解的 bean 必须组件扫描注册）；一个方法内跨多库路由禁止 `@Transactional`。
- 异步线程租户上下文自动传播（ThreadLocalAccessor + Reactor Hooks）；自建线程池一律经 `MseExecutors` 工厂（缺省 wrap + 有界队列），跨租户扫描执行域用 `*NoContext` 变体并注释理由。
- 无上下文凭证进门 = 标识符自带世界段（mse-im 回调 `/im/callback/{tenantId}/{token}`、mse-fs 素材 `/fs/asset/{tenantId}/...`、mp `/mp/callback/{tenantId}/{appId}` 范式）。
- 控制面服务（读写 sys_tenant 系）类级挂 `@Transactional(NOT_SUPPORTED)` 挂起调用方事务（范式 SysTenantRouteServiceImpl）。

## 三、平台/租户两库划分

- **平台库**（mse_platform）：控制面 sys_tenant 系 + 平台身份表/全局表/平台种子。**平台账号 platform（tenant_id=0）/平台菜单/平台侧数据只进平台库**。
- **租户库**：身份表直建通道（`-- mse-target: tenant`，零种子）；业务主库（master，缺省）随 `mse.tenant.schema.auto-migrate` 自动应用；租户域 base（缺省）= 基座全局表结构 + 菜单/权限点/默认模板种子 + 各 biz 注册——**租户库自持副本**（含分册账本 mse_<biz>_schema_history），由 mse-system 推送（启动一次+成功前退避重试）与懒初始化同通道覆盖新库。
- **红线**：base 通道脚本不得建/写基座身份表（sys_user/sys_role/sys_dept 等）；平台身份走 platform 通道、租户身份走 tenant 通道。
- 权威划分细则见 ai-mse 仓 `docs/tech/platform-tenant-divide.md`（历史存档）。

## 四、套餐与权益（entitlement）

- 租户功能范围由套餐裁剪：页面菜单（type=3）页面级粒度参与授权分组与套餐裁剪；`menu_id=NULL` 权限点=登录即授予（仅真正全局能力）。
- 平台端「套餐管理」分 PC/移动两页签（sys_menu/sys_micro_app 有 client 列：1=PC / 2=移动，独立菜单树独立裁剪）。
- 授权调整后缓存即时失效；租户端页面菜单需在套餐勾选才可见。

## 五、租户私有部署（tenant-starter）

- tenant-starter = standalone + 授权凭证 + 离线交付导入端点：租户侧单库模式运行，经离线授权与交付包开通。
- 交付管道：附件签发（license + 租户数据 JSONL + 租户变量 + schema，RS256 内层）→ deliver-studio「套件+附件」重打包 → deliver-console 安装（见 mse-deliver 仓 docs/）。
