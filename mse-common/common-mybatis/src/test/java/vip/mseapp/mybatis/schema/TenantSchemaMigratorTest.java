package vip.mseapp.mybatis.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构迁移器单测（H2 MySQL 模式）：fresh 应用、失败重试、基线跳过、checksum 冲突、ADHOC、语句切分、
 * 脚本扫描/头指令、占位符、每租户通道、框架表不触发误基线、Java 迁移、按 biz 基线。
 */
class TenantSchemaMigratorTest {

    // 内存库名必须为 public：hasUserTables 以 table_schema = DATABASE() 探测存量表，
    // H2 的 DATABASE() 返回库名、表落在 public schema，两者同名探测才生效（MySQL 天然相等）
    private static final String URL = "jdbc:h2:mem:public;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private TenantSchemaMigrator migrator;

    @BeforeEach
    void setUp() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            exec(c.conn, "DROP ALL OBJECTS");
        }
        migrator = new TenantSchemaMigrator(new TenantMigrationScanner(), "junit");
    }

    private static SchemaMigration script(String biz, String version, String sql) {
        return new SchemaMigration(biz, version, "测试脚本 " + version, SchemaMigration.TYPE_CLASSPATH,
                MigrationTarget.MASTER, false, "test/" + version, sql, TenantMigrationScanner.checksum(sql));
    }

    @Test
    void freshApplyAndStatus() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            List<SchemaMigration> migrations = List.of(
                    script("demo", "20990101_01", "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))"),
                    script("demo", "20990101_02", "ALTER TABLE t1 ADD COLUMN name VARCHAR(50)"));
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql", migrations,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(2, result.applied());
            assertEquals(0, result.failed());
            assertEquals("20990101_02", result.after());

            // 结构真实生效
            exec(c.conn, "INSERT INTO t1 (id, name) VALUES (1, 'n1')");
            assertEquals("n1", queryScalar(c.conn, "SELECT name FROM t1 WHERE id = 1"));

            // 账本按 biz 分册（两条脚本各一条流水）
            assertEquals("2", queryScalar(c.conn, "SELECT COUNT(*) FROM mse_demo_schema_history WHERE success = 1"));

            TenantSchemaMigrator.Status status = migrator.status(c.conn, "mysql", "demo");
            assertTrue(status.historyReady());
            assertEquals("20990101_02", status.currentVersion());
            // pending 来自 classpath 扫描（夹具 20990101_81 demo），已应用版本不得出现在 pending
            assertTrue(status.pending().stream().noneMatch(p -> p.startsWith("20990101_01")));
            assertTrue(status.pending().stream().noneMatch(p -> p.startsWith("20990101_02")));
            assertNull(status.lastError());
        }
    }

    @Test
    void failureRecordedThenRetried() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            List<SchemaMigration> first = List.of(
                    script("demo", "20990101_01", "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))"),
                    script("demo", "20990101_02", "INSERT INTO not_exists_table (id) VALUES (1)"));
            TenantSchemaMigrator.Result failed = migrator.migrate(c.conn, "mysql", first,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(1, failed.applied());
            assertEquals(1, failed.failed());
            assertNotNull(failed.firstError());

            // 修正脚本后重跑：失败版本重试、已成功版本跳过
            List<SchemaMigration> fixed = List.of(
                    script("demo", "20990101_01", "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))"),
                    script("demo", "20990101_02", "ALTER TABLE t1 ADD COLUMN name VARCHAR(50)"));
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql", fixed,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(1, result.applied());
            assertEquals(0, result.failed());
            assertEquals("20990101_02", result.after());
            assertNull(migrator.status(c.conn, "mysql", "demo").lastError());
        }
    }

    @Test
    void baselineSkipsOlderScripts() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            exec(c.conn, "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))");
            // 存量库接入：手工基线 20990101_01（≤基线的结构脚本不再执行）
            TenantSchemaHistoryDao dao = new TenantSchemaHistoryDao("demo");
            dao.ensureTable(c.conn, "mysql");
            dao.insert(c.conn, new TenantSchemaHistoryDao.HistoryRow(
                    "20990101_01", "结构基线", SchemaMigration.TYPE_BASELINE, "baseline", null, true, 0, null), "junit", 0L);
            List<SchemaMigration> migrations = List.of(
                    script("demo", "20990101_01", "ALTER TABLE t1 ADD COLUMN skipped VARCHAR(10)"),
                    script("demo", "20990101_02", "ALTER TABLE t1 ADD COLUMN applied VARCHAR(10)"));
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql", migrations,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(1, result.applied());
            assertEquals("20990101_02", result.after());
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 't1' AND column_name = 'applied'"));
            assertEquals("0", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 't1' AND column_name = 'skipped'"));
        }
    }

    @Test
    void checksumConflictReportedNotRerun() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            // 表由脚本自建：首跑时历史与业务表皆空，不触发存量自动基线
            List<SchemaMigration> original = List.of(
                    script("demo", "20990101_01", "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))"));
            migrator.migrate(c.conn, "mysql", original, MigrateOptions.structure(MigrationTarget.MASTER, true));

            // 同版本内容被改动：报 CONFLICT 且不重复执行（改动未生效）；conflicts 计数供启动通道阻断
            List<SchemaMigration> tampered = List.of(
                    script("demo", "20990101_01", "ALTER TABLE t1 ADD COLUMN name VARCHAR(50)"));
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql", tampered,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(0, result.applied());
            assertEquals(1, result.conflicts());
            assertTrue(result.steps().get(0).error().contains("CONFLICT"));
            assertTrue(result.firstConflict().contains("CONFLICT"));
            assertTrue(result.summary().contains("checksum 冲突 1 条"));
            assertEquals("0", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 't1' AND column_name = 'name'"));
        }
    }

    @Test
    void adhocRecordedInHistory() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            TenantSchemaMigrator.Result result = migrator.applyAdhoc(c.conn, "mysql",
                    "CREATE TABLE adhoc_t (id INT)", "临时建表");
            assertEquals(1, result.applied());
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM mse_adhoc_schema_history WHERE type = 'ADHOC' AND success = 1"));
        }
    }

    @Test
    void splitStatementsHandlesStringsAndComments() {
        String script = """
                -- 建表
                CREATE TABLE t (id INT);
                INSERT INTO t VALUES ('a;b');
                INSERT INTO t VALUES ('it''s;ok');
                -- 仅注释语句应被剔除
                ;
                """;
        List<String> statements = TenantSchemaMigrator.splitStatements(script);
        assertEquals(3, statements.size());
        assertTrue(statements.get(1).contains("'a;b'"));
        assertTrue(statements.get(2).contains("'it''s;ok'"));
    }

    @Test
    void scannerParsesNamesTargetsAndDirectives() {
        TenantMigrationScanner scanner = new TenantMigrationScanner();
        List<SchemaMigration> migrations = scanner.scan("mysql");
        assertEquals(4, migrations.size());
        assertEquals(List.of("demo", "oa", "platform", "system"), scanner.bizList("mysql"));

        SchemaMigration demo = scanner.scanBiz("mysql", "demo").get(0);
        assertEquals("20990101_81", demo.version());
        assertEquals(MigrationTarget.MASTER, demo.target());
        assertFalse(demo.perTenant());
        assertEquals("db/mysql/V20990101_81_demo__init.sql", demo.script());

        SchemaMigration system = scanner.scanBiz("mysql", "system").get(0);
        assertEquals(MigrationTarget.BASE, system.target());
        assertEquals(BaseScope.PLATFORM, system.baseScope());
        assertEquals(BaseScope.TENANT, demo.baseScope());

        SchemaMigration oa = scanner.scanBiz("mysql", "oa").get(0);
        assertTrue(oa.perTenant());
        assertEquals("20990101_81", scanner.maxVersion("mysql", "demo"));
        assertEquals("20990101_84", scanner.maxVersion("mysql"));
        assertTrue(scanner.scan("unknown_db").isEmpty());
        assertEquals(TenantMigrationScanner.checksum(demo.sql()), demo.checksum());
    }

    @Test
    void placeholdersSubstitutionEscapeAndFailFast() {
        assertEquals("INSERT INTO t VALUES (5)", SchemaPlaceholders.resolve(
                "INSERT INTO t VALUES (${tenant_id})", Map.of("tenant_id", "5")));
        // $${ 转义为字面量
        assertEquals("SELECT '${x}'", SchemaPlaceholders.resolve("SELECT '$${x}'", Map.of("x", "1")));
        // 未提供值 fail-fast
        assertThrows(IllegalStateException.class,
                () -> SchemaPlaceholders.resolve("SELECT '${missing}'", Map.of()));
    }

    @Test
    void perTenantChannelTrackedPerTenantIndependentOfBaseline() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            // 库里已有 demo 自身业务表 demo_marker（基线探针表）：结构通道触发存量基线；
            // oa_seed（主键含 tenant_id，种子脚本必须租户感知幂等）仅供每租户种子插入
            exec(c.conn, "CREATE TABLE demo_marker (id INT NOT NULL, PRIMARY KEY (id))");
            exec(c.conn, "CREATE TABLE oa_seed (id INT NOT NULL, tenant_id BIGINT NOT NULL, PRIMARY KEY (tenant_id, id))");
            TenantSchemaMigrator.Result structure = migrator.migrate(c.conn, "mysql",
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(0, structure.applied());
            assertEquals("20990101_81", queryScalar(c.conn,
                    "SELECT version FROM mse_demo_schema_history WHERE type = 'BASELINE'"));

            // 每租户通道不受结构基线约束：租户 5 首次应用（${tenant_id} 注入）
            TenantSchemaMigrator.Result t5 = migrator.migrate(c.conn, "mysql", MigrateOptions.forTenant(5L, Map.of()));
            assertEquals(1, t5.applied());
            assertEquals("5", queryScalar(c.conn, "SELECT tenant_id FROM oa_seed WHERE id = 1"));

            // 租户 6 首次同样全量应用（按 (script, tenant) 记账，互不挤占）
            TenantSchemaMigrator.Result t6 = migrator.migrate(c.conn, "mysql", MigrateOptions.forTenant(6L, Map.of()));
            assertEquals(1, t6.applied());
            assertEquals("2", queryScalar(c.conn, "SELECT COUNT(*) FROM oa_seed"));

            // 租户 5 重跑零 pending
            assertEquals(0, migrator.migrate(c.conn, "mysql", MigrateOptions.forTenant(5L, Map.of())).applied());
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM mse_oa_schema_history WHERE tenant_id = 5 AND success = 1"));
        }
    }

    @Test
    void frameworkTablesDoNotTriggerAutoBaseline() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            // 全新库但框架已建表（Flowable 抢建 ACT_/FLW_、基座 sys_、mse_ 前缀、undo_log）：不得误基线
            exec(c.conn, "CREATE TABLE ACT_RU_JOB (id INT NOT NULL, PRIMARY KEY (id))");
            exec(c.conn, "CREATE TABLE FLW_RU_BATCH (id INT NOT NULL, PRIMARY KEY (id))");
            exec(c.conn, "CREATE TABLE sys_user (id INT NOT NULL, PRIMARY KEY (id))");
            exec(c.conn, "CREATE TABLE mse_lock_table (id INT NOT NULL, PRIMARY KEY (id))");
            exec(c.conn, "CREATE TABLE undo_log (xid VARCHAR(10))");
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql",
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(1, result.applied());
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'demo_marker'"));
        }

        try (ConnectionHolder c = new ConnectionHolder()) {
            // 多积木共库（他 biz 的表存在、demo 自身表缺席）：不因他 biz 表误判存量——全新账本照常应用
            // （旧版「库里有任何业务表就基线」探针会把平台库/聚合库的同类形态误基线导致建表被整体跳过）
            exec(c.conn, "DROP TABLE IF EXISTS mse_demo_schema_history");
            exec(c.conn, "DROP TABLE IF EXISTS demo_marker");
            exec(c.conn, "CREATE TABLE oa_form (id INT NOT NULL, PRIMARY KEY (id))");
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql",
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(1, result.applied());
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'demo_marker'"));
            assertEquals("0", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM mse_demo_schema_history WHERE type = 'BASELINE'"));
        }

        try (ConnectionHolder c = new ConnectionHolder()) {
            // 真存量库（demo 自身的探针表存在）：自动基线生效，全量跳过
            exec(c.conn, "DROP TABLE IF EXISTS mse_demo_schema_history");
            exec(c.conn, "DROP TABLE IF EXISTS demo_marker");
            exec(c.conn, "CREATE TABLE demo_marker (id INT NOT NULL, PRIMARY KEY (id))");
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql",
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(0, result.applied());
            assertEquals("20990101_81", queryScalar(c.conn,
                    "SELECT MAX(version) FROM mse_demo_schema_history WHERE type = 'BASELINE'"));
        }
    }

    @Test
    void bogusLedgerBaselineSelfHeals() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            // 误判基线自愈：库里有他 biz 业务表 + 账本被写入 BASELINE（旧版粗探针遗留），
            // 而 demo 自身建表全部缺席——删除基线记账按清单补齐
            exec(c.conn, "CREATE TABLE oa_form (id INT NOT NULL, PRIMARY KEY (id))");
            TenantSchemaHistoryDao dao = new TenantSchemaHistoryDao("demo");
            dao.ensureTable(c.conn, "mysql");
            dao.insert(c.conn, new TenantSchemaHistoryDao.HistoryRow(
                    "20990101_81", "结构基线（误判）", SchemaMigration.TYPE_BASELINE, "baseline", null, true, 0, null),
                    "junit", 0L);

            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql",
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(1, result.applied());
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'demo_marker'"));
            assertEquals("0", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM mse_demo_schema_history WHERE type = 'BASELINE'"));
            // 自愈后脚本流水正常在册（幂等重跑零 pending）
            assertEquals(0, migrator.migrate(c.conn, "mysql",
                    MigrateOptions.structure(MigrationTarget.MASTER, true)).applied());
        }
    }

    @Test
    void bizOwnedInfraTablesDoNotPoisonBaselineProbes() throws SQLException {
        // 生产事故回归：业务脚本自携 undo_log（mes init 为租户独立库自建），Seata 启动抢建同名表在先——
        // 探针必须剔除共享基建表（undo_log/SYS_/MSE_/ACT_/FLW_），否则全新库被误判存量、建表脚本整体被跳过
        List<SchemaMigration> migrations = List.of(script("mes", "20990101_01",
                "CREATE TABLE IF NOT EXISTS mes_andon (id INT NOT NULL, PRIMARY KEY (id));"
                        + "CREATE TABLE IF NOT EXISTS undo_log (xid VARCHAR(10))"));

        // 全新库 + Seata 已抢建 undo_log：脚本照常应用、不得写基线
        try (ConnectionHolder c = new ConnectionHolder()) {
            exec(c.conn, "CREATE TABLE undo_log (xid VARCHAR(10))");
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql", migrations,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(1, result.applied());
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'mes_andon'"));
            assertEquals("0", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM mse_mes_schema_history WHERE type = 'BASELINE'"));
        }

        // 已损库自愈：旧版误判 BASELINE 已入账（业务表全缺席）→ 删除基线记账按清单补齐
        try (ConnectionHolder c = new ConnectionHolder()) {
            exec(c.conn, "DROP TABLE IF EXISTS mse_mes_schema_history");
            exec(c.conn, "DROP TABLE IF EXISTS mes_andon");
            TenantSchemaHistoryDao dao = new TenantSchemaHistoryDao("mes");
            dao.ensureTable(c.conn, "mysql");
            dao.insert(c.conn, new TenantSchemaHistoryDao.HistoryRow(
                    "20990101_01", "结构基线（误判）", SchemaMigration.TYPE_BASELINE, "baseline", null, true, 0, null),
                    "junit", 0L);

            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql", migrations,
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(1, result.applied());
            assertEquals("0", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM mse_mes_schema_history WHERE type = 'BASELINE'"));
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'mes_andon'"));
        }
    }

    @Test
    void baseChannelAppliesOnLegacyBaseDbWithoutAutoBaseline() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            // 基座库形态：docker init 预置 sys_ 表、历史为空——autoBaseline=false 靠脚本幂等全量应用
            exec(c.conn, "CREATE TABLE sys_menu (id INT NOT NULL, name VARCHAR(60), PRIMARY KEY (id))");
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql",
                    MigrateOptions.structure(MigrationTarget.BASE, false));
            assertEquals(1, result.applied());
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'base_marker'"));
            assertEquals("1", queryScalar(c.conn, "SELECT COUNT(*) FROM mse_system_schema_history WHERE success = 1"));

            // master 通道不碰 base 夹具（同一次扫描按 target 过滤）
            TenantSchemaMigrator.Result master = migrator.migrate(c.conn, "mysql",
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertTrue(master.steps().stream().noneMatch(s -> "system".equals(s.biz())));
        }
    }

    @Test
    void javaMigrationsRunInterleavedAndTracked() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            MseSchemaJavaMigration jm = new MseSchemaJavaMigration() {
                @Override
                public String biz() {
                    return "demo";
                }

                @Override
                public String version() {
                    return "20990101_84";
                }

                @Override
                public String description() {
                    return "Java 迁移建表";
                }

                @Override
                public void migrate(java.sql.Connection conn, MigrationContext context) throws SQLException {
                    assertEquals(0L, context.tenantId());
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE TABLE java_marker (id INT NOT NULL, PRIMARY KEY (id))");
                    }
                }
            };
            TenantSchemaMigrator javaMigrator = new TenantSchemaMigrator(new TenantMigrationScanner(), "junit", List.of(jm));
            TenantSchemaMigrator.Result result = javaMigrator.migrate(c.conn, "mysql",
                    MigrateOptions.structure(MigrationTarget.MASTER, true));
            assertEquals(2, result.applied()); // demo SQL 夹具 + Java 迁移
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'java_marker'"));
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM mse_demo_schema_history WHERE type = 'JAVA' AND success = 1"));

            // 重跑跳过；每租户通道的 Java 迁移不进结构通道
            assertEquals(0, javaMigrator.migrate(c.conn, "mysql",
                    MigrateOptions.structure(MigrationTarget.MASTER, true)).applied());
        }
    }

    @Test
    void explicitBizBaselineWinsOverMaxVersion() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            // 存量库（demo 探针表 t1 已存在）：显式按 biz 基线 = 20990101_81（而非清单最大 20990101_82）
            // → _81 记账跳过、_82 应用
            exec(c.conn, "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))");
            List<SchemaMigration> migrations = List.of(
                    script("demo", "20990101_81", "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))"),
                    script("demo", "20990101_82", "CREATE TABLE t2 (id INT NOT NULL, PRIMARY KEY (id))"));
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql", migrations,
                    MigrateOptions.structure(MigrationTarget.MASTER, true)
                            .withBizBaselines(Map.of("demo", "20990101_81")));
            assertEquals(1, result.applied());
            assertEquals("20990101_81", queryScalar(c.conn,
                    "SELECT version FROM mse_demo_schema_history WHERE type = 'BASELINE'"));
            assertEquals("1", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 't2'"));
        }
    }

    @Test
    void resolveBizBaselinesReadsMasterLedgers() throws SQLException {
        try (ConnectionHolder c = new ConnectionHolder()) {
            List<SchemaMigration> migrations = List.of(
                    script("demo", "20990101_81", "CREATE TABLE t1 (id INT NOT NULL, PRIMARY KEY (id))"),
                    script("demo", "20990101_82", "ALTER TABLE t1 ADD COLUMN name VARCHAR(50)"));
            migrator.migrate(c.conn, "mysql", migrations, MigrateOptions.structure(MigrationTarget.MASTER, true));

            Map<String, String> baselines = migrator.resolveBizBaselines(c.conn, "mysql");
            assertEquals("20990101_82", baselines.get("demo"));
        }
    }

    @Test
    void baseChannelIgnoresBaselineAndAppliesInsertOnlyScripts() throws SQLException {
        // 生产事故回归（租户独立库整组丢失协同办公/数据中台/开放能力菜单）：懒初始化②MASTER 先建
        // 业务表，③BASE 通道对菜单/权限类纯 DML 脚本探针空集、回退「全库业务表」判定——把全新库
        // 误判成存量库写 BASELINE 行，base 脚本整组被跳过且探针自愈因空集失效。BASE 通道必须
        // 不写基线、也不受账本既有 BASELINE 约束（幂等收敛靠脚本自身）。
        List<SchemaMigration> baseScripts = List.of(baseScript("oa", "20990101_01",
                "INSERT INTO sys_menu (id, name) VALUES (420, '协同办公')"));

        // ① 懒初始化形态（②步 MASTER 已建表 + autoBaseline=true 旧参数）：不得写基线，脚本照常应用
        try (ConnectionHolder c = new ConnectionHolder()) {
            exec(c.conn, "CREATE TABLE oa_form (id INT NOT NULL, PRIMARY KEY (id))");
            exec(c.conn, "CREATE TABLE sys_menu (id INT NOT NULL, name VARCHAR(60), PRIMARY KEY (id))");
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql", baseScripts,
                    MigrateOptions.structure(MigrationTarget.BASE, true));
            assertEquals(1, result.applied());
            assertEquals("1", queryScalar(c.conn, "SELECT COUNT(*) FROM sys_menu WHERE id = 420"));
            assertEquals("0", queryScalar(c.conn,
                    "SELECT COUNT(*) FROM mse_oa_schema_history WHERE type = 'BASELINE'"));
        }

        // ② 受损库形态（车队重放 autoBaseline=false + 账本残留误判 BASELINE）：无视基线补齐脚本，
        //    预演与执行同口径——base 脚本必须出现在 pending 清单（修复前会被基线静默吞掉）
        try (ConnectionHolder c = new ConnectionHolder()) {
            exec(c.conn, "DROP TABLE IF EXISTS mse_oa_schema_history");
            exec(c.conn, "DROP TABLE IF EXISTS oa_form");
            exec(c.conn, "DROP TABLE IF EXISTS sys_menu");
            exec(c.conn, "CREATE TABLE oa_form (id INT NOT NULL, PRIMARY KEY (id))");
            exec(c.conn, "CREATE TABLE sys_menu (id INT NOT NULL, name VARCHAR(60), PRIMARY KEY (id))");
            TenantSchemaHistoryDao dao = new TenantSchemaHistoryDao("oa");
            dao.ensureTable(c.conn, "mysql");
            dao.insert(c.conn, new TenantSchemaHistoryDao.HistoryRow(
                    "20990101_01", "结构基线（误判）", SchemaMigration.TYPE_BASELINE, "baseline", null, true, 0, null),
                    "junit", 0L);
            TenantSchemaMigrator.Plan plan = migrator.plan(c.conn, "mysql", baseScripts,
                    MigrateOptions.structure(MigrationTarget.BASE, false));
            assertEquals(1, plan.pending().size());
            TenantSchemaMigrator.Result result = migrator.migrate(c.conn, "mysql", baseScripts,
                    MigrateOptions.structure(MigrationTarget.BASE, false));
            assertEquals(1, result.applied());
            assertEquals("1", queryScalar(c.conn, "SELECT COUNT(*) FROM sys_menu WHERE id = 420"));
        }
    }

    private static SchemaMigration baseScript(String biz, String version, String sql) {
        return new SchemaMigration(biz, version, "测试 base 脚本 " + version, SchemaMigration.TYPE_CLASSPATH,
                MigrationTarget.BASE, false, "db/mysql/V" + version + "_x__" + biz + "_base.sql",
                sql, TenantMigrationScanner.checksum(sql));
    }

    private static void exec(java.sql.Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private static String queryScalar(java.sql.Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }

    /** 建持连接；确保 DROP ALL OBJECTS 与业务操作在同一内存库 */
    private static class ConnectionHolder implements AutoCloseable {
        final java.sql.Connection conn;

        ConnectionHolder() throws SQLException {
            this.conn = DriverManager.getConnection(URL, "sa", "");
        }

        @Override
        public void close() throws SQLException {
            conn.close();
        }
    }
}
