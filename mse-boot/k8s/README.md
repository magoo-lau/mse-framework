# mse-boot K8s 部署清单

微服务各启动器的清单**随模块存放**（每个 boot 目录自含 Dockerfile + k8s/，按需取用、任意组合）：

| 服务 | Dockerfile | K8s 清单 | 说明 |
|---|---|---|---|
| 网关 | `mse-framework/mse-boot/boot-gateway/Dockerfile` | `mse-framework/mse-boot/boot-gateway/k8s/gateway.yaml` | Deployment + Service（NodePort 30808，对外入口） |
| 系统管理 | `mse-framework/mse-boot/boot-system/Dockerfile` | `mse-framework/mse-boot/boot-system/k8s/system.yaml` | 仅 Deployment：Dubbo 以 Pod IP 注册 Nacos，无需 K8s Service |
| 租户管理（平台控制面） | `mse-framework/mse-boot/boot-platform/Dockerfile` | `mse-framework/mse-boot/boot-platform/k8s/platform.yaml` | 同上 |
| 认证 | `mse-framework/mse-boot/boot-auth/Dockerfile` | `mse-framework/mse-boot/boot-auth/k8s/auth.yaml` | 同上 |

共享环境配置 `configmap.yaml` 在本目录（所有启动器经 envFrom 注入）；单体示例清单在
`mse-sample/mse-monolith/k8s/monolith.yaml`，业务工程示例在 `mse-sample/mse-demo/demo-boot/k8s/demo.yaml`。

## 前置条件

- 中间件（MySQL / Redis / Nacos / RocketMQ / Seata）在集群内可达，或使用集群外地址。
  地址统一在 `configmap.yaml` 中配置，按实际环境修改（默认指向集群内同名 Service）。
- 已构建并推送镜像（k8s 节点需能拉取）：

```bash
IMAGE_REPO=registry.example.com/mse VERSION=v1.0.1 PUSH=1 ./mse-framework/mse-boot/build-images.sh
# 只构建指定启动器：... build-images.sh boot-system boot-auth
# 然后把各 yaml 中的 image: mse/boot-xxx:1.0.0 改为 registry.example.com/mse/boot-xxx:v1.0.1（单体镜像名为 mse/mse-monolith）
# 或用 kubectl set image / kustomize images 覆盖
```

## 部署

```bash
# 微服务模式——全量（gateway 对外，其余经 Nacos 注册发现）
kubectl apply -f mse-framework/mse-boot/k8s/configmap.yaml \
              -f mse-framework/mse-boot/boot-system/k8s/system.yaml \
              -f mse-framework/mse-boot/boot-platform/k8s/platform.yaml \
              -f mse-framework/mse-boot/boot-auth/k8s/auth.yaml \
              -f mse-framework/mse-boot/boot-gateway/k8s/gateway.yaml

# 微服务模式——按需子集（各清单相互独立，用哪个 apply 哪个；configmap 需随同/先行 apply。
# 注意子集能否支撑完整业务取决于服务间依赖，如登录链路依赖 auth + system）
kubectl apply -f mse-framework/mse-boot/k8s/configmap.yaml \
              -f mse-framework/mse-boot/boot-system/k8s/system.yaml \
              -f mse-framework/mse-boot/boot-gateway/k8s/gateway.yaml

# 单体模式（只需 mysql/redis；清单在示例区）
kubectl apply -f mse-framework/mse-boot/k8s/configmap.yaml -f mse-sample/mse-monolith/k8s/monolith.yaml

# 业务工程示例（mse-demo，local 模式仅依赖 mysql/redis）
kubectl apply -f mse-framework/mse-boot/k8s/configmap.yaml -f mse-sample/mse-demo/demo-boot/k8s/demo.yaml

kubectl -n <ns> get pods,svc
```

## 说明

- **发现机制**：system/platform/auth 是 Dubbo(tri) 提供者，向 Nacos 注册的是
  **Pod IP**（`dubbo.protocol.port=-1` 随机端口），集群内天然直连，因此它们不需要
  K8s Service；只有 gateway / monolith / demo 是 HTTP 入口，提供了 Service。
- **健康探针**：全部清单统一 exec + curl 访问 `/actuator/health` 并携带
  `X-Actuator-Token: ${MSE_ACTUATOR_TOKEN}` 请求头（各 Deployment 已注入 `MSE_ACTUATOR_TOKEN`
  环境变量，默认空 = 服务端 actuator 鉴权关闭、探针匿名可过；生产接 Secret 注入，与
  `mse.actuator.auth.token` 同键，见共享 `mse-micro-common.yaml`）。微服务启动器已引入
  spring-boot-starter-actuator（各 boot 模块 `application.yaml` 暴露 health,info）；
  网关白名单与下游注解式鉴权均已放行 `/actuator/**`。
- **优雅停机**：容器内 java 为 PID 1 直接接收 SIGTERM；`preStop sleep 5`
  为 Nacos 摘除流量留缓冲，`terminationGracePeriodSeconds: 45` 覆盖整个下线窗口。
- **JVM 内存**：默认 `-XX:MaxRAMPercentage=75.0`，按容器 memory limit 自适应，无需手写 -Xmx。
- **非 root**：镜像内以 uid 1001 运行，满足 `runAsNonRoot` 安全基线。
- **密码类配置**：示例将数据库口令放在 ConfigMap 便于演示，生产请改用 Secret。
