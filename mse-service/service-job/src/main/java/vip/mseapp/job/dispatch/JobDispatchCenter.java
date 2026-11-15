package vip.mseapp.job.dispatch;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.support.CronExpression;
import vip.mseapp.core.schedule.ScheduleControlState;
import vip.mseapp.job.entity.JobScheduleTask;
import vip.mseapp.job.mapper.JobScheduleTaskMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * mse-job 薄调度中心：只做「什么时候该跑了」——到期判定后置 {@code trigger_pending=1} + 写触发踢键，
 * 由<b>既有验证过的下行链路</b>接管（各服务调度监控线程 sync 乐观锁抢占恰一实例 → 本地执行池跑体 →
 * 运行态事件上报）。零新 RPC 契约，管理面启停/覆盖自动生效（本中心读的就是同一张 sys_schedule_task）。
 *
 * <p>自举规避：派发线程为独立守护线程（自建 ScheduledExecutorService，不注册进统一调度中心——
 * 与实例侧 mse-schedule-watcher 同款模式）。多实例天然幂等：置 pending 幂等 + 执行端乐观锁单飞；
 * 同任务重复派发以最小间隔下限防抖（执行/上报延迟内不重置）。
 *
 * <p>到期判定：生效表达式 = exprOverride 优先（纯数字=毫秒间隔 / 否则 cron），否则解析 scheduleExpr
 * 渲染值（步进 cron 如 {@code 0/20 * * * * ?} 按 cron、{@code ~every Ns} 按秒数）。间隔型=距 last_run_time ≥ 间隔；
 * cron 型=按 last_run_time 推下次时点已到。STARTUP 任务为本地启动语义，中央不派发。
 */
public class JobDispatchCenter implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(JobDispatchCenter.class);

    private static final int RECENT_CAPACITY = 50;

    private final JobScheduleTaskMapper taskMapper;

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    private final JobDispatchProperties properties;

    /** taskKey → 上次派发时间戳（毫秒）：执行/上报延迟内的重复置 pending 防抖 */
    private final Map<String, Long> lastDispatchAt = new ConcurrentHashMap<>();

    /** 最近派发明细（观测端点展示，先进先出淘汰） */
    private final ConcurrentLinkedDeque<String> recentDispatches = new ConcurrentLinkedDeque<>();

    private final AtomicLong dispatchedTotal = new AtomicLong();

    private volatile LocalDateTime lastScanAt;

    private volatile ScheduledExecutorService dispatcher;

    public JobDispatchCenter(JobScheduleTaskMapper taskMapper,
                             ObjectProvider<StringRedisTemplate> redisProvider,
                             JobDispatchProperties properties) {
        this.taskMapper = taskMapper;
        this.redisProvider = redisProvider;
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (dispatcher != null) {
            return;
        }
        dispatcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "mse-job-dispatch");
            thread.setDaemon(true);
            return thread;
        });
        dispatcher.scheduleWithFixedDelay(this::dispatchBeat,
                properties.getDispatchInitialDelay().toMillis(),
                properties.getDispatchInterval().toMillis(), TimeUnit.MILLISECONDS);
        log.info("[mse-job] 薄调度中心已启动：到期扫描每 {}（首扫 {} 后），到期→置 trigger_pending+踢键→既有链路执行",
                properties.getDispatchInterval(), properties.getDispatchInitialDelay());
    }

    /** 派发节拍：扫启用任务 → 到期判定 → 置待触发 + 踢（单任务异常不阻断本轮其余任务） */
    void dispatchBeat() {
        try {
            LocalDateTime now = LocalDateTime.now();
            lastScanAt = now;
            List<JobScheduleTask> rows = taskMapper.selectList(new LambdaQueryWrapper<JobScheduleTask>()
                    .select(JobScheduleTask::getTaskKey, JobScheduleTask::getTaskName,
                            JobScheduleTask::getOwnerService, JobScheduleTask::getTaskType,
                            JobScheduleTask::getScheduleExpr, JobScheduleTask::getExprOverride,
                            JobScheduleTask::getEnabled, JobScheduleTask::getTriggerPending,
                            JobScheduleTask::getLastRunTime)
                    .eq(JobScheduleTask::getEnabled, 1)
                    .ne(JobScheduleTask::getTaskType, "STARTUP"));
            int dispatched = 0;
            for (JobScheduleTask row : rows) {
                try {
                    if (dispatchIfDue(row, now)) {
                        dispatched++;
                    }
                } catch (Exception e) {
                    log.warn("[mse-job] 任务 {} 派发检查异常（下轮继续）: {}", row.getTaskKey(), e.toString());
                }
            }
            if (dispatched > 0) {
                log.info("[mse-job] 本轮派发 {} 个到期任务（跟踪 {} 个启用任务）", dispatched, rows.size());
            }
        } catch (Throwable t) {
            log.warn("[mse-job] 派发节拍异常（下轮继续）: {}", t.toString());
        }
    }

    /** 到期判定 + 幂等置位 + 踢键；返回是否发生派发 */
    private boolean dispatchIfDue(JobScheduleTask row, LocalDateTime now) {
        if (row.getTriggerPending() != null && row.getTriggerPending() == 1) {
            return false; // 上轮派发尚未被抢占执行：不重复置位
        }
        EffectiveExpr expr = parseEffective(row);
        if (expr == null || !isDue(expr, row.getLastRunTime(), now)) {
            return false;
        }
        long nowMs = System.currentTimeMillis();
        Long last = lastDispatchAt.get(row.getTaskKey());
        long minGapMs = Math.max(properties.getMinRedispatchGap().toMillis(),
                expr.intervalMs() != null ? expr.intervalMs() / 2 : properties.getMinRedispatchGap().toMillis());
        if (last != null && nowMs - last < minGapMs) {
            return false; // 执行/上报延迟内防抖：pending 可能仍在执行中未回写 last_run_time
        }
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<JobScheduleTask>()
                .eq(JobScheduleTask::getTaskKey, row.getTaskKey())
                .eq(JobScheduleTask::getEnabled, 1)
                .eq(JobScheduleTask::getTriggerPending, 0)
                .set(JobScheduleTask::getTriggerPending, 1));
        if (updated == 0) {
            return false; // 并发实例已置位/已暂停：幂等让位
        }
        lastDispatchAt.put(row.getTaskKey(), nowMs);
        long total = dispatchedTotal.incrementAndGet();
        recentDispatches.addLast(now.toLocalTime() + " " + row.getTaskKey() + " → " + row.getOwnerService());
        while (recentDispatches.size() > RECENT_CAPACITY) {
            recentDispatches.pollFirst();
        }
        log.info("[mse-job] 派发到期任务 #{}: {}（{}，生效 {}，上次执行 {}）", total, row.getTaskKey(),
                row.getOwnerService(), expr.raw(),
                row.getLastRunTime() == null ? "从未" : row.getLastRunTime().toLocalTime());
        kick();
        return true;
    }

    /** 生效表达式：覆盖优先（纯数字=毫秒 / 否则 cron），否则解析渲染值（cron / ~every Ns） */
    private EffectiveExpr parseEffective(JobScheduleTask row) {
        String override = row.getExprOverride();
        if (override != null && !override.isBlank()) {
            EffectiveExpr parsed = parse(override.trim());
            if (parsed == null) {
                log.warn("[mse-job] 任务 {} 覆盖表达式不可解析（{}），跳过本轮", row.getTaskKey(), override);
            }
            return parsed;
        }
        return parse(row.getScheduleExpr());
    }

    /** 表达式解析（包内可测）：纯数字=毫秒间隔；{@code ~every Ns}=秒间隔；其余按 Spring cron；非法返回 null */
    static EffectiveExpr parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.chars().allMatch(Character::isDigit)) {
            return new EffectiveExpr(trimmed, Long.parseLong(trimmed), null);
        }
        if (trimmed.startsWith("~every ") && trimmed.endsWith("s")) {
            try {
                long seconds = Long.parseLong(trimmed.substring(7, trimmed.length() - 1).trim());
                return new EffectiveExpr(trimmed, seconds * 1000, null);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        try {
            return new EffectiveExpr(trimmed, null, CronExpression.parse(trimmed));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 到期判定（包内可测）：间隔型=距上次执行 ≥ 间隔（从未执行=到期，首跑）；cron 型=按上次执行
     * 推算的下次时点已到（从未执行=到期）。
     */
    static boolean isDue(EffectiveExpr expr, LocalDateTime lastRun, LocalDateTime now) {
        if (expr.intervalMs() != null) {
            if (lastRun == null) {
                return true;
            }
            return Duration.between(lastRun, now).toMillis() >= expr.intervalMs();
        }
        if (expr.cron() != null) {
            if (lastRun == null) {
                return true;
            }
            LocalDateTime next = expr.cron().next(lastRun);
            return next != null && !next.isAfter(now);
        }
        return false;
    }

    /** 触发踢键（值变化即促实例立即 sync 抢占；Redis 缺席时执行方降级兜底节拍照常收敛） */
    private void kick() {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(ScheduleControlState.KICK_KEY, String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            log.warn("[mse-job] 触发踢键写入失败（执行方降级兜底节拍照常收敛）: {}", e.getMessage());
        }
    }

    /** 观测快照（/job/state 端点） */
    public Map<String, Object> state() {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("running", dispatcher != null && !dispatcher.isShutdown());
        out.put("dispatchIntervalMs", properties.getDispatchInterval().toMillis());
        out.put("dispatchedTotal", dispatchedTotal.get());
        out.put("lastScanAt", lastScanAt == null ? null : lastScanAt.toString());
        out.put("recentDispatches", List.copyOf(recentDispatches));
        return out;
    }

    @Override
    public void destroy() {
        ScheduledExecutorService current = dispatcher;
        if (current != null) {
            current.shutdownNow();
        }
    }

    /** 解析结果：intervalMs 与 cron 二选一（raw 供日志/观测） */
    record EffectiveExpr(String raw, Long intervalMs, CronExpression cron) {
    }
}
