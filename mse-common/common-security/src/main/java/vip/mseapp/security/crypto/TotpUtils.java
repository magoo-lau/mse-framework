package vip.mseapp.security.crypto;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * TOTP 动态码工具（RFC 6238：HMAC-SHA1 / 30 秒步长 / 6 位数字；密钥 Base32 编码 RFC 4648）。
 * 与 Google Authenticator、微软验证器等标准验证器 App 兼容；纯 JDK 实现不引第三方库。
 */
public final class TotpUtils {

    /** 步长（秒），验证器 App 默认值 */
    private static final int TIME_STEP_SECONDS = 30;

    /** 允许的时钟偏移步数（±1 = 前后各一个 30s 窗口，吸收设备时钟漂移与网络传输延迟） */
    private static final int WINDOW_STEPS = 1;

    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private TotpUtils() {
    }

    /** 生成 Base32 密钥（20 字节随机 = 32 字符，验证器扫码/手输均友好） */
    public static String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** 构造 otpauth:// 绑定 URI（验证器 App 扫码内容；issuer 展示为条目所属站点名） */
    public static String buildOtpauthUri(String issuer, String account, String secret) {
        return "otpauth://totp/" + urlEncode(issuer) + ":" + urlEncode(account)
                + "?secret=" + secret + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=6&period=" + TIME_STEP_SECONDS;
    }

    /**
     * 校验动态码：当前时间 ±{@value WINDOW_STEPS} 步窗口内任一匹配即通过（恒时比较防时序侧信道）。
     * 密钥/动态码格式非法一律返回 false 不抛异常。
     */
    public static boolean verify(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null) {
            return false;
        }
        String candidate = code.trim();
        if (!candidate.matches("\\d{6}")) {
            return false;
        }
        byte[] key = base32Decode(secret.trim());
        if (key == null) {
            return false;
        }
        long counter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        for (int i = -WINDOW_STEPS; i <= WINDOW_STEPS; i++) {
            if (constantTimeEquals(candidate, hotp(key, counter + i))) {
                return true;
            }
        }
        return false;
    }

    /** RFC 4226 HOTP：HMAC-SHA1 + 动态截断取 6 位十进制 */
    private static String hotp(byte[] key, long counter) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
            return String.format("%06d", binary % 1_000_000);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("TOTP 计算失败（JVM 必支持 HmacSHA1，不应到达）", ex);
        }
    }

    /** Base32 编码（RFC 4648 标准字母表 A-Z2-7，无填充） */
    private static String base32Encode(byte[] bytes) {
        StringBuilder sb = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bits = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(BASE32_ALPHABET[(buffer >>> (bits - 5)) & 0x1F]);
                bits -= 5;
            }
        }
        if (bits > 0) {
            sb.append(BASE32_ALPHABET[(buffer << (5 - bits)) & 0x1F]);
        }
        return sb.toString();
    }

    /** Base32 解码（忽略大小写/空格/连字符）；非法字符返回 null 而非抛异常 */
    private static byte[] base32Decode(String text) {
        StringBuilder clean = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c == ' ' || c == '-') {
                continue;
            }
            char upper = Character.toUpperCase(c);
            if ((upper < 'A' || upper > 'Z') && (upper < '2' || upper > '7')) {
                return null;
            }
            clean.append(upper);
        }
        int outLength = clean.length() * 5 / 8;
        byte[] out = new byte[outLength];
        int buffer = 0;
        int bits = 0;
        int index = 0;
        for (int i = 0; i < clean.length(); i++) {
            int value = clean.charAt(i) <= '9'
                    ? clean.charAt(i) - '2' + 26
                    : clean.charAt(i) - 'A';
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8 && index < outLength) {
                out[index++] = (byte) ((buffer >>> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return out;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /** 恒时字符串比较（防时序侧信道） */
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
