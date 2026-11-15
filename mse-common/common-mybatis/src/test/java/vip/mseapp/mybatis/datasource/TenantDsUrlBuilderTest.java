package vip.mseapp.mybatis.datasource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主库同库绑定判定（TenantDsUrlBuilder.sameTarget/parse）：方言族 + 主机 + 端口 + 库名四维比对。
 */
class TenantDsUrlBuilderTest {

    private TenantDatasourceDefinition def(String dbType, String host, Integer port, String dbName) {
        TenantDatasourceDefinition def = new TenantDatasourceDefinition();
        def.setDbType(dbType);
        def.setHost(host);
        def.setPort(port);
        def.setDbName(dbName);
        return def;
    }

    @Test
    void mysqlSameTarget() {
        assertTrue(TenantDsUrlBuilder.sameTarget(def("mysql", "127.0.0.1", 3306, "mse"),
                "jdbc:mysql://127.0.0.1:3306/mse?useUnicode=true&characterEncoding=utf8"));
    }

    @Test
    void localhostEqualsLoopback() {
        assertTrue(TenantDsUrlBuilder.sameTarget(def("mysql", "localhost", 3306, "mse"),
                "jdbc:mysql://127.0.0.1:3306/mse"));
        assertTrue(TenantDsUrlBuilder.sameTarget(def("mysql", "127.0.0.1", 3306, "mse"),
                "jdbc:mysql://localhost:3306/mse"));
    }

    @Test
    void kingbase8NormalizesToKingbase() {
        assertTrue(TenantDsUrlBuilder.sameTarget(def("kingbase", "10.0.0.8", 54321, "oa_tenant"),
                "jdbc:kingbase8://10.0.0.8:54321/oa_tenant?currentSchema=oa_tenant,public"));
    }

    @Test
    void dmSchemaParamAsDbName() {
        assertTrue(TenantDsUrlBuilder.sameTarget(def("dm", "10.0.0.9", 5236, "OA_TENANT"),
                "jdbc:dm://10.0.0.9:5236?schema=OA_TENANT&characterEncoding=UTF-8"));
    }

    @Test
    void differentDbNameIsNotAlias() {
        assertFalse(TenantDsUrlBuilder.sameTarget(def("mysql", "127.0.0.1", 3306, "mse_tenant2"),
                "jdbc:mysql://127.0.0.1:3306/mse"));
    }

    @Test
    void differentPortIsNotAlias() {
        assertFalse(TenantDsUrlBuilder.sameTarget(def("mysql", "127.0.0.1", 3307, "mse"),
                "jdbc:mysql://127.0.0.1:3306/mse"));
    }

    @Test
    void differentDbTypeIsNotAlias() {
        assertFalse(TenantDsUrlBuilder.sameTarget(def("dm", "127.0.0.1", 3306, "mse"),
                "jdbc:mysql://127.0.0.1:3306/mse"));
    }

    @Test
    void unparseableMainUrlDisablesDetection() {
        assertFalse(TenantDsUrlBuilder.sameTarget(def("mysql", "127.0.0.1", 3306, "mse"), null));
        assertFalse(TenantDsUrlBuilder.sameTarget(def("mysql", "127.0.0.1", 3306, "mse"), ""));
        assertFalse(TenantDsUrlBuilder.sameTarget(def("mysql", "127.0.0.1", 3306, "mse"), "not-a-jdbc-url"));
    }

    @Test
    void defaultPortsByDialect() {
        assertEquals(3306, TenantDsUrlBuilder.parse("jdbc:mysql://127.0.0.1/mse").port());
        assertEquals(5236, TenantDsUrlBuilder.parse("jdbc:dm://127.0.0.1?schema=X").port());
        assertEquals(54321, TenantDsUrlBuilder.parse("jdbc:kingbase8://127.0.0.1/kb").port());
        assertEquals("kingbase", TenantDsUrlBuilder.parse("jdbc:kingbase8://h:1/kb").type());
        assertEquals("X", TenantDsUrlBuilder.parse("jdbc:dm://h:1?schema=X").db());
    }
}
