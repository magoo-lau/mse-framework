package vip.mseapp.service.system.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.api.notify.dto.NotifyChannelOutcome;
import vip.mseapp.api.notify.dto.NotifyChannelView;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.system.entity.SysNotifyLog;
import vip.mseapp.service.system.entity.SysNotifyRoute;
import vip.mseapp.service.system.notify.NotifyDispatchService;
import vip.mseapp.service.system.notify.SysNotifyCatalogService.SourceEntry;
import vip.mseapp.service.system.notify.SysNotifyRouteService;
import vip.mseapp.service.system.notify.SysNotifyWebhookService;

/**
 * 框架通知中枢·租户端管理（系统管理「通知渠道设置」+ 日志管理「通知投递日志」）：
 * 路由配置（业务动作 → 渠道，框架级租户配置）、来源目录（业务积木自注册）、渠道目录代理、
 * 发送测试与投递日志查询。业务方（OA/MES/Monitor…）不再自持配置页——发送不携带渠道时由
 * 中枢按路由解析。
 */
@Validated
@RestController
@RequestMapping("/system/notify")
public class SysNotifyController {

    private final NotifyDispatchService dispatchService;
    private final SysNotifyRouteService routeService;
    private final SysNotifyWebhookService webhookService;

    public SysNotifyController(NotifyDispatchService dispatchService, SysNotifyRouteService routeService,
                               SysNotifyWebhookService webhookService) {
        this.dispatchService = dispatchService;
        this.routeService = routeService;
        this.webhookService = webhookService;
    }

    @SaCheckPermission(value = "system:notify:log", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/log/page")
    public Result<PageResult<SysNotifyLog>> logPage(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) String bizCode,
                                                    @RequestParam(required = false) String msgType) {
        Page<SysNotifyLog> page = dispatchService.pageLog(pageNum, pageSize, bizCode, msgType);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    /** 业务来源目录（本地 Provider + 登记行合并；配置页「业务来源/消息类型」下拉数据源） */
    @SaCheckPermission(value = "system:notify:route:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/source/list")
    public Result<List<SourceView>> sourceList() {
        List<SourceView> views = new ArrayList<>();
        for (SourceEntry entry : routeService.listSources()) {
            views.add(new SourceView(entry.bizCode(), entry.bizName(), entry.types(), entry.providerGroup()));
        }
        return Result.ok(views);
    }

    /** 渠道目录（当前租户授权态 + 实例可用态；配置页勾选数据源，未授权/不可用置灰提示） */
    @SaCheckPermission(value = "system:notify:route:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/channel/list")
    public Result<List<NotifyChannelView>> channelList() {
        return Result.ok(routeService.listChannels());
    }

    @SaCheckPermission(value = "system:notify:route:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/route/page")
    public Result<PageResult<SysNotifyRoute>> routePage(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String bizCode,
            @RequestParam(required = false) String msgType) {
        Page<SysNotifyRoute> page = routeService.page(pageNum, pageSize, bizCode, msgType);
        return Result.ok(PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @SaCheckPermission(value = "system:notify:route:save", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/route/save")
    public Result<Long> routeSave(@RequestBody @Valid RouteSaveRequest request) {
        return Result.ok(routeService.save(request.id(), request.bizCode(), request.msgType(),
                request.channels(), request.status(), request.remark()));
    }

    /** 发送测试：向当前登录人按指定渠道同步投递，返回逐渠道结果（连通性验证） */
    @SaCheckPermission(value = "system:notify:route:save", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/test")
    public Result<Map<String, String>> test(@RequestBody @Valid TestRequest request) {
        Map<String, String> results = new LinkedHashMap<>();
        for (NotifyChannelOutcome outcome : routeService.testChannels(request.channels())) {
            results.put(outcome.getChannel(), outcome.isOk() ? "发送成功" : outcome.getMessage());
        }
        return Result.ok(results);
    }

    /** Webhook 渠道租户级配置（地址/格式，本租户全部勾选 webhook 的路由共用） */
    @SaCheckPermission(value = "system:notify:route:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/webhook/config")
    public Result<WebhookConfigView> webhookConfig() {
        var config = webhookService.current();
        return Result.ok(new WebhookConfigView(config.getWebhookUrl(), config.getWebhookFormat()));
    }

    @SaCheckPermission(value = "system:notify:route:save", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping("/webhook/config")
    public Result<Void> webhookConfigSave(@RequestBody @Valid WebhookConfigRequest request) {
        webhookService.save(request.webhookUrl(), request.webhookFormat());
        return Result.ok();
    }

    // ==================== 视图与请求体 ====================

    /** 来源目录视图（bizCode/bizName/types 为目录原生描述） */
    public record SourceView(String bizCode, String bizName,
                             List<vip.mseapp.api.notify.dto.NotifyTypeDescriptor> types,
                             String providerGroup) {
    }

    public record RouteSaveRequest(Long id,
                                   @NotBlank(message = "业务来源不能为空") String bizCode,
                                   @NotBlank(message = "消息类型不能为空") String msgType,
                                   @NotEmpty(message = "至少选择一个投递渠道") List<String> channels,
                                   Integer status,
                                   String remark) {
    }

    public record TestRequest(@NotEmpty(message = "至少选择一个测试渠道") List<String> channels) {
    }

    /** Webhook 渠道配置视图（地址空=渠道不可用） */
    public record WebhookConfigView(String webhookUrl, String webhookFormat) {
    }

    /** Webhook 渠道配置保存请求（地址可清空即停用；非空须 http(s) 开头） */
    public record WebhookConfigRequest(String webhookUrl, String webhookFormat) {
    }
}
