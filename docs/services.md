# 基座服务积木与启动器

## 一、mse-service 四积木（SPI 自装配，控制器可开关）

| 积木 | 职责 | 开关 |
|---|---|---|
| service-system | 用户/组织/角色/菜单/字典/参数/日志审计/在线用户/子应用注册/租户域脚本推送接收端/租户数据源初始化（SharedMainBootstrap 建共享租户库） | `mse.service.system.controller.enabled` |
| service-platform | 平台控制面：租户管理（跨模块开通联动）、套餐与权益、凭证与离线交付、平台菜单（sys_tenant 系仅平台库存在，类级 `@Transactional(NOT_SUPPORTED)` 范式 SysTenantRouteServiceImpl） | `mse.service.platform.controller.enabled` |
| service-auth | 认证登录（PC device 与 mobile 会话同端互斥隔离；`/system/profile/me?client=2` 下发移动端视图） | `mse.service.auth.controller.enabled` |
| service-job | 薄调度中心（见 [scheduling.md](scheduling.md)） | `mse.service.job.controller.enabled` |

默认实现全部 `@ConditionalOnMissingBean(契约类型)` 可整体替换（如自定义 SysUserService、微服务形态 TenantRouteResolver）。

## 二、mse-boot 五启动器

| 启动器 | 应用名 | 端口 | config.import Data ID | 说明 |
|---|---|---|---|---|
| boot-gateway | mse-gateway | 48080 | mse-micro-common / mse-gateway | 网关（无 db/dubbo），路由基线含全部业务 |
| boot-system | mse-system | 48081 | common / db / dubbo / mse-system | 基座系统域 |
| boot-platform | mse-platform | 48082 | common / db / dubbo / mse-platform | 平台控制面 |
| boot-auth | mse-auth | 48083 | common / dubbo / mse-auth（无 db） | 纯认证（Dubbo 消费端） |
| boot-job | mse-job | 48084 | common / db / dubbo / mse-job | 薄调度中心 |

Nacos 地址 `${MSE_NACOS_ADDR:localhost:8848}`；管理端点 health/info/metrics/slowsql（探针带 X-Actuator-Token）。镜像构建见 [deploy.md](deploy.md)。

## 三、业务微服务端口全景

| 应用 | 端口 | | 应用 | 端口 |
|---|---|---|---|---|
| mse-oa（oa 仓） | 8090 | | mse-fs（modules 仓） | 8095 |
| mse-mp（mp 仓） | 8091 | | mse-search（modules 仓） | 8096 |
| mse-open（modules 仓） | 8092 | | mse-monitor（modules 仓） | 8097 |
| mse-data（modules 仓） | 8093 | | mse-mes（mes 仓） | 8098 |
| mse-ai（modules 仓） | 8094 | | mse-im（modules 仓） | 8099 |

## 四、消息通知（api-notify）

- 站内信/通知触达统一走 api-notify 契约（service-platform 提供平台侧实现）；业务积木依赖契约发送，不直连通知表。
- 消息中心/门户首页卡片由基座渲染（sys_notice / 门户卡片模型），业务侧只投递数据。

## 五、默认账号

| 账号 | 密码 | 端 |
|---|---|---|
| platform | 123456 | 平台端（tenant_id=0） |
| user / user123 | 123456 | 租户端（示例租户；直调管理接口 403 用于越权回归） |
