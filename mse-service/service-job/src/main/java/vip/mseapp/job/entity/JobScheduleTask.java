package vip.mseapp.job.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vip.mseapp.mybatis.data.PlatformBaseEntity;

import java.time.LocalDateTime;

/**
 * 调度任务登记行（mse-job 视角的只读/派发投影，表本体=service-platform 的 sys_schedule_task）。
 *
 * <p>仅声明派发判定所需列（生效表达式/启停/触发位/最近执行），结构演进以 service-platform
 * 实体与 platform 通道脚本为权威——本类不加列不改表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_schedule_task")
public class JobScheduleTask extends PlatformBaseEntity {

    /** 任务编号（全局唯一） */
    private String taskKey;

    /** 展示名 */
    private String taskName;

    /** 最近上报服务名（观测用） */
    private String ownerService;

    /** 类型：FIXED_DELAY / CRON / STARTUP（STARTUP 为本地启动语义，中央不派发） */
    private String taskType;

    /** 调度表达式（生效值：代码默认渲染 cron / 间隔描述） */
    private String scheduleExpr;

    /** 调度覆盖（纯数字=毫秒间隔，否则为 cron；空=按代码默认） */
    private String exprOverride;

    /** 1 启用 0 暂停（管理面写） */
    private Integer enabled;

    /** 1 待触发（mse-job 派发写；执行方 sync 乐观锁抢占后复位） */
    private Integer triggerPending;

    /** 最近一次执行开始时间（执行方上报；到期判定的锚点） */
    private LocalDateTime lastRunTime;
}
