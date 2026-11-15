package vip.mseapp.api.system.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调度状态同步结果（mse-platform → 各服务）：应答启停状态 + 已抢占的待触发指令。
 */
public class ScheduleSyncResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 各任务启用状态（taskId → enabled；缺失 = 未在管理面操作过，保持代码默认） */
    private Map<String, Boolean> enabledByTask = new HashMap<>();

    /** 各任务调度覆盖（taskId → 覆盖表达式：纯数字=毫秒 fixedDelay / cron；缺失 = 无覆盖按代码默认） */
    private Map<String, String> exprOverrideByTask = new HashMap<>();

    /** 本实例抢占到的待触发任务（乐观锁 UPDATE ... WHERE trigger_pending=1 只有一个实例抢到） */
    private List<String> triggeredTaskIds = List.of();

    public Map<String, Boolean> getEnabledByTask() {
        return enabledByTask;
    }

    public void setEnabledByTask(Map<String, Boolean> enabledByTask) {
        this.enabledByTask = enabledByTask;
    }

    public Map<String, String> getExprOverrideByTask() {
        return exprOverrideByTask;
    }

    public void setExprOverrideByTask(Map<String, String> exprOverrideByTask) {
        this.exprOverrideByTask = exprOverrideByTask;
    }

    public List<String> getTriggeredTaskIds() {
        return triggeredTaskIds;
    }

    public void setTriggeredTaskIds(List<String> triggeredTaskIds) {
        this.triggeredTaskIds = triggeredTaskIds;
    }
}
