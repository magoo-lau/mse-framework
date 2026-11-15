package vip.mseapp.api.notify;

import java.util.List;

import vip.mseapp.api.notify.dto.NotifyChannelDescriptor;
import vip.mseapp.api.notify.dto.NotifySourceDescriptor;

/**
 * 通知目录登记契约（中枢侧实现：基座 service-system 接收提供方推送并落渠道/来源目录）。
 *
 * <p>由 api-notify 的推送器在 dubbo 模式下周期调用（幂等 upsert、同码不同提供组跳过防覆盖），
 * 一般不作为业务方的消费入口。mse-system 是该契约唯一提供方，推送器编程式引用无需 group。
 */
public interface RemoteNotifyChannelRegistry {

    /**
     * 登记渠道描述（providerGroup=提供方服务名，通常为 spring.application.name）。
     *
     * @param providerGroup 提供方服务名
     * @param channels      渠道描述清单
     */
    void register(String providerGroup, List<NotifyChannelDescriptor> channels);

    /**
     * 登记业务来源描述（业务方经 {@link NotifySourceProvider} 声明，随渠道同任务推送）。
     *
     * @param providerGroup 提供方服务名
     * @param sources       来源描述清单
     */
    void registerSources(String providerGroup, List<NotifySourceDescriptor> sources);
}
