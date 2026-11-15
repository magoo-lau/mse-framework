package vip.mseapp.api.notify;

import java.util.List;

import vip.mseapp.api.notify.dto.NotifyChannelDescriptor;
import vip.mseapp.api.notify.dto.NotifyMessage;

/**
 * 通知渠道提供方契约（渠道自注册：提供方实现并声明 Bean，框架通知中枢动态感知）。
 *
 * <p>接入方式（微服务形态）：渠道提供服务（如 mse-im）实现本接口并声明自己的 Bean，再用
 * {@code MseDubboSupport.serviceConfig(...)} 以 <b>group=spring.application.name</b> 导出，
 * 同时经 api-notify 的推送器（{@code NotifyChannelRegistrationPusher}，依赖本 jar 自动生效）把
 * {@link #describeChannels()} 的渠道描述周期推送至基座 mse-system 登记——框架与各业务零改动
 * 即收录新渠道：
 * <pre>{@code
 * @Bean(initMethod = "export", destroyMethod = "unexport")
 * @ConditionalOnProperty(name = "mse.rpc.mode", havingValue = "dubbo")
 * public ServiceConfig<RemoteNotifyChannel> imNotifyChannelConfig(RemoteNotifyChannel impl, ...) {
 *     ServiceConfig<RemoteNotifyChannel> config = MseDubboSupport.serviceConfig(RemoteNotifyChannel.class, impl, bf);
 *     config.setGroup("mse-im");
 *     return config;
 * }
 * }</pre>
 * 单体聚合形态：无需导出——框架中枢（service-system）直接收集容器内全部 RemoteNotifyChannel
 * Bean，推送器自检闲置（宿主已有登记契约实现）。
 *
 * <p>一个服务可提供多个渠道，但 dubbo 形态只导出<b>一个</b>本接口实现（同 group 重复导出会
 * 被消费者随机命中）：多渠道时在 {@link #send(String, NotifyMessage)} 按 channelCode 内部分发。
 *
 * <p>需要租户授权的渠道（IM/短信等）在 {@link NotifyChannelDescriptor} 声明
 * {@code authRequired=true + entitlementCode}，并在提供方 jar 内以平台域版本化脚本
 * （{@code -- mse-target: platform}）登记对应 {@code feature.*} 权益项（开关型，默认 false）；
 * 平台管理员经基座「附加项/权益覆盖」页面按租户授权后（对整租户生效，不论哪个业务发送），
 * 渠道才可用。
 */
public interface RemoteNotifyChannel {

    /** 本服务提供的渠道清单（channelCode 全局唯一；站内信 inbox 为框架内置，提供方勿占用） */
    List<NotifyChannelDescriptor> describeChannels();

    /** 渠道在当前租户是否可用（实例/账号就绪探测；未识别的 channelCode 返回 false，不抛出） */
    boolean available(String channelCode);

    /**
     * 投递到指定渠道（框架中枢按登记目录路由调用；单人/单目标失败由实现方自行容错续发，
     * 整渠道失败抛 ServiceException 由中枢记录投递日志，不影响其他渠道）。
     *
     * @param channelCode 渠道编码（来自 describeChannels 登记的 channelCode）
     * @param message     系统消息内容（接收人/标题/正文/深链；渠道按自身形态渲染）
     */
    void send(String channelCode, NotifyMessage message);
}
