package vip.mseapp.api.system;

import vip.mseapp.api.system.dto.MessageDTO;
import vip.mseapp.core.result.PageResult;

import java.util.Collection;

/**
 * 消息服务中心契约（service-system 实现，sys_message 落库）。
 *
 * <p>写路径供业务微服务投递站内消息（如 OA 审批待办/抄送/结果/催办），消费方经
 * @RpcReference 注入：单体本地直调，微服务经 Dubbo 调用 mse-system；读路径同时支撑
 * 基座工作台消息铃铛与业务子应用自有消息页（同一份数据）。
 *
 * <p>用户语义：send 显式指定接收人；page/unreadCount/markRead/markAllRead 隐式依赖
 * 调用方用户上下文（HTTP 链路经 token 注入，Dubbo 链路经 attachment 透传），
 * 服务端按“登录人 = 接收人”隔离，个人数据无需权限点。
 */
public interface RemoteMessageService {

    /**
     * 批量投递站内消息（幂等去重由调用方保证；单条失败不中断批次）。
     *
     * @param receiverIds 接收人编号集合（自动去重，空集合直接返回）
     * @param type        消息类型（业务自定义，如 TODO/CC/RESULT/REMIND）
     * @param title       标题（可空）
     * @param content     正文
     * @param bizId       业务关联标识（如流程实例编号，可空）
     */
    void send(Collection<Long> receiverIds, String type, String title, String content, String bizId);

    /** 当前登录人消息分页（type 传业务类型精确过滤，unreadOnly=true 仅未读） */
    PageResult<MessageDTO> pageMessages(long pageNum, long pageSize, String type, Boolean unreadOnly);

    /** 当前登录人未读数 */
    long unreadCount();

    /** 标记已读（仅本人消息生效） */
    void markRead(Long id);

    /** 全部已读 */
    void markAllRead();
}
