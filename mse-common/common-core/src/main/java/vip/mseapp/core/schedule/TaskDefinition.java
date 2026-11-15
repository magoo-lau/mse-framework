package vip.mseapp.core.schedule;

/**
 * 内置后台任务的定义元数据（与 {@link MseScheduled} 注解、{@link MseTaskScheduler} 配套）。
 *
 * <p>taskId 全局唯一（跨服务不重复），是管理面（启停/触发/状态上报）的稳定主键；
 * 换接外部调度器（xxl-job 等）时同一份定义原样翻译为对端的 Job 元数据。
 */
public final class TaskDefinition {

    /** 分类：系统任务（控制面：目录扇出/权益对账/基座推送/自注册推送等，平台运维全域对象；存储值 platform 为历史值） */
    public static final String CATEGORY_PLATFORM = "platform";

    /** 分类：业务域任务（扫描/重试/采集等业务基础设施，缺省值） */
    public static final String CATEGORY_BIZ = "biz";

    /** 调度类型 */
    public enum Type {

        /** 固定间隔（上轮结束到下轮开始），语义同 {@code @Scheduled(fixedDelay)} */
        FIXED_DELAY,

        /** cron 表达式（解析经 {@link CronExpressionResolver} 注入，local 缺省实现为 Spring CronExpression） */
        CRON,

        /** 启动一次性任务：应用就绪 + 统一延迟启动窗口后执行一次 */
        STARTUP
    }

    private final String taskId;

    private final String name;

    private final String description;

    private final Type type;

    private final long initialDelayMs;

    private final long fixedDelayMs;

    private final String cron;

    /**
     * 是否参与多实例防重（true=每轮经 {@link TaskExecutionGuard} 抢锁，恰一实例执行；
     * false=各实例独立执行——状态同步这类「每实例都要收敛」的任务用 false）
     */
    private final boolean deduplicate;

    /** 任务分类（platform=系统任务/控制面 / biz=业务域任务，管理面分域展示；见 {@link #category(String)}） */
    private final String category;

    private TaskDefinition(String taskId, String name, String description, Type type,
                           long initialDelayMs, long fixedDelayMs, String cron, boolean deduplicate) {
        this(taskId, name, description, type, initialDelayMs, fixedDelayMs, cron, deduplicate, CATEGORY_BIZ);
    }

    private TaskDefinition(String taskId, String name, String description, Type type,
                           long initialDelayMs, long fixedDelayMs, String cron, boolean deduplicate,
                           String category) {
        this.taskId = taskId;
        this.name = name;
        this.description = description;
        this.type = type;
        this.initialDelayMs = initialDelayMs;
        this.fixedDelayMs = fixedDelayMs;
        this.cron = cron;
        this.deduplicate = deduplicate;
        this.category = category;
    }

    /** 返回带分类的副本（platform=系统任务/控制面 / biz=业务域任务；缺省 biz，非法值归一 biz） */
    public TaskDefinition category(String category) {
        return new TaskDefinition(taskId, name, description, type, initialDelayMs, fixedDelayMs, cron,
                deduplicate, category == null || category.isBlank() ? CATEGORY_BIZ : category);
    }

    /** 固定间隔任务（initialDelay 相对统一延迟启动窗口；多实例防重） */
    public static TaskDefinition fixedDelay(String taskId, String name, long initialDelayMs, long fixedDelayMs) {
        return fixedDelay(taskId, name, "", initialDelayMs, fixedDelayMs);
    }

    /** 固定间隔任务（带描述；多实例防重） */
    public static TaskDefinition fixedDelay(String taskId, String name, String description,
                                            long initialDelayMs, long fixedDelayMs) {
        return fixedDelay(taskId, name, description, initialDelayMs, fixedDelayMs, true);
    }

    /** 固定间隔任务（完整参数：deduplicate=false 表示各实例独立执行） */
    public static TaskDefinition fixedDelay(String taskId, String name, String description,
                                            long initialDelayMs, long fixedDelayMs, boolean deduplicate) {
        if (fixedDelayMs <= 0) {
            throw new IllegalArgumentException("任务 " + taskId + " fixedDelayMs 必须 > 0");
        }
        return new TaskDefinition(taskId, name, description, Type.FIXED_DELAY,
                Math.max(0, initialDelayMs), fixedDelayMs, null, deduplicate);
    }

    /** cron 任务 */
    public static TaskDefinition cron(String taskId, String name, String cron) {
        return cron(taskId, name, "", cron);
    }

    /** cron 任务（带描述） */
    public static TaskDefinition cron(String taskId, String name, String description, String cron) {
        return cron(taskId, name, description, cron, true);
    }

    /** cron 任务（完整参数：deduplicate=false 表示各实例独立执行） */
    public static TaskDefinition cron(String taskId, String name, String description, String cron, boolean deduplicate) {
        if (cron == null || cron.isBlank()) {
            throw new IllegalArgumentException("任务 " + taskId + " cron 不能为空");
        }
        return new TaskDefinition(taskId, name, description, Type.CRON, 0, 0, cron, deduplicate);
    }

    /** 启动一次性任务 */
    public static TaskDefinition startup(String taskId, String name) {
        return startup(taskId, name, "");
    }

    /** 启动一次性任务（带描述） */
    public static TaskDefinition startup(String taskId, String name, String description) {
        return new TaskDefinition(taskId, name, description, Type.STARTUP, 0, 0, null, true);
    }

    /**
     * 调度表达式的统一展示形式（管理面/上报用）：FIXED_DELAY 渲染为等价 cron
     * （可整除时精确，如 30 秒 → "0/30 * * * * ?" 记法、5 分钟 → "0 0/5 * * * ?"、
     * 2 小时 → "0 0 0/2 * * ?"；不可整除的罕见值降级 "~every Ns" 近似标注）；
     * CRON 原样；STARTUP 为启动一次（cron 不适用）。展示统一为 cron，实际执行语义
     * 不变（fixedDelay 仍按间隔），除非编辑覆盖为 cron。
     */
    public String expression() {
        return switch (type) {
            case CRON -> cron;
            case FIXED_DELAY -> toCronExpression(fixedDelayMs);
            case STARTUP -> "startup";
        };
    }

    /** 间隔毫秒 → 等价 cron 展示（不可整除时降级近似标注） */
    static String toCronExpression(long fixedDelayMs) {
        long seconds = Math.max(1, fixedDelayMs / 1000);
        if (seconds < 60) {
            return 60 % seconds == 0 ? "*/" + seconds + " * * * * ?" : "~every " + seconds + "s";
        }
        if (seconds == 60) {
            return "0 * * * * ?";
        }
        if (seconds < 3600) {
            return 3600 % seconds == 0 ? "0 */" + (seconds / 60) + " * * * ?" : "~every " + seconds + "s";
        }
        if (seconds == 3600) {
            return "0 0 * * * ?";
        }
        if (seconds < 86400) {
            return 86400 % seconds == 0 ? "0 0 */" + (seconds / 3600) + " * * ?" : "~every " + seconds + "s";
        }
        return "~every " + seconds + "s";
    }

    public String getTaskId() {
        return taskId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Type getType() {
        return type;
    }

    public long getInitialDelayMs() {
        return initialDelayMs;
    }

    public long getFixedDelayMs() {
        return fixedDelayMs;
    }

    public String getCron() {
        return cron;
    }

    public boolean isDeduplicate() {
        return deduplicate;
    }

    /** 任务分类（platform=系统任务/控制面 / biz=业务域任务） */
    public String getCategory() {
        return category;
    }
}
