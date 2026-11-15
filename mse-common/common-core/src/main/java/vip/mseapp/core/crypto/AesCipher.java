package vip.mseapp.core.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 对称加解密工具（ reversible 场景：租户数据源连接密码等落库密文）。
 *
 * <p>密文格式：{@code Base64(IV(12B) || GCM 密文(含 128b 认证标签))}。
 * 密钥由使用方经配置下发（如 {@code mse.tenant.dynamic-ds.encrypt-key}），
 * 支持任意长度口令（SHA-256 派生 128b 密钥）或标准 Base64 的 16/24/32 字节原始密钥。
 */
public final class AesCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private AesCipher() {
    }

    /** 由配置口令派生密钥：标准 Base64 的 16/24/32 字节按原始密钥解析，其余按口令 SHA-256 派生 */
    public static SecretKey parseKey(String configured) {
        if (configured == null || configured.isBlank()) {
            throw new IllegalArgumentException("AES 密钥不能为空");
        }
        try {
            byte[] raw = Base64.getDecoder().decode(configured.trim());
            if (raw.length == 16 || raw.length == 24 || raw.length == 32) {
                return new SecretKeySpec(raw, "AES");
            }
        } catch (IllegalArgumentException ignored) {
            // 非 Base64，按口令派生
        }
        return derive(configured.trim());
    }

    /** 口令 → 128b 密钥（SHA-256 派生） */
    public static SecretKey derive(String passphrase) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(passphrase.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("AES 密钥派生失败", e);
        }
    }

    /** 加密：返回 Base64(IV || GCM 密文) */
    public static String encrypt(String plaintext, SecretKey key) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("AES 加密失败", e);
        }
    }

    /** 解密 {@link #encrypt} 的密文；格式/密钥不符抛 IllegalStateException */
    public static String decrypt(String ciphertext, SecretKey key) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return null;
        }
        try {
            byte[] all = Base64.getDecoder().decode(ciphertext.trim());
            if (all.length <= IV_LENGTH) {
                throw new IllegalArgumentException("密文长度非法");
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_LENGTH));
            return new String(cipher.doFinal(all, IV_LENGTH, all.length - IV_LENGTH), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES 解密失败（密钥不匹配或密文损坏）", e);
        }
    }
}
