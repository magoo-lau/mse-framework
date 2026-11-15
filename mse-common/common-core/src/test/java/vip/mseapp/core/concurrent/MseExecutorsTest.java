package vip.mseapp.core.concurrent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.TenantContextPropagation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link MseExecutors} 统一线程池工厂约定验证：命名（prefix-N）、有界队列强制（未声明容量拒绝创建）、
 * 缺省 CallerRuns 背压、上下文包装缺省开（池内任务恢复提交线程租户）/刻意关（不传播）、
 * max>=core 校验。四约定是 context-propagation 规范的结构化落地，新池禁裸建。
 */
class MseExecutorsTest {

    @BeforeAll
    static void register() {
        TenantContextPropagation.register();
    }

    @AfterEach
    void clean() {
        TenantContext.clear();
    }

    @Test
    void requiresBoundedQueueCapacity() {
        assertThrows(IllegalArgumentException.class, () -> MseExecutors.builder("test-pool").build());
        assertThrows(IllegalArgumentException.class,
                () -> MseExecutors.builder("test-pool").queueCapacity(0).build());
    }

    @Test
    void rejectsMaxBelowCore() {
        assertThrows(IllegalArgumentException.class, () -> MseExecutors.builder("test-pool")
                .core(2).max(1).queueCapacity(8).build());
    }

    @Test
    void namesThreadsByPrefix() throws Exception {
        ExecutorService pool = MseExecutors.builder("mse-test-named").core(1).max(1).queueCapacity(4).build();
        try {
            AtomicReference<String> name = new AtomicReference<>();
            pool.submit(() -> name.set(Thread.currentThread().getName())).get(5, TimeUnit.SECONDS);
            assertThat(name.get()).startsWith("mse-test-named-");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void defaultWrapCarriesTenantContext() throws Exception {
        ExecutorService pool = MseExecutors.singleThread("mse-test-wrap", 4);
        TenantContext.setTenantId(200L);
        try {
            AtomicReference<Long> seen = new AtomicReference<>(null);
            pool.submit(() -> seen.set(TenantContext.getTenantId())).get(5, TimeUnit.SECONDS);
            assertThat(seen.get()).as("缺省 contextWrap=true：池内任务恢复提交线程租户").isEqualTo(200L);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void noContextVariantDoesNotCarryTenant() throws Exception {
        // 刻意无租户：先跑一个空任务占住线程（线程在无租户时创建），再带租户提交——不传播
        ExecutorService pool = MseExecutors.singleThreadNoContext("mse-test-nowrap", 4);
        try {
            pool.submit(() -> {
            }).get(5, TimeUnit.SECONDS);
            TenantContext.setTenantId(200L);
            AtomicReference<Long> seen = new AtomicReference<>(Long.MIN_VALUE);
            pool.submit(() -> seen.set(TenantContext.getTenantId())).get(5, TimeUnit.SECONDS);
            assertThat(seen.get()).as("contextWrap(false)：跨租户扫描执行域不接收提交线程租户").isNull();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void abortRejectionSurfacesWhenQueueFull() throws Exception {
        // 单线程 + 容量 1 队列 + Abort：占满线程与队列后第 3 个提交必须显式失败（验证有界语义可观测）
        ExecutorService pool = MseExecutors.builder("mse-test-abort")
                .core(1).max(1).queueCapacity(1)
                .rejection(new ThreadPoolExecutor.AbortPolicy())
                .build();
        try {
            java.util.concurrent.CountDownLatch hold = new java.util.concurrent.CountDownLatch(1);
            pool.submit(() -> {
                try {
                    hold.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            });                                      // 占住唯一线程
            pool.submit(() -> {
            });                                  // 占住队列
            assertThrows(RejectedExecutionException.class, () -> pool.submit(() -> {
            }));
            hold.countDown();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void defaultCallerRunsProvidesBackpressure() throws Exception {
        // 缺省 CallerRuns：队列满时提交线程自己执行，任务不丢
        ExecutorService pool = MseExecutors.builder("mse-test-callerruns")
                .core(1).max(1).queueCapacity(1).build();
        try {
            java.util.concurrent.CountDownLatch hold = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicInteger ran = new java.util.concurrent.atomic.AtomicInteger();
            Runnable task = () -> ran.incrementAndGet();
            Runnable held = () -> {
                try {
                    hold.await();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            };            pool.submit(held);   // 占住线程
            pool.submit(task);   // 进队列
            pool.submit(task);   // 队列满 → CallerRuns 在提交线程（本测试线程）同步执行
            assertThat(ran.get()).as("队列满时 CallerRuns 兜底执行").isGreaterThanOrEqualTo(1);
            hold.countDown();
        } finally {
            pool.shutdownNow();
        }
    }
}
