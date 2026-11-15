package vip.mseapp.core.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 租户级任务并发闸门（防噪声邻居）：按 tenantId 维度的计数限并发，纯 JDK 无外部依赖。
 *
 * <p>语义 = 非阻塞 tryAcquire：拿不到许可立即返回 false，调用方本轮跳过——
 * 扫描驱动的任务行状态未变，下一轮扫描自愈重捞，不丢不重。
 * 闸门实例按执行域隔离持有（如 data 同步池、oa webhook 工作池各自一个），跨域互不挤占。
 *
 * <p>上限运行期可热更（{@link #updateLimit}）：per-tenant 计数 + volatile 上限的计数器实现
 * （非 Semaphore——其 permits 构造期固化）；缩小时已占许可不剥夺、随自然释放收敛。
 * 热更通道经统一调度中心「调度器」设置页（Redis 键 {@code mse:schedule:gates}，
 * 实例监控线程按 {@link #key()} 差量应用），key 为 null 的闸门不参与热更。
 */
public final class TenantTaskGates {

    private final String key;

    /** 每租户并发上限（volatile：热更通道运行期可改） */
    private volatile int limit;

    /** 租户当前占用计数（tryAcquire 增、release 减） */
    private final Map<Long, AtomicInteger> counts = new ConcurrentHashMap<>();

    public TenantTaskGates(int permitsPerTenant) {
        this(null, permitsPerTenant);
    }

    /** @param key 热更参数匹配标识（如 data；null=不参与热更） */
    public TenantTaskGates(String key, int permitsPerTenant) {
        this.key = key == null || key.isBlank() ? null : key;
        this.limit = Math.max(1, permitsPerTenant);
    }

    /** 热更参数匹配标识（null=不参与热更） */
    public String key() {
        return key;
    }

    /** 每租户当前并发上限 */
    public int permitsPerTenant() {
        return limit;
    }

    /** 运行期更新每租户并发上限（缩小不剥夺已占许可，随自然释放收敛） */
    public void updateLimit(int permitsPerTenant) {
        this.limit = Math.max(1, permitsPerTenant);
    }

    /** 尝试为租户占用一个并发许可；false=该租户并发已满（null 租户不设限） */
    public boolean tryAcquire(Long tenantId) {
        if (tenantId == null) {
            return true;
        }
        AtomicInteger count = counts.computeIfAbsent(tenantId, k -> new AtomicInteger());
        int current = count.get();
        while (current < limit) {
            if (count.compareAndSet(current, current + 1)) {
                return true;
            }
            current = count.get();
        }
        return false;
    }

    /** 归还租户并发许可（与 tryAcquire 配对，放 finally） */
    public void release(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        AtomicInteger count = counts.get(tenantId);
        if (count != null) {
            count.decrementAndGet();
        }
    }

    /**
     * 组合便捷式：tryAcquire 成功则提交任务到执行器（执行完自动归还许可），失败返回 false 不提交；
     * 提交本身失败（池已关闭等 RejectedExecutionException）同样归还许可后原样抛出。
     */
    public boolean tryRun(Long tenantId, ExecutorService executor, Runnable task) {
        if (!tryAcquire(tenantId)) {
            return false;
        }
        try {
            executor.submit(() -> {
                try {
                    task.run();
                } finally {
                    release(tenantId);
                }
            });
            return true;
        } catch (RuntimeException e) {
            release(tenantId);
            throw e;
        }
    }
}
