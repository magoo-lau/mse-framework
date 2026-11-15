package vip.mseapp.mybatis.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.datasource.TablePolicy;
import vip.mseapp.core.datasource.TenantIsolationMode;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteContextHolder;
import vip.mseapp.mybatis.config.MseTenantProperties;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 租户行级隔离判定行为锁：
 * 独立库整库免条件（私有库语义）/ 共享库仅 FIELD 表追加 / 白名单与线程 ignore 放行 /
 * 无路由快照保持字段隔离现状。
 */
class MseTenantLineHandlerTest {

    private final MseTenantLineHandler handler = new MseTenantLineHandler(new MseTenantProperties());

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        TenantRouteContextHolder.clear();
    }

    private void route(TenantRoute route) {
        TenantRouteContextHolder.set(route);
    }

    @Test
    void threadIgnorePassesEverything() {
        TenantContext.setIgnoreTenant(true);
        assertTrue(handler.ignoreTable("sys_user"));
    }

    @Test
    void globalWhitelistPasses() {
        // MseTenantProperties 缺省白名单含 sys_tenant（全局表）
        assertTrue(handler.ignoreTable("sys_tenant"));
    }

    @Test
    void noRouteSnapshotKeepsFieldIsolation() {
        assertFalse(handler.ignoreTable("sys_user"));
    }

    @Test
    void independentDbSkipsAllTables() {
        route(new TenantRoute(2L, "*", TenantIsolationMode.INDEPENDENT_DB, "mse_t2", null, "tenant_id", null));
        assertTrue(handler.ignoreTable("sys_user"));
        assertTrue(handler.ignoreTable("demo_order"));
    }

    @Test
    void sharedFieldAppendsCondition() {
        route(new TenantRoute(2L, "*", TenantIsolationMode.SHARED_FIELD, "mse_pool", null, "tenant_id", null));
        assertFalse(handler.ignoreTable("sys_user"));
    }

    @Test
    void sharedSplitSkipsSplitTables() {
        route(new TenantRoute(2L, "*", TenantIsolationMode.SHARED_TABLE_SPLIT, null, "_t2", "tenant_id", null));
        // SPLIT 表靠后缀物理分表，不追加字段条件
        assertTrue(handler.ignoreTable("demo_order"));
    }

    @Test
    void tablePolicyOverridesServiceMode() {
        route(new TenantRoute(2L, "*", TenantIsolationMode.SHARED_FIELD, null, null, "tenant_id",
                Map.of("demo_order", new TablePolicy(TablePolicy.Mode.GLOBAL, null))));
        assertFalse(handler.ignoreTable("sys_user"));
        assertTrue(handler.ignoreTable("demo_order"));
    }
}
