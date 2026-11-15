package vip.mseapp.mybatis.schema;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 脚本占位符替换（Flyway 式）：{@code ${key}} 替换为占位符值；{@code $${key}} 转义为字面量
 * {@code ${key}}；未提供值的占位符 fail-fast 抛错（避免未替换模板被执行进库）。
 *
 * <p>checksum 按模板原文计算（不随租户/占位符变化），同一脚本多租户执行共用一条 checksum。
 */
public final class SchemaPlaceholders {

    /** 内置占位符：租户编号（结构/平台数据通道=0，每租户通道=实际租户） */
    public static final String TENANT_ID = "tenant_id";

    private static final Pattern PATTERN = Pattern.compile("\\$\\$\\{([A-Za-z0-9_.-]+)}|\\$\\{([A-Za-z0-9_.-]+)}");

    private SchemaPlaceholders() {
    }

    /** 模板替换；无占位符时原文返回 */
    public static String resolve(String sql, Map<String, String> placeholders) {
        if (sql == null || !sql.contains("${")) {
            return sql;
        }
        Matcher matcher = PATTERN.matcher(sql);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String escaped = matcher.group(1);
            if (escaped != null) {
                matcher.appendReplacement(out, Matcher.quoteReplacement("${" + escaped + "}"));
                continue;
            }
            String key = matcher.group(2);
            String value = placeholders.get(key);
            if (value == null) {
                throw new IllegalStateException("占位符未提供值: ${" + key + "}（mse.tenant.schema.placeholders 可配置）");
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
