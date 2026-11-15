package vip.mseapp.rpc.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.api.tenant.dto.TenantDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.schedule.MseTaskScheduler;
import vip.mseapp.core.schedule.TaskDefinition;
import vip.mseapp.mybatis.schema.TenantDataMigrator;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 每租户种子补偿器（数据面，默认装配）：周期 + 首触双通道，保证「共享库初始化完成后新建/存量
 * 租户」的每租户种子（默认角色等）不依赖 MQ 事件到达——此前 auto-upgrade 仅启动扫捕一次、
 * 无 MQ 部署的 tenant.created 事件跨 JVM 不可达，新建租户可能长期缺租户级数据。
 *
 * <ul>
 *   <li><b>周期 tick</b>（默认 5min，{@code mse.tenant.schema.tenant-data.compensate-interval-ms}）：
 *       拉平台全量租户（TenantDTO 自带 dsKey，零额外 RPC），过滤「共享库租户（dsKey 空白）且
 *       不在 doneSet」，每轮最多 {@code compensate-batch-size} 个（默认 20）逐个
 *       {@link TenantDataMigrator#migrateTenant}——幂等：账本按 (script, tenant) 去重，已就绪
 *       租户成本为一次索引查询（WP 已为账本补 tenant_id 索引）；doneSet 每
 *       {@code compensate-recheck-interval-ms}（默认 1h）清空一次，新增脚本版本也能周期重查；</li>
 *   <li><b>首触快路径</b>：{@link TenantSnapshotStore} 本进程首次加载某租户快照后回调
 *       {@link #enqueue}——新建租户秒级补偿，不等下个周期。</li>
 * </ul>
 *
 * <p><b>独立库租户（dsKey 非空）跳过</b>：其每租户种子由懒初始化直建通道在租户库内执行
 * （首次路由全量直建含 per-tenant），在内置共享库跑 forTenant 脚本只会播出无人消费的孤儿行。
 *
 * <p>调度经统一调度中心（taskId=tenant-data-compensate，多实例防重）；调度器缺席
 * （mse.scheduling.mode=none 等）时仅首触通道生效。首触经单线程 daemon 执行器异步执行
 * （不阻塞租户首请求），inflight 去重防重入。租户事件通道（TenantEventInboundHandler）
 * 优先汇入本补偿器，获得同款独立库过滤。
 */
public class TenantDataCompensator implements ApplicationListener<ApplicationReadyEvent>, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TenantDataCompensator.class);

    static final String TASK_ID = "tenant-data-compensate";

    private final ObjectProvider<TenantDataMigrator> migratorProvider;

    private final ObjectProvider<MseTaskScheduler> schedulerProvider;

    private final long intervalMs;

    private final int batchSize;

    private final long recheckIntervalMs;

    /** 首触异步执行器（单线程足够：每租户一次幂等账本检查） */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "tenant-data-compensate");
        thread.setDaemon(true);
        return thread;
    });

    /** 执行中租户（防周期与首触/事件通道重入同一租户） */
    private final Set<Long> inflight = ConcurrentHashMap.newKeySet();

    /** 本进程已确认就绪的共享库租户（recheck 周期清空重查） */
    private final Set<Long> doneSet = ConcurrentHashMap.newKeySet();

    private final AtomicLong lastRecheckAt = new AtomicLong(System.currentTimeMillis());

    private final AtomicBoolean closed = new AtomicBoolean(false);

    @RpcReference
    private RemoteTenantService remoteTenantService;

    public TenantDataCompensator(ObjectProvider<TenantDataMigrator> migratorProvider,
                                 ObjectProvider<MseTaskScheduler> schedulerProvider,
                                 long intervalMs, int batchSize, long recheckIntervalMs) {
        this.migratorProvider = migratorProvider;
        this.schedulerProvider = schedulerProvider;
        this.intervalMs = intervalMs;
        this.batchSize = Math.max(1, batchSize);
        this.recheckIntervalMs = recheckIntervalMs;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (intervalMs <= 0) {
            log.info("[mse-bridge] 每租户种子周期补偿已关闭（compensate-interval-ms<=0），仅首触通道生效");
            return;
        }
        MseTaskScheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            log.info("[mse-bridge] 调度器未装配（mse.scheduling.mode=none 等），每租户种子补偿仅首触通道生效");
            return;
        }
        scheduler.register(TaskDefinition.fixedDelay(TASK_ID, "每租户种子补偿",
                        "周期为共享库租户补跑 pending 每租户脚本（独立库租户由懒初始化直建覆盖；"
                                + "首触/事件通道之外的自愈兜底）", 0, intervalMs)
                        .category(TaskDefinition.CATEGORY_PLATFORM),
                this::tickSafely);
    }

    /** 首触/事件通道入口：异步补偿单个租户（含独立库过滤与幂等），秒级到分钟级不阻塞调用方 */
    public void enqueue(Long tenantId, String source) {
        if (tenantId == null || closed.get()) {
            return;
        }
        if (!inflight.add(tenantId)) {
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    if (compensate(tenantId, source)) {
                        doneSet.add(tenantId);
                    }
                } finally {
                    inflight.remove(tenantId);
                }
            });
        } catch (RejectedExecutionException e) {
            inflight.remove(tenantId);
            log.debug("[mse-bridge] 每租户种子补偿任务提交被拒（补偿器已关闭）: tenant={} {}", tenantId, source);
        }
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Throwable e) {
            log.warn("[mse-bridge] 每租户种子周期补偿本轮失败（下轮重试）: {}", e.getMessage());
        }
    }

    /** 周期轮：拉全量租户 → 共享库租户且未确认 → 每轮至多 batchSize 个 */
    private void tick() {
        if (System.currentTimeMillis() - lastRecheckAt.get() >= recheckIntervalMs
                && lastRecheckAt.compareAndSet(lastRecheckAt.get(), System.currentTimeMillis())) {
            doneSet.clear();
        }
        List<TenantDTO> tenants = remoteTenantService.listTenants();
        if (tenants == null || tenants.isEmpty()) {
            return;
        }
        List<Long> pending = tenants.stream()
                .filter(t -> t != null && t.getId() != null)
                .filter(t -> t.getDsKey() == null || t.getDsKey().isBlank())
                .map(TenantDTO::getId)
                .filter(id -> !doneSet.contains(id) && !inflight.contains(id))
                .limit(batchSize)
                .toList();
        if (pending.isEmpty()) {
            return;
        }
        log.debug("[mse-bridge] 每租户种子补偿本轮候选 {} 个（全量 {} 租户）", pending.size(), tenants.size());
        for (Long tenantId : pending) {
            if (compensate(tenantId, "tick")) {
                doneSet.add(tenantId);
            }
        }
    }

    /**
     * 补偿单租户：独立库租户跳过（懒初始化直建覆盖）；共享库租户补跑 pending 脚本。
     *
     * @return true=已就绪或确认无需补偿（可记 doneSet）；false=失败（下轮重试）
     */
    private boolean compensate(Long tenantId, String source) {
        TenantDataMigrator migrator = migratorProvider.getIfAvailable();
        if (migrator == null) {
            return false;
        }
        try {
            TenantDTO tenant = remoteTenantService.getTenantById(tenantId);
            if (tenant != null && tenant.getDsKey() != null && !tenant.getDsKey().isBlank()) {
                // 独立库租户：每租户种子由懒初始化直建通道在租户库内执行，共享库不播孤儿行
                log.debug("[mse-bridge] 独立库租户跳过共享库种子补偿: tenant={} dsKey={} source={}",
                        tenantId, tenant.getDsKey(), source);
                return true;
            }
        } catch (Exception e) {
            log.debug("[mse-bridge] 租户信息查询失败（按共享库租户继续补偿）: tenant={} {} {}", tenantId, source, e.getMessage());
        }
        try {
            TenantSchemaMigrator.Result result = migrator.migrateTenant(tenantId);
            if (result.applied() > 0 || result.failed() > 0 || result.conflicts() > 0) {
                log.info("[mse-bridge] 每租户种子补偿: tenant={} source={} 应用 {}、失败 {}、冲突 {}",
                        tenantId, source, result.applied(), result.failed(), result.conflicts());
            }
            return result.failed() == 0 && result.conflicts() == 0;
        } catch (Exception e) {
            log.warn("[mse-bridge] 每租户种子补偿失败（下轮重试）: tenant={} source={} {}", tenantId, source, e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            executor.shutdownNow();
        }
    }
}
