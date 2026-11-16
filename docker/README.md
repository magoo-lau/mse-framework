# docker/ —— 基础设施与配置基线（单一真源）

本目录是**全部拆分仓共享的基础设施单一真源**：MySQL / Redis / Nacos / RocketMQ / Seata / Elasticsearch 编排，以及微服务形态的 Nacos 共享基线配置。业务工程（mse-modules、mse-mes、mse-oa、mse-mp、mse-sample 各仓）本地运行时**复用本目录**，不在各自仓库复制基础设施。

## 目录结构

```
docker/
├── docker-compose.yml        # 中间件编排（见下表）
├── nacos/
│   ├── init-configs.sh       # 配置基线幂等导入（支持 --force / 定向 / NACOS_ADDR·GROUP·NAMESPACE·CONFIG_DIR）
│   └── config/               # 9 个基线 Data ID 留档（mse-micro-common/db/db-biz/dubbo + gateway/system/platform/auth/job）
├── mysql/                    # client-utf8mb4.cnf（防 docker exec 中文乱码）+ demo 示例租户脚本
├── dameng/ kingbase/         # 信创方言（README + demo 脚本）
├── rocketmq/broker.conf
├── elasticsearch/Dockerfile  # mse/elasticsearch:9.2.8-smartcn 镜像构建（smartcn 预装）
├── reset-mse-db.bat          # Windows 重置库脚本
└── backup/                   # 历史库快照（仅存档）
```

## 服务清单

| 服务 | 镜像 | 端口 | 用途 |
|---|---|---|---|
| mysql | mysql:8.4 | 3306 | 平台库 mse_platform（结构/种子已版本化进 service-platform jar，首启自动应用） |
| redis | redis:7-alpine | 6379 | 缓存/会话（宿主端口可经 MSE_REDIS_HOST_PORT 覆盖） |
| nacos | nacos-server:v3.0.0 | 8848/9848/18080 | 配置中心+注册（控制台 http://localhost:18080） |
| nacos-config-init | curlimages/curl | - | 一次性任务：Nacos 就绪后幂等导入 config/ 基线 |
| rocketmq-namesrv/broker | rocketmq:5.3.1 | 9876/10911 | MQ（按需） |
| seata | seata-server:2.6.0.jdk21 | 8091 | 分布式事务（按需） |
| elasticsearch | mse/elasticsearch:9.2.8-smartcn | 9200 | 搜索引擎（mse-search 默认引擎，按需） |

## 常用命令

```sh
cd docker

# 最小集（单体 mse-monolith 只需这两个）
docker compose up -d mysql redis

# 微服务形态全量
docker compose up -d nacos rocketmq-namesrv rocketmq-broker seata
docker compose up -d mysql redis nacos rocketmq-namesrv rocketmq-broker seata elasticsearch

# 只补搜索引擎
docker compose up -d elasticsearch

# 配置基线手动强制同步（默认幂等跳过已存在 Data ID）
NACOS_ADDR=localhost:8848 sh nacos/init-configs.sh --force
```

## 业务服务如何接入（跨仓约定）

业务启动器本地仅留「应用名 + spring.config.import Data ID 清单」，端口/数据源/Dubbo 等全在 Nacos：

1. 本目录 `docker compose up -d mysql redis nacos`（nacos-config-init 自动导入 9 个框架基线）。
2. 到业务仓执行其服务配置导入：`CONFIG_DIR=<业务仓>/docker/nacos/config sh <本仓>/docker/nacos/init-configs.sh`（只增量导入该业务自己的 Data ID，如 mse-oa.yaml）。
3. 回业务仓 `java -jar <biz>-boot/target/<biz>-boot-1.0.0-SNAPSHOT.jar`（网关 mse-gateway 路由基线已含全部业务路由）。

镜像构建：框架启动器 `sh mse-boot/build-images.sh`；业务启动器在各业务仓（每个 boot 自带 Dockerfile，context=该业务仓根），详见各仓 `docs/docker.md`。

## 端口全景

| 启动器 | 端口 | | 业务微服务 | 端口 |
|---|---|---|---|---|
| mse-gateway | 48080 | | mse-oa | 8090 |
| mse-system | 48081 | | mse-mp | 8091 |
| mse-platform | 48082 | | mse-open | 8092 |
| mse-auth | 48083 | | mse-data | 8093 |
| mse-job | 48084 | | mse-ai | 8094 |
| mse-monolith（sample 仓） | 8080 | | mse-fs | 8095 |
| | | | mse-search | 8096 |
| | | | mse-monitor | 8097 |
| | | | mse-mes（mes 仓） | 8098 |
| | | | mse-im | 8099 |
