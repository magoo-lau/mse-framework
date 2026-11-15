package vip.mseapp.service.system.notify;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.mseapp.api.notify.dto.NotifyChannelOutcome;
import vip.mseapp.api.notify.dto.NotifyMessage;
import vip.mseapp.api.notify.dto.NotifySendRequest;
import vip.mseapp.core.concurrent.MseExecutors;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.service.system.entity.SysNotifyLog;
import vip.mseapp.service.system.mapper.SysNotifyLogMapper;
import vip.mseapp.service.system.notify.SysNotifyCatalogService.CatalogEntry;

/**
 * 通知派发器（框架中枢核心）：逐渠道投递并记投递日志。
 *
 * <p>尽力而为语义：单渠道失败（未登记/未授权/不可用/投递异常）跳过并记录，不影响其他渠道、
 * 不向调用方抛出。逐渠道门禁次序：登记目录 → 租户授权（权益开关，对整租户生效）→
 * 渠道可用（实例就绪探测）→ 接收人匹配 → 投递。渠道解析：请求携带渠道则用之，
 * 为空时按租户路由 {@code (bizCode,msgType)} 解析（未配置默认仅站内信）——业务方发送零配置。
 *
 * <p>接收人语义：Webhook 等群发渠道（receiverRequired=false）无接收人也投递；
 * 消息无接收人且命中渠道全部需要接收人时整体忽略（debug 留痕）。
 *
 * <p>异步经 {@link MseExecutors} 命名有界池（缺省 wrap 传播租户/用户上下文；CallerRuns 背压，
 * 通知不可丢弃故不引入 TenantTaskGates 跳过语义）。
 */
@Service
public class NotifyDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotifyDispatchService.class);

    /** 请求未携带渠道且无路由命中时的默认渠道（站内信） */
    public static final String DEFAULT_CHANNEL = "inbox";

    private static final int ERROR_MAX = 500;

    private final SysNotifyCatalogService catalogService;
    private final NotifyChannelRouter router;
    private final NotifyChannelAuthService authService;
    private final NotifyRouteResolver routeResolver;
    private final SysNotifyLogMapper logMapper;

    private final ExecutorService executor = MseExecutors.builder("mse-sys-notify")
            .core(1).max(2).queueCapacity(2000).build();

    public NotifyDispatchService(SysNotifyCatalogService catalogService,
                                 NotifyChannelRouter router,
                                 NotifyChannelAuthService authService,
                                 NotifyRouteResolver routeResolver,
                                 SysNotifyLogMapper logMapper) {
        this.catalogService = catalogService;
        this.router = router;
        this.authService = authService;
        this.routeResolver = routeResolver;
        this.logMapper = logMapper;
    }

    /** 异步派发（生产方标准入口） */
    public void dispatch(NotifySendRequest request) {
        if (request == null || request.getMessage() == null) {
            return;
        }
        NotifyMessage message = request.getMessage();
        List<String> channelCodes = resolveCodes(request.getChannels(), message);
        if (!hasReceiver(message) && allRequireReceivers(channelCodes)) {
            log.debug("[notify] 通知无接收人且命中渠道均需个人接收人，忽略: biz={}, type={}",
                    message.getBizCode(), message.getMsgType());
            return;
        }
        try {
            executor.execute(() -> run(message, channelCodes));
        } catch (Exception e) {
            // CallerRuns 背压下仅在池关闭等极端场景到达
            log.error("[notify] 派发任务提交失败: biz={}, type={} - {}",
                    message.getBizCode(), message.getMsgType(), e.getMessage());
        }
    }

    /**
     * 同步执行渠道投递（异步任务与「发送测试」共用；执行完记录投递日志）。
     * 须在租户上下文内调用（异步任务经 wrap 传播，Web/RPC 线程天然具备）。
     */
    public List<NotifyChannelOutcome> run(NotifyMessage message, List<String> channelCodes) {
        Long tenantId = TenantContext.getTenantId();
        Set<String> distinct = new LinkedHashSet<>(resolveCodes(channelCodes, message));
        boolean receiverPresent = hasReceiver(message);
        List<NotifyChannelOutcome> outcomes = new ArrayList<>();
        for (String code : distinct) {
            outcomes.add(runChannel(code, tenantId, message, receiverPresent));
        }
        recordLog(message, distinct, outcomes);
        return outcomes;
    }

    private NotifyChannelOutcome runChannel(String channelCode, Long tenantId,
                                            NotifyMessage message, boolean receiverPresent) {
        var entryOpt = catalogService.find(channelCode);
        if (entryOpt.isEmpty()) {
            return new NotifyChannelOutcome(channelCode, false, "渠道未登记（提供方未部署或已下线）");
        }
        CatalogEntry entry = entryOpt.get();
        if (entry.receiverRequired() && !receiverPresent) {
            return new NotifyChannelOutcome(channelCode, false, "无接收人（渠道需个人接收人）");
        }
        if (!authService.authorized(entry, tenantId)) {
            return new NotifyChannelOutcome(channelCode, false, "渠道未授权（权益开关未开通）");
        }
        try {
            if (!router.available(entry)) {
                return new NotifyChannelOutcome(channelCode, false, "渠道不可用（实例未就绪或已停用）");
            }
            router.send(entry, message);
            return new NotifyChannelOutcome(channelCode, true, null);
        } catch (Exception e) {
            log.warn("[notify] 渠道投递失败: {} - {}", channelCode, e.getMessage());
            return new NotifyChannelOutcome(channelCode, false, abbreviate(e.getMessage()));
        }
    }

    private void recordLog(NotifyMessage message, Set<String> planned, List<NotifyChannelOutcome> outcomes) {
        try {
            SysNotifyLog row = new SysNotifyLog();
            row.setBizCode(message.getBizCode());
            row.setMsgType(message.getMsgType());
            row.setTitle(message.getTitle());
            row.setReceiverCount(message.getReceiverIds() == null ? 0 : message.getReceiverIds().size());
            row.setChannels(String.join(",", planned));
            row.setOkChannels(outcomes.stream().filter(NotifyChannelOutcome::isOk).map(NotifyChannelOutcome::getChannel)
                    .reduce((a, b) -> a + "," + b).orElse(null));
            row.setFailChannels(outcomes.stream().filter(o -> !o.isOk()).map(NotifyChannelOutcome::getChannel)
                    .reduce((a, b) -> a + "," + b).orElse(null));
            row.setError(abbreviate(outcomes.stream().filter(o -> !o.isOk() && o.getMessage() != null)
                    .map(o -> o.getChannel() + ": " + o.getMessage())
                    .reduce((a, b) -> a + "；" + b).orElse(null)));
            logMapper.insert(row);
        } catch (Exception e) {
            log.warn("[notify] 投递日志记录失败（不影响派发）: {}", e.getMessage());
        }
    }

    /** 投递日志分页（当前租户，时间倒序） */
    public Page<SysNotifyLog> pageLog(long pageNum, long pageSize, String bizCode, String msgType) {
        return logMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysNotifyLog>()
                        .eq(bizCode != null && !bizCode.isBlank(), SysNotifyLog::getBizCode, bizCode)
                        .eq(msgType != null && !msgType.isBlank(), SysNotifyLog::getMsgType, msgType)
                        .orderByDesc(SysNotifyLog::getId));
    }

    /** 渠道解析：请求携带渠道则用之；为空按租户路由 (bizCode,msgType) 解析，仍未命中默认站内信 */
    private List<String> resolveCodes(List<String> channels, NotifyMessage message) {
        Set<String> codes = new LinkedHashSet<>();
        if (channels != null) {
            for (String code : channels) {
                if (code != null && !code.isBlank()) {
                    codes.add(code.trim());
                }
            }
        }
        if (codes.isEmpty() && message != null) {
            codes.addAll(routeResolver.resolveChannels(message.getBizCode(), message.getMsgType()));
        }
        return codes.isEmpty() ? List.of(DEFAULT_CHANNEL) : List.copyOf(codes);
    }

    private static boolean hasReceiver(NotifyMessage message) {
        return message.getReceiverIds() != null && !message.getReceiverIds().isEmpty();
    }

    /** 命中渠道是否全部需要个人接收人（未登记渠道按需要处理，保守跳过） */
    private boolean allRequireReceivers(List<String> channelCodes) {
        for (String code : channelCodes) {
            var entry = catalogService.find(code);
            if (entry.isPresent() && !entry.get().receiverRequired()) {
                return false;
            }
        }
        return true;
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= ERROR_MAX ? value : value.substring(0, ERROR_MAX);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
