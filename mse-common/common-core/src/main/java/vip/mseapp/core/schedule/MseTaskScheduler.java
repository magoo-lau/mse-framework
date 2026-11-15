package vip.mseapp.core.schedule;

import java.util.List;

/**
 * 内置任务调度器 SPI（声明与实现分离——任务只面向本接口，换调度器不碰任务代码）。
 *
 * <p>内置实现为 {@link LocalMseTaskScheduler}（进程内线程池 + 延迟启动 + 分布式执行互斥）；
 * 接入外部调度器（xxl-job 等）时新起一个 starter 提供本接口的适配实现
 * （{@code @AutoConfigureBefore} 本仓库的 MseSchedulingAutoConfiguration，或配置
 * {@code mse.scheduling.mode} 关掉 local 实现），任务声明与业务代码零改动：
 * fixedDelay 对应 xxl-job「固定速度」、cron 对应 cron、startup 任务在应用侧本地执行。
 */
public interface MseTaskScheduler {

    /**
     * 注册任务（幂等：taskId 重复时保留先注册者并告警）。
     *
     * @param definition 任务定义
     * @param runnable   任务体（允许抛异常，由实现统一捕获记录，不影响后续轮次）
     */
    void register(TaskDefinition definition, TaskRunnable runnable);

    /**
     * 注册或更新任务（对账场景：周期性重放期望态，如「扫描配置表 → 登记租户任务」——
     * {@link #register} 对已存在 taskId 只忽略不更新，无法承接配置变化）：
     * 不存在则注册（同 {@link #register}）；已存在且定义（类型/名称/描述/表达式）未变则
     * no-op（重放安全，不打扰运行中的链）；已存在且代码默认定义变化——无管理面覆盖时按新
     * 定义重排，有覆盖时仅更新代码默认（覆盖清除后还原到最新代码默认，不与管理面打架）。
     *
     * @return 是否发生了注册或重排（定义未变 / 被静态开关禁用 = false）
     */
    default boolean registerOrUpdate(TaskDefinition definition, TaskRunnable runnable) {
        register(definition, runnable);
        return true;
    }

    /**
     * 注销任务（对账场景：配置删除/下线后收回调度）：取消排程链并移出注册表，快照/上报
     * 随之消失（管理面该行失去联系以 report_time 停更标识）。运行中的当轮任务体不中断，
     * 自然结束后不再续链；运行态统计（次数/最近结果）随之丢弃。
     *
     * @return 是否已注销（任务不存在 = false）
     */
    default boolean unregister(String taskId) {
        return false;
    }

    /** 全量运行态快照（按 taskId 排序） */
    List<TaskState> snapshot();

    /** 暂停周期执行（管理面动作；返回 false = 任务不存在） */
    boolean pause(String taskId);

    /** 恢复周期执行（管理面动作；返回 false = 任务不存在） */
    boolean resume(String taskId);

    /** 立即触发一次（管理面动作，异步执行，跳过暂停位与执行互斥——抢占语义由调用方保证） */
    boolean triggerNow(String taskId);

    /**
     * 运行期调整调度（管理面「编辑」动作）：覆盖表达式为纯数字（毫秒，fixedDelay）或 cron 文本，
     * null/空 = 恢复代码默认。取消当前排程按新定义重排；STARTUP 任务不支持（返回 false）。
     * 外部调度器适配器可不实现（缺省 false=不支持，管理面在对端控制台完成）。
     *
     * @return 是否已生效（任务不存在 / 表达式非法 / 类型不支持 = false）
     */
    default boolean applySchedule(String taskId, String exprOverride) {
        return false;
    }

    /**
     * 增量拉取本进程的运行日志（执行成功/失败记录，环形缓冲，供状态同步器上行管理面）。
     *
     * @param lastSeq 上次已拉取的全局序号（首次传 0）
     * @return globalSeq 大于 lastSeq 的记录（可能为空；外部调度器适配器缺省无日志）
     */
    default List<TaskRunRecord> runsSince(long lastSeq) {
        return List.of();
    }

    /** 任务体：允许抛异常（与 Runnable 的差异即在此，调度实现负责兜底记录） */
    @FunctionalInterface
    interface TaskRunnable {

        void run() throws Exception;
    }
}
