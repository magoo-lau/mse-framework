package vip.mseapp.api.notify.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 系统消息内容类（框架统一的消息格式：各渠道按自身形态渲染）。
 *
 * <p>站内信渠道落标题/正文/bizId 深链；IM 渠道带 linkUrl 渲染链接卡片（无则纯文本）；
 * 短信渠道取标题+正文纯文本。接收人显式携带，租户经 RPC 上下文隐式透传（契约不传 tenantId）。
 */
public class NotifyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务来源码（如 oa/mes；投递日志归档维度） */
    private String bizCode;

    /** 消息类型（业务自定义动作语义，如 TODO/CC/RESULT/REMIND/TEST；日志归档维度） */
    private String msgType;

    /** 接收人编号清单（去重由中枢负责） */
    private List<Long> receiverIds;

    /** 标题（可空） */
    private String title;

    /** 正文 */
    private String content;

    /** 业务关联标识（站内信前端据此深链，如流程实例编号） */
    private String bizId;

    /** 跳转地址（可空；推荐以 / 开头的基座内相对路径如 /sub/oa/detail/task/xxx——业务方只拼自己
     * 的前端路由，站点域名由渠道结合自身基址知识补全；也接受绝对 http(s) 地址原样透传。
     * 卡片渠道有值时渲染跳转卡片，无值发纯文本） */
    private String linkUrl;

    public NotifyMessage() {
    }

    public NotifyMessage(String bizCode, String msgType, List<Long> receiverIds,
                         String title, String content, String bizId, String linkUrl) {
        this.bizCode = bizCode;
        this.msgType = msgType;
        this.receiverIds = receiverIds;
        this.title = title;
        this.content = content;
        this.bizId = bizId;
        this.linkUrl = linkUrl;
    }

    public String getBizCode() {
        return bizCode;
    }

    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public List<Long> getReceiverIds() {
        return receiverIds;
    }

    public void setReceiverIds(List<Long> receiverIds) {
        this.receiverIds = receiverIds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }
}
