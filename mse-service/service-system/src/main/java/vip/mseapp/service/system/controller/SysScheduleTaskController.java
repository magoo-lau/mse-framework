package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.api.system.RemoteScheduleStateApi;
import vip.mseapp.api.system.dto.ScheduleTaskAdminDTO;
import vip.mseapp.api.system.dto.ScheduleTaskLogDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
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
 * 内置任务调度管理·租户端入口（`/system/schedule/**`，LOGIN_TYPE_TENANT）。
 *
 * <p>经 {@code RemoteScheduleStateApi} 契约消费（standalone 为 service-system 的本库
 * 回落实现 local 直调；SaaS/单体为 service-platform 实现——单体 local 直调、微服务
 * dubbo 远调）。可见性与可操作性由**套餐圈定 + 权限码**门控：租户端菜单 195 经
 * base 通道播种至各租户库，默认基础套餐不含（SaaS 租户无感）；平台侧离线交付导出
 * 签发凭证时经 extra-menu-ids 默认并入（standalone 的 superAdmin 由 license 圈定
 * 的菜单域自然可见）。
 */
@RestController
@RequestMapping("/system/schedule")
public class SysScheduleTaskController {

    @RpcReference
    private RemoteScheduleStateApi scheduleStateApi;

    /** standalone 私有部署形态（页面渲染分支：私部租户即运维，渲染完整运维页） */
    @org.springframework.beans.factory.annotation.Value("${mse.standalone.enabled:false}")
    private boolean standalone;

    @SaCheckPermission(value = "schedule:task:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/page")
    public Result<PageResult<ScheduleTaskAdminDTO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                         @RequestParam(defaultValue = "20") long pageSize,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) String serviceName,
                                                         @RequestParam(required = false) String category) {
        return Result.ok(scheduleStateApi.listTasks(pageNum, pageSize, keyword, serviceName, category));
    }

    @SaCheckPermission(value = "schedule:task:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/{taskKey}/logs")
    public Result<PageResult<ScheduleTaskLogDTO>> logs(@PathVariable String taskKey,
                                                       @RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize,
                                                       @RequestParam(required = false) String result) {
        return Result.ok(scheduleStateApi.listLogs(taskKey, pageNum, pageSize, result));
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
    @SaCheckPermission(value = "schedule:task:toggle", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/{taskKey}/toggle")
    public Result<Boolean> toggle(@PathVariable String taskKey, @RequestParam boolean enabled) {
        return Result.ok(scheduleStateApi.toggle(taskKey, enabled));
    }

    @OperLog(module = "schedule:task", type = "立即触发")
    @SaCheckPermission(value = "schedule:task:trigger", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/{taskKey}/trigger")
    public Result<Boolean> trigger(@PathVariable String taskKey) {
        return Result.ok(scheduleStateApi.trigger(taskKey));
    }

    /** 编辑调度覆盖（cron）与详细说明（body：{ exprOverride, remark }；exprOverride 清空=恢复代码默认） */
    @OperLog(module = "schedule:task", type = "编辑")
    @SaCheckPermission(value = "schedule:task:edit", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping("/{taskKey}")
    public Result<Boolean> update(@PathVariable String taskKey, @RequestBody ScheduleEditRequest request) {
        boolean updated = scheduleStateApi.updateTask(taskKey, request.exprOverride(), request.remark());
        if (!updated) {
            return Result.fail(400, "任务不存在或覆盖表达式非法（合法 cron，或清空恢复默认）");
        }
        return Result.ok(Boolean.TRUE);
    }

    record ScheduleEditRequest(String exprOverride, String remark) {
    }

    // ==================== 形态探测与调度器设置（极简提示 + standalone 运维页） ====================

    /**
     * 部署形态探测（仅登录，无权限码——页面渲染分支用）：standalone=true 时租户端渲染运维页
     * （私部租户即运维，无平台端、无「调度器」页签）；false 时仅渲染一行极简提示——本页仅面向
     * 离线/私部租户，SaaS/单体租户的定时任务在各业务模块管理页内配置。
     */
    @SaCheckLogin(type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/form-factor")
    public Result<java.util.Map<String, Boolean>> formFactor() {
        return Result.ok(java.util.Map.of("standalone", standalone));
    }

    /** 读调度器设置（透传契约；standalone 读本库回落实现） */
    @SaCheckPermission(value = "schedule:task:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/settings")
    public Result<java.util.Map<String, String>> settings() {
        return Result.ok(scheduleStateApi.getScheduleSettings());
    }

    /** 更新租户并发闸门上限（透传契约；standalone 写本库回落实现） */
    @OperLog(module = "schedule:task", type = "调度器设置")
    @SaCheckPermission(value = "schedule:task:edit", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping("/settings")
    public Result<Boolean> updateSettings(@RequestBody java.util.Map<String, Integer> gateLimits) {
        boolean updated = scheduleStateApi.updateScheduleSettings(gateLimits);
        if (!updated) {
            return Result.fail(400, "参数非法（键须为字母数字._-、上限 ≥ 1）");
        }
        return Result.ok(Boolean.TRUE);
    }
}
