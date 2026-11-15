package vip.mseapp.service.platform.rpc;

import vip.mseapp.api.system.RemoteScheduleStateApi;
import vip.mseapp.api.system.dto.ScheduleSyncResultDTO;
import vip.mseapp.api.system.dto.ScheduleTaskAdminDTO;
import vip.mseapp.api.system.dto.ScheduleTaskLogDTO;
import vip.mseapp.api.system.dto.ScheduleTaskReportDTO;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.platform.service.SysScheduleTaskService;

import java.util.List;

/**
 * 调度状态契约实现（平台库 sys_schedule_task / sys_schedule_task_log）：各服务
 * starter-bridge 状态同步器周期调用（local 直调 / dubbo 远调），落库聚合 + 应答
 * 启停/覆盖/触发；管理端方法（分页列表/启停/触发/编辑/执行日志）供平台端控制器与
 * （standalone 下）租户端控制器经契约消费。
 */
@RpcService(RemoteScheduleStateApi.class)
public class RemoteScheduleStateApiImpl implements RemoteScheduleStateApi {

    private final SysScheduleTaskService scheduleTaskService;

    public RemoteScheduleStateApiImpl(SysScheduleTaskService scheduleTaskService) {
        this.scheduleTaskService = scheduleTaskService;
    }

    @Override
    public ScheduleSyncResultDTO sync(ScheduleTaskReportDTO report) {
        return scheduleTaskService.sync(report);
    }

    @Override
    public PageResult<ScheduleTaskAdminDTO> listTasks(long pageNum, long pageSize, String keyword,
                                                      String serviceName, String category) {
        return scheduleTaskService.listTasks(pageNum, pageSize, keyword, serviceName, category);
    }

    @Override
    public boolean toggle(String taskKey, boolean enabled) {
        return scheduleTaskService.toggle(taskKey, enabled);
    }

    @Override
    public boolean trigger(String taskKey) {
        return scheduleTaskService.trigger(taskKey);
    }

    @Override
    public boolean updateTask(String taskKey, String exprOverride, String remark) {
        return scheduleTaskService.updateTask(taskKey, exprOverride, remark);
    }

    @Override
    public int appendLogs(List<ScheduleTaskLogDTO> logs) {
        return scheduleTaskService.appendLogs(logs);
    }

    @Override
    public PageResult<ScheduleTaskLogDTO> listLogs(String taskKey, long pageNum, long pageSize, String result) {
        return scheduleTaskService.listLogs(taskKey, pageNum, pageSize, result);
    }

    @Override
    public java.util.Map<String, String> getScheduleSettings() {
        return scheduleTaskService.getSettings();
    }

    @Override
    public boolean updateScheduleSettings(java.util.Map<String, Integer> gateLimits) {
        return scheduleTaskService.updateScheduleSettings(gateLimits);
    }
}
