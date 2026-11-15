package vip.mseapp.api.system;

import java.io.Serializable;

/**
 * 登录双因子认证契约（service-system 实现，认证服务 service-auth 消费）。
 *
 * <p>TOTP 密钥与邮箱动态码的所有权收在 service-system（有库侧），密钥/验证码本体不出该服务；
 * 认证服务只拿布尔结果与绑定二维码等非敏感数据。所有方法显式携带租户编号（登录链路无租户上下文），
 * 实现内部 runAs。
 */
public interface RemoteAuthService {

    /** 校验已绑定的 TOTP 动态码（未绑定/码错/密钥异常一律 false） */
    boolean verifyTotp(Long tenantId, Long userId, String code);

    /**
     * 发送登录邮箱动态码（6 位数字，5 分钟有效；60s 冷却、校验限 5 次）。
     *
     * @return 发送结果（携带脱敏邮箱供登录页展示）
     * @throws vip.mseapp.core.exception.ServiceException SMTP 未配置 / 用户无邮箱 / 发送冷却中 / 发送失败
     */
    EmailChallenge sendLoginEmailCode(Long tenantId, Long userId);

    /** 校验邮箱动态码（一次性，码错/过期/超尝试次数一律 false） */
    boolean verifyEmailCode(Long tenantId, Long userId, String code);

    /**
     * 生成 TOTP 绑定挑战（密钥暂存服务端 10 分钟，验证通过才落库；登录强制绑定与个人中心绑定共用）。
     *
     * @return 密钥 + otpauth URI + 二维码 PNG（base64 data URL）
     */
    TotpEnrollment createPendingTotp(Long tenantId, Long userId);

    /** 确认绑定：校验动态码与 pending 密钥一致后写入 sys_user.totp_secret */
    boolean confirmPendingTotp(Long tenantId, Long userId, String code);

    /** 平台重置租户管理员 TOTP（一次性开通写入：按租户 + 用户名定位，不回读租户库；目标不存在或非管理员抛错） */
    void resetTenantAdminTotp(Long tenantId, String username);

    /** 邮箱动态码通道是否可用（通道开关开启 + SMTP 已配置 + 用户已填邮箱；登录页据此显隐降级入口） */
    boolean isEmailChannelAvailable(Long tenantId, Long userId);

    /** TOTP 绑定挑战：二维码由服务端渲染（前端零依赖），secret 同时返回供手输 */
    record TotpEnrollment(String secret, String otpauthUri, String qrImage) implements Serializable {
    }

    /** 邮箱动态码发送结果 */
    record EmailChallenge(boolean sent, String maskedEmail) implements Serializable {
    }
}
