package vip.mseapp.core.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * 本地调度实现（{@code mse.scheduling.mode=local|central}，缺省 local）：进程内线程池统一调度，
 * 核心语义——
 * <ul>
 *   <li><b>调度与执行分离（卡死隔离）</b>：触发池（{@code mse-task-N}，只算到期并提交）与
 *       执行池（{@code mse-task-worker-N}，有界队列，只跑任务体）物理分离——任务体卡死
 *       （无超时 HTTP/死锁/慢 SQL）只占执行池线程、只停自己那条链，永不拖垮触发池与其他任务；
 *       执行池饱和时新轮次提交被拒 → WARN 跳过本轮并照常续链（不丢链、饱和可见）；</li>
 *   <li><b>链式排程</b>：到点 → 提交执行池 → <em>任务体完成后</em>再排下一轮（fixedDelay 的
 *       end→start 语义不变、同任务天然不重叠；cron 算下一时点；STARTUP 单发）；运行期重排
 *       （{@link #applySchedule}）以链代号（generation）失效旧链防双链；</li>
 *   <li><b>延迟启动</b>：{@link #start()} 由装配件在 ApplicationReadyEvent 后调用
 *       （晚于全部 Runner——结构迁移等启动动作已完成），再叠加全局 startupDelay；</li>
 *   <li><b>central 模式</b>（{@code mse.scheduling.mode=central}）：本地不做周期排程（节奏由
 *       mse-job 薄调度中心经 trigger_pending/踢键链路下发，{@link #triggerNow} 即派发入口），
 *       注册/快照/上报/手动触发照常；STARTUP 启动任务仍本地执行（启动时序语义）；</li>
 *   <li><b>多实例防重</b>：周期/启动任务每轮先经 {@link TaskExecutionGuard} 抢分布式锁，
 *       抢不到即让位（at-least-one 执行，不重复）；人工触发不经互斥（抢占语义由下发方保证）；</li>
 *   <li><b>静态关停</b>：构造注入 {@code registrationFilter}（mse.scheduling.tasks.&lt;id&gt;.enabled），
 *       被禁任务不注册不排程不上报（零成本，优先级高于管理面暂停）；</li>
 *   <li><b>管理面</b>：注册表 + 运行态快照 + 暂停/恢复/立即触发/运行期重排 + 更新注册/注销
 *       （对账场景：外部配置源周期重放期望态，registerOrUpdate 定义未变 no-op、变化无覆盖重排；
 *       unregister 收回调度，快照/上报随之消失）。</li>
 * </ul>
 */
public class LocalMseTaskScheduler implements MseTaskScheduler, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(LocalMseTaskScheduler.class);

    /** 覆盖间隔下限（毫秒）：防止误配成 1ms 打爆调度池 */
    private static final long MIN_OVERRIDE_DELAY_MS = 1000;

    /** 触发池：只算到期并提交执行池，永不执行任务体（恒轻量，结构性免疫任务体卡死） */
    private final ScheduledExecutorService triggerExecutor;

    /** 执行池：有界队列，只跑任务体（卡死只停自身链；饱和跳轮可见） */
    private final ThreadPoolExecutor workerPool;

    private final long startupDelayMs;

    /** central 模式：本地不做周期排程（STARTUP 例外），节奏由 mse-job 中央派发 */
    private final boolean centralDispatched;

    private final TaskExecutionGuard guard;

    private final CronExpressionResolver cronResolver;

    /** 静态注册过滤器（mse.scheduling.tasks.<id>.enabled，null = 全放行） */
    private final Predicate<String> registrationFilter;

    private final Map<String, TaskEntry> tasks = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean(false);

    /** 进程标识（运行日志幂等主键组成：bootId:taskId:runIndex，重启换新避免序号复用撞主键） */
    private final String bootId = UUID.randomUUID().toString().substring(0, 8);

    /** 运行日志全局序号（单调递增，状态同步器按此增量拉取） */
    private final AtomicLong runSeq = new AtomicLong();

    /** 运行日志环形缓冲（全任务合计，先进先出淘汰；容量内保证同步器拉取窗口不丢） */
    private final ConcurrentLinkedDeque<TaskRunRecord> runLog = new ConcurrentLinkedDeque<>();

    private static final int RUN_LOG_CAPACITY = 1000;

    private static final AtomicLong THREAD_SEQ = new AtomicLong();

    private static final AtomicLong WORKER_SEQ = new AtomicLong();

    private static final class TaskEntry {

        /** 代码声明的原始定义（覆盖清除时还原用；对账更新 registerOrUpdate 时原位替换） */
        volatile TaskDefinition originalDefinition;

        volatile TaskDefinition effectiveDefinition;

        volatile TaskRunnable runnable;

        final TaskState state;

        volatile ScheduledFuture<?> future;

        /** 链代号：运行期重排/重排时递增，运行中旧代的续链因代数不符而失效（防双链） */
        final AtomicLong chainGen = new AtomicLong();

        TaskEntry(TaskDefinition definition, TaskRunnable runnable) {
            this.originalDefinition = definition;
            this.effectiveDefinition = definition;
            this.runnable = runnable;
            this.state = new TaskState(definition);
        }
    }

    public LocalMseTaskScheduler(int poolSize, long startupDelayMs,
                                 TaskExecutionGuard guard, CronExpressionResolver cronResolver) {
        this(poolSize, startupDelayMs, guard, cronResolver, null);
    }

    public LocalMseTaskScheduler(int poolSize, long startupDelayMs,
                                 TaskExecutionGuard guard, CronExpressionResolver cronResolver,
                                 Predicate<String> registrationFilter) {
        this(poolSize, Math.max(2, poolSize), 128, startupDelayMs, guard, cronResolver,
                registrationFilter, false);
    }

    public LocalMseTaskScheduler(int triggerPoolSize, int executorPoolSize, int executorQueueCapacity,
                                 long startupDelayMs, TaskExecutionGuard guard, CronExpressionResolver cronResolver,
                                 Predicate<String> registrationFilter, boolean centralDispatched) {
        this.triggerExecutor = Executors.newScheduledThreadPool(Math.max(1, triggerPoolSize), r -> {
            Thread thread = new Thread(r, "mse-task-" + THREAD_SEQ.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        this.workerPool = new ThreadPoolExecutor(Math.max(1, executorPoolSize), Math.max(1, executorPoolSize),
                60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(Math.max(16, executorQueueCapacity)), r -> {
            Thread thread = new Thread(r, "mse-task-worker-" + WORKER_SEQ.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
        this.startupDelayMs = Math.max(0, startupDelayMs);
        this.guard = guard;
        this.cronResolver = cronResolver;
        this.registrationFilter = registrationFilter;
        this.centralDispatched = centralDispatched;
    }

    @Override
    public void register(TaskDefinition definition, TaskRunnable runnable) {
        if (registrationFilter != null && !registrationFilter.test(definition.getTaskId())) {
            log.info("[mse-schedule] 任务被静态开关禁用（mse.scheduling.tasks.{}.enabled=false），不注册", definition.getTaskId());
            return;
        }
        TaskEntry entry = new TaskEntry(definition, runnable);
        if (tasks.putIfAbsent(definition.getTaskId(), entry) != null) {
            log.warn("[mse-schedule] 任务重复注册（保留先注册者）: {}", definition.getTaskId());
            return;
        }
        // 迟到注册（start 之后才注册的任务，如推送器在就绪后按需注册）：立即排程，
        // 仍叠加统一延迟启动窗口，语义与启动期注册一致；central 模式仅 STARTUP 本地执行
        if (started.get()) {
            if (centralDispatched && definition.getType() != TaskDefinition.Type.STARTUP) {
                log.info("[mse-schedule] 任务注册（central 模式，本地不排程，由中央派发）: {}", definition.getTaskId());
                return;
            }
            schedule(entry, true);
            log.info("[mse-schedule] 任务注册（调度已启动，{}ms 后纳入）: {}", startupDelayMs, definition.getTaskId());
        }
    }

    @Override
    public boolean registerOrUpdate(TaskDefinition definition, TaskRunnable runnable) {
        if (registrationFilter != null && !registrationFilter.test(definition.getTaskId())) {
            log.info("[mse-schedule] 任务被静态开关禁用（mse.scheduling.tasks.{}.enabled=false），不注册", definition.getTaskId());
            return false;
        }
        TaskEntry entry = tasks.get(definition.getTaskId());
        if (entry == null) {
            register(definition, runnable);
            return true;
        }
        synchronized (entry) {
            if (sameDefinition(entry.originalDefinition, definition)) {
                return false; // 期望态未变：对账重放 no-op，不打扰运行中的链
            }
            boolean overrideActive = entry.effectiveDefinition != entry.originalDefinition;
            entry.originalDefinition = definition;
            entry.runnable = runnable;
            if (overrideActive) {
                // 管理面覆盖生效中：只更新代码默认，不动生效调度（覆盖清除后还原到最新代码默认）
                log.info("[mse-schedule] 任务代码默认已更新（管理面覆盖生效中，不打断）: {}", definition.getTaskId());
                return true;
            }
            entry.effectiveDefinition = definition;
            entry.state.setDefinition(definition);
            if (centralDispatched && definition.getType() != TaskDefinition.Type.STARTUP) {
                log.info("[mse-schedule] 任务定义已更新（central 模式，本地无链，中央按新值派发）: {}", definition.getTaskId());
                return true;
            }
            cancel(entry);
            if (started.get()) {
                schedule(entry, false);
            }
            log.info("[mse-schedule] 任务定义已更新并重排: {}（{}）", definition.getTaskId(), definition.expression());
        }
        return true;
    }

    /** 对账期望态对比（类型/名称/描述/调度原语值一致即视为未变——不用 expression()：fixedDelay 渲染为等价 cron 有精度损失） */
    private static boolean sameDefinition(TaskDefinition a, TaskDefinition b) {
        return a.getType() == b.getType()
                && a.getFixedDelayMs() == b.getFixedDelayMs()
                && Objects.equals(a.getCron(), b.getCron())
                && a.getName().equals(b.getName())
                && a.getDescription().equals(b.getDescription());
    }

    /** 应用就绪后由装配件调用：打印任务清单并统一延迟排程（重复调用幂等） */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        List<TaskState> snapshot = sortedSnapshot();
        if (snapshot.isEmpty()) {
            log.info("[mse-schedule] 调度启动：无内置任务");
            return;
        }
        if (centralDispatched) {
            log.info("[mse-schedule] 调度启动（central 模式）：{} 个内置任务，本地不排程——周期由 mse-job 中央派发"
                            + "（trigger_pending+踢键链路，手动触发不受影响），仅 STARTUP 任务本地执行", snapshot.size());
        } else {
            log.info("[mse-schedule] 调度启动：{} 个内置任务，统一延迟 {} 后开始（触发池 {} / 执行池 {} 隔离，任务体卡死只停自身）",
                    snapshot.size(), Duration.ofMillis(startupDelayMs), "mse-task-N", "mse-task-worker-N");
        }
        for (TaskState state : snapshot) {
            TaskDefinition d = state.getDefinition();
            log.info("[mse-schedule]   - {} [{}] {}（{}）", d.getTaskId(), d.getType(),
                    d.getName().isBlank() ? d.getTaskId() : d.getName(), d.expression());
        }
        tasks.values().forEach(entry -> {
            if (centralDispatched && entry.effectiveDefinition.getType() != TaskDefinition.Type.STARTUP) {
                return; // central：周期任务不本地排程
            }
            schedule(entry, true);
        });
    }

    /**
     * 排程首轮（统一入口）：按类型算首轮延迟，链代号自增使旧链失效。
     *
     * @param entry           任务
     * @param withStartupDelay 是否叠加统一延迟启动窗口（启动期/迟到注册=true；运行期重排=false）
     */
    private void schedule(TaskEntry entry, boolean withStartupDelay) {
        TaskDefinition d = entry.effectiveDefinition;
        long firstDelay = switch (d.getType()) {
            case STARTUP -> withStartupDelay ? startupDelayMs : 0;
            case FIXED_DELAY -> withStartupDelay ? startupDelayMs + d.getInitialDelayMs() : d.getFixedDelayMs();
            case CRON -> nextCronDelayMs(entry, withStartupDelay);
        };
        if (firstDelay < 0) {
            return; // cron 无后续点/非法：已告警，停止排程
        }
        long gen = entry.chainGen.incrementAndGet();
        entry.future = triggerExecutor.schedule(() -> fire(entry, gen), firstDelay, TimeUnit.MILLISECONDS);
    }

    /** 触发池回调：只做提交，永不执行任务体；暂停让位但续链（恢复即回节奏） */
    private void fire(TaskEntry entry, long gen) {
        if (gen != entry.chainGen.get()) {
            return; // 已被重排接管（新链已排）
        }
        if (entry.state.isPaused()) {
            chainNext(entry, gen);
            return;
        }
        try {
            workerPool.execute(() -> {
                try {
                    runGuarded(entry);
                } finally {
                    chainNext(entry, gen);
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("[mse-schedule] 任务 {} 执行池饱和，本轮跳过（链继续，下轮重试）", taskId(entry));
            chainNext(entry, gen);
        }
    }

    /** 续链：任务体完成后（或暂停让位/饱和跳轮）排下一轮；fixedDelay=间隔、cron=下一时点、STARTUP=终结 */
    private void chainNext(TaskEntry entry, long gen) {
        if (gen != entry.chainGen.get()) {
            return; // 已被重排接管，防双链
        }
        if (!started.get()) {
            return; // 关闭中
        }
        TaskDefinition d = entry.effectiveDefinition;
        long delay = switch (d.getType()) {
            case STARTUP -> -1L;
            case FIXED_DELAY -> d.getFixedDelayMs();
            case CRON -> nextCronDelayMs(entry, false);
        };
        if (delay < 0) {
            return;
        }
        entry.future = triggerExecutor.schedule(() -> fire(entry, gen), delay, TimeUnit.MILLISECONDS);
    }

    /** cron 下次触发延迟（毫秒）；-1=无后续点/非法/缺解析器（已按场景告警），调用方停止排程 */
    private long nextCronDelayMs(TaskEntry entry, boolean withStartupDelay) {
        if (cronResolver == null) {
            log.error("[mse-schedule] cron 任务 {} 无法排程：未注入 CronExpressionResolver", taskId(entry));
            return -1;
        }
        try {
            Instant base = Instant.now().plusMillis(withStartupDelay ? startupDelayMs : 0);
            Instant next = cronResolver.nextExecution(entry.effectiveDefinition.getCron(), base);
            if (next == null) {
                log.warn("[mse-schedule] cron 任务 {} 无后续触发点，停止排程", taskId(entry));
                return -1;
            }
            return Math.max(0, Duration.between(Instant.now(), next).toMillis());
        } catch (IllegalArgumentException e) {
            log.error("[mse-schedule] cron 任务 {} 表达式非法（{}），停止排程", taskId(entry), e.getMessage());
            return -1;
        }
    }

    @Override
    public boolean applySchedule(String taskId, String exprOverride) {
        TaskEntry entry = tasks.get(taskId);
        if (entry == null) {
            return false;
        }
        if (entry.originalDefinition.getType() == TaskDefinition.Type.STARTUP) {
            log.warn("[mse-schedule] STARTUP 任务 {} 不支持调度覆盖", taskId);
            return false;
        }
        TaskDefinition next;
        try {
            next = resolveOverride(entry.originalDefinition, exprOverride);
        } catch (IllegalArgumentException e) {
            log.warn("[mse-schedule] 任务 {} 覆盖表达式非法（{}），保持原调度", taskId, e.getMessage());
            return false;
        }
        synchronized (entry) {
            cancel(entry);
            entry.effectiveDefinition = next;
            entry.state.setDefinition(next);
            if (centralDispatched) {
                // central：本地无链可重排；覆盖落库后由 mse-job 读库按新表达式派发
                log.info("[mse-schedule] 任务 {} 调度已调整为: {}（central 模式，中央按新值派发）", taskId, next.expression());
                return true;
            }
            // 运行期重排不叠加启动延迟窗：fixedDelay 覆盖以新间隔为首轮延迟（下个完整周期生效），
            // cron 从当下推算下次触发；还原代码默认时同样即时重排
            schedule(entry, false);
            log.info("[mse-schedule] 任务 {} 调度已调整为: {}", taskId, next.expression());
        }
        return true;
    }

    /** 覆盖表达式解析：纯数字（≥1000ms）= fixedDelay 覆盖；非数字 = cron；空 = 还原代码默认 */
    private TaskDefinition resolveOverride(TaskDefinition original, String exprOverride) {
        String trimmed = exprOverride == null ? "" : exprOverride.trim();
        if (trimmed.isEmpty()) {
            return original;
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            long delayMs = Long.parseLong(trimmed);
            if (delayMs < MIN_OVERRIDE_DELAY_MS) {
                throw new IllegalArgumentException("覆盖间隔不得低于 " + MIN_OVERRIDE_DELAY_MS + "ms");
            }
            return TaskDefinition.fixedDelay(original.getTaskId(), original.getName(),
                    original.getDescription(), delayMs, delayMs, original.isDeduplicate());
        }
        if (cronResolver == null) {
            throw new IllegalArgumentException("未注入 CronExpressionResolver，无法解析 cron 覆盖");
        }
        cronResolver.nextExecution(trimmed, Instant.now()); // 非法表达式在此抛出
        return TaskDefinition.cron(original.getTaskId(), original.getName(),
                original.getDescription(), trimmed, original.isDeduplicate());
    }

    private void cancel(TaskEntry entry) {
        ScheduledFuture<?> future = entry.future;
        if (future != null) {
            future.cancel(false);
            entry.future = null;
        }
    }

    /** 周期/启动执行入口（执行池线程）：暂停让位 + 互斥抢锁（绝不抛异常——抛出会断链） */
    private void runGuarded(TaskEntry entry) {
        if (entry.state.isPaused()) {
            return;
        }
        if (guard != null && entry.effectiveDefinition.isDeduplicate()) {
            try {
                guard.tryRun(taskId(entry), () -> runDirect(entry));
            } catch (Throwable t) {
                // 互斥层自身故障（guard 实现约定不抛，此处防御）：按本实例执行兜底
                log.warn("[mse-schedule] 任务 {} 互斥层异常，按本实例执行: {}", taskId(entry), t.toString());
                runDirect(entry);
            }
            return;
        }
        runDirect(entry);
    }

    /** 实际执行：统计成功/失败 + 运行日志入环形缓冲，异常只记录不外抛 */
    private void runDirect(TaskEntry entry) {
        TaskState state = entry.state;
        String taskId = entry.originalDefinition.getTaskId();
        long start = System.currentTimeMillis();
        state.markRunning(start);
        boolean success = false;
        String error = null;
        try {
            entry.runnable.run();
            state.markSuccess(start);
            success = true;
        } catch (Throwable t) {
            state.markFailure(start, t);
            success = false;
            error = t.toString();
            log.warn("[mse-schedule] 任务 {} 执行失败: {}", taskId, t.toString(), t);
        }
        recordRun(taskId, state.getRunCount(), start, success, error);
    }

    private void recordRun(String taskId, long runIndex, long startMs, boolean success, String error) {
        String errorBrief = error == null || error.isEmpty() ? null
                : (error.length() > 1000 ? error.substring(0, 1000) : error);
        long durationMs = System.currentTimeMillis() - startMs;
        runLog.addLast(new TaskRunRecord(bootId + ":" + taskId + ":" + runIndex,
                runSeq.incrementAndGet(), taskId, runIndex, startMs, durationMs, success, errorBrief));
        while (runLog.size() > RUN_LOG_CAPACITY) {
            runLog.pollFirst();
        }
    }

    @Override
    public List<TaskRunRecord> runsSince(long lastSeq) {
        List<TaskRunRecord> records = new ArrayList<>();
        for (TaskRunRecord record : runLog) {
            if (record.getGlobalSeq() > lastSeq) {
                records.add(record);
            }
        }
        return records;
    }

    @Override
    public List<TaskState> snapshot() {
        return sortedSnapshot();
    }

    private List<TaskState> sortedSnapshot() {
        return TaskState.sorted(new ArrayList<>(tasks.values().stream().map(e -> e.state).toList()));
    }

    @Override
    public boolean pause(String taskId) {
        TaskEntry entry = tasks.get(taskId);
        if (entry == null) {
            return false;
        }
        entry.state.setPaused(true);
        return true;
    }

    @Override
    public boolean resume(String taskId) {
        TaskEntry entry = tasks.get(taskId);
        if (entry == null) {
            return false;
        }
        entry.state.setPaused(false);
        return true;
    }

    @Override
    public boolean unregister(String taskId) {
        TaskEntry entry = tasks.remove(taskId);
        if (entry == null) {
            return false;
        }
        synchronized (entry) {
            // 链代号自增：在跑旧代的 fire/chainNext 因代数不符自灭，运行中当轮自然结束后不再续链
            entry.chainGen.incrementAndGet();
            cancel(entry);
        }
        log.info("[mse-schedule] 任务已注销（移出注册表，快照/上报随之消失）: {}", taskId);
        return true;
    }

    @Override
    public boolean triggerNow(String taskId) {
        TaskEntry entry = tasks.get(taskId);
        if (entry == null) {
            return false;
        }
        // 人工触发/central 中央派发入口：跳过暂停位与互斥（管理端显式意图；单飞由下发方乐观锁抢占保证），
        // 提交执行池（卡死只停自身，不占触发池）
        try {
            workerPool.execute(() -> runDirect(entry));
        } catch (RejectedExecutionException e) {
            log.warn("[mse-schedule] 任务 {} 执行池饱和，触发未受理（稍后重试或扩容 executor-pool-size）", taskId);
            return false;
        }
        return true;
    }

    @Override
    public void close() {
        started.set(false);
        triggerExecutor.shutdownNow();
        workerPool.shutdownNow();
    }

    private static String taskId(TaskEntry entry) {
        return entry.originalDefinition.getTaskId();
    }
}
