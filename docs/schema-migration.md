# db/ 版本化脚本规范

业务 SQL 不再维护独立 sql/ 目录，改为 `<module>/src/main/resources/db/<方言>/` 版本化幂等脚本，**jar 自带、免手工执行、免登记**。

## 一、命名与账本

```
V{yyyyMMdd}_{NN}_<biz>__<描述>.sql
```

- **biz 段必须为积木名**——路由分册账本 `mse_<biz>_schema_history`，同 biz 内版本字典序即时间序。
- 文件名不匹配的脚本不会被扫描；**已应用脚本不再修改**（checksum 冲突），变更一律新增版本文件。
- 方言目录：mysql / dm / kingbase（基座 service-system/service-platform 自带三方言全量基线；业务积木按需提供）。

## 二、头指令（目标与粒度）

| 指令 | 含义 |
|---|---|
| `-- mse-target: master`（缺省） | 业务主库 + 各租户独立库（业务表 DDL + 示例数据） |
| `-- mse-target: base` | 基座全局表结构 + 种子（配合 `-- mse-base-scope`） |
| `-- mse-target: tenant` | 租户库身份表直建通道（零种子） |
| `-- mse-target: platform` | 平台库专用（控制面 sys_tenant 系 + 平台身份表/全局表/平台种子，service-platform 专用） |
| `-- mse-per-tenant: true` | 每租户种子脚本，按 (script, tenant) 记账 |
| `-- mse-base-scope: platform|tenant` | 缺省 tenant：平台域落平台库、租户域落租户库车队 |

## 三、执行通道

- `mse.tenant.schema.auto-migrate`（默认开启）随服务启动应用 master 脚本（失败阻断启动；多实例 Redis 锁按库互斥）。
- 租户库经「租户数据源初始化/懒初始化」直建：TENANT 身份表 + MASTER 业务表 + BASE base 副本三通道自动就绪。
- 租户域脚本双路分发：平台库目录摄入（真源）+ 登记库车队重放（`TenantFleetBaseReplayer`）落缺省共享库与各独立租户库，靠脚本幂等收敛。
- **红线**：base 通道脚本不得建/写基座身份表（sys_user/sys_role/sys_dept 等，扫描器护栏 WARN）——平台身份走 platform 通道、租户身份走 tenant 通道。

## 四、幂等写法要求

- `CREATE TABLE IF NOT EXISTS`；种子 `WHERE NOT EXISTS` 判重；兼容修复用条件 UPDATE。
- 权限点一律挂靠工程自己的页面菜单（`menu_id` 指向 sys_menu type=3）才参与授权分组与套餐裁剪；`menu_id=NULL` 即通用权限（登录即授予）。
- id 分段统一登记在 mse-agent-plugin 仓 `plugins/mse-dev/references/alloc-tables.md`（先查后用、用后登记）。
