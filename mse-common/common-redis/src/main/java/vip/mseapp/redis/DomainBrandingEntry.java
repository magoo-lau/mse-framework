package vip.mseapp.redis;

import java.io.Serializable;

/**
 * 登录页域名个性化缓存条目（Redis hash value，按域名寻址）。
 *
 * <p>租户域条目（域名绑定了租户）：携带 tenant 三要素 + 租户自助品牌（sys_tenant_branding，
 * 未配置时品牌字段为 null——登录页仍按默认样式渲染，但租户编码可锁定/隐藏）；
 * 平台域条目（平台自有域名，sys_domain_config）：tenant 三要素为 null，仅品牌字段。
 */
public class DomainBrandingEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 绑定租户编号；null = 平台自有域名（非租户绑定） */
    private Long tenantId;

    /** 绑定租户编码（登录页锁定/隐藏租户编码用）；平台域为 null */
    private String tenantCode;

    /** 绑定租户名称（登录页欢迎语）；平台域为 null */
    private String tenantName;

    /** 登录页标题；空 = 前端默认标题 */
    private String siteTitle;

    /** Logo 图片地址（http(s):// 或 / 开头路径）；空 = 不显示 */
    private String siteLogo;

    /** 登录背景图地址；空 = 背景色/默认渐变 */
    private String loginBackground;

    /** 背景色或渐变（CSS background 值）；背景图存在时垫底 */
    private String backgroundColor;

    /** 版权文案（登录页底部）；空 = 不显示 */
    private String copyright;

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
