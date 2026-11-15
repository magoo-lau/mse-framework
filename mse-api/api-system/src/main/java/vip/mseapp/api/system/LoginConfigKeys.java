package vip.mseapp.api.system;

/**
 * 登录安全配置键（sys_config 内置参数；平台默认行 tenant_id=0，租户插同键行即覆盖）。
 *
 * <p>service-auth（登录链路读取生效）与 service-system（「登录安全设置」页读写）共同依赖本常量类，
 * 保证键名单点维护。后台「登录安全设置」页即时生效；参数服务不可用时读取方按各自代码默认值 fail-open。
 */
public final class LoginConfigKeys {

    /** 图形验证码模式：none=关闭 / risk=风控触发（默认）/ always=每次登录 */
    public static final String CAPTCHA_MODE = "sys.login.captchaMode";

    /** 风控触发阈值：同账号连续登录失败次数（10 分钟滑动窗口） */
    public static final String CAPTCHA_USER_FAIL_THRESHOLD = "sys.login.captchaUserFailThreshold";

    /** 风控触发阈值：同 IP 连续登录失败次数（10 分钟滑动窗口） */
    public static final String CAPTCHA_IP_FAIL_THRESHOLD = "sys.login.captchaIpFailThreshold";

    /** 双因子认证模式：off=关闭（默认）/ optional=绑定 TOTP 的用户生效 / required=强制全员（登录时先绑定） */
    public static final String TOTP_MODE = "sys.login.totpMode";

    /** 邮箱动态码降级通道开关（true/false；还需部署侧 SMTP 配置与用户邮箱齐备才真正可用） */
    public static final String EMAIL_OTP = "sys.login.emailOtp";

    /** 允许多端登录（false=单点登录） */
    public static final String MULTI_DEVICE = "sys.login.multiDevice";

    /** 同端互斥（multiDevice=true 时生效） */
    public static final String SAME_DEVICE_EXCLUSIVE = "sys.login.sameDeviceExclusive";

    /** 平台界面名称（TOTP 绑定二维码 issuer 等） */
    public static final String PLATFORM_NAME = "sys.platform.name";

    private LoginConfigKeys() {
    }
}
