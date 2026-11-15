package vip.mseapp.api.notify;

import java.util.List;

import vip.mseapp.api.notify.dto.NotifySourceDescriptor;

/**
 * 通知业务来源声明 SPI（业务积木自注册）：声明本模块的业务来源码与消息类型清单，
 * 供框架路由配置页下拉与保存校验。与 {@link RemoteNotifyChannel}（渠道提供方）相对，
 * 本接口面向「发送通知的业务方」——依赖 api-notify 即生效：
 *
 * <ul>
 *   <li>单体聚合形态：框架目录直接收集容器内 Provider Bean；</li>
 *   <li>微服务形态：api-notify 推送器随渠道任务周期重推自登记（幂等刷新）。</li>
 * </ul>
 *
 * <p>声明为元信息而非硬约束：未声明来源仍可发送（路由按通配/兜底解析），声明只决定
 * 配置页可见性与校验口径。参考实现：oa-bpm {@code OaNotifySourceProvider}。
 */
public interface NotifySourceProvider {

    /**
     * 声明来源清单（一个模块通常一个来源；bizCode 全局唯一，重复登记跳过防覆盖）。
     *
     * @return 来源描述清单
     */
    List<NotifySourceDescriptor> describeSources();
}
