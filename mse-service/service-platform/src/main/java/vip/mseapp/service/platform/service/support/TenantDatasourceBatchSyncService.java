package vip.mseapp.service.platform.service.support;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.service.platform.entity.SysTenantDatasource;
import vip.mseapp.service.platform.mapper.SysTenantDatasourceMapper;
import vip.mseapp.service.platform.service.SysTenantDatasourceService.BatchSyncItemView;
import vip.mseapp.service.platform.service.SysTenantDatasourceService.BatchSyncStatusView;
import vip.mseapp.service.platform.service.SysTenantDatasourceService.DatasourceView;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 租户数据源批量同步（自 SysTenantDatasourceServiceImpl 拆出的内部协作者，业务工程勿依赖）：
 * 逐库「迁移 + 结构比对」的后台串行执行、Redis hash 进度双写（进程内兜底）与批次状态读取。
 */
// 控制面协作者：类级 NOT_SUPPORTED 防错库堤坝，勿删（范式见 SysTenantRouteServiceImpl 类注释）
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class TenantDatasourceBatchSyncService {

    private static final Logger log = LoggerFactory.getLogger(TenantDatasourceBatchSyncService.class);

    /** 批量同步进度 Redis hash：field=dsKey（或 __meta__），value=「status|Base64(message)」（消息含换行/引号，Base64 免转义） */
    private static final String BATCH_PROGRESS_KEY = "mse:platform:batch-sync";

    private static final String BATCH_META_FIELD = "__meta__";

    /** 批量同步进度保留时长（Redis；过期自动清理，界面只关心最近一批） */
    private static final Duration BATCH_PROGRESS_TTL = Duration.ofHours(24);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final SysTenantDatasourceMapper datasourceMapper;
    private final TenantSchemaOpsService schemaOps;

    /** 批量同步后台执行（单线程串行逐库；低频管理动作，有界队列防积压（满则提交失败），逐库跨实例互斥由统一锁兜底） */
    private final ExecutorService batchSyncExecutor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS,
            new java.util.concurrent.ArrayBlockingQueue<>(2),
            r -> {
                Thread thread = new Thread(r, "tenant-ds-batch-sync");
                thread.setDaemon(true);
                return thread;
            },
            new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

    /** 批次进行中标记（同一时刻至多一个批次；提交/执行/收尾全路径复位） */
    private final AtomicBoolean batchSyncRunning = new AtomicBoolean(false);

    /** 无 Redis 兜底：进程内最近一批进度（平台端通常单实例，可见性足够） */
    private final Map<String, String> batchProgressFallback = new ConcurrentHashMap<>();

    public TenantDatasourceBatchSyncService(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                            SysTenantDatasourceMapper datasourceMapper,
                                            TenantSchemaOpsService schemaOps) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.datasourceMapper = datasourceMapper;
        this.schemaOps = schemaOps;
    }

    /**
     * 批量同步（异步）：逐库「迁移 + 结构比对」耗时随库数量线性增长，同步等待必撞前端超时——
     * 本方法只登记批次（校验迁移器装配 + 无进行中批次校验 + 进度初始化）即返回，逐库执行交后台
     * 单线程，进度经 {@link #status()} 轮询，逐库结果同时落行 sync_*（与单库同步同一通道）。
     * 目标清单由调用方（门面）经 listOptions 收集过滤。
     */
    public String submit(List<Long> ids, List<DatasourceView> targets) {
        schemaOps.requireMigrator();
        if (targets.isEmpty()) {
            return "无可同步的界面数据源（内置主库由各服务启动自动迁移负责）";
        }
        if (!batchSyncRunning.compareAndSet(false, true)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "已有批量同步批次在执行，请经批次状态轮询等待完成后再发起");
        }
        try {
            publishBatchMeta(targets.size(), 0, 0, true);
            for (DatasourceView t : targets) {
                publishBatchItem(t.dsKey(), null, "排队中");
            }
            batchSyncExecutor.execute(() -> runBatchSync(ids, targets));
        } catch (RuntimeException e) {
            batchSyncRunning.set(false);
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR.getCode(), "批量同步任务提交失败，请稍后重试");
        }
        return "批量同步任务已提交（" + targets.size() + " 个数据源，后台逐库执行、失败不中断），进度经批次状态轮询查看";
    }

    /** 批次执行体：逐库调用单库 syncSchema（状态落行/锁语义完全复用），失败不中断，收尾复位批次标记 */
    private void runBatchSync(List<Long> ids, List<DatasourceView> targets) {
        int done = 0;
        int failed = 0;
        try {
            for (int i = 0; i < targets.size(); i++) {
                DatasourceView target = targets.get(i);
                String message;
                try {
                    SysTenantDatasource ds = datasourceMapper.selectById(ids.get(i));
                    if (ds == null) {
                        throw new ServiceException(GlobalErrorCode.NOT_FOUND);
                    }
                    message = schemaOps.syncSchema(ids.get(i), ds);
                    done++;
                    publishBatchItem(target.dsKey(), 1, message);
                } catch (Exception e) {
                    message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                    failed++;
                    publishBatchItem(target.dsKey(), 2, message);
                }
                publishBatchMeta(targets.size(), done, failed, true);
            }
        } finally {
            publishBatchMeta(targets.size(), done, failed, false);
            batchSyncRunning.set(false);
            log.info("[mse-tenant-ds] 批量同步批次完成: 共 {} 库、成功 {}、失败 {}", targets.size(), done, failed);
        }
    }

    /** 批次元信息写入（Redis hash + 进程内兜底双写，TTL 24h） */
    private void publishBatchMeta(int total, int done, int failed, boolean running) {
        String encoded = (running ? 1 : 0) + "|" + total + "|" + done + "|" + failed;
        publishProgress(BATCH_META_FIELD, encoded);
    }

    /** 逐库进度写入（message 含换行/引号，Base64 免转义） */
    private void publishBatchItem(String dsKey, Integer status, String message) {
        String encoded = (status == null ? "" : String.valueOf(status)) + "|" + Base64.getEncoder()
                .encodeToString((message == null ? "" : message).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        publishProgress(dsKey, encoded);
    }

    /** 进度双写：Redis hash（跨实例可见）+ 进程内 map（无 Redis 兜底） */
    private void publishProgress(String field, String value) {
        batchProgressFallback.put(field, value);
        StringRedisTemplate template = redisTemplateProvider.getIfAvailable();
        if (template != null) {
            try {
                template.opsForHash().put(BATCH_PROGRESS_KEY, field, value);
                template.expire(BATCH_PROGRESS_KEY, BATCH_PROGRESS_TTL);
            } catch (Exception e) {
                log.debug("[mse-tenant-ds] 批量同步进度写 Redis 失败（进程内兜底）: {}", e.getMessage());
            }
        }
    }

    /** 批次进度读取（轮询通道）：优先 Redis，缺失回落进程内；无任何批次记录时返回空视图 */
    public BatchSyncStatusView status() {
        Map<String, String> raw = new LinkedHashMap<>();
        StringRedisTemplate template = redisTemplateProvider.getIfAvailable();
        if (template != null) {
            try {
                for (Map.Entry<Object, Object> entry : template.opsForHash().entries(BATCH_PROGRESS_KEY).entrySet()) {
                    raw.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            } catch (Exception e) {
                log.debug("[mse-tenant-ds] 批量同步进度读 Redis 失败（回落进程内）: {}", e.getMessage());
            }
        }
        if (raw.isEmpty()) {
            raw.putAll(batchProgressFallback);
        }
        boolean running = batchSyncRunning.get();
        int total = 0;
        int done = 0;
        int failed = 0;
        List<BatchSyncItemView> items = new ArrayList<>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String value = entry.getValue();
            if (BATCH_META_FIELD.equals(entry.getKey())) {
                String[] meta = value.split("\\|");
                if (meta.length == 4) {
                    running = "1".equals(meta[0]) || running;
                    total = Integer.parseInt(meta[1]);
                    done = Integer.parseInt(meta[2]);
                    failed = Integer.parseInt(meta[3]);
                }
                continue;
            }
            String[] parts = value.split("\\|", 2);
            Integer status = parts[0].isEmpty() ? null : Integer.valueOf(parts[0]);
            String message = "";
            if (parts.length == 2 && !parts[1].isEmpty()) {
                message = new String(Base64.getDecoder().decode(parts[1]), java.nio.charset.StandardCharsets.UTF_8);
            }
            items.add(new BatchSyncItemView(entry.getKey(), null, null, status, message));
        }
        return new BatchSyncStatusView(running, total, done, failed, items);
    }

    @PreDestroy
    public void shutdownBatchSyncExecutor() {
        batchSyncExecutor.shutdownNow();
    }
}
