# 契约积木（mse-api）与 RPC 规范

## 一、四份基座契约

| 契约 | 内容 | 典型消费方 |
|---|---|---|
| api-system | 鉴权数据源、组织/用户查询等系统域契约 | 各业务 service（经 starter-bridge 微服务回退 / 单体本地实现） |
| api-tenant | 租户路由（TenantRouteResolver）、租户控制面消费契约 | 各业务 service / 微服务形态 |
| api-fs | 文件存储 FsService（RustFS S3 兼容/本地降级语义）+ FsAssetUrls | 业务模块文件存储接口化（mse-fs 优先、无则本地降级） |
| api-notify | 消息通知（站内信/推送域） | oa/mp/mes 等需要触达的业务 |

契约 = 接口 + Serializable DTO；`api-*` 是跨模块依赖**唯一合法形态**（业务积木之间不依赖对方 service）。

## 二、RPC 双模式

- 实现类标 `@RpcService(契约.class)` 并由装配类 `@Bean + @ConditionalOnMissingBean` 注册。
- 消费方 `@RpcReference` 注入：单体 `mse.rpc.mode=local` 本地直调；微服务 `dubbo` 远程调用——**消费方代码零改动**。
- 微服务形态基座数据（鉴权数据源、组织查询、租户路由）由 starter-bridge 的 RPC 回退实现提供，业务服务不依赖基座 service jar。
- Dubbo 导出细节：多工具服务 boot 侧经 ai-api `DispatchingAiTool` 单门面导出（同 group 只能导出一个 RemoteAiTool，group=应用名）。

## 三、对外开放契约（mse-open 泛化分发）

- 业务能力开放给第三方 = 只动业务仓：自有 `xxx-api` 契约（机器语义：方法签名统一 `String 方法名(String requestJson)`、操作者显式携带 operatorUserId、租户经上下文隐式透传）+ 一个 `OpenApiContributor` Bean（code/name/path/httpMethod/target 清单）。
- mse-open 按目录 target 泛化分发（dubbo GenericService / 单体反射），应用就绪后目录自动推送（幂等 upsert + 周期重推自愈），管理端「API 目录」可启停即时生效。
- 先例：mse-oa（oa-api + OaOpenApiContributor）、mse-ai（契约复用仅 Contributor）。

## 四、文件存储接入标准（api-fs）

需要文件存储的业务模块：引 api-fs，模块存储接口化 + **双嵌套条件装配**：

- Fs 实现：`@Conditional(FsAvailableCondition.class)` 内 `@RpcReference`；
- Local 实现：`@Conditional(FsUnavailableCondition.class)`（取反）+ `@ConditionalOnMissingBean`——两侧条件必须互斥，勿依赖嵌套类声明顺序兜底；`afterName` 排在 `FsServiceAutoConfiguration` 之后。
- 引入/部署 mse-fs 即统一存储，未引入自动回落本地磁盘。先例：oa-form `OaFileStorage`、service-platform `DomainAssetStorage`、service-system `AvatarStorage`、ai-service `KbStorageService`。
- 登录页素材等免登录场景用 `accessLevel=public` 走公开素材通道 `/fs/asset/{tenantId}/{fileId}.{后缀}`（URL 自带租户世界段，`FsAssetUrls.publicAssetUrl` 规范拼法）。
