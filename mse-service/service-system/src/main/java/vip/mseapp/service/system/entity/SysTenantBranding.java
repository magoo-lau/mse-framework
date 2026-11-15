package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户品牌个性化（租户世界数据，每租户一行）：租户自助定制的登录页样式。
 *
 * <p>与平台库的 sys_domain_config（平台自有品牌）双轨：登录页解析先按域名绑定
 * 命中租户读本表，未命中再回落平台配置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_branding")
public class SysTenantBranding extends BaseEntity {

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
}
