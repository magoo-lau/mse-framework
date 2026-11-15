package vip.mseapp.core.license;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 授权凭证编解码（手写紧凑 JWT：header.payload.signature，RS256 = SHA256withRSA）。
 *
 * <p>零第三方依赖：三段式 base64url + JDK RSA 签名。payload 为固定字段的极简 JSON
 * （{@link LicensePayload} 各字段手写序列化/解析，字符串做 JSON 转义），仅承担
 * license 场景，不作为通用 JSON 工具使用。
 *
 * <p>密钥约定：PKCS#8 私钥 / X.509 公钥的 Base64 单行文本（配置项直接可填）。
 * 生成示例：{@code openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048}
 * → 私钥 {@code openssl pkey -in key.pem}（BEGIN PRIVATE KEY 段体），
 * 公钥 {@code openssl pkey -in key.pem -pubout}（BEGIN PUBLIC KEY 段体）。
 * Base64 单行提取私钥用 {@code openssl pkcs8 -topk8 -nocrypt -in key.pem -outform DER | base64 -w 0}——
 * 勿用 {@code pkey -outform DER}（OpenSSL 3 对 RSA 私钥 DER 透传 PKCS#1，密钥解析报 algid parse error）。
 */
public final class LicenseCodec {

    private static final String HEADER = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"; // {"alg":"RS256","typ":"JWT"}

    private LicenseCodec() {
    }

    /** 签发：载荷序列化 + 私钥签名，返回三段式 token */
    public static String issue(LicensePayload payload, String privateKeyBase64) {
        try {
            PrivateKey key = privateKey(privateKeyBase64);
            byte[] body = serialize(payload).getBytes(StandardCharsets.UTF_8);
            String signingInput = HEADER + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(body);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            String sig = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
            return signingInput + "." + sig;
        } catch (Exception e) {
            throw new IllegalStateException("授权凭证签发失败: " + e.getMessage(), e);
        }
    }

    /** 验签 + 解析：签名不符/格式非法抛 {@link LicenseException}（调用方按锁定/宽限语义处置） */
    public static LicensePayload verify(String token, String publicKeyBase64) {
        if (token == null || token.isBlank()) {
            throw new LicenseException("授权凭证为空");
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            throw new LicenseException("授权凭证格式非法");
        }
        try {
            PublicKey key = publicKey(publicKeyBase64);
            byte[] signature = Base64.getUrlDecoder().decode(parts[2]);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!verifier.verify(signature)) {
                throw new LicenseException("授权凭证签名校验失败（非平台签发或已篡改）");
            }
            byte[] body = Base64.getUrlDecoder().decode(parts[1]);
            return parse(new String(body, StandardCharsets.UTF_8));
        } catch (LicenseException e) {
            throw e;
        } catch (Exception e) {
            throw new LicenseException("授权凭证校验失败: " + e.getMessage(), e);
        }
    }

    /** 凭证无效（验签/解析失败） */
    public static class LicenseException extends RuntimeException {
        public LicenseException(String message) {
            super(message);
        }

        public LicenseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // ===== 密钥解析 =====

    /** PKCS#8 Base64 单行 → 私钥（包内复用：DeliverCodec 部署包签名同一密钥体系） */
    static PrivateKey privateKey(String base64) throws Exception {
        byte[] der = Base64.getMimeDecoder().decode(base64);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    /** X.509 Base64 单行 → 公钥（包内复用：DeliverCodec 部署包验签同一公钥） */
    static PublicKey publicKey(String base64) throws Exception {
        byte[] der = Base64.getMimeDecoder().decode(base64);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    /**
     * 从平台私钥派生验签公钥（X.509 Base64 单行）：RSA 公钥可由 CRT 私钥的模元/公指数还原——
     * 平台只配一把私钥即可同时签发凭证、签名部署包并把公钥内嵌进交付物
     * （delivery/vars.env 预填变量/部署包配置模板），租户侧无需另行索取公钥。
     */
    public static String publicKeyBase64Of(String privateKeyBase64) {
        try {
            java.security.interfaces.RSAPrivateCrtKey priv =
                    (java.security.interfaces.RSAPrivateCrtKey) privateKey(privateKeyBase64);
            PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(
                    new java.security.spec.RSAPublicKeySpec(priv.getModulus(), priv.getPublicExponent()));
            return Base64.getEncoder().encodeToString(pub.getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("从私钥派生公钥失败: " + e.getMessage(), e);
        }
    }

    // ===== 极简 JSON（字段固定，字符串转义） =====

    private static String serialize(LicensePayload p) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        field(sb, "licenseId", p.licenseId()).append(',');
        field(sb, "tenantCode", p.tenantCode()).append(',');
        field(sb, "tenantName", p.tenantName()).append(',');
        field(sb, "packageName", p.packageName()).append(',');
        sb.append("\"menuIds\":[");
        if (p.menuIds() != null) {
            for (int i = 0; i < p.menuIds().size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(p.menuIds().get(i));
            }
        }
        sb.append("],\"features\":{");
        if (p.features() != null) {
            boolean first = true;
            for (Map.Entry<String, String> entry : p.features().entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                field(sb, entry.getKey(), entry.getValue());
            }
        }
        sb.append("},\"issuedAt\":").append(p.issuedAt())
                .append(",\"expiresAt\":").append(p.expiresAt()).append('}');
        return sb.toString();
    }

    private static StringBuilder field(StringBuilder sb, String key, String value) {
        return sb.append(quote(key)).append(':').append(value == null ? "null" : quote(value));
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private static LicensePayload parse(String json) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("licenseId", stringValue(json, "licenseId"));
        values.put("tenantCode", stringValue(json, "tenantCode"));
        values.put("tenantName", stringValue(json, "tenantName"));
        values.put("packageName", stringValue(json, "packageName"));
        String menuIdsSection = section(json, "menuIds");
        List<Long> menuIds = new ArrayList<>();
        for (String token : menuIdsSection.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                menuIds.add(Long.parseLong(trimmed));
            }
        }
        Map<String, String> features = new LinkedHashMap<>();
        String featuresSection = section(json, "features");
        int p = 0;
        while (p < featuresSection.length()) {
            int keyStart = featuresSection.indexOf('"', p);
            if (keyStart < 0) {
                break;
            }
            int keyEnd = featuresSection.indexOf('"', keyStart + 1);
            int colon = featuresSection.indexOf(':', keyEnd);
            int valueStart = featuresSection.indexOf('"', colon);
            int valueEnd = valueEnd(featuresSection, valueStart);
            features.put(unescape(featuresSection.substring(keyStart + 1, keyEnd)),
                    unescape(featuresSection.substring(valueStart + 1, valueEnd)));
            p = valueEnd + 1;
        }
        long issuedAt = Long.parseLong(numberValue(json, "issuedAt"));
        long expiresAt = Long.parseLong(numberValue(json, "expiresAt"));
        return new LicensePayload(values.get("licenseId"), values.get("tenantCode"), values.get("tenantName"),
                values.get("packageName"), List.copyOf(menuIds), Map.copyOf(features), issuedAt, expiresAt);
    }

    private static String stringValue(String json, String key) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx < 0) {
            return null;
        }
        int start = json.indexOf('"', json.indexOf(':', keyIdx) + 1);
        int end = valueEnd(json, start);
        return unescape(json.substring(start + 1, end));
    }

    private static String numberValue(String json, String key) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        int start = json.indexOf(':', keyIdx) + 1;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return json.substring(start, end).trim();
    }

    /** 取顶层 key 的 [ ] 或 { } 段体 */
    private static String section(String json, String key) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx < 0) {
            return "";
        }
        int open = json.indexOf(':', keyIdx) + 1;
        while (open < json.length() && json.charAt(open) != '{' && json.charAt(open) != '[') {
            open++;
        }
        if (open >= json.length()) {
            return "";
        }
        char openChar = json.charAt(open);
        char closeChar = openChar == '{' ? '}' : ']';
        int depth = 0;
        for (int i = open; i < json.length(); i++) {
            if (json.charAt(i) == openChar) {
                depth++;
            } else if (json.charAt(i) == closeChar) {
                depth--;
                if (depth == 0) {
                    return json.substring(open + 1, i);
                }
            }
        }
        return "";
    }

    /** 字符串值结束引号（跳过转义） */
    private static int valueEnd(String json, int start) {
        int i = start + 1;
        while (i < json.length()) {
            if (json.charAt(i) == '\\') {
                i += 2;
                continue;
            }
            if (json.charAt(i) == '"') {
                return i;
            }
            i++;
        }
        return json.length() - 1;
    }

    private static String unescape(String value) {
        if (value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char next = value.charAt(++i);
            switch (next) {
                case '"' -> sb.append('"');
                case '\\' -> sb.append('\\');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    sb.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> sb.append(next);
            }
        }
        return sb.toString();
    }
}
