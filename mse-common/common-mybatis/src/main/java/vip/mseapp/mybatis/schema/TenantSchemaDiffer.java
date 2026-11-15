package vip.mseapp.mybatis.schema;

import vip.mseapp.mybatis.datasource.TenantDbCloner;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 结构比对同步（MySQL 先行，与克隆式初始化的方言范围一致）：主库 ↔ 租户库逐表比对
 * information_schema，自动补「加法缺漏」（缺表 / 缺列 / 缺索引）；非加法漂移
 * （类型/可空不一致、目标库多余列、疑似 SPLIT 分表）只进报告不自动修改。
 *
 * <p>定位是迁移脚本的收敛兜底：迁移管刻意的变更（含非加法 DDL 与数据回填），
 * 本类管结构对齐（如机制上线前的存量库、脚本遗漏的加列）。
 */
public class TenantSchemaDiffer {

    /** 疑似 SPLIT 分表的表名前缀（目标库存在 name_* 表时视为分表变体，不再补建逻辑表） */
    private static final Pattern SUFFIX_HINT = Pattern.compile("^.+_.+$");

    /** 比对报告：自动执行的动作 + 仅提示的漂移 */
    public record Report(int tablesCreated, int columnsAdded, int indexesAdded,
                         List<String> actions, List<String> warnings) {

        public String summary() {
            StringBuilder sb = new StringBuilder("结构比对：补表 ").append(tablesCreated())
                    .append("、补列 ").append(columnsAdded()).append("、补索引 ").append(indexesAdded());
            for (String warning : warnings()) {
                sb.append("\n! ").append(warning);
            }
            return sb.toString();
        }
    }

    /**
     * 比对并补齐加法缺漏（仅 MySQL；幂等，只增不改不删）。
     *
     * @param ignoreTables 全局表白名单（mse.tenant.ignore-tables，与克隆同一份）
     */
    public Report sync(Connection master, Connection target, Set<String> ignoreTables) throws SQLException {
        List<String> actions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> targetTables = lower(TenantDbCloner.existingTables(target));
        List<String> masterTables = TenantDbCloner.tenantTables(master, ignoreTables).stream()
                .filter(t -> !ignoreTables.contains(t.toLowerCase(Locale.ROOT)))
                .toList();

        // 1. 缺表：克隆补齐（存在 name_* 变体时视为 SPLIT 分表，跳过并提示）
        List<String> missingTables = new ArrayList<>();
        for (String table : masterTables) {
            if (targetTables.contains(table.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (hasSuffixVariant(targetTables, table)) {
                warnings.add("表 " + table + " 在目标库存在 name_* 分表变体，疑似 SPLIT 策略表，"
                        + "请走「生成分表」而非结构同步");
                continue;
            }
            missingTables.add(table);
        }
        TenantDbCloner.CloneResult clone = TenantDbCloner.cloneMissing(master, target, missingTables, Map.of());
        for (String table : missingTables) {
            actions.add("补表 " + table);
        }

        // 2/3. 缺列、缺索引；4. 漂移报告
        Map<String, ColumnMeta> masterColumns = loadColumns(master);
        Map<String, ColumnMeta> targetColumns = loadColumns(target);
        Map<String, IndexMeta> masterIndexes = loadIndexes(master);
        Set<String> targetIndexNames = loadIndexNames(target);

        Set<String> commonTables = new LinkedHashSet<>();
        masterTables.forEach(t -> {
            if (targetTables.contains(t.toLowerCase(Locale.ROOT))) {
                commonTables.add(t);
            }
        });

        for (String table : commonTables) {
            Map<String, ColumnMeta> masterCols = byTable(masterColumns, table);
            Map<String, ColumnMeta> targetCols = byTable(targetColumns, table);
            for (Map.Entry<String, ColumnMeta> entry : masterCols.entrySet()) {
                String column = entry.getKey();
                ColumnMeta meta = entry.getValue();
                ColumnMeta existing = targetCols.get(column);
                if (existing == null) {
                    String ddl = addColumnDdl(table, meta);
                    if (ddl == null) {
                        warnings.add("列 " + table + "." + column + " 含自动增/生成等需人工处理的属性，未自动补齐");
                        continue;
                    }
                    execute(target, ddl);
                    actions.add("补列 " + table + "." + column + "（" + meta.columnType() + "）");
                } else if (!meta.columnType().equalsIgnoreCase(existing.columnType())
                        || !meta.nullable().equals(existing.nullable())) {
                    warnings.add("列定义漂移 " + table + "." + column + "：主库 " + meta.columnType()
                            + (meta.nullable().equals("YES") ? " NULL" : " NOT NULL") + " / 目标库 "
                            + existing.columnType() + (existing.nullable().equals("YES") ? " NULL" : " NOT NULL")
                            + "，仅提示不自动修改");
                }
            }
            for (String column : targetCols.keySet()) {
                if (!masterCols.containsKey(column)) {
                    warnings.add("目标库多余列 " + table + "." + column + "（主库无此列），仅提示不自动删除");
                }
            }
        }

        for (Map.Entry<String, IndexMeta> entry : masterIndexes.entrySet()) {
            String key = entry.getKey();
            IndexMeta meta = entry.getValue();
            if (!commonTables.contains(meta.table()) || targetIndexNames.contains(key.toLowerCase(Locale.ROOT))) {
                continue;
            }
            String ddl = meta.toDdl();
            if (ddl == null) {
                warnings.add("索引 " + key + "（" + meta.indexType() + "）暂不支持自动补齐，请人工处理");
                continue;
            }
            execute(target, ddl);
            actions.add("补索引 " + key);
        }

        return new Report(clone.created(), countPrefix(actions, "补列"), countPrefix(actions, "补索引"),
                List.copyOf(actions), List.copyOf(warnings));
    }

    // ===== 元数据读取（MySQL information_schema）=====

    private record ColumnMeta(String table, String column, String columnType, String nullable,
                              String columnDefault, String extra, String comment) {
    }

    private record IndexMeta(String table, String name, boolean unique, String indexType, List<String> columns) {

        /** FULLTEXT/SPATIAL 语法差异大，返回 null 交人工 */
        private String toDdl() {
            if (!"BTREE".equalsIgnoreCase(indexType)) {
                return null;
            }
            return "ALTER TABLE `" + table + "` ADD " + (unique ? "UNIQUE " : "") + "INDEX `" + name + "` ("
                    + String.join(", ", columns.stream().map(c -> "`" + c + "`").toList()) + ")";
        }
    }

    private Map<String, ColumnMeta> loadColumns(Connection conn) throws SQLException {
        Map<String, ColumnMeta> map = new LinkedHashMap<>();
        String sql = "SELECT table_name, column_name, column_type, is_nullable, column_default, extra, column_comment"
                + " FROM information_schema.columns WHERE table_schema = DATABASE()";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ColumnMeta meta = new ColumnMeta(rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7));
                map.put(key(meta.table(), meta.column()), meta);
            }
        }
        return map;
    }

    private Map<String, IndexMeta> loadIndexes(Connection conn) throws SQLException {
        Map<String, IndexMeta> map = new LinkedHashMap<>();
        String sql = "SELECT table_name, index_name, non_unique, index_type, column_name, seq_in_index"
                + " FROM information_schema.statistics WHERE table_schema = DATABASE() AND index_name <> 'PRIMARY'"
                + " ORDER BY table_name, index_name, seq_in_index";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String table = rs.getString(1);
                String name = rs.getString(2);
                IndexMeta meta = map.get(key(table, name));
                if (meta == null) {
                    meta = new IndexMeta(table, name, rs.getInt(3) == 0, rs.getString(4), new ArrayList<>());
                    map.put(key(table, name), meta);
                }
                meta.columns().add(rs.getString(5));
            }
        }
        return map;
    }

    private Set<String> loadIndexNames(Connection conn) throws SQLException {
        Set<String> names = new LinkedHashSet<>();
        String sql = "SELECT DISTINCT table_name, index_name FROM information_schema.statistics"
                + " WHERE table_schema = DATABASE()";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                names.add(key(rs.getString(1), rs.getString(2)).toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    // ===== DDL 生成（只增不改）=====

    /** 返回 null = 含需人工处理的属性（AUTO_INCREMENT / 生成列 / 表达式默认值） */
    private String addColumnDdl(String table, ColumnMeta meta) {
        String extra = meta.extra() == null ? "" : meta.extra().toUpperCase(Locale.ROOT);
        if (extra.contains("AUTO_INCREMENT") || extra.contains("GENERATED") || extra.contains("VIRTUAL")
                || extra.contains("STORED")) {
            return null;
        }
        StringBuilder ddl = new StringBuilder("ALTER TABLE `").append(TenantDbCloner.identifier(table, "表名"))
                .append("` ADD COLUMN `").append(TenantDbCloner.identifier(meta.column(), "列名")).append("` ")
                .append(meta.columnType());
        if ("NO".equalsIgnoreCase(meta.nullable())) {
            ddl.append(" NOT NULL");
        }
        String rendered = renderDefault(meta.columnDefault());
        if (rendered == null && meta.columnDefault() != null) {
            return null;
        }
        if (rendered != null) {
            ddl.append(" DEFAULT ").append(rendered);
        }
        if (extra.contains("ON UPDATE CURRENT_TIMESTAMP")) {
            ddl.append(" ON UPDATE CURRENT_TIMESTAMP");
        }
        if (meta.comment() != null && !meta.comment().isBlank()) {
            ddl.append(" COMMENT '").append(meta.comment().replace("'", "''")).append("'");
        }
        return ddl.toString();
    }

    /** 字面量/关键字智能渲染；返回 null 表示表达式默认值等需人工场景（columnDefault 非空但渲染不了） */
    private String renderDefault(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        String upper = value.toUpperCase(Locale.ROOT);
        if (upper.equals("CURRENT_TIMESTAMP") || upper.equals("NOW()")
                || upper.equals("NULL") || upper.matches("-?\\d+(\\.\\d+)?")) {
            return value;
        }
        if (value.startsWith("(")) {
            return null;
        }
        if (containsControl(value)) {
            return null;
        }
        return "'" + value.replace("'", "''") + "'";
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c)) {
                return true;
            }
        }
        return false;
    }

    // ===== 内部 =====

    private static void execute(Connection target, String ddl) throws SQLException {
        try (Statement stmt = target.createStatement()) {
            stmt.execute(ddl);
        }
    }

    private static String key(String table, String column) {
        return table + "." + column;
    }

    private static Map<String, ColumnMeta> byTable(Map<String, ColumnMeta> columns, String table) {
        Map<String, ColumnMeta> out = new LinkedHashMap<>();
        String prefix = table + ".";
        columns.forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                out.put(k.substring(prefix.length()), v);
            }
        });
        return out;
    }

    /** 目标库存在 name_1 / name_a1 之类后缀变体（保守：至少多一段下划线） */
    private static boolean hasSuffixVariant(Set<String> targetTables, String table) {
        for (String candidate : targetTables) {
            if (candidate.startsWith(table.toLowerCase(Locale.ROOT) + "_")
                    && SUFFIX_HINT.matcher(candidate).matches()) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> lower(Set<String> tables) {
        Set<String> out = new LinkedHashSet<>();
        tables.forEach(t -> out.add(t.toLowerCase(Locale.ROOT)));
        return out;
    }

    private static int countPrefix(List<String> actions, String prefix) {
        return (int) actions.stream().filter(a -> a.startsWith(prefix)).count();
    }
}
