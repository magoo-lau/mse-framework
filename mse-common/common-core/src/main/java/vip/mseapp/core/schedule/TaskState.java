package vip.mseapp.core.schedule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 任务运行态快照（注册表行 + 最近一次执行统计）。字段全部 volatile：调度线程写、
 * 管理/上报线程读，只求最终一致，不做精确计数。
 */
public class TaskState {

    /** 生效定义（运行期重排时整体替换；代码原始定义由调度器内部持有） */
    private volatile TaskDefinition definition;

    private volatile boolean paused;

    private volatile boolean running;

    private volatile long lastRunStartMs;

    private volatile long lastRunEndMs;

    private volatile String lastResult = "";

    private volatile String lastError = "";

    private volatile long runCount;

    private volatile long errorCount;

    TaskState(TaskDefinition definition) {
        this.definition = definition;
    }

    void markRunning(long startMs) {
        running = true;
        lastRunStartMs = startMs;
    }

    void markSuccess(long startMs) {
        running = false;
        lastRunEndMs = System.currentTimeMillis();
        lastResult = "SUCCESS";
        lastError = "";
        runCount++;
    }

    void markFailure(long startMs, Throwable error) {
        running = false;
        lastRunEndMs = System.currentTimeMillis();
        lastResult = "FAILED";
        lastError = error == null ? "" : error.toString();
        runCount++;
        errorCount++;
    }

    void setPaused(boolean paused) {
        this.paused = paused;
    }

    /** 运行期重排时同步生效定义（快照/上报展示生效调度） */
    void setDefinition(TaskDefinition definition) {
        this.definition = definition;
    }

    /** 按 taskId 排序的稳定快照（管理面展示与状态上报用） */
    public static List<TaskState> sorted(List<TaskState> states) {
        List<TaskState> copy = new ArrayList<>(states);
        copy.sort(Comparator.comparing(s -> s.definition.getTaskId()));
        return copy;
    }

    public TaskDefinition getDefinition() {
        return definition;
    }

    public String getTaskId() {
        return definition.getTaskId();
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isRunning() {
        return running;
    }

    public long getLastRunStartMs() {
        return lastRunStartMs;
    }

    public long getLastRunEndMs() {
        return lastRunEndMs;
    }

    /** 最近一次执行耗时（毫秒），未执行过为 0 */
    public long getLastDurationMs() {
        return Math.max(0, lastRunEndMs - lastRunStartMs);
    }

    public String getLastResult() {
        return lastResult;
    }

    public String getLastError() {
        return lastError;
    }

    public long getRunCount() {
        return runCount;
    }

    public long getErrorCount() {
        return errorCount;
    }
}
