package vip.mseapp.mybatis.schema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结构迁移历史表（mse_&lt;biz&gt;_schema_history）JDBC 访问：建表（三方言）+ 已应用版本读取 + 流水写入。
 *
 * <p>每个业务标识（biz）在每个参与演进的库各持一张账本——业务主库记 master 通道、基座库记 base
 * 通道、租户库记克隆通道，同名表行集互不重叠；tenant_id 维度区分结构通道（0）与每租户通道
 * （实际租户）。仅经本类原生 JDBC 访问，不映射实体、不参与租户插件改写（白名单按
 * mse_%_schema_history 通配豁免）；建表时幂等补齐 tenant_id 索引（tenant 维度查询免全表扫描）。
 */
public class TenantSchemaHistoryDao {

    private final String table;

    /** @param biz 服务标识（[a-z][a-z0-9]*，由文件名正则保证；此处防御性再校验防拼接注入） */
    public TenantSchemaHistoryDao(String biz) {
        if (biz == null || !biz.matches("[a-z][a-z0-9]*")) {
            throw new IllegalArgumentException("非法账本 biz 标识: " + biz);
        }
        this.table = "mse_" + biz + "_schema_history";
    }

    public String table() {
        return table;
    }

    /** 单条历史流水（取每版本最新一条） */
    public record HistoryRow(String version, String description, String type, String script,
                             String checksum, boolean success, int executionMs, String errorMsg) {
    }

    /** 确保历史表存在（先探针后建表；并发建表竞态由 already-exists 容错收敛），并幂等补齐 tenant_id 索引 */
    public void ensureTable(Connection conn, String dbType) throws SQLException {
        if (!tableExists(conn)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableDdl(dbType));
            } catch (SQLException e) {
                String message = e.getMessage();
                if (message == null || !(message.contains("already exists") || message.contains("已存在"))) {
                    throw e;
                }
            }
        }
        ensureIndex(conn);
    }

    /**
     * 幂等补齐 tenant_id 索引：账本高频查询 {@code WHERE tenant_id = ? ORDER BY id}（每租户通道
     * 逐租户读取），无索引时全表扫描随流水增长线性变慢。新建表与存量表统一走本方法
     * （CREATE INDEX 无 IF NOT EXISTS 语法的三方言通用幂等：以「索引已存在」错误吞掉实现；
     * mysql=Duplicate key name 1061 / dm、kingbase=already exists 系错误信息）。
     */
    private void ensureIndex(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX " + table + "_tenant_idx ON " + table + " (tenant_id)");
        } catch (SQLException e) {
            String message = e.getMessage();
            if (message != null && (message.contains("already exists") || message.contains("已存在")
                    || message.toLowerCase(java.util.Locale.ROOT).contains("duplicate key name"))) {
                return;
            }
            throw e;
        }
    }

    /** 历史表是否存在（直接探针：任何查询错误视为不存在，真实问题由建表/后续语句暴露） */
    public boolean tableExists(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1 FROM " + table + " WHERE 1 = 0");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 指定租户通道的每脚本最新一条流水（同脚本多行时后写覆盖），key=script。
     * 结构通道传 0，每租户通道传实际租户；BASELINE/ADHOC 类流水 script 为固定标记。
     */
    public Map<String, HistoryRow> appliedRows(Connection conn, long tenantId) throws SQLException {
        Map<String, HistoryRow> rows = new LinkedHashMap<>();
        String sql = "SELECT version, description, type, script, checksum, success, execution_ms, error_msg"
                + " FROM " + table + " WHERE tenant_id = ? ORDER BY id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.put(rs.getString(4), new HistoryRow(rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getInt(6) == 1, rs.getInt(7), rs.getString(8)));
                }
            }
        }
        return rows;
    }

    /** 结构通道当前生效版本 = 已成功应用的最大版本（含基线；仅统计 tenant_id=0 行） */
    public String maxVersion(Connection conn) throws SQLException {
        String sql = "SELECT MAX(version) FROM " + table + " WHERE success = 1 AND tenant_id = 0";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    /** 结构通道最近一条仍处于失败状态的流水（此后重试成功则不算失败），无则返回 null */
    public HistoryRow lastFailure(Connection conn) throws SQLException {
        String sql = "SELECT version, description, type, script, checksum, success, execution_ms, error_msg"
                + " FROM " + table + " h WHERE h.tenant_id = 0 AND h.success = 0 AND NOT EXISTS ("
                + "SELECT 1 FROM " + table + " h2 WHERE h2.version = h.version AND h2.tenant_id = 0 AND h2.success = 1)"
                + " ORDER BY h.id DESC";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? new HistoryRow(rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), false, rs.getInt(7), rs.getString(8)) : null;
        }
    }

    public void insert(Connection conn, HistoryRow row, String operator, long tenantId) throws SQLException {
        String sql = "INSERT INTO " + table
                + " (tenant_id, version, description, type, script, checksum, installed_by, installed_on, success, execution_ms, error_msg)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tenantId);
            ps.setString(2, row.version());
            ps.setString(3, truncate(row.description(), 200));
            ps.setString(4, row.type());
            ps.setString(5, truncate(row.script(), 500));
            ps.setString(6, row.checksum());
            ps.setString(7, truncate(operator, 64));
            ps.setTimestamp(8, new Timestamp(System.currentTimeMillis()));
            ps.setInt(9, row.success() ? 1 : 0);
            ps.setInt(10, row.executionMs());
            ps.setString(11, truncate(row.errorMsg(), 1000));
            ps.executeUpdate();
        }
    }

    /**
     * 删除结构通道（tenant_id=0）的 BASELINE 流水：误判基线自愈用——账本基线声称「存量结构已包含」
     * 而库内连该 biz 自身的建表都没有时，删除记账让清单按全新库补齐（幂等脚本双保险）。
     */
    public void deleteBaseline(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM " + table
                    + " WHERE tenant_id = 0 AND type = '" + SchemaMigration.TYPE_BASELINE + "'");
        }
    }

    private String createTableDdl(String dbType) {
        return switch (normalize(dbType)) {
            case "dm" -> "CREATE TABLE " + table + " ("
                    + "id BIGINT IDENTITY(1,1) NOT NULL, "
                    + "tenant_id BIGINT DEFAULT 0 NOT NULL, "
                    + "version VARCHAR(64), description VARCHAR(200), type VARCHAR(20), script VARCHAR(500), "
                    + "checksum VARCHAR(64), installed_by VARCHAR(64), installed_on TIMESTAMP NOT NULL, "
                    + "success INT DEFAULT 1 NOT NULL, execution_ms INT DEFAULT 0 NOT NULL, error_msg VARCHAR(1000), "
                    + "PRIMARY KEY (id))";
            case "kingbase" -> "CREATE TABLE " + table + " ("
                    + "id BIGSERIAL PRIMARY KEY, "
                    + "tenant_id BIGINT DEFAULT 0 NOT NULL, "
                    + "version VARCHAR(64), description VARCHAR(200), type VARCHAR(20), script VARCHAR(500), "
                    + "checksum VARCHAR(64), installed_by VARCHAR(64), installed_on TIMESTAMP NOT NULL, "
                    + "success INT DEFAULT 1 NOT NULL, execution_ms INT DEFAULT 0 NOT NULL, error_msg VARCHAR(1000))";
            default -> "CREATE TABLE " + table + " ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT, "
                    + "tenant_id BIGINT NOT NULL DEFAULT 0, "
                    + "version VARCHAR(64), description VARCHAR(200), type VARCHAR(20), script VARCHAR(500), "
                    + "checksum VARCHAR(64), installed_by VARCHAR(64), installed_on datetime NOT NULL, "
                    + "success int NOT NULL DEFAULT 1, execution_ms int NOT NULL DEFAULT 0, error_msg VARCHAR(1000), "
                    + "PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        };
    }

    private static String normalize(String dbType) {
        return dbType == null ? "mysql" : dbType.trim().toLowerCase();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
