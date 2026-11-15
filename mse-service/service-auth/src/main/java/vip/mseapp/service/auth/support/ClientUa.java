package vip.mseapp.service.auth.support;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端信息解析：IP（转发头优先）+ UA 关键词归类浏览器/操作系统。
 * 登录日志投递与登录会话终端扩展信息共用，规则保持够用即可。
 */
public final class ClientUa {

    private ClientUa() {
    }

    public static String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int idx = forwarded.indexOf(',');
            return idx > 0 ? forwarded.substring(0, idx).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public static String resolveBrowser(String ua) {
        if (ua == null || ua.isBlank()) {
            return "Unknown";
        }
        if (ua.contains("Edg/")) {
            return "Edge";
        }
        if (ua.contains("Chrome/") && !ua.contains("Chromium")) {
            return "Chrome";
        }
        if (ua.contains("Chromium")) {
            return "Chromium";
        }
        if (ua.contains("Firefox/")) {
            return "Firefox";
        }
        if (ua.contains("Safari/") && ua.contains("Version/")) {
            return "Safari";
        }
        if (ua.contains("MSIE") || ua.contains("Trident/")) {
            return "IE";
        }
        return "Other";
    }

    public static String resolveOs(String ua) {
        if (ua == null || ua.isBlank()) {
            return "Unknown";
        }
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("iPhone") || ua.contains("iPad")) {
            return "iOS";
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("Mac OS")) {
            return "macOS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return "Other";
    }
}
