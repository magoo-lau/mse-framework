package vip.mseapp.mybatis.datasource;

import vip.mseapp.mybatis.config.MseTenantProperties;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 租户库表结构克隆工具（MySQL）。
 *
 * <p>来源表识别：源库中含 tenant_id 列的 BASE TABLE（排除全局表白名单）+ undo_log；
 * 仅克隆结构不拷数据；renames 支持克隆同时改表名（分表后缀场景，如 order → order_t001）。
 * 幂等：目标库已存在的表跳过。
 */
public final class TenantDbCloner {

    /** SQL 标识符（库名/表名）白名单：拼接 DDL 前防御性校验 */
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z0-9_$]+$");

    public record CloneResult(int created, int existed, List<String> tables) {
    }

    private TenantDbCloner() {
    }

    /** 源库中含 tenant_id 列的 BASE TABLE（租户业务表），排除全局表白名单，附 undo_log */
    public static List<String> tenantTables(Connection source, Set<String> ignoreTables) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT DISTINCT t.table_name FROM information_schema.tables t"
                + " JOIN information_schema.columns c ON c.table_schema = t.table_schema AND c.table_name = t.table_name"
                + " WHERE t.table_schema = DATABASE() AND t.table_type = 'BASE TABLE' AND c.column_name = 'tenant_id'";
        try (PreparedStatement ps = source.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String table = rs.getString(1);
                if (!ignoreTables.contains(table.toLowerCase(Locale.ROOT))) {
                    tables.add(identifier(table, "表名"));
                }
            }
        }
        if (!tables.contains("undo_log") && tableExists(source, "undo_log")) {
            tables.add("undo_log");
        }
        return tables;
    }

    /** 全局表白名单（小写集合） */
    public static Set<String> ignoreTableNames(MseTenantProperties properties) {
        return properties.getIgnoreTables().stream()
                .map(t -> t.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    /** 目标库已有表名集合（小写） */
    public static Set<String> existingTables(Connection target) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (PreparedStatement ps = target.prepareStatement(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tables.add(rs.getString(1).toLowerCase(Locale.ROOT));
            }
        }
        return tables;
    }

    /**
     * 克隆缺失的表结构（幂等）。
     *
     * @param renames 表名改写（源逻辑表 → 目标物理表，如 order → order_t001）；无改写传空 Map
     */
    public static CloneResult cloneMissing(Connection source, Connection target, List<String> tables,
                                           Map<String, String> renames) throws SQLException {
        Map<String, String> renameMap = renames == null ? Map.of() : renames;
        Set<String> existing = existingTables(target);
        int created = 0;
        int existed = 0;
        for (String table : tables) {
            String targetTable = renameMap.getOrDefault(table, table);
            String targetIdentifier = identifier(targetTable, "目标表名");
            if (existing.contains(targetIdentifier.toLowerCase(Locale.ROOT))) {
                existed++;
                continue;
            }
            String ddl = showCreateTable(source, table);
            // 整体替换 "CREATE TABLE `源表`" 前缀（兼容改名场景），保留列定义等其余部分
            String rewritten = ddl.replaceFirst("(?i)CREATE TABLE `[^`]+`",
                    "CREATE TABLE IF NOT EXISTS `" + targetIdentifier + "`");
            try (Statement targetStmt = target.createStatement()) {
                targetStmt.execute(rewritten);
                created++;
            }
        }
        return new CloneResult(created, existed, tables);
    }

    /** 分表后缀表名（逻辑表 → 逻辑表+后缀） */
    public static Map<String, String> suffixRenames(List<String> tables, String suffix) {
        Map<String, String> renames = new LinkedHashMap<>();
        if (suffix == null || suffix.isBlank()) {
            return renames;
        }
        for (String table : tables) {
            renames.put(table, table + suffix);
        }
        return renames;
    }

    public static boolean tableExists(Connection source, String table) throws SQLException {
        try (PreparedStatement ps = source.prepareStatement(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?")) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public static String showCreateTable(Connection source, String table) throws SQLException {
        try (Statement stmt = source.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
            if (!rs.next()) {
                throw new SQLException("SHOW CREATE TABLE 无结果: " + table);
            }
            return rs.getString(2);
        }
    }

    public static String identifier(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (!IDENTIFIER.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(field + "非法（仅允许字母/数字/下划线/$）：" + value);
        }
        return trimmed;
    }
}
