package vip.mseapp.api.system.dto;

import java.io.Serializable;

/**
 * 租户品牌个性化（内部 RPC 契约）：租户自助定制的登录页标题 / Logo / 背景 / 版权。
 */
public class BrandDTO implements Serializable {

    private static final long serialVersionUID = 1L;

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
