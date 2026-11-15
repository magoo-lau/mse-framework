package vip.mseapp.service.auth.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.mseapp.api.system.LoginConfigKeys;
import vip.mseapp.api.system.RemoteConfigService;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.constant.MseConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 登录策略解析（sys_config 内置参数，后台「登录安全设置」页可改、即时生效）。
 *
 * <ul>
 *   <li>{@code sys.login.multiDevice}：是否允许多端/多处并发登录；false=单点登录，登录时顶掉该账号全部旧会话；</li>
 *   <li>{@code sys.login.sameDeviceExclusive}：同端互斥（multiDevice=true 时生效），登录时只顶掉同设备旧会话；</li>
 *   <li>{@code sys.login.captchaMode}：图形验证码 none=关闭 / risk=风控触发 / always=每次登录；</li>
 *   <li>{@code sys.login.captchaUserFailThreshold} / {@code captchaIpFailThreshold}：risk 模式下同账号/同 IP
 *       连续登录失败阈值（10 分钟滑动窗口计数，由 {@link LoginCaptchaService} 维护）；</li>
 *   <li>{@code sys.login.totpMode}：双因子认证 off=关闭 / optional=已绑定 TOTP 的用户生效 / required=强制全员；</li>
 *   <li>{@code sys.login.emailOtp}：邮箱动态码降级通道开关。</li>
 * </ul>
 * 读取顺序（两端独立，单世界生效值）：本租户覆盖行 → 本库默认模板行（tenant_id=0 同库副本）→ 代码默认
 * （允许多端、不同端不互斥、验证码 risk、双因子 off、邮箱通道关——兼容历史行为）；
 * 平台会话即平台库自身参数行。参数服务不可用时 fail-open 用默认值，登录链路不因配置读取失败而中断。
 */
public class LoginPolicyService {

    public static final String KEY_MULTI_DEVICE = LoginConfigKeys.MULTI_DEVICE;
    public static final String KEY_SAME_DEVICE_EXCLUSIVE = LoginConfigKeys.SAME_DEVICE_EXCLUSIVE;

    private static final Logger log = LoggerFactory.getLogger(LoginPolicyService.class);

    private static final List<String> KEYS = List.of(
            LoginConfigKeys.MULTI_DEVICE,
            LoginConfigKeys.SAME_DEVICE_EXCLUSIVE,
            LoginConfigKeys.CAPTCHA_MODE,
            LoginConfigKeys.CAPTCHA_USER_FAIL_THRESHOLD,
            LoginConfigKeys.CAPTCHA_IP_FAIL_THRESHOLD,
            LoginConfigKeys.TOTP_MODE,
            LoginConfigKeys.EMAIL_OTP);

    /** 异常值兜底默认：验证码风控触发、双因子关闭、邮箱通道关 */
    private static final String DEFAULT_CAPTCHA_MODE = "risk";
    private static final String DEFAULT_TOTP_MODE = "off";

    @RpcReference
    private RemoteConfigService remoteConfigService;

    public LoginPolicy resolve(Long tenantId) {
        try {
            Long effectiveTenantId = tenantId == null ? MseConstants.PLATFORM_TENANT_ID : tenantId;
            Map<String, String> values = readQuietly(effectiveTenantId);
            // 仅显式 false 关闭（异常值保持开放）；仅显式 true 开启互斥（异常值不误伤）
            boolean multiDevice = !"false".equalsIgnoreCase(values.get(LoginConfigKeys.MULTI_DEVICE));
            boolean sameDeviceExclusive = "true".equalsIgnoreCase(values.get(LoginConfigKeys.SAME_DEVICE_EXCLUSIVE));
            return new LoginPolicy(
                    multiDevice,
                    sameDeviceExclusive,
                    modeOrDefault(values.get(LoginConfigKeys.CAPTCHA_MODE), CAPTCHA_MODES, DEFAULT_CAPTCHA_MODE),
                    intOrDefault(values.get(LoginConfigKeys.CAPTCHA_USER_FAIL_THRESHOLD), 3),
                    intOrDefault(values.get(LoginConfigKeys.CAPTCHA_IP_FAIL_THRESHOLD), 10),
                    modeOrDefault(values.get(LoginConfigKeys.TOTP_MODE), TOTP_MODES, DEFAULT_TOTP_MODE),
                    "true".equalsIgnoreCase(values.get(LoginConfigKeys.EMAIL_OTP)));
        } catch (Exception ex) {
            log.warn("登录策略解析失败，使用默认策略（多端并发/验证码风控/双因子关闭）", ex);
            return LoginPolicy.defaults();
        }
    }

    private Map<String, String> readQuietly(Long tenantId) {
        try {
            Map<String, String> values = remoteConfigService.getConfigValues(tenantId, KEYS);
            return values == null ? Map.of() : values;
        } catch (Exception ex) {
            log.warn("登录策略参数读取失败：tenantId={}", tenantId, ex);
            return Map.of();
        }
    }

    private static final Set<String> CAPTCHA_MODES = Set.of("none", "risk", "always");
    private static final Set<String> TOTP_MODES = Set.of("off", "optional", "required");

    private static String modeOrDefault(String value, Set<String> allowed, String defaultValue) {
        return value != null && allowed.contains(value.toLowerCase()) ? value.toLowerCase() : defaultValue;
    }

    private static int intOrDefault(String value, int defaultValue) {
        try {
            int parsed = Integer.parseInt(value == null ? "" : value.trim());
            return parsed >= 1 && parsed <= 100 ? parsed : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /** 解析后的登录策略（验证码/双因子/会话策略一体） */
    public record LoginPolicy(boolean multiDevice, boolean sameDeviceExclusive,
                              String captchaMode, int captchaUserFailThreshold, int captchaIpFailThreshold,
                              String totpMode, boolean emailOtp) {

        public static LoginPolicy defaults() {
            return new LoginPolicy(true, false, "risk", 3, 10, "off", false);
        }

        /** always=每次登录都要验证码 */
        public boolean captchaAlways() {
            return "always".equals(captchaMode);
        }

        /** risk=风控触发模式（是否实际触发由失败计数决定） */
        public boolean captchaRisk() {
            return "risk".equals(captchaMode);
        }

        /** 双因子开关：optional=绑定用户生效；required=全员（未绑定走登录绑定流程） */
        public boolean totpOptional() {
            return "optional".equals(totpMode);
        }

        public boolean totpRequiredAll() {
            return "required".equals(totpMode);
        }

        /** 该用户本次登录是否进入双因子流程 */
        public boolean twoFactorRequired(boolean totpBound) {
            if (totpRequiredAll()) {
                return true;
            }
            return totpOptional() && totpBound;
        }
    }
}
