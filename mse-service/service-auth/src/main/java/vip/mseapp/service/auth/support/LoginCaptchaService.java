package vip.mseapp.service.auth.support;

import com.wf.captcha.SpecCaptcha;
import org.springframework.data.redis.core.StringRedisTemplate;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.service.auth.support.LoginPolicyService.LoginPolicy;

import java.time.Duration;
import java.util.UUID;

/**
 * 登录图形验证码 + 登录风控计数（Redis 全程，无库依赖）。
 *
 * <p>验证码：字符图（SpecCaptcha；ArithmeticCaptcha 依赖 javax.script JS 引擎，JDK 15+ 移除 Nashorn 后
 * 生成图片必 NPE，勿改回），Redis 暂存 2 分钟一次性（校验即删，忽略大小写）。
 * 风控：同账号 / 同 IP 连续登录失败 INCR + 10 分钟滑动窗口，登录成功双清零；
 * captchaMode=risk 时计数达阈值即要求验证码——平时无感、被攻击时自动升级。
 * 验证码失败只计 IP（攻击信号），密码/动态码失败双计（账号 + IP）。
 */
public class LoginCaptchaService {

    private static final String KEY_CAPTCHA = "auth:captcha:";
    private static final String KEY_FAIL_USER = "auth:fail:user:";
    private static final String KEY_FAIL_IP = "auth:fail:ip:";

    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(2);
    private static final Duration FAIL_WINDOW = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;

    public LoginCaptchaService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 生成验证码挑战（登录页预检入口）：不需要时返回 required=false（前端隐藏验证码行）。
     */
    public CaptchaChallenge challenge(LoginPolicy policy, Long tenantId, String username, String ip) {
        if (!required(policy, tenantId, username, ip)) {
            return new CaptchaChallenge(false, null, null);
        }
        SpecCaptcha captcha = new SpecCaptcha(112, 36);
        String code = captcha.text();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(KEY_CAPTCHA + captchaId, code.toLowerCase(), CAPTCHA_TTL);
        return new CaptchaChallenge(true, captchaId, captcha.toBase64());
    }

    /** 是否要求验证码：always；或 risk 且同账号/同 IP 失败计数达阈值 */
    public boolean required(LoginPolicy policy, Long tenantId, String username, String ip) {
        if (policy.captchaAlways()) {
            return true;
        }
        if (!policy.captchaRisk()) {
            return false;
        }
        if (count(userFailKey(tenantId, username)) >= policy.captchaUserFailThreshold()) {
            return true;
        }
        return count(ipFailKey(ip)) >= policy.captchaIpFailThreshold();
    }

    /**
     * 登录主链路强制复核（防绕过登录页预检）：要求验证码而未通过即抛错。
     * 在密码校验之前调用，验证码失败不计入账号失败计数（只计 IP）。
     */
    public void verify(LoginPolicy policy, Long tenantId, String username, String ip,
                       String captchaId, String captchaCode) {
        if (!required(policy, tenantId, username, ip)) {
            return;
        }
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
            recordIpFailure(ip);
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "请输入图形验证码");
        }
        String key = KEY_CAPTCHA + captchaId;
        String expected = redis.opsForValue().get(key);
        if (expected != null) {
            redis.delete(key);
        }
        if (expected == null || !expected.equals(captchaCode.trim().toLowerCase())) {
            recordIpFailure(ip);
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "图形验证码错误或已过期");
        }
    }

    /** 密码 / 动态码校验失败：账号 + IP 双计数（风控信号源） */
    public void recordPasswordFailure(Long tenantId, String username, String ip) {
        increment(userFailKey(tenantId, username));
        recordIpFailure(ip);
    }

    /** 登录成功（含双因子阶段完成）：双清零 */
    public void resetFailures(Long tenantId, String username, String ip) {
        redis.delete(userFailKey(tenantId, username));
        redis.delete(ipFailKey(ip));
    }

    private void recordIpFailure(String ip) {
        increment(ipFailKey(ip));
    }

    private void increment(String key) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, FAIL_WINDOW);
        }
    }

    private int count(String key) {
        String value = redis.opsForValue().get(key);
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /** 平台登录（tenant 0）与租户登录共用键空间；tenantId 为空归一为 0 */
    private static String userFailKey(Long tenantId, String username) {
        return KEY_FAIL_USER + (tenantId == null ? 0L : tenantId) + ":" + username;
    }

    private static String ipFailKey(String ip) {
        return KEY_FAIL_IP + (ip == null ? "unknown" : ip);
    }

    /** 验证码挑战（image 为 base64 data URL，可直接进 img 标签） */
    public record CaptchaChallenge(boolean required, String captchaId, String image) {
    }
}
