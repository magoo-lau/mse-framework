package vip.mseapp.core.schedule;

import java.io.Serializable;

/**
 * 任务单次执行记录（调度器进程内采集，经状态同步器上行至管理面日志表）。
 * id = bootId:taskId:runIndex 全局幂等——重启重报由接收端按主键去重，无副作用。
 */
public final class TaskRunRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 幂等主键（bootId:taskId:runIndex） */
    private final String id;

    private final long globalSeq;

    private final String taskId;

    /** 本次执行序号（进程内该任务第 N 次，随 runCount） */
    private final long runIndex;

    private final long startMs;

    private final long durationMs;

    private final boolean success;

    /** 失败原因摘要（成功为空） */
    private final String error;

    public TaskRunRecord(String id, long globalSeq, String taskId, long runIndex,
                         long startMs, long durationMs, boolean success, String error) {
        this.id = id;
        this.globalSeq = globalSeq;
        this.taskId = taskId;
        this.runIndex = runIndex;
        this.startMs = startMs;
        this.durationMs = durationMs;
        this.success = success;
        this.error = error;
    }

    public String getId() {
        return id;
    }

    public long getGlobalSeq() {
        return globalSeq;
    }

    public String getTaskId() {
        return taskId;
    }

    public long getRunIndex() {
        return runIndex;
    }

    public long getStartMs() {
        return startMs;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }
}
