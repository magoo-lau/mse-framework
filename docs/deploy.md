# 容器化部署（Docker / K8s）

## 一、镜像三通道

| 通道 | 适用 | 说明 |
|---|---|---|
| 各启动器目录 `Dockerfile` | 源码构建 | 多阶段：Maven 构建（BuildKit `.m2` 缓存挂载）→ Spring Boot tools jarmode 分层解包 → JRE 21 运行层（非 root、MaxRAMPercentage=75、exec java 优雅停机）。**context=本仓根**，源码自包含无需宿主 Maven/JDK |
| `mse-boot/Dockerfile.prebuilt` | 快速出镜像 | 本机已 `mvn package` 后，context=模块目录直接打运行层镜像 |
| `mse-boot/build-images.sh` | 批量/按名 | boot-gateway/system/platform/auth/job 五启动器；`IMAGE_REPO`/`VERSION`/`PUSH=1` 环境变量 |

```sh
sh mse-boot/build-images.sh                          # 全部（mse/xxx:1.0.0）
sh mse-boot/build-images.sh boot-system boot-auth    # 子集
IMAGE_REPO=registry.example.com/mse VERSION=v1.0.1 PUSH=1 sh mse-boot/build-images.sh
```

业务工程镜像（mse-modules 各 biz、mse-mes/mse-oa/mse-mp、mse-monolith）在**各自仓库**构建（boot 自带 Dockerfile，context=各自仓根），见各仓 `docker/README.md`。

## 二、compose 应用容器

本仓 `docker/docker-compose.yml` 只编排**中间件**；应用容器编排在各业务仓（如 mse-sample 仓的 mse-monolith 服务，`--profile apps`）。

## 三、K8s

- `mse-boot/k8s/configmap.yaml`：共享 mse-config（MSE_NACOS_ADDR 等，Pod 内经 Nacos 配置中的 MSE_* 占位符生效）——**全体系共享**，业务仓 k8s 清单复用。
- 业务仓各 `<biz>-boot/k8s/<biz>.yaml`：Deployment + Service（readiness/liveness exec 探针带 `X-Actuator-Token` 头、preStop sleep 缓冲、非 root、资源 request/limit）。
- 应用镜像自带 curl（temurin 21-jre）；探针端口 = 服务端口。
- Actuator 鉴权：服务端 `mse.actuator.auth.token`（见 mse-micro-common.yaml 基线）；空值 = 探针匿名可过，生产接 Secret 注入 `MSE_ACTUATOR_TOKEN`。

## 四、信创数据库（达梦 DM8 / 人大金仓 KingbaseES）

- 微服务切换：`--spring.profiles.active=dm|kingbase`——数据源/方言由共享 mse-micro-db.yaml 的 profile 段提供，业务基线无需改动。
- `docker/dameng/`、`docker/kingbase/`：方言 README + demo 示例脚本；三方言 JDBC 驱动已由 micro-starter 内置。
- 版本化脚本三方言目录：基座 service-system/service-platform 自带全量基线（db/mysql|dm|kingbase），业务积木按需提供。
