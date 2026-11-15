package vip.mseapp.job.dispatch;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 薄调度中心到期判定单测：生效表达式解析（覆盖/渲染值三形态）+ 间隔型/cron 型到期判定。
 */
class JobDispatchCenterTest {

    @Test
    void parseSupportsIntervalDigitsEveryAndCron() {
        JobDispatchCenter.EffectiveExpr digits = JobDispatchCenter.parse("30000");
        assertNotNull(digits);
        assertNotNull(digits.intervalMs());
        assertTrue(digits.intervalMs() == 30000);

        JobDispatchCenter.EffectiveExpr every = JobDispatchCenter.parse("~every 45s");
        assertNotNull(every);
        assertTrue(every.intervalMs() == 45000);

        // fixedDelay 渲染的等价 cron（Spring CronExpression 接受 ? 记法）
        JobDispatchCenter.EffectiveExpr cron = JobDispatchCenter.parse("*/20 * * * * ?");
        assertNotNull(cron);
        assertNotNull(cron.cron());

        assertNull(JobDispatchCenter.parse(null));
        assertNull(JobDispatchCenter.parse("  "));
        assertNull(JobDispatchCenter.parse("not a cron"));
        assertNull(JobDispatchCenter.parse("~every Xs"));
    }

    @Test
    void intervalDueJudgement() {
        JobDispatchCenter.EffectiveExpr expr = JobDispatchCenter.parse("20000");
        LocalDateTime now = LocalDateTime.of(2026, 10, 1, 12, 0, 0);
        // 从未执行：到期（首跑）
        assertTrue(JobDispatchCenter.isDue(expr, null, now));
        // 15s 前跑过：未到 20s 间隔
        assertFalse(JobDispatchCenter.isDue(expr, now.minusSeconds(15), now));
        // 20s 前跑过：到期
        assertTrue(JobDispatchCenter.isDue(expr, now.minusSeconds(20), now));
        // 1 分钟前跑过：到期
        assertTrue(JobDispatchCenter.isDue(expr, now.minusSeconds(60), now));
    }

    @Test
    void cronDueJudgement() {
        // 每分钟第 0 秒
        JobDispatchCenter.EffectiveExpr expr = JobDispatchCenter.parse("0 * * * * ?");
        LocalDateTime lastRun = LocalDateTime.of(2026, 10, 1, 12, 0, 0);
        // 12:00:00 跑过，12:00:30 未到 12:01:00
        assertFalse(JobDispatchCenter.isDue(expr, lastRun, lastRun.plusSeconds(30)));
        // 12:01:00 到点
        assertTrue(JobDispatchCenter.isDue(expr, lastRun, lastRun.plusMinutes(1)));
        // 12:05:00 到点
        assertTrue(JobDispatchCenter.isDue(expr, lastRun, lastRun.plusMinutes(5)));
        // 从未执行：到期
        assertTrue(JobDispatchCenter.isDue(expr, null, lastRun));
    }
}
