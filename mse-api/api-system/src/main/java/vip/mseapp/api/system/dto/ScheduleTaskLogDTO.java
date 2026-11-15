package vip.mseapp.api.system.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务执行日志（调度器进程内采集 → 状态同步器上行 → sys_schedule_task_log）。
 * id = bootId:taskId:runIndex，幂等去重键（重启重报无副作用）。
 */
public class ScheduleTaskLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 幂等主键（bootId:taskId:runIndex） */
    private String id;

    private String taskKey;

    /** 上报服务名（执行实例） */
    private String serviceName;

    /** 本次执行开始时间 */
    private LocalDateTime startTime;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** SUCCESS / FAILED */
    private String result;

    /** 失败原因摘要（成功为空） */
    private String error;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(String taskKey) {
        this.taskKey = taskKey;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
