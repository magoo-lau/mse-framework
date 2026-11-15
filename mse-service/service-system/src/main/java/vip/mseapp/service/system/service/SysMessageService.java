package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.api.system.dto.MessageDTO;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysMessage;

import java.util.Collection;

/**
 * 消息服务中心（接收人个人数据：读侧隐式依赖登录态，写侧显式指定接收人）。
 * 写入入口为 {@code RemoteMessageService} 契约（业务微服务投递），读侧支撑
 * 基座消息铃铛与子应用自有消息页。
 */
public interface SysMessageService extends IRepository<SysMessage> {

    /** 批量投递（去重、跳过空接收人；随调用方事务落库） */
    void send(Collection<Long> receiverIds, String type, String title, String content, String bizId);

    /** 当前登录人消息分页（type 精确过滤，unreadOnly 仅未读） */
    PageResult<MessageDTO> pageMessages(long pageNum, long pageSize, String type, Boolean unreadOnly);

    /** 当前登录人未读数 */
    long unreadCount();

    /** 标记已读（仅本人消息生效） */
    void markRead(Long id);

    /** 当前登录人全部已读 */
    void markAllRead();
}
