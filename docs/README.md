# mse-framework 文档索引

框架区积木底座的技术文档（最终态；本仓自 ai-mse 单仓拆分，历史设计快照保留在 ai-mse 仓 docs/ 不再随行）。

## 入门

| 文档 | 内容 |
|---|---|
| [../README.md](../README.md) | 仓库概览、模块结构、快速开始 |
| [architecture.md](architecture.md) | 积木体系：三区结构、SPI 接入标准、依赖边界、工具与装配分离 |
| [build.md](build.md) | 构建脚本（build.sh / build-ui.sh）、版本一致性、跨仓构建顺序、CI |
| [run-microservices.md](run-microservices.md) | 微服务运行：中间件 compose、Nacos 配置管理、启动顺序、网关路由 |
| [deploy.md](deploy.md) | 容器化部署：镜像三通道、K8s、信创数据库 |

## 领域专题

| 文档 | 内容 |
|---|---|
| [tenancy.md](tenancy.md) | 多租户：四世界路由、平台/租户两库划分、套餐权益、租户私有部署 |
| [schema-migration.md](schema-migration.md) | db/ 版本化脚本规范（类 Flyway、方言、通道与粒度） |
| [scheduling.md](scheduling.md) | 任务调度中心：体系结构、接入标准、闸门与热更 |

## 模块参考

| 文档 | 内容 |
|---|---|
| [modules.md](modules.md) | common-*（9 件工具积木）与 starter-*（装配件与形态启动器）清单与要点 |
| [api.md](api.md) | 契约积木 api-system / api-tenant / api-fs / api-notify 与 RPC 规范 |
| [services.md](services.md) | 基座 service-system / platform / auth / job 与六启动器、消息通知 |
| [frontend.md](frontend.md) | 前端基座 mse-ui / mse-ui-mobile / mse-ui-shared 与子应用拼法、微前端约定 |

## 跨仓约定（重要）

本仓是拆分体系的**底座与基础设施单一真源**：业务工程在 mse-modules 仓（7 工程）与 mse-mes / mse-oa / mse-mp 仓；示例与模板在 mse-sample 仓；交付工具在 mse-deliver / mse-collect-kit 仓；开发者插件在 mse-agent-plugin 仓。`spring-boot.version` 升级需同步本仓三处 + mse-sample 仓一处（见 [build.md](build.md)）。
