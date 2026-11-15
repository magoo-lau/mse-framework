package vip.mseapp.api.system;

import vip.mseapp.api.system.dto.ScheduleSyncResultDTO;
import vip.mseapp.api.system.dto.ScheduleTaskAdminDTO;
import vip.mseapp.api.system.dto.ScheduleTaskLogDTO;
import vip.mseapp.api.system.dto.ScheduleTaskReportDTO;
import vip.mseapp.core.result.PageResult;

import java.util.List;

/**
 * 内置任务调度状态契约（mse-platform 实现，各服务经 starter-bridge 调度监控线程消费；
 * standalone 离线私有部署由 service-system 的回落实现承接）。
 *
 * <p>local 调度模式的管理面数据通道（下行双通道、上行单通道）：管理端「任务调度」页的
 * 启停/编辑落库后写透 Redis 控制哈希（mse:schedule:control），各实例的调度监控线程
 * （独立守护线程，不注册进调度中心）控制节拍（缺省 5s）HGETALL 秒级应用启停与调度覆盖
 * ——快通道；本契约的 {@link #sync} 应答（DB 事实源）为兜底通道（缺省 15s，Redis 缺席或
 * 哈希滞后时仍收敛）。触发为乐观锁抢占（恰好一个实例执行），仍经 sync 下发。上行经 sync
 * 周期上报任务定义快照/运行态（report_time 判活数据源），执行日志经 {@link #appendLogs}
 * 增量上行（幂等主键去重）。xxl-job 等外部调度模式下不走本通道（对端控制台接管管理）。
 *
 * <p>幂等语义：上报 upsert（定义/运行态每次刷新随代码收敛，管理面字段
 * enabled/exprOverride/remark/trigger_pending 永不覆盖）；mse-platform 未就绪时
 * 消费端 WARN 自愈重试，任务按本地默认继续执行。
 */
public interface RemoteScheduleStateApi {

    /**
     * 上报任务快照并拉取启停/覆盖/触发指令（单次往返完成上下行；下行兜底通道，
     * 快通道为 Redis 控制哈希）。
     *
     * @param report 本服务任务清单与运行态
     * @return 启停状态 + 调度覆盖 + 本实例抢占到的待触发任务清单
     */
    ScheduleSyncResultDTO sync(ScheduleTaskReportDTO report);

    /** 管理端：任务列表分页（keyword 模糊匹配编号/名称/说明；serviceName 按服务过滤；category 分域过滤 platform/biz，空=全部） */
    PageResult<ScheduleTaskAdminDTO> listTasks(long pageNum, long pageSize, String keyword, String serviceName,
                                               String category);

    /** 管理端：启停（落库 + 写透控制缓存，全域实例秒级收敛；sync 通道兜底） */
    boolean toggle(String taskKey, boolean enabled);

    /** 管理端：下发触发（置 pending，由某实例同步时抢占执行；任务不存在返回 false） */
    boolean trigger(String taskKey);

    /**
     * 管理端：编辑调度覆盖与详细说明。
     *
     * @param exprOverride 调度覆盖：纯数字（≥1000，毫秒 fixedDelay）或合法 cron；
     *                     null/空 = 清除覆盖恢复代码默认；STARTUP 任务不支持覆盖
     * @param remark       详细说明（≤1000 字；null = 不变，空串 = 清空）
     * @return 任务不存在或覆盖表达式非法返回 false
     */
    boolean updateTask(String taskKey, String exprOverride, String remark);

    /** 状态同步器：增量上行执行日志（幂等主键去重；返回落库条数，失败抛异常由调用方重试） */
    int appendLogs(List<ScheduleTaskLogDTO> logs);

    /**
     * 管理端：执行日志分页查询。
     *
     * @param result 结果过滤：SUCCESS / FAILED；null = 全部
     */
    PageResult<ScheduleTaskLogDTO> listLogs(String taskKey, long pageNum, long pageSize, String result);

    /**
     * 管理端（「调度器」设置页）：读全部调度器设置（key→value；租户并发闸门为
     * {@code gates.<key>=limit} 行；空 Map=未配置，调用方回落代码默认）。
     */
    java.util.Map<String, String> getScheduleSettings();

    /**
     * 管理端（「调度器」设置页）：更新租户并发闸门上限（key=TenantTaskGates.key()，如
     * data 等 gates.<key>）。落库 + 写透 Redis 闸门参数键，各实例监控线程
     * 控制节拍差量应用（5s 级收敛）。
     *
     * @return false=参数非法（空键/上限 &lt; 1）
     */
    boolean updateScheduleSettings(java.util.Map<String, Integer> gateLimits);
}
