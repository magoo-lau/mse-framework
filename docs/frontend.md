# 前端体系（三独立仓）与子应用拼法

## 一、三仓分工（均独立于本仓）

| 仓库 | 角色 |
|---|---|
| [mse-ui](https://github.com/magoo-lau/mse-ui) | PC 基座（wujie 微前端主应用）：登录/工作台/系统管理 + 子应用容器；单仓双模式构建 `dist/tenant`（租户端）+ `dist/platform`（平台运营端，admin 视角） |
| [mse-ui-mobile](https://github.com/magoo-lau/mse-ui-mobile) | 移动基站（Vue3 + Vant 4）：租户端单模式 `base=/m/`；登录/Tabbar 工作台宫格（client=2 移动子应用）/我的/wujie 容器 |
| [mse-ui-shared](https://github.com/magoo-lau/mse-ui-shared) | 子应用公共运行时**源码包**：wujie 契约类型 / 请求工厂 createRequest / auth store 工厂 / 权限指令工厂 / 主题桥接（双端两版）/ Pagination / CronInput——以 **npm git 依赖** `github:magoo-lau/mse-ui-shared#release/1.0.0` 被全部前端消费（源码直出、无构建产物），对外交付备选 npm pack |

本仓（mse-framework）为纯后端底座；前端三仓各自独立构建（build/type-check 详见各仓 README）。

## 二、mse-ui-shared 消费约定（npm git 依赖）

- **引用**：package.json 依赖写 `github:magoo-lau/mse-ui-shared#release/1.0.0`；各前端工程所在**仓根** .npmrc 声明 `allow-git=github.com/magoo-lau/mse-ui-shared`（npm ≥10.5 默认禁 git 依赖 EALLOWGIT，需显式放行；多仓空格分隔续写）。
- **安装**：`npm install` / `npm ci` 原生解析（package-lock 记录 resolved git URL + commit，锁定可复现）。
- **升级**：独立仓 `release/1.0.0` 分支前进后，消费方 `npm install mse-ui-shared`（或删 node_modules + npm ci）刷新 lock 中 commit。
- **改源码**：在 mse-ui-shared 仓改 → 推 `release/1.0.0` 分支 → 消费方重装。
- **离线/代理**：npm 拉取 GitHub 需网络可达（必要时 `npm config set proxy/https-proxy`）。

## 三、子应用接入（wujie）

1. **工程**：复制 mse-sample 仓 mse-ui-sub（PC）或 mse-ui-mobile-sub（Vant 4）模板；改 name、dev 端口、独立运行 fallback 前缀 `/sub/<name>`、构建 `--base=/sub-apps/<name>/`（移动 `--base=/m-apps/<name>/`）。
2. **公共逻辑一律消费 mse-ui-shared**（npm git 依赖 + vite `optimizeDeps.exclude: ['mse-ui-shared']`），勿在业务工程复制实现；按钮权限 `v-permission`（本地薄包装 createPermissionDirective）。
3. **登录态**：基座经 wujie props 下发 `{ urlPrefix, token, tenantId, platform, nickname, permissions }`，变化经 bus `auth:change`；401 经 `sub:auth-expired` 交基座统一登出；路由切换经 `sub:title` 上报 `{ urlPrefix, path, title }`（meta.title 中文）。
4. **基座注册**：业务仓版本化脚本（推荐）登记 `sys_micro_app` + `sys_menu` type=3 页面菜单 + `sys_permission` 权限点（随 jar 推送，双路分发）；或平台端「子应用管理」「菜单管理」界面登记；id 号段登记在 mse-agent-plugin 仓 references/alloc-tables.md。
5. **授权口径「能力共享、入口分离」**：client 列 1=PC / 2=移动端独立菜单树独立裁剪；移动目录统一挂 550「移动工作台」；权限点不分端不重复建。
6. **请求代理**：子应用 dev proxy `/api` 指向同一后端（单体 8080 / 网关 48080）。

## 四、UI 风格与主题

- **统一紧凑型**：Element Plus 全局 `size: 'small'`（经 mse-ui-shared/pc `elementPlusUseOptions` 注册，勿逐组件重复写 size）；信息密度优先、间距收紧。
- `fixPopperContainer()`（shared pc 入口，main.ts 调一次）修复 wujie 嵌入模式下浮层漂移。
- **主题 = 基座统一下发**：深浅色（html.dark）与主题色由基座 stores/theme.ts 管理（localStorage `mse-theme` 设备级），经 wujie props `theme` + bus `theme:change` 下发，子应用经主题桥接跟随；页面样式一律 Element Plus 变量，**禁止硬编码浅色值**。菜单布局 left/top/mix、多页签开关同属设备级偏好；多页签另受租户参数 `sys.ui.tagsPolicy` 三态管控。门户首页另有管理员「主题库」（--mse-* 变量覆盖默认基线），与全局深浅色可组合。
- 剪贴板统一走「clipboard API 优先 + execCommand 隐藏 textarea 兜底」。

## 五、生产部署（nginx 要点）

- 双 server：租户端域名挂 dist/tenant、平台端域名挂 dist/platform；`/sub-apps/<name>/` alias 子应用产物、`/m/` 与 `/m-apps/<name>/` 移动侧；`/api/` 反代 48080（网关）或 8080（单体）。

## 六、微前端已知限制

wujie 生命周期与样式隔离的边界情况以基座运行时为准；子应用独立运行（fallback 前缀）仅限开发调试。详细接入步骤见 mse-ui-sub 模板 README 与 mse-agent-plugin 仓技能。
