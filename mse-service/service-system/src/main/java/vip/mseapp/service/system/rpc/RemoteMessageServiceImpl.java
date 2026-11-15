package vip.mseapp.service.system.rpc;

import vip.mseapp.api.system.RemoteMessageService;
import vip.mseapp.api.system.dto.MessageDTO;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.service.SysMessageService;

import java.util.Collection;

/**
 * 消息服务中心契约实现：写路径供业务微服务投递站内消息（OA 审批待办/抄送/结果/催办等），
 * 读路径按调用方登录态隔离，支撑基座工作台消息铃铛与业务子应用自有消息页。
 */
@RpcService(RemoteMessageService.class)
public class RemoteMessageServiceImpl implements RemoteMessageService {

    private final SysMessageService messageService;

    public RemoteMessageServiceImpl(SysMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void send(Collection<Long> receiverIds, String type, String title, String content, String bizId) {
        messageService.send(receiverIds, type, title, content, bizId);
    }

    @Override
    public PageResult<MessageDTO> pageMessages(long pageNum, long pageSize, String type, Boolean unreadOnly) {
        return messageService.pageMessages(pageNum, pageSize, type, unreadOnly);
    }

    @Override
    public long unreadCount() {
        return messageService.unreadCount();
    }

    @Override
    public void markRead(Long id) {
        messageService.markRead(id);
    }

    @Override
    public void markAllRead() {
        messageService.markAllRead();
    }
}
