package vip.mseapp.service.system.notify;

import java.util.List;

import vip.mseapp.api.notify.RemoteNotifyChannelRegistry;
import vip.mseapp.api.notify.dto.NotifyChannelDescriptor;
import vip.mseapp.api.notify.dto.NotifySourceDescriptor;
import vip.mseapp.core.annotation.RpcService;

/**
 * 通知目录登记契约实现：接收提供方（dubbo 模式）推送的渠道/业务来源描述并落目录
 * （幂等 upsert 与防覆盖语义见 {@link SysNotifyCatalogService}）。
 */
@RpcService(RemoteNotifyChannelRegistry.class)
public class RemoteNotifyChannelRegistryImpl implements RemoteNotifyChannelRegistry {

    private final SysNotifyCatalogService catalogService;

    public RemoteNotifyChannelRegistryImpl(SysNotifyCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public void register(String providerGroup, List<NotifyChannelDescriptor> channels) {
        catalogService.register(providerGroup, channels);
    }

    @Override
    public void registerSources(String providerGroup, List<NotifySourceDescriptor> sources) {
        catalogService.registerSources(providerGroup, sources);
    }
}
