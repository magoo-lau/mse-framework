package vip.mseapp.core.schedule;

import java.time.Instant;

/**
 * cron 表达式解析 SPI：计算某时刻之后的下一次执行时间。common-core 保持零 Spring 依赖，
 * 由装配件（starter-web）注入 Spring {@code CronExpression} 实现；外部调度器适配器
 * 可换用自己的 cron 语义。
 */
@FunctionalInterface
public interface CronExpressionResolver {

    /**
     * @param cron  cron 表达式（Spring CronExpression 语法）
     * @param after 基准时点
     * @return 下一次执行时间；表达式无后续触发点（如 L 语义耗尽）返回 null
     * @throws IllegalArgumentException 表达式非法
     */
    Instant nextExecution(String cron, Instant after);
}
