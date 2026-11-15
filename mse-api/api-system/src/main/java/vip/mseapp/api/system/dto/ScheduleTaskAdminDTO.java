package vip.mseapp.api.system.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务调度管理视图（管理端列表行，双端共用）：上报定义/运行态 + 管理面字段
 * （enabled/exprOverride/remark——上报永不覆盖）。
 */
public class ScheduleTaskAdminDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String taskKey;

    private String ownerService;

    private String schedulerMode;

    private String taskName;

    private String description;

    private String taskType;

    /** 任务分类：platform（系统任务/控制面）/ biz（业务域任务） */
    private String taskCategory;

    /** 生效调度表达式（代码默认，或覆盖后的值——覆盖时 exprOverride 非空） */
    private String scheduleExpr;

    /** 1 启用 0 暂停（管理面写） */
    private Integer enabled;

    /** 调度覆盖（纯数字=毫秒 fixedDelay，否则为 cron；空=按代码默认） */
    private String exprOverride;

    /** 详细说明（管理端编辑，上报不覆盖；空时前端回落 description） */
    private String remark;

    private LocalDateTime lastRunTime;

    private Long lastDurationMs;

    private String lastResult;

    private String lastError;

    private Long runCount;

    private Long errorCount;

    private LocalDateTime reportTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public String getOwnerService() {
        return ownerService;
    }

    public void setOwnerService(String ownerService) {
        this.ownerService = ownerService;
    }

    public String getSchedulerMode() {
        return schedulerMode;
    }

    public void setSchedulerMode(String schedulerMode) {
        this.schedulerMode = schedulerMode;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskCategory() {
        return taskCategory;
    }

    public void setTaskCategory(String taskCategory) {
        this.taskCategory = taskCategory;
    }

    public String getScheduleExpr() {
        return scheduleExpr;
    }

    public void setScheduleExpr(String scheduleExpr) {
        this.scheduleExpr = scheduleExpr;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public String getExprOverride() {
        return exprOverride;
    }

    public void setExprOverride(String exprOverride) {
        this.exprOverride = exprOverride;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getLastRunTime() {
        return lastRunTime;
    }

    public void setLastRunTime(LocalDateTime lastRunTime) {
        this.lastRunTime = lastRunTime;
    }

    public Long getLastDurationMs() {
        return lastDurationMs;
    }

    public void setLastDurationMs(Long lastDurationMs) {
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

    public Long getRunCount() {
        return runCount;
    }

    public void setRunCount(Long runCount) {
        this.runCount = runCount;
    }

    public Long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Long errorCount) {
        this.errorCount = errorCount;
    }

    public LocalDateTime getReportTime() {
        return reportTime;
    }

    public void setReportTime(LocalDateTime reportTime) {
        this.reportTime = reportTime;
    }
}
