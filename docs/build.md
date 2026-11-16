# 构建与 CI

## 一、本仓构建

```sh
sh build.sh                       # mvn install（缺省 -DskipTests）
MSE_RUN_ALL_TESTS=1 sh build.sh   # 放开单元测试
sh build-ui.sh                    # 前端三工程：mse-ui-shared → mse-ui → mse-mobile（npm ci + type-check + build）
sh build-ui.sh mse-ui             # 子集构建
```

- build-ui.sh 清单顺序有语义：mse-ui-shared 必须排在消费者之前（file: 源码直出）。
- type-check 门禁试点：mse-ui / mse-ui-mobile / mse-ui-shared 三工程。
- Windows 本地注意：dev server 运行中会锁 native 二进制，npm ci 前先停 dev server。

## 二、版本一致性（跨仓同步点）

`spring-boot.version`（当前 4.0.8）需同步**四处**：

| 位置 | 仓库 |
|---|---|
| `pom.xml` properties | 本仓 |
| `mse-dependencies` | 本仓 |
| `mse-starter/mse-starter-parent` | 本仓 |
| `mse-sample/pom.xml` properties | **mse-sample 仓** |

第三方版本唯一仲裁在 mse-dependencies（BOM import，属性不继承传递）；根 pom 只保留自身插件管理消费的 spring-boot.version。

## 三、跨仓构建顺序

```
1. mse-framework（本仓）  sh build.sh
2. mse-modules 仓         sh build.sh     # ai/data/fs/im/monitor/open/search
3. mse-mp / mse-oa / mse-mes 仓  sh build.sh
4. mse-sample 仓          sh build.sh     # mse-monolith 聚合全部 biz 发布件，必须最后
```

上游任何变更后，下游仓重新构建即可（全部经本地/私服仓库坐标解析，无文件系统路径依赖）。

## 四、CI（.github/workflows/ci.yml）

- **backend**：temurin 21 + maven 缓存，`mvn -B -ntp install`（含测试；Testcontainers 用例无 Docker 自动跳过）。
- **frontend**：node 22 + npm 缓存，`sh build-ui.sh`。
- 触发：push main / 全部 pull_request；concurrency 按 ref 取消进行中。

## 五、其他验证口径

- 后端改动：`mvn install` 通过（建议含测试）。
- 前端改动：`npm run build` 通过；mse-ui / mse-ui-mobile 另须 `npm run type-check` 零错误。
- 涉租户/权限行为：按 docs/tenancy.md 的口径人工回归。
