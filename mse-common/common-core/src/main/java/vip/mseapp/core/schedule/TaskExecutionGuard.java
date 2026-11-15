package vip.mseapp.core.schedule;

/**
 * 任务执行互斥（local 模式的多实例防重）：每轮执行前抢分布式锁，抢不到说明其他实例
 * 本轮已执行，直接让位。缺省实现为 no-op（单实例语义）；starter-redis 提供 Redis 版
 * （Redisson 看门狗锁，Redis 故障时放行执行——可用性优先于防重）。
 */
@FunctionalInterface
public interface TaskExecutionGuard {

    /**
     * @param taskId 任务编号（实现自行加锁前缀）
     * @param action 任务体
     * @return 是否由本实例执行（false = 未抢到执行权，本轮让位）
     */
    boolean tryRun(String taskId, Runnable action);
}
