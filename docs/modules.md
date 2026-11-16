# common-* 与 starter-* 模块参考

## 一、common-*（技术积木，纯工具、无自动装配）

| 模块 | 要点 |
|---|---|
| common-core | TenantContext（世界上下文）/ MseExecutors（线程池工厂：缺省 wrap+有界队列+命名，`*NoContext` 变体）/ TenantTaskGates（并发闸门 tryRun/tryAcquire+release）/ 通用工具 |
| common-web | web 层通用（租户上下文传播：ThreadLocalAccessor + Reactor Hooks.enableAutomaticContextPropagation） |
| common-mybatis | 租户行级过滤/分表路由的 MyBatis-Plus 支撑 |
| common-redis | Redis 访问与键约定 |
| common-rpc | @RpcService/@RpcReference 与 Dubbo/local 双模式支撑 |
| common-mq | MQ 抽象（TenantEvent 等；未配 RocketMQ 时事件发送降级 noop） |
| common-security | Sa-Token 集成与鉴权数据源契约 |
| common-ai | Spring AI 2.x 通用支撑 |
| common-mail | 邮件能力 |

## 二、starter-*（自动装配件与启动器）

**装配件**（业务工程引 jar 即得装配，工具本体经传递依赖带入）：

| 模块 | 装配内容 |
|---|---|
| starter-web | web 基线 + 租户上下文透传（Dubbo/HTTP 过滤器、@Async TaskDecorator）+ actuator（health/info/metrics/slowsql，mse.actuator.auth.token 鉴权） |
| starter-mybatis | 多数据源 + TenantRouteTransactionManager（doBegin 按租户上下文切库）+ 动态数据源注册 |
| starter-security | Sa-Token 会话/鉴权（PC device 与 mobile 会话同端互斥隔离） |
| starter-rpc | Dubbo local/dubbo 双模式（mse.rpc.mode 由形态启动器低位注入默认值） |
| starter-bridge | 微服务形态基座契约消费桥接（鉴权数据源/组织查询/租户路由的 RPC 回退实现 + 租户域脚本推送）；单体形态基座本地实现存在时自动让位 |
| starter-seata | @GlobalTransactional 编译期依赖（optional；运行期由 micro-starter 内置提供） |

**形态启动器**（使用方选型即引入整个形态）：

| 启动器 | 作用 |
|---|---|
| mono-starter | 单体形态入口：聚合基座三积木与全部装配件；`mse.rpc.mode=local` |
| micro-starter | 微服务形态入口：Nacos 配置/注册 + Dubbo + starter-bridge + Seata 内置 + actuator + 三方言 JDBC 驱动；`mse.rpc.mode=dubbo` |
| mq-starter | RocketMQ 按需（单体/微服务均可选装，与形态无关） |
| tenant-starter | 租户私有部署：standalone + 授权凭证 + 离线交付导入端点 |
| mail-starter | 邮件按需 |
| mse-starter-parent | 业务工程统一父 pom（java21/编译插件/spring-boot 插件/基座 BOM/仓库配置；`<relativePath/>` 仓库解析） |

## 三、使用要点

- 业务工程固定依赖四件：starter-web / starter-mybatis / starter-security / starter-rpc。
- 直接写 `@GlobalTransactional` 的模块编译期引 starter-seata（optional）。
- 显式配置（yaml/Nacos）永远可覆盖启动器注入的默认值。
