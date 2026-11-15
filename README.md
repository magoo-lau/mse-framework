# mse-framework —— 框架区（积木底座）

对使用方**只读**的底座：RBAC 权限、多租户、认证、系统管理、技术工具与自动装配（mse-common + mse-starter）、契约、微服务启动器、前端基座与部署脚本。业务/示例代码禁止进入本区（示例在 `mse-sample/`，业务在 `mse-modules/`）。

| 目录 | 内容 |
| --- | --- |
| `mse-dependencies/` | 统一版本仲裁 BOM（所有第三方版本只在此处声明） |
| `mse-common/` | 技术工具（纯工具/通用能力，无自动装配）：common-core / web / mybatis / redis / rpc / mq / security / ai |
| `mse-starter/` | 启动器区：starter-* 内部件（自动装配，工具本体在 mse-common）+ mono/micro/mq-starter 使用方入口 + mse-starter-parent 统一父 pom |
| `mse-api/` | 契约积木：api-system、api-tenant、api-fs（跨服务 RPC 唯一合法依赖） |
| `mse-service/` | 业务积木：service-system / tenant / auth（SPI 自装配，默认实现可被同契约 Bean 覆盖） |
| `mse-boot/` | 微服务启动器：boot-gateway / auth / system / tenant（Nacos + Dubbo，本地仅 application.yaml 经 spring.config.import 接入 Nacos） |
| `mse-ui/` | 前端基座（Vue3 + wujie 微前端主应用，租户端/平台端双模式构建） |
| `docker/` | docker-compose（MySQL/Redis/Nacos/RocketMQ/Seata/Elasticsearch）、MySQL/达梦/金仓初始化 SQL、Nacos 配置留档与写入脚本 |

构建：仓库根目录 `mvn install -DskipTests`（只聚合本框架区；mse-modules 各业务工程与 mse-sample 均独立构建，顺序：框架区 → mse-modules → mse-sample）。模块命名与 SPI 接入标准见根目录 [AGENTS.md](../AGENTS.md)。
