package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 内置任务调度登记（平台库，跨服务聚合）：各服务 local 调度模式经 starter-bridge
 * 状态同步器周期上报的任务定义快照 + 运行态；管理端启停/触发落本表，全域实例
 * 在一个同步周期内收敛（触发为乐观锁抢占，恰好一个实例执行）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_schedule_task")
public class SysScheduleTask extends PlatformBaseEntity {

    /** 任务编号（全局唯一，与代码侧 @MseScheduled.taskId 一致） */
    private String taskKey;

    /** 最近上报的服务名（spring.application.name；同任务多实例时为最近执行者） */
    private String ownerService;

    /** 调度模式（local / xxl-job / ...） */
    private String schedulerMode;

    /** 展示名 */
    private String taskName;

    /** 描述 */
    private String description;

    /** 类型：FIXED_DELAY / CRON / STARTUP */
    private String taskType;

    /** 任务分类：platform（系统任务/控制面）/ biz（业务域任务，缺省）；随代码上报收敛 */
    private String taskCategory;

    /** 调度表达式（生效值：代码默认，或覆盖后的值——覆盖时 exprOverride 非空） */
    private String scheduleExpr;

    /** 调度覆盖（纯数字=毫秒 fixedDelay，否则为 cron；空=按代码默认；管理面写，上报不覆盖） */
    private String exprOverride;

    /** 详细说明（管理端编辑，上报不覆盖；空时前端回落 description） */
    private String remark;

    /** 1 启用 0 暂停（管理面写；上报不覆盖） */
    private Integer enabled;

    /** 1 待触发（管理面写；同步时乐观锁抢占后复位，上报不覆盖） */
    private Integer triggerPending;

    /** 最近一次执行开始时间（执行实例上报） */
    private LocalDateTime lastRunTime;

    /** 最近一次执行耗时（毫秒） */
    private Long lastDurationMs;

    /** 最近一次结果：SUCCESS / FAILED（空=未执行过） */
    private String lastResult;

    /** 最近一次失败原因摘要 */
    private String lastError;

    /** 累计执行次数 */
    private Long runCount;

    /** 累计失败次数 */
    private Long errorCount;

    /** 最近上报时间（管理面判活：超过 3 个同步周期未上报视为灰显） */
    private LocalDateTime reportTime;
}
