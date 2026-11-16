# AGENTS.md —— mse-framework 框架仓协作约定

本文件面向所有在本仓库工作的 AI 助手与开发者。本仓库自 ai-mse 单仓（源 commit `cc5410a3`）拆分独立，是全部业务工程的积木底座，**对使用方只读**。

## 一、仓库定位与结构

```
mse-framework/
├── pom.xml                 # 自持根 pom（吸收原单仓根 pom：坐标/BOM import/公共依赖/插件管理）
├── mse-dependencies/       # 统一版本仲裁 BOM（所有第三方版本只在此声明）
├── mse-common/             # 技术积木：common-core/web/mybatis/redis/rpc/mq/security/ai/mail（纯工具，无自动装配）
├── mse-starter/            # 装配件：starter-web/mybatis/security/rpc/seata/bridge + mono/micro/mq/tenant/mail 启动器 + mse-starter-parent
├── mse-api/                # 契约积木：api-system/api-tenant/api-fs/api-notify（跨服务 RPC 唯一合法依赖）
├── mse-service/            # 基座业务积木：service-system/platform/auth/job（RBAC、多租户控制面、认证、系统管理、薄调度中心）
├── mse-boot/               # 微服务启动器：boot-gateway/system/platform/auth/job + build-images.sh + k8s/configmap
├── mse-ui/                 # 前端基座（Vue3 + wujie 微前端主应用，租户端 dist/tenant + 平台端 dist/platform 双模式）
├── mse-ui-mobile/          # 移动基站（Vue3 + Vant 4，base=/m/）
├── mse-ui-shared/          # 子应用公共运行时源码包（file: 源码直出：请求工厂/auth store/权限指令/主题桥接/Pagination/CronInput）
└── docker/                 # 部署脚本：docker-compose、Nacos 基线配置 + init-configs.sh、信创方言、ES 镜像构建
```

## 二、积木原则（红线）

1. **只沉淀与具体业务无关的基础能力**：RBAC、多租户、认证、系统管理（mse-service）、技术组件工具与自动装配（mse-common + mse-starter）、契约（mse-api）、微服务启动器（mse-boot）、前端基座。业务功能、示例/演示功能一律不进本仓（示例在 mse-sample 仓，业务在 mse-modules / mse-mes / mse-oa / mse-mp 各仓）。
2. **用不到的能力不裁剪**：禁止为"去掉不需要的功能"修改或删除本仓内容——用不到就不引入（后端不依赖 jar、前端不登记子应用）。
3. **工具与装配严格分离**：`common-*` 只放工具/通用能力（无 `AutoConfiguration`、无 imports 文件）；自动装配一律放 `starter-*`（装配类 FQCN 与工具同包名，业务工程零感知）。`mse.rpc.mode` 由形态启动器决定：micro-starter 默认 dubbo、mono-starter 保持 local，显式配置可覆盖。
4. **默认实现全部可替换**：service 模块各默认实现带 `@ConditionalOnMissingBean(契约类型)`；控制器开关 `mse.service.<system|platform|auth|job>.controller.enabled`（业务积木同款 `mse.service.<biz>.controller.enabled`）。

## 三、service 层 SPI 接入标准（本仓基座与全部业务积木一致，硬性要求）

1. 两个自动装配类（放模块 `autoconfigure` 包）：
   - `XxxServiceAutoConfiguration`：`@AutoConfiguration(before = MseMybatisAutoConfiguration.class)` + `@MapperScan(mapper 包, annotationClass = Mapper.class)` + `@ComponentScan` 非 controller 组件包；可整体替换的契约实现用 `@Bean + @ConditionalOnMissingBean` 显式声明；服务身份声明 `@TenantServiceModule("<应用名>")`。
   - `XxxControllerAutoConfiguration`：`@ConditionalOnWebApplication(SERVLET)` + `@ConditionalOnProperty(prefix = "mse.service.<name>.controller", name = "enabled", matchIfMissing = true)` + `@ComponentScan(controller 包)`。
2. `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 登记两个 FQCN。
3. 启动器一律纯 `@SpringBootApplication`（无 scanBasePackages、无 @MapperScan），启动类放 `vip.mseapp.boot.<名>`——SPI 是唯一装配入口。
4. RPC 契约：接口 + Serializable DTO 放 api 模块；实现标 `@RpcService(契约.class)` 并由装配类 `@Bean` 注册；消费方 `@RpcReference`（单体 local 本地直调 / 微服务 dubbo 远程，消费方零改动）。

## 四、租户与事务红线（最高优先级）

- **tenant_id 即世界编号**：平台=0（平台库专属）、独立库租户→专属库、共享租户→绑定共享池+tenant_id 注入、共享分表→后缀。路由判定全在基座拦截器，业务端裸调或显式 `runAs` 锚定世界；`runIgnore` 仅限平台登记源与跨租户全局视图/扫描两类场景。
- **写操作**一律 `@Transactional(rollbackFor = Exception.class)`；跨服务/跨库全局入口再叠加 `@GlobalTransactional`（@GlobalTransactional bean 必须组件扫描注册，不能 AutoConfiguration 工厂方法）。一个方法内跨多库路由禁止 `@Transactional`。
- **事务开启前必须已处于目标世界**：`@Transactional` 方法内再 `runAs` 写库无效（doBegin 已钉死连接）；跨租户后台扫描禁止 @Transactional 包裹。
- 自建线程池经 common-core `MseExecutors` 工厂；跨租户扫描执行域用 `*NoContext` 变体并注释理由。

## 五、版本一致性（跨仓同步点）

`spring-boot.version` 需同步四处：**本仓 pom.xml / mse-dependencies / mse-starter-parent** + **mse-sample 仓 mse-sample/pom.xml**（跨仓！升级基座版本时两个仓库一起改）。第三方版本唯一仲裁在 mse-dependencies。

## 六、构建、镜像与验证

- `sh build.sh`（缺省 skipTests，`MSE_RUN_ALL_TESTS=1` 放开）；`sh build-ui.sh`（ui-shared → ui → mobile）。
- 镜像：`sh mse-boot/build-images.sh`（boot-gateway/system/platform/auth/job 五启动器，各模块目录自带 Dockerfile，context=本仓根；Dockerfile.prebuilt 供本机已 package 快速出镜像）。业务工程镜像在各业务仓构建，基础设施（MySQL/Redis/Nacos/RocketMQ/Seata/ES）compose 真源在本仓 `docker/`。
- 验证底线：`mvn install` 通过（建议含测试）；mse-ui / mse-ui-mobile `npm run type-check` 零错误；涉租户/权限行为按 docs/verification.md 人工回归。

## 七、文档与提交纪律

- 改动框架区能力同步更新 `README.md` 与 `docs/`；历史记录类文档按当时事实不改写。
- 每次变更完成即 commit 一次（含新增文件）。
- 退役类/机制改名：全仓 grep 清理活文本残留，退役标识符登记到拆分源头仓（ai-mse）`scripts/arch-retired.txt` 的口径不再适用本仓，本仓以 docs 内约定为准并保持命名一致。
