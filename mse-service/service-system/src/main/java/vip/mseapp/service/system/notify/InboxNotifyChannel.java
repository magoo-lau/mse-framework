package vip.mseapp.service.system.notify;

import java.util.List;

import vip.mseapp.api.notify.RemoteNotifyChannel;
import vip.mseapp.api.notify.dto.NotifyChannelDescriptor;
import vip.mseapp.api.notify.dto.NotifyMessage;
import vip.mseapp.service.system.service.SysMessageService;

/**
 * 框架内置站内信渠道：直接本地调用基座消息中心 {@link SysMessageService} 落 sys_message
 * （前端铃铛/消息中心页读取）。全租户默认可用（authRequired=false，不消费权益开关）；
 * msgType 原样作为基座消息 type。
 */
public class InboxNotifyChannel implements RemoteNotifyChannel {

    /** 渠道编码：站内信 */
    public static final String CODE = "inbox";

    private final SysMessageService messageService;

    public InboxNotifyChannel(SysMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public List<NotifyChannelDescriptor> describeChannels() {
        return List.of(new NotifyChannelDescriptor(CODE, "站内通知",
                "基座消息中心（顶栏铃铛/消息中心页），全租户默认可用", false, null));
    }

    @Override
    public boolean available(String channelCode) {
        return CODE.equals(channelCode);
    }

    @Override
    public void send(String channelCode, NotifyMessage message) {
        if (!CODE.equals(channelCode)) {
            throw new IllegalArgumentException("未知渠道编码：" + channelCode);
        }
        messageService.send(message.getReceiverIds(), message.getMsgType(),
                message.getTitle(), message.getContent(), message.getBizId());
    }
}
