package vip.mseapp.service.system.service;

import vip.mseapp.api.system.RemoteAuthService.TotpEnrollment;

/**
 * 登录双因子认证服务（TOTP 主因子 + 邮箱动态码降级通道）。
 *
 * <p>密钥与验证码的所有权在本服务（有库 + 有 Redis 侧）：TOTP 密钥落 sys_user.totp_secret，
 * 绑定挑战（pending）与邮箱验证码暂存 Redis；认证服务（service-auth）经 RemoteAuthService
 * 契约消费布尔结果，密钥/验证码本体不出本服务。所有方法显式携带租户编号（登录链路无租户上下文）。
 */
public interface TwoFactorService {

    /** 是否已绑定 TOTP（显式租户语义） */
    boolean isTotpBound(Long tenantId, Long userId);

    /**
     * 当前生效的双因子模式（本租户 → 平台域 → 代码默认 off；非法值归一为 off）。
     * 与登录链路 LoginPolicyService 同一合并语义，供 /me 下发前端显隐个人绑定入口。
     */
    String effectiveTotpMode(Long tenantId);

    /**
     * 生成 TOTP 绑定挑战：密钥暂存 Redis 10 分钟（未验证不落库），返回密钥 + otpauth URI + 二维码。
     * 登录强制绑定与个人中心绑定共用；重复调用以最后一次为准。
     */
    TotpEnrollment createPendingTotp(Long tenantId, Long userId);

    /** 确认绑定：校验动态码与 pending 密钥一致后写入 sys_user.totp_secret；pending 过期抛错 */
    boolean confirmPendingTotp(Long tenantId, Long userId, String code);

    /** 校验已绑定的 TOTP 动态码（未绑定/码错/密钥异常一律 false） */
    boolean verifyTotp(Long tenantId, Long userId, String code);

    /** 解绑：校验动态码通过后清除密钥（个人中心自助，丢失手机走管理端重置） */
    void unbindTotp(Long tenantId, Long userId, String code);

    /** 管理端重置（本租户域）：直接清除密钥与暂存态 */
    void resetTotp(Long tenantId, Long userId);

    /** 平台重置租户管理员（显式租户 + 超管条件，目标不存在或非管理员抛错） */
    void resetTenantAdminTotp(Long tenantId, String username);

    /** 邮箱动态码通道是否可用（SMTP 已配置 且 用户已填邮箱 且 通道开关开启） */
    boolean isEmailChannelAvailable(Long tenantId, Long userId);

    /**
     * 发送登录邮箱动态码（6 位数字，5 分钟有效；60s 发送冷却）。
     *
     * @return 脱敏邮箱（如 a***@example.com）
     * @throws vip.mseapp.core.exception.ServiceException 通道不可用 / 冷却中 / 发送失败
     */
    String sendLoginEmailCode(Long tenantId, Long userId);

    /** 校验邮箱动态码（一次性；码错/过期/超尝试次数一律 false） */
    boolean verifyEmailCode(Long tenantId, Long userId, String code);
}
