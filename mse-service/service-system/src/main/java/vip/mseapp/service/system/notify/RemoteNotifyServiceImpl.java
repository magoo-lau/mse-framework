package vip.mseapp.service.system.notify;

import java.util.ArrayList;
import java.util.List;

import vip.mseapp.api.notify.RemoteNotifyService;
import vip.mseapp.api.notify.dto.NotifyChannelOutcome;
import vip.mseapp.api.notify.dto.NotifyChannelView;
import vip.mseapp.api.notify.dto.NotifySendRequest;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.service.system.notify.SysNotifyCatalogService.CatalogEntry;

/**
 * 通知发送契约实现（框架中枢）：业务层经 @RpcReference 投递（单体 local 本地直调）。
 * 渠道目录/租户授权/调度/投递日志均在本实现背后的 service-system 完成，业务层零感知。
 */
@RpcService(RemoteNotifyService.class)
public class RemoteNotifyServiceImpl implements RemoteNotifyService {

    private final NotifyDispatchService dispatchService;
    private final SysNotifyCatalogService catalogService;
    private final NotifyChannelAuthService authService;
    private final NotifyChannelRouter router;

    public RemoteNotifyServiceImpl(NotifyDispatchService dispatchService,
                                   SysNotifyCatalogService catalogService,
                                   NotifyChannelAuthService authService,
                                   NotifyChannelRouter router) {
        this.dispatchService = dispatchService;
        this.catalogService = catalogService;
        this.authService = authService;
        this.router = router;
    }

    @Override
    public void send(NotifySendRequest request) {
        dispatchService.dispatch(request);
    }

    @Override
    public List<NotifyChannelOutcome> sendSync(NotifySendRequest request) {
        if (request == null || request.getMessage() == null) {
            return List.of();
        }
        // 空接收人交给派发器门禁（群发渠道无接收人也投递，需接收人渠道记失败原因）
        return dispatchService.run(request.getMessage(), request.getChannels());
    }

    @Override
    public List<NotifyChannelView> listChannels() {
        Long tenantId = TenantContext.getTenantId();
        List<NotifyChannelView> views = new ArrayList<>();
        for (CatalogEntry entry : catalogService.catalog()) {
            boolean authorized = authService.authorized(entry, tenantId);
            boolean available = authorized && router.available(entry);
            views.add(new NotifyChannelView(entry.channelCode(), entry.channelName(), entry.description(),
                    entry.authRequired(), entry.entitlementCode(), authorized, available,
                    entry.receiverRequired(), entry.local() ? "内置" : entry.providerGroup()));
        }
        return views;
    }
}
