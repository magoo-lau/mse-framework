# 微服务运行

## 一、中间件（本仓 docker/ 是全部拆分仓的单一真源）

```sh
cd docker
docker compose up -d mysql redis nacos                    # 微服务最小集（nacos-config-init 自动导入基线）
docker compose up -d rocketmq-namesrv rocketmq-broker seata   # MQ / 分布式事务（按需）
docker compose up -d elasticsearch                        # 搜索引擎（mse-search 用，按需）
```

| 服务 | 端口 | 说明 |
|---|---|---|
| mysql 8.4 | 3306 | 平台库 mse_platform（结构/种子版本化进 service jar，首启自动应用） |
| redis 7 | 6379 | 宿主端口可经 `MSE_REDIS_HOST_PORT` 覆盖 |
| nacos v3 | 8848/9848/18080 | 控制台 http://localhost:18080 |
| rocketmq | 9876/10911 | namesrv + broker |
| seata 2.6 | 8091 | STORE_MODE=file |
| elasticsearch | 9200 | mse/elasticsearch:9.2.8-smartcn（本地构建，smartcn 预装） |

## 二、Nacos 配置管理

微服务启动器本地 `application.yaml` 只有「应用名 + spring.config.import」；真实配置在 Nacos，基线真源在本仓：

- **9 个基线 Data ID**（`docker/nacos/config/`）：mse-micro-common / mse-micro-db / mse-micro-db-biz / mse-micro-dubbo / mse-gateway / mse-system / mse-platform / mse-auth / mse-job。
- **导入**：`docker compose up -d nacos` 时 nacos-config-init 一次性任务自动幂等导入；手动：`NACOS_ADDR=localhost:8848 sh docker/nacos/init-configs.sh [--force]`。
- **业务服务专属配置**：在各业务仓 `<biz>-boot` 工程的 `docker/nacos/config/mse-<biz>.yaml` 留档，导入方式：`CONFIG_DIR=<业务仓>/docker/nacos/config sh <本仓>/docker/nacos/init-configs.sh`（幂等：已存在 Data ID 跳过，不覆盖线上修改；`--force` 强制同步基线）。
- 生效优先级：Nacos 远端 > 本地 application.yaml > 启动器内置默认。

## 三、启动顺序（基座四件 + 网关）

```sh
sh build.sh   # 先构建本仓
java -jar mse-boot/boot-system/target/boot-system-1.0.0-SNAPSHOT.jar     # 48081
java -jar mse-boot/boot-platform/target/boot-platform-1.0.0-SNAPSHOT.jar # 48082
java -jar mse-boot/boot-auth/target/boot-auth-1.0.0-SNAPSHOT.jar         # 48083
java -jar mse-boot/boot-gateway/target/boot-gateway-1.0.0-SNAPSHOT.jar   # 48080
java -jar mse-boot/boot-job/target/boot-job-1.0.0-SNAPSHOT.jar           # 48084（薄调度中心，按需）
```

随后按各业务仓文档启动业务服务（端口 8090-8099，见 [../docker/README.md](../docker/README.md) 端口全景表）。

## 四、网关路由

路由基线 `docker/nacos/config/mse-gateway.yaml` 已含全部业务（`Path=/<前缀>/** → lb://mse-<biz>`，lb 走 Nacos 服务发现）；免登录白名单 `mse.gateway.ignore-urls` 整体替换语义，按需在 Nacos 热更新。

## 五、单体模式（指向 mse-sample 仓）

单体聚合（mono-starter + 全部 biz 积木，`mse.rpc.mode=local`，仅需 MySQL+Redis）在 **mse-sample 仓 mse-monolith**——跨仓构建顺序见 [build.md](build.md)。
