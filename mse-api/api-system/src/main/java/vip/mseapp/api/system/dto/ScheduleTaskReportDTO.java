package vip.mseapp.api.system.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 调度状态上报（starter-bridge 状态同步器 → mse-platform）：本服务全部任务的
 * 定义快照 + 运行态。同一 taskId 多实例部署时各实例独立上报，平台侧按 task_key
 * 合并（运行态取最近上报者，即实际执行实例）。
 */
public class ScheduleTaskReportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 上报服务名（spring.application.name） */
    private String serviceName;

    /** 调度模式（mse.scheduling.mode：local/xxl-job/...，管理面展示） */
    private String schedulerMode;

    /** 本服务全部任务运行态 */
    private List<ScheduleTaskStateDTO> tasks;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getSchedulerMode() {
        return schedulerMode;
    }

    public void setSchedulerMode(String schedulerMode) {
        this.schedulerMode = schedulerMode;
    }

    public List<ScheduleTaskStateDTO> getTasks() {
        return tasks;
    }

    public void setTasks(List<ScheduleTaskStateDTO> tasks) {
        this.tasks = tasks;
    }
}
