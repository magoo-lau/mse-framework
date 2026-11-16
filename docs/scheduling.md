# 任务调度中心（service-job / boot-job）

薄调度中心：平台端「调度器」页统一管理定时任务与并发闸门，任务本体分布在各服务积木内（自扫描自注册），控制面只管触发与观测。

## 一、体系结构

- **控制面**：mse-job（48084）持有调度元数据（sys_schedule_setting 平台库），按 cron/间隔触发，经契约分发到各服务执行域；任务执行状态/日志回收上报。
- **执行域**：各服务积木内置 Scheduler（如 MonitorScheduler 平台库轮、DataEngineConfig 范式），扫描类任务自带分片与自愈（闸门满则跳过、下轮自愈）。
- **热更**：控制面改动经 Redis 5s 级差量下发（如 `gates.<key>` 闸门并发上限），运行期生效不重启。
- 跨租户后台扫描：多库轮询范式——首个世界是平台库时必须显式 `DynamicDataSourceContextHolder.push(平台库key)` 再扫；**禁止 @Transactional 包裹跨租户扫描**（语句级路由 / 先 runAs 再 TransactionTemplate）。

## 二、任务接入标准（服务积木侧）

1. 自有装配类 `@Bean` 注册扫描器/执行器（组件扫描可达，勿纯工厂方法）。
2. 每租户并发上限用 common-core `TenantTaskGates`（`tryRun` / `tryAcquire`+`release` 配对；键=业务前缀短横线小写如 `erp-push`，先在 mse-agent-plugin 仓 references/alloc-tables.md 闸门键节登记防冲突；注入侧 `@Qualifier` 指名防多闸同型歧义）。
3. 想让闸门出现在平台端「调度器」页：补一条自有平台域种子脚本（`-- mse-target: base -- mse-base-scope: platform` 向 sys_schedule_setting 插 `gates.<key>` 行，remark=「名称（每租户）｜说明」即页签展示名）。
4. 自建线程池经 `MseExecutors`；跨租户扫描执行域用 `*NoContext` 变体并注释理由。

## 三、运维要点

- 平台端页签：任务/闸门列表、启停、并发上限热更（`gates.<key>`→Redis 5s 级差量应用）。
- boot-job 无独立 k8s 清单与镜像名单时的构建：`sh mse-boot/build-images.sh boot-job`（本仓已纳入）。
- 调度元数据在平台库；业务执行数据在各自世界——控制面查询跨库只读汇总，不落业务库连接。
