package vip.mseapp.security.session;

import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpLogic;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * 会话管理公共支撑：会话标识编解码 + 按 loginId 的定向下线。
 *
 * <p><b>会话标识</b> = SHA-256(tokenValue) 前 16 位十六进制：在线会话列表对外只暴露不可逆摘要，
 * 原始 token 永不出服务端（防止凭证泄露/会话劫持）；下线时按摘要比对定位真实 token。
 *
 * <p><b>终端扩展信息键</b>：登录时经 {@code SaLoginParameter#setTerminalExtra} 写入
 * {@link SaTerminalInfo}（IP/浏览器/操作系统），会话管理页据此展示登录环境。
 */
public final class MseSessionSupport {

    public static final String EXTRA_IP = "ip";
    public static final String EXTRA_BROWSER = "browser";
    public static final String EXTRA_OS = "os";

    private MseSessionSupport() {
    }

    /** tokenValue → SHA-256 前 16 位十六进制（对外会话标识） */
    public static String sessionId(String tokenValue) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(tokenValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    /** 下线指定账号的某条会话（按会话标识比对；未命中返回 false，由调用方决定提示） */
    public static boolean kickBySessionId(StpLogic stpLogic, Object loginId, String sessionId) {
        for (String tokenValue : stpLogic.getTokenValueListByLoginId(loginId)) {
            if (sessionId(tokenValue).equals(sessionId)) {
                stpLogic.kickoutByTokenValue(tokenValue);
                return true;
            }
        }
        return false;
    }

    /** 下线指定账号除 excludeTokenValue 外的全部会话，返回实际下线条数 */
    public static int kickOthers(StpLogic stpLogic, Object loginId, String excludeTokenValue) {
        int count = 0;
        for (String tokenValue : stpLogic.getTokenValueListByLoginId(loginId)) {
            if (tokenValue.equals(excludeTokenValue)) {
                continue;
            }
            stpLogic.kickoutByTokenValue(tokenValue);
            count++;
        }
        return count;
    }

    /** 读取终端扩展信息（历史会话未记录时返回 null） */
    public static String extra(SaTerminalInfo terminal, String key) {
        Object value = terminal.getExtra(key);
        return value == null ? null : value.toString();
    }

    /** 终端设备类型兜底：历史会话/未传设备时按 pc 展示 */
    public static String deviceType(SaTerminalInfo terminal) {
        String device = terminal.getDeviceType();
        return device == null || device.isBlank() ? "pc" : device;
    }
}
