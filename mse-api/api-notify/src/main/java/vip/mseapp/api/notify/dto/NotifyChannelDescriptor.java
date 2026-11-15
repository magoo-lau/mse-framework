package vip.mseapp.api.notify.dto;

import java.io.Serializable;

/**
 * 通知渠道描述（提供方自声明；channelCode 全局唯一，站内信 inbox 为框架内置）。
 *
 * <p>授权模型：站内信等基础渠道 {@code authRequired=false} 全租户可用；IM/短信等
 * {@code authRequired=true} 且必须携带 {@code entitlementCode}（基座权益开关项编码，
 * 如 {@code feature.im.notify}）——提供方须在自有 jar 内以平台域脚本登记该项（默认 false），
 * 平台管理员按租户授权后（对整租户生效）渠道才可用。
 */
public class NotifyChannelDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 渠道编码（全局唯一，小写短横线/下划线风格，如 inbox/im/sms） */
    private String channelCode;

    /** 展示名（如「IM 消息」） */
    private String channelName;

    /** 渠道说明（配置页辅助文案，可空） */
    private String description;

    /** 是否需要租户授权（true 时租户须持有 entitlementCode 对应权益开关才可用） */
    private boolean authRequired;

    /** 权益开关编码（authRequired=true 时必填，feature.* 项） */
    private String entitlementCode;

    /** 是否需要个人接收人（null/true=按 receiverIds 投递；false=群发渠道如 Webhook，无接收人也投递） */
    private Boolean receiverRequired;

    public NotifyChannelDescriptor() {
    }

    public NotifyChannelDescriptor(String channelCode, String channelName, String description,
                                   boolean authRequired, String entitlementCode) {
        this(channelCode, channelName, description, authRequired, entitlementCode, null);
    }

    public NotifyChannelDescriptor(String channelCode, String channelName, String description,
                                   boolean authRequired, String entitlementCode, Boolean receiverRequired) {
        this.channelCode = channelCode;
        this.channelName = channelName;
        this.description = description;
        this.authRequired = authRequired;
        this.entitlementCode = entitlementCode;
        this.receiverRequired = receiverRequired;
    }

    /** 便捷判定：未声明视为需要接收人（存量渠道兼容口径） */
    public boolean isReceiverRequired() {
        return receiverRequired == null || receiverRequired;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAuthRequired() {
        return authRequired;
    }

    public void setAuthRequired(boolean authRequired) {
        this.authRequired = authRequired;
    }

    public String getEntitlementCode() {
        return entitlementCode;
    }

    public void setEntitlementCode(String entitlementCode) {
        this.entitlementCode = entitlementCode;
    }

    public Boolean getReceiverRequired() {
        return receiverRequired;
    }

    public void setReceiverRequired(Boolean receiverRequired) {
        this.receiverRequired = receiverRequired;
    }
}
