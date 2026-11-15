package vip.mseapp.api.notify.dto;

import java.io.Serializable;

/**
 * 渠道视图（当前租户视角：登记 + 授权态 + 实例可用态），供业务端配置页展示勾选。
 */
public class NotifyChannelView implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 渠道编码 */
    private String code;

    /** 展示名 */
    private String name;

    /** 渠道说明 */
    private String description;

    /** 是否需租户授权 */
    private boolean authRequired;

    /** 权益开关编码（authRequired=true 时非空） */
    private String entitlementCode;

    /** 当前租户是否已授权（false 时配置页应置灰） */
    private boolean authorized;

    /** 渠道实例当前是否就绪（授权但实例未配置/停用时 false） */
    private boolean available;

    /** 是否需要个人接收人（false=群发渠道如 Webhook，接收人为空也可投递） */
    private boolean receiverRequired = true;

    /** 提供方（内置=框架内置；其余为提供方服务名） */
    private String provider;

    public NotifyChannelView() {
    }

    public NotifyChannelView(String code, String name, String description, boolean authRequired,
                             String entitlementCode, boolean authorized, boolean available, String provider) {
        this(code, name, description, authRequired, entitlementCode, authorized, available, true, provider);
    }

    public NotifyChannelView(String code, String name, String description, boolean authRequired,
                             String entitlementCode, boolean authorized, boolean available,
                             boolean receiverRequired, String provider) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.authRequired = authRequired;
        this.entitlementCode = entitlementCode;
        this.authorized = authorized;
        this.available = available;
        this.receiverRequired = receiverRequired;
        this.provider = provider;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isReceiverRequired() {
        return receiverRequired;
    }

    public void setReceiverRequired(boolean receiverRequired) {
        this.receiverRequired = receiverRequired;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
