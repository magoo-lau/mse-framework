package vip.mseapp.mybatis.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 迁移预演（{@link TenantSchemaMigrator#plan}）单测（H2 MySQL 模式）：只读性（不建账本不执行语句）、
 * 与 migrate 同口径的过滤/排序/基线判定（fresh 库全量清单、已应用排除、checksum 冲突检出、
 * 存量库自动基线跳过）。启动预演日志与管理端 schema-plan 端点都建立在该计算上。
 */
class TenantSchemaMigratorPlanTest {

    // 库名必须为 public：存量探针以 table_schema = DATABASE() 判定（H2 表落 public schema，
    // 两者同名探测才生效，见 TenantSchemaMigratorTest 同款注释；surefire 串行执行 + 各自 setUp 全清）
    private static final String URL = "jdbc:h2:mem:public;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private TenantSchemaMigrator migrator;

    @BeforeEach
    void setUp() throws SQLException {
        try (Conn c = new Conn()) {
            exec(c.conn, "DROP ALL OBJECTS");
        }
        migrator = new TenantSchemaMigrator(new TenantMigrationScanner(), "junit");
    }

    private static SchemaMigration script(String biz, String version, String sql) {
        return new SchemaMigration(biz, version, "测试脚本 " + version, SchemaMigration.TYPE_CLASSPATH,
                MigrationTarget.MASTER, false, "test/" + version, sql, TenantMigrationScanner.checksum(sql));
    }

    @Test
    void freshDbPlanIsReadOnlyAndListsAllInOrder() throws SQLException {
        try (Conn c = new Conn()) {
            List<SchemaMigration> migrations = List.of(
                    script("demo", "20990101_01", "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))"),
                    script("demo", "20990101_02", "ALTER TABLE t1 ADD COLUMN name VARCHAR(50)"));
            TenantSchemaMigrator.Plan plan = migrator.plan(c.conn, "mysql", migrations,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(2, plan.pending().size());
            assertEquals("20990101_01", plan.pending().get(0).version());
            assertEquals("20990101_02", plan.pending().get(1).version());
            assertTrue(plan.notes().stream().anyMatch(n -> n.contains("账本未建")));
            assertTrue(plan.conflicts().isEmpty());
            // 只读性：账本未建、目标表未建、无任何 DDL 落库
            assertEquals(0, count(c.conn, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()"));
        }
    }

    @Test
    void appliedScriptsExcludedAndModifiedScriptReportedAsConflict() throws SQLException {
        try (Conn c = new Conn()) {
            List<SchemaMigration> migrations = List.of(
                    script("demo", "20990101_01", "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))"),
                    script("demo", "20990101_02", "ALTER TABLE t1 ADD COLUMN name VARCHAR(50)"));
            migrator.migrate(c.conn, "mysql", migrations, MigrateOptions.structure(MigrationTarget.MASTER, true));

            // 已应用脚本（01 原样）排除；02 内容被改动（同 script 名不同 checksum）→ 冲突检出
            List<SchemaMigration> after = List.of(
                    migrations.get(0),
                    script("demo", "20990101_02", "ALTER TABLE t1 ADD COLUMN remark VARCHAR(100)"));
            TenantSchemaMigrator.Plan plan = migrator.plan(c.conn, "mysql", after,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(0, plan.pending().size());
            assertEquals(1, plan.conflicts().size());
            assertTrue(plan.conflicts().get(0).contains("20990101_02"));
        }
    }

    @Test
    void legacyDbAutoBaselineReportedAndSkipped() throws SQLException {
        try (Conn c = new Conn()) {
            // 模拟存量库：t1 已存在（未走账本），实际执行将按探针判定打基线并跳过 ≤ 基线脚本
            exec(c.conn, "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))");
            List<SchemaMigration> migrations = List.of(
                    script("demo", "20990101_01", "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))"),
                    script("demo", "20990101_02", "ALTER TABLE t1 ADD COLUMN name VARCHAR(50)"));
            TenantSchemaMigrator.Plan plan = migrator.plan(c.conn, "mysql", migrations,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(0, plan.pending().size());
            assertTrue(plan.notes().stream().anyMatch(n -> n.contains("存量结构") && n.contains("基线")));
            // 只读性：未落基线记账（存量库本无账本表，预演也不得创建）
            assertEquals(0, count(c.conn, "SELECT COUNT(*) FROM information_schema.tables"
                    + " WHERE table_schema = DATABASE() AND UPPER(table_name) = 'MSE_DEMO_SCHEMA_HISTORY'"));
        }
    }

    @Test
    void planThenMigrateSameOutcome() throws SQLException {
        try (Conn c = new Conn()) {
            List<SchemaMigration> migrations = List.of(
                    script("demo", "20990101_01", "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))"));
            TenantSchemaMigrator.Plan plan = migrator.plan(c.conn, "mysql", migrations,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(1, plan.pending().size());
            // 预演后实际执行：清单条数=应用条数；再次预演清零（幂等收敛口径一致）
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql", migrations,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(plan.pending().size(), result.applied());
            TenantSchemaMigrator.Plan again = migrator.plan(c.conn, "mysql", migrations,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(0, again.pending().size());
            assertFalse(again.notes().stream().anyMatch(n -> n.contains("账本未建")));
        }
    }

    private static void exec(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static String scalar(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static int count(Connection conn, String sql) throws SQLException {
        return Integer.parseInt(scalar(conn, sql));
    }

    private static class Conn implements AutoCloseable {
        final Connection conn;

        Conn() throws SQLException {
            this.conn = DriverManager.getConnection(URL, "sa", "");
        }

        @Override
        public void close() throws SQLException {
            conn.close();
        }
    }
}
