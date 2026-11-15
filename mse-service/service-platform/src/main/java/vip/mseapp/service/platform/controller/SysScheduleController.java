package vip.mseapp.service.platform.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.api.system.dto.ScheduleTaskAdminDTO;
import vip.mseapp.api.system.dto.ScheduleTaskLogDTO;
import vip.mseapp.service.platform.service.SysScheduleTaskService;
import vip.mseapp.web.log.OperLog;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 内置任务调度管理（平台运营端；sys_schedule_task / sys_schedule_task_log 为平台库全局表；
 * 页面为双端同页 system/Schedule，租户端入口仅在 standalone 离线私有部署存在）。
 *
 * <p>列表数据来自各服务调度中心的事件上报（启动一次 + 运行事件，无周期心跳）；启停/编辑落库后
 * 经 Redis 控制缓存 5s 级收敛，触发落库后经踢键促使实例立即 sync 抢占（恰好一个实例执行，
 * ≤5-10s 生效）——页面交互需提示「已下发，稍后生效」。
 * 编辑语义：调度覆盖统一按 cron 编辑（图形化生成器见前端 CronInput），清空恢复代码默认；
 * 生效调度列统一展示 cron（fixedDelay 任务渲染等价 cron）。执行日志每任务保留最近 200 条。
 * 静态开关（mse.scheduling.tasks.&lt;id&gt;.enabled=false）禁用的任务不注册不上报，不出现在本列表。
 */
@RestController
@RequestMapping("/tenant/schedule")
public class SysScheduleController {

    private final SysScheduleTaskService scheduleTaskService;

    public SysScheduleController(SysScheduleTaskService scheduleTaskService) {
        this.scheduleTaskService = scheduleTaskService;
    }

    @SaCheckPermission(value = "schedule:task:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/page")
    public Result<PageResult<ScheduleTaskAdminDTO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                         @RequestParam(defaultValue = "20") long pageSize,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String serviceName,
                                                         @RequestParam(required = false) String category) {
        return Result.ok(scheduleTaskService.listTasks(pageNum, pageSize, keyword, serviceName, category));
    }

    @SaCheckPermission(value = "schedule:task:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/{taskKey}/logs")
    public Result<PageResult<ScheduleTaskLogDTO>> logs(@PathVariable String taskKey,
                                                       @RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize,
                                                       @RequestParam(required = false) String result) {
        return Result.ok(scheduleTaskService.listLogs(taskKey, pageNum, pageSize, result));
    }

    /** cron 表达式校验 + 最近 10 次执行时间预览（编辑弹窗图形化生成器共用） */
    @GetMapping("/cron-preview")
    public Result<CronPreview> cronPreview(@RequestParam String cron) {
        return Result.ok(previewCron(cron));
    }

    static CronPreview previewCron(String cron) {
        String expression = cron == null ? "" : cron.trim();
        if (expression.isEmpty()) {
            return new CronPreview(false, "cron 表达式为空", List.of());
        }
        CronExpression parsed;
        try {
            parsed = CronExpression.parse(expression);
        } catch (IllegalArgumentException e) {
            return new CronPreview(false, "表达式非法: " + e.getMessage(), List.of());
        }
        List<String> times = new ArrayList<>(10);
        LocalDateTime time = LocalDateTime.now().withNano(0);
        while (times.size() < 10) {
            time = parsed.next(time);
            if (time == null) {
                break;
            }
            times.add(time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return new CronPreview(true, null, times);
    }

    record CronPreview(boolean valid, String message, List<String> nextTimes) {
    }

    @OperLog(module = "schedule:task", type = "启停")
    @SaCheckPermission(value = "schedule:task:toggle", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/{taskKey}/toggle")
    public Result<Boolean> toggle(@PathVariable String taskKey, @RequestParam boolean enabled) {
        return Result.ok(scheduleTaskService.toggle(taskKey, enabled));
    }

    @OperLog(module = "schedule:task", type = "立即触发")
    @SaCheckPermission(value = "schedule:task:trigger", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/{taskKey}/trigger")
    public Result<Boolean> trigger(@PathVariable String taskKey) {
        return Result.ok(scheduleTaskService.trigger(taskKey));
    }

    /** 编辑调度覆盖与详细说明（body：{ exprOverride, remark }；exprOverride 清空=恢复代码默认） */
    @OperLog(module = "schedule:task", type = "编辑")
    @SaCheckPermission(value = "schedule:task:edit", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/{taskKey}")
    public Result<Boolean> update(@PathVariable String taskKey, @RequestBody ScheduleEditRequest request) {
        boolean updated = scheduleTaskService.updateTask(taskKey, request.exprOverride(), request.remark());
        if (!updated) {
            return Result.fail(400, "任务不存在或覆盖表达式非法（纯数字 ≥1000 毫秒，或合法 cron）");
        }
        return Result.ok(Boolean.TRUE);
    }

    record ScheduleEditRequest(String exprOverride, String remark) {
    }

    // ==================== 调度器设置（「调度器」页签：租户并发闸门热更） ====================

    /** 读全部调度器设置（闸门参数为 gates.<key>=limit 行；空=未配置回落代码默认） */
    @SaCheckPermission(value = "schedule:task:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/settings")
    public Result<java.util.Map<String, String>> settings() {
        return Result.ok(scheduleTaskService.getSettings());
    }

    /**
     * 读闸门定义目录（key→remark：「调度器」页签动态渲染，前端内置行 + 本目录多出的键；
     * 第三方业务积木经自有平台域种子脚本自注册即出现，无需改前端）。
     */
    @SaCheckPermission(value = "schedule:task:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/settings/gate-defs")
    public Result<java.util.Map<String, String>> gateDefs() {
        return Result.ok(scheduleTaskService.getGateDefs());
    }

    /**
     * 更新租户并发闸门上限（body：{ "<gates.key>": <limit> }，如 { "data": 2 }）。
     * 落库 + 写透 Redis 闸门参数键，各实例监控线程 5s 级差量应用。
     */
    @OperLog(module = "schedule:task", type = "调度器设置")
    @SaCheckPermission(value = "schedule:task:edit", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/settings")
    public Result<Boolean> updateSettings(@RequestBody java.util.Map<String, Integer> gateLimits) {
        boolean updated = scheduleTaskService.updateScheduleSettings(gateLimits);
        if (!updated) {
            return Result.fail(400, "参数非法（键须为字母数字._-、上限 ≥ 1）或无配置存储");
        }
        return Result.ok(Boolean.TRUE);
    }
}
