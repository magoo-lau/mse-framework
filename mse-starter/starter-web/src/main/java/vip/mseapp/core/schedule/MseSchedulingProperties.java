package vip.mseapp.core.schedule;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * 统一调度配置（{@code mse.scheduling.*}）。
 *
 * <p>mode 切换语义：local（缺省，内置本地调度）/ central（本地不排程，节奏由 mse-job
 * 薄调度中心经 trigger_pending/踢键链路派发；注册/上报/手动触发照常，STARTUP 任务仍本地）/
 * none（禁用全部内置后台任务）/ 其他值（xxl-job 等外部调度器，需另行引入对应适配 starter
 * 提供自己的 {@link MseTaskScheduler} 实现——local 实现对未知 mode 自动让位）。
 */
@ConfigurationProperties(prefix = "mse.scheduling")
public class MseSchedulingProperties {

    /** 调度模式：local | none | 外部调度器名（适配器自备实现） */
    private String mode = "local";

    /** 统一延迟启动窗口：应用就绪（晚于结构迁移等全部 Runner）后再等这么久才开始调度 */
    private Duration startupDelay = Duration.ofSeconds(30);

    /** local 触发线程池大小（只算到期并提交，不执行任务体——恒轻量） */
    private int poolSize = 4;

    /** 任务执行线程池大小（只跑任务体；任务体卡死只停自身链，不拖垮触发池） */
    private int executorPoolSize = 4;

    /** 任务执行池有界队列容量（满则本轮提交被拒：WARN 跳轮续链，不丢链、饱和可见） */
    private int executorQueueCapacity = 128;

    /** 状态同步周期（starter-bridge 上报任务快照/拉取启停与触发指令的间隔） */
    private Duration stateSyncInterval = Duration.ofSeconds(15);

    /** 是否启用多实例执行互斥（false = 各实例独立执行，适合明确单实例部署） */
    private boolean guardEnabled = true;

    /** 单任务静态开关（{@code mse.scheduling.tasks.<taskId>.enabled}，缺省 true）：
     * false 的任务不注册不排程不上报（零成本，比管理面暂停更彻底；适合「没用该模块」的场景，
     * 如 mes-andon-escalate.enabled=false）。优先级最高，任务不进平台端「任务调度」页。
     */
    private Map<String, TaskToggle> tasks = new java.util.LinkedHashMap<>();

    /** 单任务开关项（enabled 缺省 true） */
    public static class TaskToggle {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Duration getStartupDelay() {
        return startupDelay;
    }

    public void setStartupDelay(Duration startupDelay) {
        this.startupDelay = startupDelay;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getExecutorPoolSize() {
        return executorPoolSize;
    }

    public void setExecutorPoolSize(int executorPoolSize) {
        this.executorPoolSize = executorPoolSize;
    }

    public int getExecutorQueueCapacity() {
        return executorQueueCapacity;
    }

    public void setExecutorQueueCapacity(int executorQueueCapacity) {
        this.executorQueueCapacity = executorQueueCapacity;
    }

    public Duration getStateSyncInterval() {
        return stateSyncInterval;
    }

    public void setStateSyncInterval(Duration stateSyncInterval) {
        this.stateSyncInterval = stateSyncInterval;
    }

    public boolean isGuardEnabled() {
        return guardEnabled;
    }

    public void setGuardEnabled(boolean guardEnabled) {
        this.guardEnabled = guardEnabled;
    }

    public Map<String, TaskToggle> getTasks() {
        return tasks;
    }

    public void setTasks(Map<String, TaskToggle> tasks) {
        this.tasks = tasks;
    }

    /** 静态禁用的任务编号清单（启动日志提示用）；全部放行返回空 */
    public java.util.List<String> disabledTaskIds() {
        return tasks.entrySet().stream()
                .filter(e -> !e.getValue().isEnabled())
                .map(Map.Entry::getKey)
                .toList();
    }
}
