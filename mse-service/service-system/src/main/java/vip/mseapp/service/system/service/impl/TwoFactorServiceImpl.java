package vip.mseapp.service.system.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vip.mseapp.api.system.LoginConfigKeys;
import vip.mseapp.api.system.RemoteAuthService.TotpEnrollment;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.mail.MseMailSender;
import vip.mseapp.security.crypto.TotpUtils;
import vip.mseapp.service.system.entity.SysUser;
import vip.mseapp.service.system.service.SysConfigService;
import vip.mseapp.service.system.service.SysUserService;
import vip.mseapp.service.system.service.TwoFactorService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 双因子认证服务实现（TOTP 主因子 + 邮箱动态码降级通道）。
 *
 * <p>TOTP 绑定走「pending 挑战」两段式：密钥先生成暂存 Redis（10 分钟），验证动态码通过才落库，
 * 避免用户扫码中途放弃留下未知密钥导致锁死。邮箱通道可用性 = MseMailSender 装配且 SMTP 就绪
 * 且用户已填邮箱且 sys.login.emailOtp 开启，四者缺一即不可用（登录页据此隐藏入口）。
 */
public class TwoFactorServiceImpl implements TwoFactorService {

    private static final String KEY_PENDING = "auth:totp:pending:";
    private static final String KEY_EMAIL_CODE = "auth:emailcode:";
    private static final String KEY_EMAIL_COOL = "auth:emailcode:cool:";
    private static final String KEY_EMAIL_ATT = "auth:emailcode:att:";

    private static final Duration PENDING_TTL = Duration.ofMinutes(10);
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration COOL_TTL = Duration.ofSeconds(60);
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private static final Set<String> TOTP_MODES = Set.of("off", "optional", "required");
    private static final String DEFAULT_TOTP_MODE = "off";

    private final SysUserService userService;
    private final SysConfigService configService;
    private final StringRedisTemplate redis;
    private final MseMailSender mailSender;

    public TwoFactorServiceImpl(SysUserService userService, SysConfigService configService,
                                StringRedisTemplate redis, ObjectProvider<MseMailSender> mailSender) {
        this.userService = userService;
        this.configService = configService;
        this.redis = redis;
        this.mailSender = mailSender.getIfAvailable();
    }

    @Override
    public boolean isTotpBound(Long tenantId, Long userId) {
        SysUser user = loadUser(tenantId, userId);
        return StringUtils.hasText(user.getTotpSecret());
    }

    @Override
    public String effectiveTotpMode(Long tenantId) {
        String mode = effective(tenantId, LoginConfigKeys.TOTP_MODE);
        return mode != null && TOTP_MODES.contains(mode.toLowerCase()) ? mode.toLowerCase() : DEFAULT_TOTP_MODE;
    }

    @Override
    public TotpEnrollment createPendingTotp(Long tenantId, Long userId) {
        SysUser user = loadUser(tenantId, userId);
        String secret = TotpUtils.generateSecret();
        String issuer = platformName();
        String otpauthUri = TotpUtils.buildOtpauthUri(issuer, user.getUsername(), secret);
        redis.opsForValue().set(pendingKey(tenantId, userId), secret, PENDING_TTL);
        return new TotpEnrollment(secret, otpauthUri, qrPngDataUrl(otpauthUri));
    }

    @Override
    public boolean confirmPendingTotp(Long tenantId, Long userId, String code) {
        // 不加 @Transactional：公开端点（/auth/login/2fa）无租户上下文，入口开事务会把连接钉死在
        // 平台库，内部 runAs(租户) 不重绑 → update 落错库 0 行（事务开启前必须已处于目标世界）。
        // updateTotpSecret 为单条语句级路由自含事务语义；pending 键有 TTL，redis.delete 失败无害。
        String key = pendingKey(tenantId, userId);
        String secret = redis.opsForValue().get(key);
        if (!StringUtils.hasText(secret)) {
            throw new ServiceException(400, "绑定二维码已过期，请重新获取");
        }
        if (!TotpUtils.verify(secret, code)) {
            return false;
        }
        userService.updateTotpSecret(tenantId, userId, secret);
        redis.delete(key);
        return true;
    }

    @Override
    public boolean verifyTotp(Long tenantId, Long userId, String code) {
        SysUser user = loadUser(tenantId, userId);
        if (!StringUtils.hasText(user.getTotpSecret())) {
            return false;
        }
        return TotpUtils.verify(user.getTotpSecret(), code);
    }

    @Override
    public void unbindTotp(Long tenantId, Long userId, String code) {
        SysUser user = loadUser(tenantId, userId);
        if (!StringUtils.hasText(user.getTotpSecret()) || !TotpUtils.verify(user.getTotpSecret(), code)) {
            throw new ServiceException(400, "动态验证码错误");
        }
        clearTotp(tenantId, userId);
    }

    @Override
    public void resetTotp(Long tenantId, Long userId) {
        clearTotp(tenantId, userId);
    }

    @Override
    public void resetTenantAdminTotp(Long tenantId, String username) {
        // 按用户名定位（写入链路内的伴随读取，仅取 userId 供状态清理），再执行重置
        var user = userService.getByUsername(tenantId, username);
        if (user == null || !Boolean.TRUE.equals(user.getSuperAdmin())) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "租户管理员不存在：" + username);
        }
        userService.resetSuperAdminTotp(tenantId, username);
        clearRedisState(tenantId, user.getId());
    }

    @Override
    public boolean isEmailChannelAvailable(Long tenantId, Long userId) {
        if (mailSender == null || !mailSender.isAvailable()) {
            return false;
        }
        if (!"true".equalsIgnoreCase(effective(tenantId, LoginConfigKeys.EMAIL_OTP))) {
            return false;
        }
        SysUser user = loadUser(tenantId, userId);
        return StringUtils.hasText(user.getEmail());
    }

    @Override
    public String sendLoginEmailCode(Long tenantId, Long userId) {
        SysUser user = loadUser(tenantId, userId);
        if (mailSender == null || !mailSender.isAvailable()) {
            throw new ServiceException(500, "邮件服务未配置，邮箱验证码通道不可用");
        }
        if (!StringUtils.hasText(user.getEmail())) {
            throw new ServiceException(400, "当前账号未绑定邮箱，请联系管理员维护邮箱后使用");
        }
        String coolKey = coolKey(tenantId, userId);
        if (Boolean.TRUE.equals(redis.hasKey(coolKey))) {
            throw new ServiceException(429, "验证码发送过于频繁，请稍后再试");
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        String platform = platformName();
        try {
            mailSender.send(user.getEmail(), "【" + platform + "】登录验证码",
                    "您正在登录 " + platform + "，验证码：" + code + "，" + CODE_TTL.toMinutes() + " 分钟内有效。"
                            + "若非本人操作，请忽略本邮件并注意账号安全。");
        } catch (ServiceException ex) {
            redis.delete(codeKey(tenantId, userId));
            throw ex;
        }
        redis.opsForValue().set(codeKey(tenantId, userId), code, CODE_TTL);
        redis.opsForValue().set(coolKey, "1", COOL_TTL);
        redis.delete(attKey(tenantId, userId));
        return maskEmail(user.getEmail());
    }

    @Override
    public boolean verifyEmailCode(Long tenantId, Long userId, String code) {
        String key = codeKey(tenantId, userId);
        String expected = redis.opsForValue().get(key);
        if (!StringUtils.hasText(expected)) {
            return false;
        }
        // 校验限次：超限即作废当前验证码（攻击者无法对同一码无限尝试）
        Long attempts = redis.opsForValue().increment(attKey(tenantId, userId));
        redis.expire(attKey(tenantId, userId), CODE_TTL);
        if (attempts != null && attempts > MAX_VERIFY_ATTEMPTS) {
            redis.delete(key);
            return false;
        }
        if (!expected.equals(code == null ? null : code.trim())) {
            return false;
        }
        redis.delete(key);
        redis.delete(attKey(tenantId, userId));
        return true;
    }

    // -------------------- 内部工具 --------------------

    private SysUser loadUser(Long tenantId, Long userId) {
        SysUser user = TenantContext.runAs(normalize(tenantId), () -> userService.getById(userId));
        if (user == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "用户不存在：" + userId);
        }
        return user;
    }

    private void clearTotp(Long tenantId, Long userId) {
        userService.updateTotpSecret(tenantId, userId, null);
        clearRedisState(tenantId, userId);
    }

    private void clearRedisState(Long tenantId, Long userId) {
        redis.delete(pendingKey(tenantId, userId));
        redis.delete(codeKey(tenantId, userId));
        redis.delete(coolKey(tenantId, userId));
        redis.delete(attKey(tenantId, userId));
    }

    /**
     * 有效邮箱通道开关（与登录策略参数同一读取语义，两端独立单世界生效值）：
     * 本租户覆盖行 → 本库默认模板行（getConfigValueByKey 内回退，平台会话即平台库自身行）。
     */
    private String effective(Long tenantId, String key) {
        return TenantContext.runAs(normalize(tenantId), () -> configService.getConfigValueByKey(key));
    }

    private String platformName() {
        String name = TenantContext.runAsPlatform(
                () -> configService.getConfigValueByKey(LoginConfigKeys.PLATFORM_NAME));
        return StringUtils.hasText(name) ? name : "管理平台";
    }

    private String qrPngDataUrl(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 240, 240);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", os);
            return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (WriterException | IOException ex) {
            throw new ServiceException(500, "二维码生成失败：" + ex.getMessage());
        }
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private Long normalize(Long tenantId) {
        return tenantId == null ? MseConstants.PLATFORM_TENANT_ID : tenantId;
    }

    private String pendingKey(Long tenantId, Long userId) {
        return KEY_PENDING + normalize(tenantId) + ":" + userId;
    }

    private String codeKey(Long tenantId, Long userId) {
        return KEY_EMAIL_CODE + normalize(tenantId) + ":" + userId;
    }

    private String coolKey(Long tenantId, Long userId) {
        return KEY_EMAIL_COOL + normalize(tenantId) + ":" + userId;
    }

    private String attKey(Long tenantId, Long userId) {
        return KEY_EMAIL_ATT + normalize(tenantId) + ":" + userId;
    }
}
