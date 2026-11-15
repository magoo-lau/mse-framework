package vip.mseapp.service.platform.service.deliver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.mseapp.mybatis.datasource.TenantDbCloner;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 租户数据导出器（离线交付）：JDBC 元数据驱动，逐表流式写出 JSONL——
 * 首行 schema 头（列名/JDBC 类型/主键/类别），此后每行一个 JSON 对象；二进制列 Base64，
 * 数值列保数值，时间列按驱动文本渲染，格式与方言无关（导入端按 schema 头还原）。
 *
 * <p>表清单与 {@code TenantDbCloner.tenantTables} 同口径：排除全局表/账本/快照等
 * 非「租户数据」表；独立库=整库用户表，共享库=含 tenant_id 列的表按租户过滤。
 */
public class TenantDataExporter {

    private static final Logger log = LoggerFactory.getLogger(TenantDataExporter.class);

    /** 导出强制排除表（小写）：基座全局表副本/控制面缓存/账本/回滚日志——均随 jar 或由导入端自管 */
    private static final Set<String> EXCLUDED_TABLES = Set.of(
            "undo_log", "sys_tenant", "sys_tenant_snapshot", "sys_domain_config",
            "sys_menu", "sys_permission", "sys_micro_app",
            "sys_home_template", "sys_home_user_setting",
            "deliver_import_record");

    /** JSONL schema 头列描述 */
    public record ColumnMeta(String name, int jdbcType, boolean primaryKey, boolean numeric, boolean binary) {
    }

    /** 表 schema（JSONL 首行） */
    public record TableSchema(String table, List<ColumnMeta> columns) {
    }

    /** Jackson 2：JSONL 导出行（时间/二进制列已在 value() 归一为字符串，无 java.time 进 mapper），版本由 Boot 4 的 jackson-2-bom 托管；
     *  与导入端 DeliverImportService 同轨，如切 Jackson 3 需两端成对切并核对行字节兼容 */
    private final ObjectMapper mapper = new ObjectMapper();

    private final long maxTableRows;

    public TenantDataExporter(long maxTableRows) {
        this.maxTableRows = maxTableRows;
    }

    /**
     * 目标库可导出表清单（字典序）。shared=true 时仅含 tenant_id 列的表（行级隔离导出的前提），
     * false（独立库）为整库用户表；两模式均排除全局表/账本/控制面缓存（EXCLUDED_TABLES +
     * ignoreTables + mse_%_schema_history 账本 + Flowable 引擎表）。
     */
    public List<String> exportTables(Connection conn, boolean shared, Set<String> ignoreTables) throws SQLException {
        String tenantFilter = shared ? " AND c.column_name = 'tenant_id'" : "";
        String sql = "SELECT DISTINCT t.table_name FROM information_schema.tables t"
                + (shared ? " JOIN information_schema.columns c ON c.table_schema = t.table_schema"
                + " AND c.table_name = t.table_name" : "")
                + " WHERE t.table_schema = DATABASE() AND t.table_type = 'BASE TABLE'" + tenantFilter;
        List<String> tables = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String table = rs.getString(1);
                String lower = table.toLowerCase(Locale.ROOT);
                if (lower.startsWith("mse_") && lower.endsWith("_schema_history")) {
                    continue;
                }
                if (lower.startsWith("act_") || lower.startsWith("flw_")) {
                    continue;
                }
                if (EXCLUDED_TABLES.contains(lower) || ignoreTables.contains(lower)) {
                    continue;
                }
                tables.add(TenantDbCloner.identifier(table, "表名"));
            }
        }
        tables.sort(String::compareTo);
        return tables;
    }

    /** 表数据行数（shared 按 tenant_id 过滤） */
    public long countRows(Connection conn, String table, boolean shared, long tenantId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `" + table + "`" + (shared ? " WHERE tenant_id = ?" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (shared) {
                ps.setLong(1, tenantId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * 逐表流式导出 JSONL 到输出流（MySQL fetch 流式，不缓冲整表），返回行数。
     * 首行 schema 头，数据行按主键排序（无主键表按自然序）；行数超上限抛出终止导出。
     */
    public long writeTableJsonl(Connection conn, String table, boolean shared, long tenantId,
                                OutputStream out) throws SQLException, IOException {
        TableSchema schema = readSchema(conn, table);
        StringBuilder orderBy = new StringBuilder();
        schema.columns().stream().filter(ColumnMeta::primaryKey).findFirst().ifPresent(
                pk -> orderBy.append(" ORDER BY `").append(pk.name()).append("`"));
        String sql = "SELECT * FROM `" + table + "`" + (shared ? " WHERE tenant_id = ?" : "") + orderBy;
        long rows = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql,
                ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ps.setFetchSize(Integer.MIN_VALUE);
            if (shared) {
                ps.setLong(1, tenantId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
                writer.write(mapper.writeValueAsString(schemaMap(schema)));
                writer.write("\n");
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    writer.write(mapper.writeValueAsString(rowMap(meta, rs)));
                    writer.write("\n");
                    rows++;
                    if (maxTableRows > 0 && rows > maxTableRows) {
                        throw new SQLException("表 " + table + " 行数超过单表上限 " + maxTableRows
                                + "（mse.deliver.export.max-table-rows），请收窄导出范围或调整上限");
                    }
                }
                writer.flush();
            }
        }
        log.info("[mse-deliver] 表 {} 导出 {} 行", table, rows);
        return rows;
    }

    /** schema 头 Map（JSONL 首行） */
    private Map<String, Object> schemaMap(TableSchema schema) {
        Map<String, Object> head = new LinkedHashMap<>();
        head.put("table", schema.table());
        List<Map<String, Object>> columns = new ArrayList<>();
        for (ColumnMeta column : schema.columns()) {
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("name", column.name());
            col.put("jdbcType", column.jdbcType());
            col.put("pk", column.primaryKey());
            col.put("numeric", column.numeric());
            col.put("binary", column.binary());
            columns.add(col);
        }
        head.put("columns", columns);
        return head;
    }

    /** 行 → JSON 对象（LinkedHashMap 保持列序） */
    private Map<String, Object> rowMap(ResultSetMetaData meta, ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            row.put(meta.getColumnLabel(i), value(meta.getColumnType(i), rs, i));
        }
        return row;
    }

    /** 按列类别取值：数值/布尔保原生，二进制 Base64，其余驱动文本（时间/字符串），NULL → null */
    private Object value(int jdbcType, ResultSet rs, int index) throws SQLException {
        Object raw = rs.getObject(index);
        if (raw == null) {
            return null;
        }
        if (isBinary(jdbcType)) {
            return Base64.getEncoder().encodeToString(rs.getBytes(index));
        }
        if (isNumeric(jdbcType)) {
            if (raw instanceof Number || raw instanceof Boolean) {
                return raw;
            }
            return new java.math.BigDecimal(raw.toString());
        }
        return rs.getString(index);
    }

    /** 读取表 schema（列名/JDBC 类型/主键/类别） */
    public TableSchema readSchema(Connection conn, String table) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        Set<String> primaryKeys = new LinkedHashSet<>();
        try (ResultSet rs = meta.getPrimaryKeys(conn.getCatalog(), null, table)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }
        List<ColumnMeta> columns = new ArrayList<>();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, null)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                int type = rs.getInt("DATA_TYPE");
                columns.add(new ColumnMeta(name, type, primaryKeys.contains(name),
                        isNumeric(type), isBinary(type)));
            }
        }
        if (columns.isEmpty()) {
            throw new SQLException("读取表结构为空: " + table);
        }
        return new TableSchema(table, List.copyOf(columns));
    }

    /**
     * 各账本（mse_&lt;biz&gt;_schema_history）结构通道当前版本：biz → maxVersion。
     * 按目标库实际账本表发现（不依赖本端 classpath——微服务形态导出端只见基座积木，
     * 业务积木账本同样要进 requires 基线），无账本的 biz 不在列。
     */
    public Map<String, String> ledgerVersions(Connection conn) throws SQLException {
        Map<String, String> out = new LinkedHashMap<>();
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()"
                + " AND table_name LIKE 'mse\\_%\\_schema\\_history'";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String tableName = rs.getString(1).toLowerCase(Locale.ROOT);
                String biz = tableName.substring("mse_".length(), tableName.length() - "_schema_history".length());
                if (biz.isBlank() || "adhoc".equals(biz)) {
                    continue;
                }
                String version = maxLedgerVersion(conn, tableName);
                if (version != null) {
                    out.put(biz, version);
                }
            }
        }
        return out;
    }

    private String maxLedgerVersion(Connection conn, String ledgerTable) throws SQLException {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(
                "SELECT MAX(version) FROM " + ledgerTable + " WHERE success = 1 AND tenant_id = 0")) {
            rs.next();
            return rs.getString(1);
        }
    }

    static boolean isNumeric(int jdbcType) {
        return switch (jdbcType) {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT,
                 Types.REAL, Types.FLOAT, Types.DOUBLE, Types.DECIMAL, Types.NUMERIC,
                 Types.BIT, Types.BOOLEAN -> true;
            default -> false;
        };
    }

    static boolean isBinary(int jdbcType) {
        return switch (jdbcType) {
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> true;
            default -> false;
        };
    }

    /** SHA-256（hex 小写） */
    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
