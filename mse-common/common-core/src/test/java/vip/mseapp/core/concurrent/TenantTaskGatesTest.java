package vip.mseapp.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TenantTaskGates} 租户并发闸门：tryAcquire/release 配对、按租户独立限流、
 * null 租户不设限、热更缩容不剥夺、tryRun 组合式（满则不提交/finally 归还/提交失败归还）。
 */
class TenantTaskGatesTest {

    @Test
    void acquireReleasePerTenantIsolation() {
        TenantTaskGates gates = new TenantTaskGates("test", 2);
        assertTrue(gates.tryAcquire(100L));
        assertTrue(gates.tryAcquire(100L));
        assertFalse(gates.tryAcquire(100L));   // 租户 100 已满
        assertTrue(gates.tryAcquire(200L));    // 租户 200 独立配额
        gates.release(100L);
        assertTrue(gates.tryAcquire(100L));    // 归还后可再取
    }

    @Test
    void nullTenantIsUnlimited() {
        TenantTaskGates gates = new TenantTaskGates(1);
        for (int i = 0; i < 10; i++) {
            assertTrue(gates.tryAcquire(null), "null 租户（跨租户/无上下文任务）不设限");
        }
    }

    @Test
    void shrinkDoesNotDepriveHeldPermits() {
        TenantTaskGates gates = new TenantTaskGates("test", 3);
        assertTrue(gates.tryAcquire(100L));
        assertTrue(gates.tryAcquire(100L));
        assertTrue(gates.tryAcquire(100L));
        gates.updateLimit(1);                  // 热更缩容：已占 3 个不剥夺
        assertThat(gates.permitsPerTenant()).isEqualTo(1);
        assertFalse(gates.tryAcquire(100L));   // 收敛期内仍满
        gates.release(100L);
        gates.release(100L);
        gates.release(100L);
        assertTrue(gates.tryAcquire(100L));    // 自然释放后按新上限
        assertFalse(gates.tryAcquire(100L));
    }

    @Test
    void tryRunSkipsWhenFullAndReleasesAfterTask() throws Exception {
        TenantTaskGates gates = new TenantTaskGates("test", 1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            AtomicInteger ran = new AtomicInteger();
            java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
            assertTrue(gates.tryRun(100L, pool, () -> {
                try {
                    release.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }));                                  // 占住唯一许可
            assertFalse(gates.tryRun(100L, pool, ran::incrementAndGet));   // 满：不提交
            assertThat(ran.get()).isZero();
            release.countDown();
            assertTrue(gates.tryRun(100L, pool, ran::incrementAndGet));    // 任务完成后许可已归还
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
            assertThat(ran.get()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void tryRunReleasesPermitOnSubmitRejection() {
        TenantTaskGates gates = new TenantTaskGates("test", 1);
        assertTrue(gates.tryAcquire(100L));   // 占住许可，tryRun 应仍可提交（许可独立）——改用已关池验证归还
        ExecutorService dead = Executors.newSingleThreadExecutor();
        dead.shutdownNow();
        assertThrows(RejectedExecutionException.class, () -> gates.tryRun(200L, dead, () -> {
        }));
        // 提交失败后租户 200 的许可已归还：可再次尝试（此处用新池验证配额未被扣）
        ExecutorService fresh = Executors.newSingleThreadExecutor();
        try {
            assertTrue(gates.tryRun(200L, fresh, () -> {
            }));
        } finally {
            fresh.shutdownNow();
        }
    }
}
