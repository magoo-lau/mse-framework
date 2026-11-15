package vip.mseapp.core.context;

import io.micrometer.context.ContextExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 租户上下文跨线程传播（micrometer context-propagation 适配）：
 * 经 {@code ContextExecutorService.wrap} 包装的线程池在任务执行时恢复提交线程的租户号；
 * IGNORE 刻意不传播（「跳过隔离」是调用点临时语义，不得跨线程泄漏）。
 *
 * <p>桩线程池除上述约定外还有一个易混淆机制须隔离：TenantContext 的 ThreadLocal 是
 * TransmittableThreadLocal（继承 InheritableThreadLocal），<b>线程创建瞬间</b>会天然继承
 * 创建线程的上下文——与 wrap/裸池无关。因此每个用例先在无上下文时预热单线程池（规避创建期继承），
 * 再验证复用线程上的传播语义（生产池通常已预热，这正是关注场景）。
 */
class TenantContextPropagationTest {

    @BeforeAll
    static void register() {
        TenantContextPropagation.register();
    }

    @AfterEach
    void clean() {
        TenantContext.clear();
    }

    /** 单线程池先空跑一个任务：工作线程在无上下文时创建，排除 ITL 创建期继承的干扰 */
    private static ExecutorService warmed(boolean wrapped) throws Exception {
        ExecutorService pool = wrapped
                ? ContextExecutorService.wrap(Executors.newSingleThreadExecutor())
                : Executors.newSingleThreadExecutor();
        TenantContext.clear();
        pool.submit(() -> {
        }).get(5, TimeUnit.SECONDS);
        return pool;
    }

    @Test
    void wrappedExecutorCarriesTenantId() throws Exception {
        ExecutorService pool = warmed(true);
        TenantContext.setTenantId(200L);
        try {
            AtomicReference<Long> seen = new AtomicReference<>(null);
            pool.submit(() -> seen.set(TenantContext.getTenantId())).get(5, TimeUnit.SECONDS);
            assertThat(seen.get()).as("wrap 池内任务恢复提交线程的租户号").isEqualTo(200L);
        } finally {
            pool.shutdownNow();
        }
        assertThat(TenantContext.getTenantId()).as("提交线程自身上下文不受影响").isEqualTo(200L);
    }

    @Test
    void workerThreadIsCleanAfterTask() throws Exception {
        ExecutorService pool = warmed(true);
        try {
            // 带租户任务执行完毕后，工作线程租户恢复为空（快照 close 走 reset 语义）
            TenantContext.setTenantId(200L);
            pool.submit(() -> {
            }).get(5, TimeUnit.SECONDS);
            TenantContext.clear();
            AtomicReference<Long> seen = new AtomicReference<>(Long.MIN_VALUE);
            pool.submit(() -> seen.set(TenantContext.getTenantId())).get(5, TimeUnit.SECONDS);
            assertThat(seen.get()).as("上一任务的租户不得残留到下一任务").isNull();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void ignoreFlagIsDeliberatelyNotPropagated() throws Exception {
        ExecutorService pool = warmed(true);
        TenantContext.setTenantId(1L);
        TenantContext.setIgnoreTenant(true);
        try {
            AtomicReference<Boolean> ignoreSeen = new AtomicReference<>(null);
            AtomicReference<Long> tenantSeen = new AtomicReference<>(null);
            pool.submit(() -> {
                ignoreSeen.set(TenantContext.isIgnoreTenant());
                tenantSeen.set(TenantContext.getTenantId());
            }).get(5, TimeUnit.SECONDS);
            assertThat(tenantSeen.get()).as("租户号正常传播").isEqualTo(1L);
            assertThat(ignoreSeen.get()).as("IGNORE 不跨线程传播（防跳过隔离语义泄漏）").isFalse();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void unwrappedExecutorDoesNotCarryTenantId() throws Exception {
        // 反例锚点：裸（未 wrap）线程池不传播——复用线程上租户号丢失，
        // 这正是业务侧自建池必须 wrap（或提交点显式 runAs）的原因
        ExecutorService pool = warmed(false);
        TenantContext.setTenantId(200L);
        try {
            AtomicReference<Long> seen = new AtomicReference<>(Long.MIN_VALUE);
            pool.submit(() -> seen.set(TenantContext.getTenantId())).get(5, TimeUnit.SECONDS);
            assertThat(seen.get()).as("裸池复用线程上租户号丢失").isNull();
        } finally {
            pool.shutdownNow();
        }
    }
}
