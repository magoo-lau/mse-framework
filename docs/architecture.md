# 架构与积木体系

## 一、三层积木

```
框架区（本仓，只读） ──▶ 使用方工程（mse-modules / mse-mes / mse-oa / mse-mp 各仓）
                        ──▶ 示例参考（mse-sample 仓）
```

- **mse-dependencies**：第三方版本唯一仲裁 BOM。所有版本只在这里声明，业务工程经 `dependencyManagement import` 消费。
- **mse-common**：技术积木（common-core/web/mybatis/redis/rpc/mq/security/ai/mail）。**纯工具与通用能力，无自动装配、无 imports 文件**。
- **mse-starter**：自动装配件（starter-web/mybatis/security/rpc/seata/bridge）与形态入口启动器（mono/micro/mq/tenant/mail-starter）+ mse-starter-parent。装配类 FQCN 与工具同包名——业务工程引 starter 即得全部装配，零感知。
- **mse-api**：契约积木（api-system/api-tenant/api-fs/api-notify）。接口 + Serializable DTO，跨模块 RPC 唯一合法依赖。
- **mse-service**：基座业务积木（service-system/platform/auth/job）。RBAC、多租户控制面、认证、系统管理、薄调度中心，全部 SPI 自装配。
- **mse-boot**：微服务启动器（boot-gateway/system/platform/auth/job）。纯 `@SpringBootApplication`，Nacos + Dubbo 接入。
- **mse-ui / mse-ui-mobile / mse-ui-shared**：PC 基座（wujie 主应用）、移动基站、子应用公共运行时源码包。

## 二、SPI 接入标准（service 层硬性要求）

每个服务积木必须经 Spring Boot SPI 接入，启动器引 jar 即得全部能力（Controller/Service/Mapper 就绪）：

1. **两个自动装配类**（放模块 `autoconfigure` 包）：
   - `XxxServiceAutoConfiguration`：`@AutoConfiguration(before = MseMybatisAutoConfiguration.class)` + `@MapperScan(自己的 mapper 包, annotationClass = Mapper.class)` + `@ComponentScan` 非 controller 组件包（service/support/config/engine/mq/security 等）；可整体替换的契约实现用 `@Bean + @ConditionalOnMissingBean(契约类型)` 显式声明；类标 `@TenantServiceModule("<应用名>")`（服务身份声明，单体聚合时租户隔离策略零 yaml 生效）。
   - `XxxControllerAutoConfiguration`：`@AutoConfiguration` + `@ConditionalOnWebApplication(SERVLET)` + `@ConditionalOnProperty(prefix = "mse.service.<name>.controller", name = "enabled", matchIfMissing = true)` + `@ComponentScan(controller 包)`——保留"关闭默认控制器、自有控制器替代"开关。
2. `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 登记两个 FQCN。
3. **启动器一律纯 `@SpringBootApplication`**：无 scanBasePackages、无 @MapperScan；启动类放 `vip.mseapp.boot.<名>`（与积木包树零重叠）——SPI 是唯一装配入口，这也是开关与替换语义生效的前提。

## 三、依赖边界（check 项）

- 框架区各 pom **禁止依赖任何业务件**（demo-/mp-/oa-/im- 等）；common-* 禁止依赖 starter-*/service-*/boot-*。
- 跨模块只用契约：业务积木之间不依赖对方 service，只依赖对方 api 契约经 RPC 调用（单体 local 本地直调 / 微服务 dubbo 远程，消费方代码零改动）。
- 基座能力默认实现全部 `@ConditionalOnMissingBean(契约类型)`——使用方声明同契约 Bean 即整体替换；基座控制器可用 `mse.service.<name>.controller.enabled=false` 关闭后自有替代。

## 四、RPC 契约规范

- 契约放 `api-*` 模块：接口 + Serializable DTO；实现类标 `@RpcService(契约.class)` 并由装配类 `@Bean` 注册；消费方 `@RpcReference` 注入。
- 开放平台对外契约（机器语义）：方法签名统一 `String 方法名(String requestJson)`，操作者显式携带（operatorUserId），租户经上下文隐式透传。

## 五、形态与启动器选型

| 启动器 | 形态/能力 | 说明 |
|---|---|---|
| mono-starter | 单体聚合 | 已聚合基座三积木与全部装配件；`mse.rpc.mode=local`；仅需 MySQL/Redis |
| micro-starter | 微服务 | Nacos 配置/注册 + Dubbo + starter-bridge 契约消费桥接 + Seata 内置 + 三方言 JDBC 驱动；默认 `mse.rpc.mode=dubbo`（低位注入，显式配置可覆盖） |
| mq-starter | RocketMQ | 按需选装，与形态无关；未配时事件发送降级 noop |
| tenant-starter | 租户私有部署 | standalone + 授权凭证 + 离线交付导入端点 |
| mail-starter | 邮件 | 按需 |
