package vip.mseapp.api.notify;

import java.util.List;

import vip.mseapp.api.notify.dto.NotifyChannelOutcome;
import vip.mseapp.api.notify.dto.NotifyChannelView;
import vip.mseapp.api.notify.dto.NotifySendRequest;

/**
 * 消息通知发送契约（框架通知中枢，实现=基座 service-system）。
 *
 * <p>分层语义：业务层（OA/MES…）自行保存「业务动作 → 渠道」的选择（各自的配置表/配置页），
 * 发送时把解析出的渠道随 {@link NotifySendRequest} 一并交给本契约；渠道目录、租户授权
 * （权益开关，对整租户生效）、异步调度、投递日志均由框架层实现。
 *
 * <p>内容格式由框架契约提供：{@link vip.mseapp.api.notify.dto.NotifyMessage} 即系统消息类
 * （标题/正文/业务关联/跳转地址），各渠道自行渲染（站内=消息行、IM=链接卡片、短信=纯文本）。
 *
 * <p>投递为异步尽力而为语义：单渠道失败（未登记/未授权/不可用/投递异常）跳过并记录投递日志，
 * 不影响其他渠道、不向调用方抛出。
 */
public interface RemoteNotifyService {

    /**
     * 异步投递（生产方标准入口；channels 为空时默认仅站内信）。
     *
     * @param request 目标渠道 + 通知内容
     */
    void send(NotifySendRequest request);

    /**
     * 同步投递并返回逐渠道结果（测试/诊断用：配置页「发送测试」走此入口即时回显；
     * 同样记录投递日志）。须在租户上下文内调用。
     *
     * @param request 目标渠道 + 通知内容
     * @return 逐渠道结果（ok=false 时 message 为原因摘要）
     */
    List<NotifyChannelOutcome> sendSync(NotifySendRequest request);

    /**
     * 当前租户可用的渠道清单（登记目录 + 授权态 + 实例可用态），供各业务端配置页代理展示。
     *
     * @return 渠道视图（authorized=false 表示租户未获授权、available=false 表示实例未就绪）
     */
    List<NotifyChannelView> listChannels();
}
