package vip.mseapp.service.auth.support;

import org.springframework.data.redis.core.StringRedisTemplate;
import vip.mseapp.core.exception.ServiceException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * 双因子阶段临时凭证（密码已通过、第二因子未验证的中间态）。
 *
 * <p>Redis hash 暂存 5 分钟，持有登录上下文（租户/用户/平台侧/设备/绑定引导标记等）；
 * 校验尝试独立计数，超限自动作废——攻击者每轮完整登录最多试 5 个动态码。
 * 临时凭证与 Sa-Token 会话无关，换取正式 token 前不具备任何已登录能力。
 */
public class TwoFactorTokenService {

    private static final String KEY_TOKEN = "auth:2fa:";
    private static final String KEY_ATTEMPT = "auth:2fa:att:";

    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redis;

    public TwoFactorTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 密码校验通过后签发临时凭证 */
    public String issue(PendingLogin pending) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = KEY_TOKEN + token;
        redis.opsForHash().putAll(key, Map.of(
                "tenantId", String.valueOf(pending.tenantId()),
                "userId", String.valueOf(pending.userId()),
                "username", nullSafe(pending.username()),
                "nickname", nullSafe(pending.nickname()),
                "platform", String.valueOf(pending.platform()),
                "device", nullSafe(pending.device()),
                "enroll", String.valueOf(pending.enroll()),
                "emailAvailable", String.valueOf(pending.emailAvailable()),
                "maskedEmail", nullSafe(pending.maskedEmail())));
        redis.expire(key, TOKEN_TTL);
        return token;
    }

    /** 读取临时凭证（过期/不存在即抛错，提示重新走完整登录） */
    public PendingLogin require(String token) {
        Map<Object, Object> entries = redis.opsForHash().entries(KEY_TOKEN + token);
        if (entries.isEmpty()) {
            throw new ServiceException(401, "双因子验证会话已过期，请重新登录");
        }
        return new PendingLogin(
                Long.valueOf((String) entries.get("tenantId")),
                Long.valueOf((String) entries.get("userId")),
                orNull((String) entries.get("username")),
                orNull((String) entries.get("nickname")),
                Boolean.parseBoolean((String) entries.get("platform")),
                orNull((String) entries.get("device")),
                Boolean.parseBoolean((String) entries.get("enroll")),
                Boolean.parseBoolean((String) entries.get("emailAvailable")),
                orNull((String) entries.get("maskedEmail")));
    }

    /** 动态码校验失败记录一次；超限作废临时凭证（下次尝试将收到"会话已过期"） */
    public void recordFailure(String token) {
        String attKey = KEY_ATTEMPT + token;
        Long attempts = redis.opsForValue().increment(attKey);
        redis.expire(attKey, TOKEN_TTL);
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            redis.delete(KEY_TOKEN + token);
            redis.delete(attKey);
        }
    }

    /** 动态码校验成功：消费凭证（换取正式 token） */
    public void consume(String token) {
        redis.delete(KEY_TOKEN + token);
        redis.delete(KEY_ATTEMPT + token);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String orNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * 双因子阶段登录上下文。
     *
     * @param enroll         是否为强制绑定流程（required 模式且用户未绑定：需先扫码绑定再验证）
     * @param emailAvailable 邮箱动态码通道是否可用（签发时按开关 + 用户邮箱 + SMTP 就绪判定）
     * @param maskedEmail    脱敏邮箱（登录页展示发送目标，如 a***@example.com）
     */
    public record PendingLogin(Long tenantId, Long userId, String username, String nickname,
                               boolean platform, String device, boolean enroll,
                               boolean emailAvailable, String maskedEmail) {
    }
}
