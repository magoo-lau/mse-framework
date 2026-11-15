package vip.mseapp.mybatis.datasource;

import org.junit.jupiter.api.Test;
import vip.mseapp.core.datasource.TenantIsolationMode;
import vip.mseapp.core.datasource.TenantRoute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 路由降级决策（0.1.0 语义）：绑定库不可本地执行时收敛缺省共享库 + tenant_id 行级隔离
 * （绝不裸奔）——同库别名 / 未注册 / 正常注册三条路径；无缺省共享库时快速失败（primary=平台库
 * 绝不静默承接租户语句）。
 */
class TenantDsExecutorInterceptorTest {

    private static final String SHARED_KEY = "shared-main";

    /** 按需伪装别名/注册状态/内置键（基类构造仅赋值，可安全子类化） */
    private static TenantDsRegistry registry(boolean alias, boolean registered, String builtinKey) {
        return new TenantDsRegistry(null, null, null, "*", null, null, null) {
            @Override
            public boolean isMainDbAlias(String dsKey) {
                return alias;
            }

            @Override
            public boolean isRegistered(String dsKey) {
                return registered;
            }

            @Override
            public String builtinKey() {
                return builtinKey;
            }
        };
    }

    private static TenantRoute independentRoute() {
        return new TenantRoute(2L, "mse-system", TenantIsolationMode.INDEPENDENT_DB, "mse_t2", null, "tenant_id", null);
    }

    @Test
    void mainDbAliasConvergesToSharedFieldOnSharedPool() {
        TenantRoute downgraded = TenantDsExecutorInterceptor.downgradeRoute(
                registry(true, false, SHARED_KEY), 2L, "mse-system", independentRoute());
        assertTrue(downgraded.routeDatasource());
        assertEquals(SHARED_KEY, downgraded.getDsKey());
        assertTrue(downgraded.getMode() == TenantIsolationMode.SHARED_FIELD);
    }

    @Test
    void unregisteredDsConvergesToSharedFieldOnSharedPool() {
        TenantRoute downgraded = TenantDsExecutorInterceptor.downgradeRoute(
                registry(false, false, SHARED_KEY), 2L, "mse-system", independentRoute());
        assertTrue(downgraded.routeDatasource());
        assertEquals(SHARED_KEY, downgraded.getDsKey());
        assertTrue(downgraded.getMode() == TenantIsolationMode.SHARED_FIELD);
    }

    @Test
    void unregisteredWithoutSharedFailsFast() {
        assertThrows(IllegalStateException.class, () -> TenantDsExecutorInterceptor.downgradeRoute(
                registry(false, false, null), 2L, "mse-system", independentRoute()));
    }

    @Test
    void registeredDsKeepsRoute() {
        TenantRoute route = independentRoute();
        assertSame(route, TenantDsExecutorInterceptor.downgradeRoute(
                registry(false, true, SHARED_KEY), 2L, "mse-system", route));
    }

    @Test
    void routeWithoutDsKeyUnchanged() {
        // 显式无 dsKey 的路由（如平台实体缺省路由）不触发降级与缺省库解析——无缺省库时同样原样返回
        TenantRoute route = new TenantRoute(2L, "mse-system", TenantIsolationMode.SHARED_FIELD,
                null, null, "tenant_id", null);
        assertFalse(route.routeDatasource());
        assertSame(route, TenantDsExecutorInterceptor.downgradeRoute(
                registry(false, false, SHARED_KEY), 2L, "mse-system", route));
        assertSame(route, TenantDsExecutorInterceptor.downgradeRoute(
                registry(false, false, null), 2L, "mse-system", route));
    }

    // ===== 事务绑定库一致性护栏（fast-fail：单体聚合下应用缺省服务与语句所属服务解析出不同库）=====

    @Test
    void txBoundDsMismatchProducesErrorWithBothSides() {
        String mismatch = TenantDsExecutorInterceptor.transactionDsMismatch(
                new TenantRouteTransactionManager.BoundDs("ds-a", "mse-app"),
                "ds-b", "mse-mes", "vip.mseapp.mes.mapper.MesOrderMapper");
        assertNotNull(mismatch);
        assertTrue(mismatch.contains("ds-a"));
        assertTrue(mismatch.contains("ds-b"));
        assertTrue(mismatch.contains("MesOrderMapper"));
        assertTrue(mismatch.contains("@GlobalTransactional"));
    }

    @Test
    void txBoundDsMatchOrNullBoundPasses() {
        assertNull(TenantDsExecutorInterceptor.transactionDsMismatch(
                new TenantRouteTransactionManager.BoundDs("ds-a", "mse-app"),
                "ds-a", "mse-mes", "vip.mseapp.mes.mapper.MesOrderMapper"));
        // 事务未经 TenantRouteTransactionManager 绑定（无记录）：不适用，行为不变
        assertNull(TenantDsExecutorInterceptor.transactionDsMismatch(
                null, "ds-b", "mse-mes", "vip.mseapp.mes.mapper.MesOrderMapper"));
    }
}
