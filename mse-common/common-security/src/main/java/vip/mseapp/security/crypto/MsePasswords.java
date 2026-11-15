package vip.mseapp.security.crypto;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 口令散列工具（BCrypt，仅引入 spring-security-crypto）。
 */
public final class MsePasswords {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private MsePasswords() {
    }

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return rawPassword != null && encodedPassword != null && ENCODER.matches(rawPassword, encodedPassword);
    }
}
