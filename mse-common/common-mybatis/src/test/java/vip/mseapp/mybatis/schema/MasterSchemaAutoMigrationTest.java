package vip.mseapp.mybatis.schema;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import vip.mseapp.mybatis.config.MseTenantProperties;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 启动自动迁移单测（H2 MySQL 模式）：fresh 应用、无 Redis 退化、失败阻断、重复执行零 pending。
 */
class MasterSchemaAutoMigrationTest {

    private static final String URL = "jdbc:h2:mem:mse_schema_auto_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private SimpleDriverDataSource h2;

    @BeforeEach
    void setUp() throws SQLException {
        try (Statement stmt = DriverManager.getConnection(URL, "sa", "").createStatement()) {
            stmt.execute("DROP ALL OBJECTS");
        }
        h2 = new SimpleDriverDataSource(new org.h2.Driver(), URL, "sa", "");
    }

    /** 无 Redis：进程内串行兜底。0.1.0 起 TENANT/MASTER 通道不再于启动期落 primary
     *  （租户库动态生成——Seeder eager + 懒初始化），platform 通道经 primary 回退仍全量应用 */
    @Test
    void startupAppliesPlatformOnlyWithoutTenantMaster() throws Exception {
        MasterSchemaAutoMigration runner = runner();
        runner.run(null);

        // master 夹具不进 primary（demo_marker 不建、demo 账本表压根不存在）
        assertEquals("0", queryScalar(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'demo_marker'"));
        assertEquals("0", queryScalar(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'mse_demo_schema_history'"));
        // base 夹具不进 master 通道（非基座应用交由推送通道）
        assertEquals("0", queryScalar(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'base_marker'"));
        // platform 通道（primary 回退落点）仍应用
        assertEquals("1", queryScalar(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'sys_platform_marker'"));

        // 重复启动（多实例先后启动）：零 pending、不重复执行、不报错
        assertDoesNotThrow(() -> runner().run(null));
        assertEquals("1", queryScalar(
                "SELECT COUNT(*) FROM mse_platform_schema_history WHERE version = '20990101_84' AND success = 1"));
    }

    /** 平台通道基线回归：全新库在 platform 通道之前已被他通道业务表污染（如 base 通道先建的 mon_*）时，
     *  控制面/平台域脚本（建表全为 SYS_ 前缀、探针被扣成空集）仍须全量应用——不写 BASELINE（autoBaseline=false） */
    @Test
    void platformChannelAppliesFullyDespitePreexistingUserTables() throws Exception {
        try (var conn = h2.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE mon_record (id INT NOT NULL, PRIMARY KEY (id))");
        }
        runner().run(null);

        assertEquals("1", queryScalar(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'sys_platform_marker'"));
        assertEquals("0", queryScalar(
                "SELECT COUNT(*) FROM mse_platform_schema_history WHERE type = 'BASELINE'"));
        assertEquals("1", queryScalar(
                "SELECT COUNT(*) FROM mse_platform_schema_history WHERE version = '20990101_84' AND success = 1"));
    }

    /** 迁移失败：fail-fast 抛异常阻断启动，失败流水已入账本 */
    @Test
    void failureBlocksStartup() {
        TenantSchemaMigrator failing = new TenantSchemaMigrator(new TenantMigrationScanner(), "junit") {
            @Override
            public Result migrate(java.sql.Connection target, String dbType, MigrateOptions options) {
                throw new IllegalStateException("boom 20990101_81");
            }
        };
        MasterSchemaAutoMigration runner = new MasterSchemaAutoMigration(
                providerOf(fakeRouting()), "master", emptyProvider(), failing,
                new TenantMigrationScanner(), schema()) {
            @Override
            protected String detectDbType(java.sql.Connection master) {
                return "mysql";
            }
        };
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> runner.run(null));
        assertTrue(error.getMessage().contains("boom"));
    }

    /** checksum 冲突（已应用脚本被改动）同样阻断启动——新代码不得连旧结构库运行 */
    @Test
    void conflictBlocksStartup() {
        TenantSchemaMigrator conflicting = new TenantSchemaMigrator(new TenantMigrationScanner(), "junit") {
            @Override
            public Result migrate(java.sql.Connection target, String dbType, MigrateOptions options) {
                return new Result("20990101_80", "20990101_80", 0, 0, 1, List.of(new StepResult(
                        "demo", "20990101_01", SchemaMigration.TYPE_CLASSPATH, "test/V20990101_01.sql",
                        true, 0, "CONFLICT：脚本已应用但内容有改动（历史 checksum 不一致），请人工核对")));
            }
        };
        MasterSchemaAutoMigration runner = new MasterSchemaAutoMigration(
                providerOf(fakeRouting()), "master", emptyProvider(), conflicting,
                new TenantMigrationScanner(), schema()) {
            @Override
            protected String detectDbType(java.sql.Connection master) {
                return "mysql";
            }
        };
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> runner.run(null));
        assertTrue(error.getMessage().contains("checksum 冲突"));
    }

    /** dynamic-datasource 缺失：明确报错而非静默跳过 */
    @Test
    void missingRoutingFailsFast() {
        MasterSchemaAutoMigration runner = new MasterSchemaAutoMigration(
                emptyProvider(), "master", emptyProvider(),
                new TenantSchemaMigrator(new TenantMigrationScanner(), "junit"),
                new TenantMigrationScanner(), schema());
        assertThrows(IllegalStateException.class, () -> runner.run(null));
    }

    private MasterSchemaAutoMigration runner() {
        return new MasterSchemaAutoMigration(
                providerOf(fakeRouting()), "master", emptyProvider(),
                new TenantSchemaMigrator(new TenantMigrationScanner(), "junit"),
                new TenantMigrationScanner(), schema()) {
            @Override
            protected String detectDbType(java.sql.Connection master) {
                return "mysql";
            }
        };
    }

    private static MseTenantProperties.Schema schema() {
        return new MseTenantProperties.Schema();
    }

    // ===== 桩 =====

    private DynamicRoutingDataSource fakeRouting() {
        return new DynamicRoutingDataSource(List.of()) {
            @Override
            public Map<String, DataSource> getDataSources() {
                return Map.of("master", h2);
            }

            @Override
            public java.sql.Connection getConnection() throws SQLException {
                return h2.getConnection();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> providerOf(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }
        };
    }

    private static <T> ObjectProvider<T> emptyProvider() {
        return new ObjectProvider<>() {
            @Override
            public T getIfAvailable() {
                return null;
            }
        };
    }

    private static String queryScalar(String sql) throws SQLException {
        try (PreparedStatement ps = DriverManager.getConnection(URL, "sa", "").prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }
}
