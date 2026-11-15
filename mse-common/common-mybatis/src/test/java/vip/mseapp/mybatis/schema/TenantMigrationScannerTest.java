package vip.mseapp.mybatis.schema;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分域护栏：base 通道脚本建/写基座身份表的识别（平台身份归 platform 通道/平台库、
 * 租户身份归 tenant 通道/租户库；sys_menu/sys_config 等全局表/默认模板不在护栏范围）。
 */
class TenantMigrationScannerTest {

    @Test
    void findsIdentityTableDdlAndSeeds() {
        String sql = """
                -- mse-target: base
                CREATE TABLE IF NOT EXISTS sys_user (id BIGINT);
                insert into sys_role(id) values (1);
                INSERT INTO sys_dept (id) SELECT 900 FROM DUAL;
                TRUNCATE TABLE sys_user_post;
                """;
        Set<String> out = TenantMigrationScanner.findIdentityTables(sql);
        assertEquals(Set.of("SYS_USER", "SYS_ROLE", "SYS_DEPT", "SYS_USER_POST"), out);
    }

    @Test
    void ignoresGlobalTablesAndPlainReferences() {
        String sql = """
                -- mse-target: base
                CREATE TABLE IF NOT EXISTS sys_menu (id BIGINT);
                INSERT INTO sys_config (id) VALUES (1);
                INSERT INTO sys_dict_data (id) VALUES (1);
                INSERT INTO sys_micro_app (id) VALUES (1);
                -- 注释里提到 sys_user 不算引用
                SELECT 1 FROM sys_user_parent WHERE id = 1;
                """;
        assertTrue(TenantMigrationScanner.findIdentityTables(sql).isEmpty());
    }

    @Test
    void blankOrNullSqlYieldsEmpty() {
        assertTrue(TenantMigrationScanner.findIdentityTables(null).isEmpty());
        assertTrue(TenantMigrationScanner.findIdentityTables("").isEmpty());
    }

    /** 命名不符的夹具（V20990101_90_demo_badname.sql，缺 __ 分隔）：缺省 WARN 跳过，strict-scan 抛异常 */
    @Test
    void malformedFilenameSkippedByDefaultAndFailsOnStrict() {
        TenantMigrationScanner lenient = new TenantMigrationScanner();
        assertEquals(4, lenient.scan("mysql").size());

        TenantMigrationScanner strict = new TenantMigrationScanner(
                TenantMigrationScanner.DEFAULT_LOCATION_PATTERN, true);
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> strict.scan("mysql"));
        assertTrue(error.getMessage().contains("badname"));
    }
}
