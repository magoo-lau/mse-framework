package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 域名个性化配置（平台级全局表，domain 唯一）：按访问域名定制登录页样式。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_domain_config")
public class SysDomainConfig extends PlatformBaseEntity {

    /** 域名（完整主机名，小写，全局唯一） */
    private String domain;

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

    private String remark;

    /** 0 启用 1 停用（停用后登录页回退默认样式） */
    private Integer status;
}
