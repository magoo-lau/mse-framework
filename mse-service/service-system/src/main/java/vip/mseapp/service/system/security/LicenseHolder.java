package vip.mseapp.service.system.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.mseapp.core.license.LicenseCodec;
import vip.mseapp.core.license.LicensePayload;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 授权凭证持有器（standalone 私有部署模式）：加载、验签并缓存平台签发的 license，
 * 提供「有效/宽限/失效」三态判定，是租户侧权限域（菜单/权限码/角色授权）的真源载体。
 *
 * <p>token 来源二选一：{@code mse.license.token}（配置直填）或 {@code mse.license.file}
 * （文件路径，定时重扫支持续期热更新——文件替换后下一轮生效，无需重启）。
 * 验签公钥 {@code mse.license.public-key} 必配（RS256）。
 *
 * <p>状态机：验签通过且未到期 = {@code VALID}；到期但在宽限期内（
 * {@code mse.license.grace-days}，默认 15 天）= {@code GRACE}（仅管理员可登录做续期）；
 * 超宽限 = {@code EXPIRED}（锁定登录）；无凭证/验签失败 = {@code MISSING}（同锁定）。
 */
public class LicenseHolder {

    private static final Logger log = LoggerFactory.getLogger(LicenseHolder.class);

    public enum State { VALID, GRACE, EXPIRED, MISSING }

    private final String publicKey;
    private final String inlineToken;
    private final String licenseFile;
    private final String tenantCode;
    private final long graceDays;

    private final AtomicReference<LicensePayload> cached = new AtomicReference<>();
    private volatile long lastFileRead;
    private volatile String lastFileContent;

    public LicenseHolder(String publicKey, String inlineToken, String licenseFile,
                         String tenantCode, long graceDays) {
        this.publicKey = publicKey;
        this.inlineToken = inlineToken;
        this.licenseFile = licenseFile;
        this.tenantCode = tenantCode;
        this.graceDays = graceDays;
    }

    /** 当前凭证状态（按需重读文件；内存缓存验签结果，文件变更才重新验签） */
    public State state() {
        LicensePayload payload = payload();
        if (payload == null) {
            return State.MISSING;
        }
        long now = System.currentTimeMillis();
        if (payload.expiresAt() > now) {
            return State.VALID;
        }
        return payload.expiresAt() + graceDays * 24 * 3600_000L > now ? State.GRACE : State.EXPIRED;
    }

    /** 当前载荷（无效时 null；文件模式变更后自动重验） */
    public LicensePayload payload() {
        String token = inlineToken;
        if ((token == null || token.isBlank()) && licenseFile != null && !licenseFile.isBlank()) {
            token = readFileToken();
        }
        if (token == null || token.isBlank()) {
            return null;
        }
        LicensePayload cachedPayload = cached.get();
        if (cachedPayload != null && token.equals(lastFileContent)) {
            return cachedPayload;
        }
        try {
            LicensePayload payload = LicenseCodec.verify(token, publicKey);
            // 租户编码防串用：本机配置的租户必须与凭证一致
            if (tenantCode != null && !tenantCode.isBlank() && !tenantCode.equals(payload.tenantCode())) {
                log.warn("[mse-license] 凭证租户编码不匹配: 凭证={} 本机={}", payload.tenantCode(), tenantCode);
                return null;
            }
            cached.set(payload);
            lastFileContent = token;
            return payload;
        } catch (Exception e) {
            log.warn("[mse-license] 凭证校验失败: {}", e.getMessage());
            return null;
        }
    }

    /** 凭证是否允许登录（VALID 任意用户；GRACE 仅管理员——由调用方叠加 super_admin 判定） */
    public boolean loginAllowed() {
        State state = state();
        return state == State.VALID || state == State.GRACE;
    }

    /** 宽限期内提示文案（界面续期引导） */
    public String graceMessage() {
        LicensePayload payload = payload();
        if (payload == null) {
            return "授权凭证缺失或无效，请联系平台运营获取授权";
        }
        return "授权已到期（凭证 " + payload.licenseId() + "），处于宽限期，请尽快向平台续期";
    }

    private String readFileToken() {
        try {
            Path path = Path.of(licenseFile);
            if (!Files.isReadable(path)) {
                return lastFileContent;
            }
            long mtime = Files.getLastModifiedTime(path).toMillis();
            if (mtime == lastFileRead) {
                return lastFileContent;
            }
            lastFileRead = mtime;
            String content = Files.readString(path).trim();
            log.info("[mse-license] 授权凭证文件已加载: {}", path);
            return content;
        } catch (Exception e) {
            log.warn("[mse-license] 授权凭证文件读取失败: {}", e.getMessage());
            return lastFileContent;
        }
    }
}
