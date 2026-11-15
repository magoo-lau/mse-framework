package vip.mseapp.api.system.dto;

import java.io.Serializable;

/**
 * 内置任务运行态上报条目（各服务调度中心 → mse-platform 的 sys_schedule_task 行投影）。
 *
 * <p>定义字段（taskId/name/description/type/expression）与运行态字段（lastRun 族、
 * runCount、errorCount）每次上报刷新——任务改名/改周期随代码自然收敛。
 */
public class ScheduleTaskStateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务编号（全局唯一，管理面主键） */
    private String taskId;

    /** 展示名 */
    private String name;

    /** 描述 */
    private String description;

    /** 类型：FIXED_DELAY / CRON / STARTUP */
    private String type;

    /** 调度表达式（cron 原文 / 间隔描述） */
    private String expression;

    /** 任务分类：platform（系统任务/控制面）/ biz（业务域任务，缺省） */
    private String category;

    /** 最近一次执行开始时间（epoch 毫秒，0=未执行过） */
    private long lastRunStartMs;

    /** 最近一次执行结束时间（epoch 毫秒） */
    private long lastRunEndMs;

    /** 最近一次执行耗时（毫秒） */
    private long lastDurationMs;

    /** 最近一次结果：SUCCESS / FAILED（空=未执行过） */
    private String lastResult;

    /** 最近一次失败原因摘要（成功时清空） */
    private String lastError;

    /** 累计执行次数 */
    private long runCount;

    /** 累计失败次数 */
    private long errorCount;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getLastRunStartMs() {
        return lastRunStartMs;
    }

    public void setLastRunStartMs(long lastRunStartMs) {
        this.lastRunStartMs = lastRunStartMs;
    }

    public long getLastRunEndMs() {
        return lastRunEndMs;
    }

    public void setLastRunEndMs(long lastRunEndMs) {
        this.lastRunEndMs = lastRunEndMs;
    }

    public long getLastDurationMs() {
        return lastDurationMs;
    }

    public void setLastDurationMs(long lastDurationMs) {
        this.lastDurationMs = lastDurationMs;
    }

    public String getLastResult() {
        return lastResult;
    }

    public void setLastResult(String lastResult) {
        this.lastResult = lastResult;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public long getRunCount() {
        return runCount;
    }

    public void setRunCount(long runCount) {
        this.runCount = runCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }
}
