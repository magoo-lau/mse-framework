package vip.mseapp.rpc.bridge;

import org.springframework.context.event.EventListener;
import vip.mseapp.mq.TenantEvent;

/**
 * 数据面租户事件监听器：控制面（mse-platform）发布的租户事件在本服务的进程内直达通道
 * （单体聚合形态——TenantEventSender 无 MQ 时回落 Spring 事件发布）。处理逻辑统一在
 * {@link TenantEventInboundHandler}（与 MQ 通道共用）。
 */
public class TenantDataUpgradeListener {

    private final TenantEventInboundHandler handler;

    public TenantDataUpgradeListener(TenantEventInboundHandler handler) {
        this.handler = handler;
    }

    @EventListener
    public void onTenantEvent(TenantEvent event) {
        handler.handle(event);
    }
}
