package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 内置任务执行日志（平台库）：调度器进程内采集，starter-bridge 状态同步器增量上行。
 * 幂等键 log_key = bootId:taskId:runIndex（重启重报按此去重）；每任务保留最近 200 条
 * （追加后剪枝）。继承 PlatformBaseEntity 获平台库路由与租户行级豁免（同 sys_schedule_task）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_schedule_task_log")
public class SysScheduleTaskLog extends PlatformBaseEntity {

    /** 幂等键（bootId:taskId:runIndex） */
    private String logKey;

    private String taskKey;

    /** 执行实例服务名 */
    private String ownerService;

    /** 执行开始时间 */
    private LocalDateTime startTime;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** SUCCESS / FAILED */
    private String result;

    /** 失败原因摘要 */
    private String errorMsg;
}
