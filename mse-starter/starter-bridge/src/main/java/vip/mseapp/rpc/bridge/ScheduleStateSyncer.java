package vip.mseapp.rpc.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import vip.mseapp.api.system.RemoteScheduleStateApi;
import vip.mseapp.api.system.dto.ScheduleSyncResultDTO;
import vip.mseapp.api.system.dto.ScheduleTaskReportDTO;
import vip.mseapp.api.system.dto.ScheduleTaskStateDTO;
import vip.mseapp.core.schedule.MseTaskScheduler;
import vip.mseapp.core.schedule.ScheduleControlState;
import vip.mseapp.core.schedule.TaskState;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 调度状态监控线程（local 模式管理面数据通道）：独立守护线程（不注册进调度中心任务清单，
 * 停掉全部任务也停不掉它——管理指令通道永不断流，这是「任务调度」页可恢复操作的结构保证）。
 * 事件驱动、空闲零 SQL（任务全部禁用时只剩每控制节拍一次 Redis HGETALL）：
 * <ul>
 *   <li><b>启动一次全量 sync</b>：就绪即上报任务定义（新任务首次落行）并取回启停/覆盖——
 *       暂停态先于首轮任务生效；</li>
 *   <li><b>控制节拍</b>（{@code mse.scheduling.control-poll-interval}，缺省 5s）：HGETALL
 *       Redis 控制哈希 {@link ScheduleControlState#CONTROL_HASH_KEY}，差量应用启停与调度覆盖
 *       ——快通道，管理面落库写透后秒级收敛；哈希缺失/字段缺失=无指令、不动作。同拍顺带两个
 *       事件检查：<b>触发踢</b>{@link ScheduleControlState#KICK_KEY} 值变化（管理面「立即触发」
 *       落库后写入）→ 立即全量 sync 抢占 trigger_pending（乐观锁，恰好一个实例执行）；
 *       本地有<b>新运行记录</b> → 只把跑过的任务运行态与执行日志上行（部分上报，未跑任务零流量）；</li>
 *   <li><b>降级兜底节拍</b>（{@code mse.scheduling.state-sync-interval}，缺省 15s）：仅当
 *       Redis 缺席/故障或控制哈希缺失（本地有任务）时执行——全量 sync 按 DB 事实源收敛启停/
 *       覆盖/触发；平台侧 sync 尾部的去抖重建会在 60s 内补回控制哈希，快通道恢复后自动退出降级。</li>
 * </ul>
 *
 * <p>契约解析与 {@link BaseDataScriptPusher} 同款双形态：local（单体）容器内直调
 * {@link RemoteScheduleStateApi} 唯一实现（聚合 service-platform 即存在）；dubbo（微服务）
 * 经 {@link ScheduleStateDubboReference} 泛化远调 mse-platform，check=false 启动不依赖
 * 对端就绪。mse-platform 未就绪时告警一次后静默重试（下周期自愈），任务按本地默认继续。
 */
public class ScheduleStateSyncer implements ApplicationListener<ApplicationReadyEvent>,
        org.springframework.beans.factory.DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ScheduleStateSyncer.class);

    private final org.springframework.core.env.Environment environment;

    private final ConfigurableListableBeanFactory beanFactory;

    private final ObjectProvider<MseTaskScheduler> schedulerProvider;

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    /** 本容器全部租户并发闸门（「调度器」设置页热更参数按 key 差量应用；无闸门时空转） */
    private final ObjectProvider<vip.mseapp.core.concurrent.TenantTaskGates> gatesProvider;

    private final long syncIntervalMs;

    private final long controlIntervalMs;

    /** 本实例已应用的调度覆盖（taskId → 覆盖值；null=代码默认）——差量重排，避免每周期重排 */
    private final Map<String, String> appliedOverrides = new ConcurrentHashMap<>();

    /** 本实例已应用的启停态（taskId → 目标态）——控制节拍差量应用，避免每周期重复 pause/resume */
    private final Map<String, Boolean> appliedEnabled = new ConcurrentHashMap<>();

    /** 本实例已应用的租户闸门上限（gates.key → limit）——控制节拍差量应用 */
    private final Map<String, Integer> appliedGateLimits = new ConcurrentHashMap<>();

    /** 执行日志上行游标（全局序号；成功一条推进一条） */
    private volatile long lastLogSeq;

    /** 监控线程（就绪后启动，destroy 关闭；null=未启动：无调度器或非 local 模式） */
    private volatile ScheduledExecutorService watcher;

    /** 快通道降级标志（Redis 缺席/故障或控制哈希缺失）：true 时兜底节拍执行全量 sync */
    private volatile boolean degraded = false;

    /** 最近已见的触发踢值（null=未见/键不存在）——值变化即发起全量 sync 抢占 */
    private volatile String lastKick;

    private final AtomicBoolean platformAbsentWarned = new AtomicBoolean(false);

    public ScheduleStateSyncer(org.springframework.core.env.Environment environment,
                               ConfigurableListableBeanFactory beanFactory,
                               ObjectProvider<MseTaskScheduler> schedulerProvider,
                               ObjectProvider<StringRedisTemplate> redisProvider,
                               ObjectProvider<vip.mseapp.core.concurrent.TenantTaskGates> gatesProvider) {
        this.environment = environment;
        this.beanFactory = beanFactory;
        this.schedulerProvider = schedulerProvider;
        this.redisProvider = redisProvider;
        this.gatesProvider = gatesProvider;
        this.syncIntervalMs = DurationStyle.detectAndParse(
                environment.getProperty("mse.scheduling.state-sync-interval", "15s")).toMillis();
        this.controlIntervalMs = DurationStyle.detectAndParse(
                environment.getProperty("mse.scheduling.control-poll-interval", "5s")).toMillis();
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        MseTaskScheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            return; // 无调度器（mode=none 等）：无任务无状态可同步
        }
        String mode = environment.getProperty("mse.scheduling.mode", "local");
        // local=本地排程；central=本地不排程（mse-job 中央派发）——两者均需本通道（触发接收+上报）；
        // 其他（xxl-job 等外部调度器）不经 mse-platform 同步
        if (!"local".equalsIgnoreCase(mode) && !"central".equalsIgnoreCase(mode)) {
            log.info("[mse-schedule] 调度模式 {}（外部调度器），任务状态不经 mse-platform 同步", mode);
            return;
        }
        // 就绪先全量同步一次（定义上报 + 暂停态先于首轮任务生效），再启动监控线程
        syncOnce(scheduler);
        watcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "mse-schedule-watcher");
            thread.setDaemon(true);
            return thread;
        });
        watcher.scheduleWithFixedDelay(() -> controlBeat(scheduler), 0, controlIntervalMs, TimeUnit.MILLISECONDS);
        watcher.scheduleWithFixedDelay(() -> fallbackBeat(scheduler), syncIntervalMs, syncIntervalMs, TimeUnit.MILLISECONDS);
        log.info("[mse-schedule] 调度监控线程已启动：控制节拍 {}s（Redis 快通道 + 触发踢/运行事件上报），"
                        + "降级兜底节拍 {}s（仅 Redis 缺席或控制哈希缺失时执行全量 sync）",
                controlIntervalMs / 1000, syncIntervalMs / 1000);
    }

    /** 控制节拍：差量应用控制哈希 + 事件检查（触发踢 → 全量 sync；新运行记录 → 部分上报） */
    private void controlBeat(MseTaskScheduler scheduler) {
        boolean degradedNow = applyControl(scheduler);
        if (degraded != degradedNow) {
            degraded = degradedNow;
            if (degradedNow) {
                log.warn("[mse-schedule] 控制快通道降级（Redis 缺席/故障或控制哈希缺失），"
                        + "兜底节拍每 {}s 经 sync 通道按 DB 收敛，恢复后自动切回", syncIntervalMs / 1000);
            } else {
                log.info("[mse-schedule] 控制快通道已恢复（Redis 控制哈希可用）");
            }
        }
        if (degradedNow) {
            return; // 降级期间事件路径停走：兜底节拍的全量 sync 已覆盖上行与下行抢占
        }
        if (kickChanged()) {
            syncOnce(scheduler);
            return;
        }
        applyGateLimits();
        pushRunEvents(scheduler);
    }

    /**
     * 租户并发闸门差量应用：读 Redis 闸门参数键（管理面「调度器」设置页写透），与本实例
     * 上次已应用值比对，变化才按 key 匹配本容器的 {@code TenantTaskGates} 更新上限——
     * 本容器无对应 key 的闸门（参数为其他服务域）记入已应用缓存避免重复解析。
     */
    private void applyGateLimits() {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        Map<String, Integer> limits;
        try {
            limits = ScheduleControlState.decodeGates(redis.opsForValue().get(ScheduleControlState.GATES_KEY));
        } catch (Exception e) {
            return; // Redis 抖动：下周期重试（闸门保持当前上限，不断流）
        }
        if (limits.isEmpty()) {
            return; // 键缺失/空 = 无指令不动作（回落各闸门构造默认）
        }
        limits.forEach((key, limit) -> {
            if (appliedGateLimits.get(key) != null && appliedGateLimits.get(key).equals(limit)) {
                return;
            }
            vip.mseapp.core.concurrent.TenantTaskGates matched = null;
            for (vip.mseapp.core.concurrent.TenantTaskGates gates : gatesProvider) {
                if (key.equals(gates.key())) {
                    matched = gates;
                    break;
                }
            }
            if (matched != null) {
                matched.updateLimit(limit);
                log.info("[mse-schedule] 租户并发闸门上限已应用: {} -> {}（每租户）", key, limit);
            }
            appliedGateLimits.put(key, limit);
        });
    }

    /** 降级兜底节拍：仅在快通道降级时执行全量 sync（上行快照/日志 + 下行启停/覆盖/触发收敛） */
    private void fallbackBeat(MseTaskScheduler scheduler) {
        if (!degraded) {
            return;
        }
        syncOnce(scheduler);
    }

    /** 控制节拍主体：HGETALL 控制哈希差量应用启停与调度覆盖；返回快通道是否降级 */
    private boolean applyControl(MseTaskScheduler scheduler) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return true;
        }
        try {
            Map<Object, Object> entries = redis.opsForHash().entries(ScheduleControlState.CONTROL_HASH_KEY);
            if (entries.isEmpty() && !scheduler.snapshot().isEmpty()) {
                return true; // 哈希缺失（Redis 被清空/平台侧未重建）：降级，sync 通道按 DB 收敛并自愈
            }
            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                String taskKey = String.valueOf(entry.getKey());
                ScheduleControlState state = ScheduleControlState.decode(String.valueOf(entry.getValue()));
                if (state == null) {
                    continue; // 非法条目按"无指令"跳过
                }
                Boolean applied = appliedEnabled.get(taskKey);
                if (state.enabled() != Boolean.TRUE.equals(applied)) {
                    if (state.enabled()) {
                        scheduler.resume(taskKey);
                    } else {
                        scheduler.pause(taskKey);
                    }
                    appliedEnabled.put(taskKey, state.enabled());
                    log.info("[mse-schedule] 控制缓存启停指令已应用: {} -> {}", taskKey,
                            state.enabled() ? "恢复" : "暂停");
                }
                applyOverride(scheduler, taskKey, state.exprOverride());
            }
            return false;
        } catch (Exception e) {
            log.warn("[mse-schedule] 控制缓存轮询失败（降级期间 sync 通道兜底，下周期重试）: {}", e.getMessage());
            return true;
        }
    }

    /** 触发踢检查：踢值变化（含启动首拍读到停机前遗留值）即返回 true，由调用方发起全量 sync 抢占 */
    private boolean kickChanged() {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }
        try {
            String value = redis.opsForValue().get(ScheduleControlState.KICK_KEY);
            if (Objects.equals(value, lastKick)) {
                return false;
            }
            lastKick = value;
            return value != null;
        } catch (Exception e) {
            return false; // Redis 抖动：主循环的降级检测会接管
        }
    }

    /** 调度覆盖差量重排：与本实例上次已应用的值比对，变化才下发（null = 还原代码默认） */
    private void applyOverride(MseTaskScheduler scheduler, String taskId, String override) {
        String applied = appliedOverrides.get(taskId);
        boolean changed = override == null ? applied != null : !override.equals(applied);
        if (!changed) {
            return;
        }
        if (scheduler.applySchedule(taskId, override)) {
            appliedOverrides.put(taskId, override);
            log.info("[mse-schedule] 管理端调度覆盖已应用: {} -> {}",
                    taskId, override == null || override.isBlank() ? "（代码默认）" : override);
        } else if (scheduler.snapshot().stream().noneMatch(s -> s.getTaskId().equals(taskId))) {
            // 本实例没有该任务（归属其他服务/被静态开关禁用）：无需应用，也不再重试比对
            appliedOverrides.put(taskId, override);
        }
    }

    /** 上行次数上限（单周期）：超出部分下周期继续（环形缓冲 1000 条内不丢） */
    private static final int LOG_BATCH_LIMIT = 200;

    /** 全量同步：上行全部任务定义/运行态快照 + 执行日志，下行启停/覆盖/触发抢占（启动/触发踢/降级兜底） */
    private void syncOnce(MseTaskScheduler scheduler) {
        try {
            RemoteScheduleStateApi api = resolveRegistry();
            if (api == null) {
                return;
            }
            ScheduleTaskReportDTO report = buildReport(scheduler, null);
            ScheduleSyncResultDTO result = api.sync(report);
            applyResult(scheduler, result);
            pushRunLogs(scheduler, api);
        } catch (Exception e) {
            log.warn("[mse-schedule] 任务状态同步失败（触发踢/降级场景下周期或下次事件重试）: {}", e.getMessage());
        }
    }

    /** 运行事件上报：只把产生新运行记录的任务上行（部分上报 + 执行日志），未跑任务零流量 */
    private void pushRunEvents(MseTaskScheduler scheduler) {
        try {
            List<vip.mseapp.core.schedule.TaskRunRecord> pending = scheduler.runsSince(lastLogSeq);
            if (pending.isEmpty()) {
                return;
            }
            List<vip.mseapp.core.schedule.TaskRunRecord> batch =
                    pending.size() > LOG_BATCH_LIMIT ? pending.subList(0, LOG_BATCH_LIMIT) : pending;
            Set<String> ranTaskIds = new LinkedHashSet<>();
            batch.forEach(record -> ranTaskIds.add(record.getTaskId()));
            RemoteScheduleStateApi api = resolveRegistry();
            if (api == null) {
                return; // 上行无门：游标不推进，下周期重试
            }
            ScheduleSyncResultDTO result = api.sync(buildReport(scheduler, ranTaskIds));
            applyResult(scheduler, result);
            api.appendLogs(toLogDtos(batch));
            lastLogSeq = batch.get(batch.size() - 1).getGlobalSeq();
        } catch (Exception e) {
            log.warn("[mse-schedule] 运行事件上报失败（下周期重试）: {}", e.getMessage());
        }
    }

    /** 增量上行执行日志：按全局序号游标拉取，单批上行（整批成功才推进游标；幂等主键去重） */
    private void pushRunLogs(MseTaskScheduler scheduler, RemoteScheduleStateApi api) {
        List<vip.mseapp.core.schedule.TaskRunRecord> pending = scheduler.runsSince(lastLogSeq);
        if (pending.isEmpty()) {
            return;
        }
        List<vip.mseapp.core.schedule.TaskRunRecord> batch =
                pending.size() > LOG_BATCH_LIMIT ? pending.subList(0, LOG_BATCH_LIMIT) : pending;
        api.appendLogs(toLogDtos(batch));
        lastLogSeq = batch.get(batch.size() - 1).getGlobalSeq();
    }

    private List<vip.mseapp.api.system.dto.ScheduleTaskLogDTO> toLogDtos(
            List<vip.mseapp.core.schedule.TaskRunRecord> batch) {
        String serviceName = environment.getProperty("spring.application.name", "unknown");
        return batch.stream().map(record -> {
            vip.mseapp.api.system.dto.ScheduleTaskLogDTO dto = new vip.mseapp.api.system.dto.ScheduleTaskLogDTO();
            dto.setId(record.getId());
            dto.setTaskKey(record.getTaskId());
            dto.setServiceName(serviceName);
            dto.setStartTime(java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(record.getStartMs()), java.time.ZoneId.systemDefault()));
            dto.setDurationMs(record.getDurationMs());
            dto.setResult(record.isSuccess() ? "SUCCESS" : "FAILED");
            dto.setError(record.getError());
            return dto;
        }).toList();
    }

    /** 构建上报快照；taskIds 非 null 时只包含指定任务（运行事件部分上报），null 为全量 */
    private ScheduleTaskReportDTO buildReport(MseTaskScheduler scheduler, Set<String> taskIds) {
        ScheduleTaskReportDTO report = new ScheduleTaskReportDTO();
        report.setServiceName(environment.getProperty("spring.application.name", "unknown"));
        report.setSchedulerMode(environment.getProperty("mse.scheduling.mode", "local"));
        List<TaskState> snapshot = scheduler.snapshot();
        report.setTasks(snapshot.stream()
                .filter(state -> taskIds == null || taskIds.contains(state.getTaskId()))
                .map(state -> {
                    ScheduleTaskStateDTO dto = new ScheduleTaskStateDTO();
                    dto.setTaskId(state.getTaskId());
                    dto.setName(state.getDefinition().getName());
                    dto.setDescription(state.getDefinition().getDescription());
                    dto.setType(state.getDefinition().getType().name());
                    dto.setExpression(state.getDefinition().expression());
                    dto.setCategory(state.getDefinition().getCategory());
                    dto.setLastRunStartMs(state.getLastRunStartMs());
                    dto.setLastRunEndMs(state.getLastRunEndMs());
                    dto.setLastDurationMs(state.getLastDurationMs());
                    dto.setLastResult(state.getLastResult());
                    dto.setLastError(truncate(state.getLastError(), 1000));
                    dto.setRunCount(state.getRunCount());
                    dto.setErrorCount(state.getErrorCount());
                    return dto;
                }).toList());
        return report;
    }

    /** sync 兜底下行：启停/覆盖/触发（DB 事实源，快通道降级或触发踢场景收敛） */
    private void applyResult(MseTaskScheduler scheduler, ScheduleSyncResultDTO result) {
        result.getEnabledByTask().forEach((taskId, enabled) -> {
            if (Boolean.FALSE.equals(enabled)) {
                scheduler.pause(taskId);
            } else {
                scheduler.resume(taskId);
            }
            appliedEnabled.put(taskId, !Boolean.FALSE.equals(enabled));
        });
        applyOverrides(scheduler, result.getExprOverrideByTask());
        for (String taskId : result.getTriggeredTaskIds()) {
            scheduler.triggerNow(taskId);
            log.info("[mse-schedule] 管理端触发任务（已抢占）: {}", taskId);
        }
    }

    /** sync 通道的覆盖差量应用（复用单任务差量逻辑） */
    private void applyOverrides(MseTaskScheduler scheduler, Map<String, String> overrideByTask) {
        if (overrideByTask == null || overrideByTask.isEmpty()) {
            return;
        }
        overrideByTask.forEach((taskId, override) -> applyOverride(scheduler, taskId, override));
    }

    private RemoteScheduleStateApi resolveRegistry() {
        if ("dubbo".equalsIgnoreCase(environment.getProperty("mse.rpc.mode", "local"))
                && dubboPresent()) {
            // 仅在 dubbo 分支触达 ScheduleStateDubboReference（宿主无 Dubbo 时该类不被加载）
            return ScheduleStateDubboReference.proxy(environment);
        }
        var candidates = beanFactory.getBeansOfType(RemoteScheduleStateApi.class);
        if (candidates.isEmpty()) {
            if (platformAbsentWarned.compareAndSet(false, true)) {
                log.warn("[mse-schedule] 容器内无 RemoteScheduleStateApi 实现（宿主未聚合 service-platform），"
                        + "任务状态暂不同步（任务按本地默认执行，下周期重试）");
            }
            return null;
        }
        if (candidates.size() > 1) {
            if (platformAbsentWarned.compareAndSet(false, true)) {
                log.warn("[mse-schedule] 容器内存在多个 RemoteScheduleStateApi 实现 {}，任务状态暂不同步",
                        candidates.keySet());
            }
            return null;
        }
        return candidates.values().iterator().next();
    }

    private boolean dubboPresent() {
        try {
            Class.forName("org.apache.dubbo.config.ReferenceConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void destroy() {
        ScheduledExecutorService current = watcher;
        if (current != null) {
            current.shutdownNow();
        }
        ScheduleStateDubboReference.destroy();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
