package vip.mseapp.api.tenant.dto;

import java.io.Serializable;

/**
 * 域名个性化配置（内部 RPC 契约）：按访问域名定制登录页标题 / Logo / 背景 / 版权。
 */
public class DomainConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 命中的域名（完整主机名，小写） */
    private String domain;

    /** 域名绑定的租户编号；null = 平台自有域名（租户自助绑定/平台管理端配置二选一命中） */
    private Long tenantId;

    /** 域名绑定租户的编码（登录页据此锁定并隐藏租户编码）；平台域为 null */
    private String tenantCode;

    /** 域名绑定租户的名称（登录页欢迎语）；平台域为 null */
    private String tenantName;

    /** 登录页标题；空 = 使用前端默认标题 */
    private String siteTitle;

    /** Logo 图片地址（http(s):// 或 / 开头路径）；空 = 不显示 */
    private String siteLogo;

    /** 登录背景图地址；空 = 使用背景色/默认渐变 */
    private String loginBackground;

    /** 背景色或渐变（CSS background 值）；背景图存在时垫底 */
    private String backgroundColor;

    /** 版权文案（登录页底部）；空 = 不显示 */
    private String copyright;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getSiteTitle() {
        return siteTitle;
    }

    public void setSiteTitle(String siteTitle) {
        this.siteTitle = siteTitle;
    }

    public String getSiteLogo() {
        return siteLogo;
    }

    public void setSiteLogo(String siteLogo) {
        this.siteLogo = siteLogo;
    }

    public String getLoginBackground() {
        return loginBackground;
    }

    public void setLoginBackground(String loginBackground) {
        this.loginBackground = loginBackground;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }
}
