package vip.mseapp.core.schedule;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 调度与执行分离（卡死隔离）验证：任务体在执行池跑、触发池只提交——
 * 一个卡死任务体（永久阻塞）不得影响其他任务的周期执行。
 */
class LocalMseTaskSchedulerTest {

    @Test
    void stuckTaskBodyDoesNotStallOtherTasks() throws Exception {
        try (LocalMseTaskScheduler scheduler = new LocalMseTaskScheduler(
                1, 2, 8, 0, null, null, null, false)) {
            // 任务 A：首跑即永久阻塞（模拟无超时 HTTP/死锁——旧实现会占死唯一调度线程）
            AtomicInteger stuckRuns = new AtomicInteger();
            CountDownLatch stuckStarted = new CountDownLatch(1);
            scheduler.register(TaskDefinition.fixedDelay("stuck", "卡死任务", 1_000, 1_000),
                    () -> {
                        stuckRuns.incrementAndGet();
                        stuckStarted.countDown();
                        try {
                            Thread.sleep(Long.MAX_VALUE);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
            // 任务 B：50ms 间隔持续跑
            AtomicInteger beatRuns = new AtomicInteger();
            scheduler.register(TaskDefinition.fixedDelay("beat", "心跳任务", 0, 50),
                    beatRuns::incrementAndGet);
            scheduler.start();
            assertTrue(stuckStarted.await(5, TimeUnit.SECONDS), "卡死任务应已开跑");
            Thread.sleep(1_500);
            // 无隔离的旧实现：触发池被 A 占死，B 一次都不会跑
            assertTrue(beatRuns.get() >= 5, "心跳任务不应被卡死任务拖垮，实跑 " + beatRuns.get() + " 次");
            assertEquals(1, stuckRuns.get(), "卡死任务同任务不重叠（链式：体未完成不排下一轮）");
        }
    }

    @Test
    void triggerNowRunsBodyOnWorkerPool() throws Exception {
        try (LocalMseTaskScheduler scheduler = new LocalMseTaskScheduler(
                1, 2, 8, 0, null, null, null, false)) {
            AtomicInteger beatRuns = new AtomicInteger();
            scheduler.register(TaskDefinition.fixedDelay("beat", "心跳", 0, 50), beatRuns::incrementAndGet);
            scheduler.register(TaskDefinition.startup("manual", "手动", ""), () -> {
            });
            scheduler.start();
            scheduler.triggerNow("manual");
            // manual 无任务体断言 latch——以 triggerNow 返回 true 且后续快照有记录为准
            assertTrue(scheduler.triggerNow("beat"));
            Thread.sleep(300);
            assertTrue(beatRuns.get() >= 1);
            List<TaskState> snapshot = scheduler.snapshot();
            assertTrue(snapshot.stream().anyMatch(s -> s.getTaskId().equals("beat")));
        }
    }

    @Test
    void centralModeDoesNotSchedulePeriodicTasksLocally() throws Exception {
        try (LocalMseTaskScheduler scheduler = new LocalMseTaskScheduler(
                1, 2, 8, 0, null, null, null, true)) {
            AtomicInteger periodicRuns = new AtomicInteger();
            AtomicInteger startupRuns = new AtomicInteger();
            scheduler.register(TaskDefinition.fixedDelay("periodic", "周期", 0, 50), periodicRuns::incrementAndGet);
            scheduler.register(TaskDefinition.startup("boot", "启动", ""), startupRuns::incrementAndGet);
            scheduler.start();
            Thread.sleep(500);
            assertEquals(0, periodicRuns.get(), "central 模式周期任务不本地排程");
            assertTrue(startupRuns.get() >= 1, "STARTUP 任务仍本地执行");
            // 中央派发入口照常：triggerNow 可跑
            scheduler.triggerNow("periodic");
            Thread.sleep(200);
            assertEquals(1, periodicRuns.get(), "central 模式手动/中央派发入口可用");
        }
    }

    @Test
    void unregisterStopsFiringAndRemovesFromSnapshot() throws Exception {
        try (LocalMseTaskScheduler scheduler = new LocalMseTaskScheduler(
                1, 2, 8, 0, null, null, null, false)) {
            AtomicInteger runs = new AtomicInteger();
            scheduler.register(TaskDefinition.fixedDelay("temp", "临时", 0, 50), runs::incrementAndGet);
            scheduler.start();
            Thread.sleep(300);
            int before = runs.get();
            assertTrue(before >= 2, "注销前应有周期执行");
            assertTrue(scheduler.unregister("temp"));
            Thread.sleep(300);
            // 注销瞬间在跑的一代允许收尾触发一次（否则计时竞态偶发失败）；收尾后必须完全停止
            assertTrue(runs.get() <= before + 1, "注销后最多再触发一次（注销瞬间在跑的一代收尾）");
            int settled = runs.get();
            Thread.sleep(200);
            assertEquals(settled, runs.get(), "收尾一代自灭后不再触发（旧代续链因代数不符自灭）");
            assertTrue(scheduler.snapshot().stream().noneMatch(s -> s.getTaskId().equals("temp")),
                    "快照不含已注销任务");
            assertFalse(scheduler.unregister("temp"), "重复注销返回 false");
        }
    }

    @Test
    void registerOrUpdateReplayIsNoOpAndChangeReTimes() throws Exception {
        try (LocalMseTaskScheduler scheduler = new LocalMseTaskScheduler(
                1, 2, 8, 0, null, null, null, false)) {
            AtomicInteger runs = new AtomicInteger();
            scheduler.registerOrUpdate(TaskDefinition.fixedDelay("rep", "重放", 0, 50), runs::incrementAndGet);
            scheduler.start();
            Thread.sleep(300);
            assertTrue(runs.get() >= 3, "50ms 间隔应已多次执行");
            // 同定义重放：no-op（不打扰运行中的链）
            assertFalse(scheduler.registerOrUpdate(TaskDefinition.fixedDelay("rep", "重放", 0, 50), runs::incrementAndGet));
            // 定义变化（50ms → 1000ms）：无覆盖，按新定义重排
            assertTrue(scheduler.registerOrUpdate(TaskDefinition.fixedDelay("rep", "重放", 0, 1_000), runs::incrementAndGet));
            int atChange = runs.get();
            Thread.sleep(600);
            assertTrue(runs.get() <= atChange + 1, "重排为 1000ms 后 600ms 窗口内应基本不再触发，实跑 "
                    + (runs.get() - atChange) + " 次");
        }
    }

    @Test
    void registerOrUpdateDoesNotStompActiveOverride() throws Exception {
        try (LocalMseTaskScheduler scheduler = new LocalMseTaskScheduler(
                1, 2, 8, 0, null, null, null, false)) {
            AtomicInteger runs = new AtomicInteger();
            scheduler.register(TaskDefinition.fixedDelay("rep", "重放", 0, 50), runs::incrementAndGet);
            scheduler.start();
            Thread.sleep(200);
            // 管理面覆盖为 1500ms：生效调度变慢
            assertTrue(scheduler.applySchedule("rep", "1500"));
            int overridden = runs.get();
            Thread.sleep(700);
            assertTrue(runs.get() <= overridden + 1, "覆盖 1500ms 后应接近停止");
            // 对账重放新定义（80ms）：覆盖生效中——只更新代码默认，不打断覆盖
            assertTrue(scheduler.registerOrUpdate(TaskDefinition.fixedDelay("rep", "重放", 0, 80), runs::incrementAndGet));
            int afterUpdate = runs.get();
            Thread.sleep(500);
            assertTrue(runs.get() <= afterUpdate + 1, "覆盖生效中更新代码默认不得打断覆盖");
            // 覆盖清除：还原到最新代码默认（80ms，而非注册时的 50ms）
            assertTrue(scheduler.applySchedule("rep", ""));
            int cleared = runs.get();
            Thread.sleep(500);
            assertTrue(runs.get() >= cleared + 3, "覆盖清除后应按最新代码默认（80ms）恢复快节奏");
        }
    }
}
