package vip.mseapp.mybatis.datasource;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.datasource.TenantIsolationMode;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteResolver;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TenantRouteTransactionManager#doBegin} 路由决策单测：覆盖 resolveCurrentDsKey 全部分支
 * （无上下文 / runIgnore / 平台已注册与未注册 / 共享库字段隔离有/无内置库 / 共享分表 /
 * 独立库含懒初始化 / 未注册降级 / 解析失败收敛 / 解析器缺席），以及 push/poll 栈平衡（正常与异常路径）。
 *
 * <p>路由结果经「doBegin 期间 dynamic-ds 栈顶 key」观测：桩路由器复刻运行时语义
 * （栈顶 key 决定物理连接、null = primary），与 MasterSchemaAutoMigrationTest 同款匿名子类模式。
 */
class TenantRouteTransactionManagerTest {

    private static final String URL = "jdbc:h2:mem:txmgr_route;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private final AtomicReference<String> boundKey = new AtomicReference<>();

    private Map<String, DataSource> dsMap;
    private DataSource master;
    private DynamicRoutingDataSource routing;
    private TenantRouteResolver resolver;
    private TenantDsRegistry registry;
    private TenantDsLazyInitializer lazyInit;

    @BeforeEach
    void setUp() {
        master = h2();
        DataSource shared = h2();
        DataSource tenant = h2();
        dsMap = new HashMap<>();
        dsMap.put("master", master);
        dsMap.put("shared", shared);
        dsMap.put("t_200", tenant);
        routing = new DynamicRoutingDataSource(java.util.List.of()) {
            @Override
            public Map<String, DataSource> getDataSources() {
                return dsMap;
            }

            @Override
            public Connection getConnection() throws SQLException {
                String key = DynamicDataSourceContextHolder.peek();
                boundKey.set(key);
                return (key == null ? master : dsMap.getOrDefault(key, master)).getConnection();
            }
        };
        resolver = mock(TenantRouteResolver.class);
        registry = mock(TenantDsRegistry.class);
        lazyInit = mock(TenantDsLazyInitializer.class);
        when(registry.builtinKey()).thenReturn("shared");
        when(registry.isMainDbAlias(anyString())).thenReturn(false);
        when(registry.isRegistered(anyString())).thenReturn(false);
        when(registry.isRegistered("t_200")).thenReturn(true);
    }

    @AfterEach
    void cleanContext() {
        TenantContext.clear();
        // 栈必须归零：任何用例泄漏 push 都会污染后续测试
        while (DynamicDataSourceContextHolder.peek() != null) {
            DynamicDataSourceContextHolder.poll();
        }
    }

    private TenantRouteTransactionManager manager() {
        TenantRouteTransactionManager manager = new TenantRouteTransactionManager(providerOf(resolver), "mse-test",
                providerOf(lazyInit), providerOf(registry), providerOf(routing), "platform");
        manager.setDataSource(routing);
        return manager;
    }

    /** 在租户上下文外直接开/提交事务（无上下文路径） */
    private void runTx(TenantRouteTransactionManager manager) {
        TransactionStatus status = manager.getTransaction(new DefaultTransactionDefinition());
        manager.commit(status);
    }

    // ===== 无上下文 / runIgnore：不干预（落 primary 原语义）=====

    @Test
    void noContextFallsPrimary() {
        runTx(manager());
        assertThat(boundKey.get()).as("无租户上下文不得 push 任何 key").isNull();
    }

    @Test
    void ignoreFallsPrimary() {
        TenantContext.runAs(100L, () -> TenantContext.runIgnore(() -> runTx(manager())));
        assertThat(boundKey.get()).as("runIgnore 优先级最高，事务落 primary").isNull();
    }

    // ===== 平台会话（tenant 0）=====

    @Test
    void platformSessionBindsPlatformDs() {
        dsMap.put("platform", master);
        TenantContext.runAsPlatform(() -> runTx(manager()));
        assertThat(boundKey.get()).isEqualTo("platform");
    }

    @Test
    void platformUnregisteredFallsPrimary() {
        // platformDsKey 未注册（如业务服务未接平台库）：保持 primary 原语义
        TenantContext.runAsPlatform(() -> runTx(manager()));
        assertThat(boundKey.get()).isNull();
    }

    // ===== 共享库字段隔离（SHARED_FIELD，dsKey=null）→ 内置共享库 =====

    @Test
    void sharedFieldBindsBuiltinShared() {
        TenantRoute route = TenantRoute.sharedDefault(100L, "mse-test", null);
        when(resolver.resolveRoute(100L, "mse-test")).thenReturn(route);
        TenantContext.runAs(100L, () -> runTx(manager()));
        assertThat(boundKey.get()).isEqualTo("shared");
        verify(lazyInit, never()).ensureInitialized(anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sharedFieldWithoutBuiltinFallsPrimary() {
        when(registry.builtinKey()).thenReturn(null);
        TenantRoute route = TenantRoute.sharedDefault(100L, "mse-test", null);
        when(resolver.resolveRoute(100L, "mse-test")).thenReturn(route);
        TenantContext.runAs(100L, () -> runTx(manager()));
        assertThat(boundKey.get()).as("无内置共享库时落 primary（缺表报错可见）").isNull();
    }

    // ===== 共享分表（SHARED_TABLE_SPLIT，dsKey=null）→ 落 primary（与语句级拦截器一致）=====

    @Test
    void sharedSplitFallsPrimary() {
        TenantRoute route = new TenantRoute(100L, "mse-test", TenantIsolationMode.SHARED_TABLE_SPLIT,
                null, "_t100", "tenant_id", null);
        when(resolver.resolveRoute(100L, "mse-test")).thenReturn(route);
        TenantContext.runAs(100L, () -> runTx(manager()));
        assertThat(boundKey.get()).isNull();
    }

    // ===== 独立库：绑定专属库 + 首连懒初始化 =====

    @Test
    void independentBindsTenantDsAndLazyInits() {
        TenantRoute route = new TenantRoute(200L, "mse-test", TenantIsolationMode.INDEPENDENT_DB,
                "t_200", null, "tenant_id", null);
        when(resolver.resolveRoute(200L, "mse-test")).thenReturn(route);
        TenantContext.runAs(200L, () -> runTx(manager()));
        assertThat(boundKey.get()).isEqualTo("t_200");
        verify(lazyInit).ensureInitialized(eq("t_200"), same(route));
    }

    @Test
    void lazyInitFailureConvergesBuiltinShared() {
        TenantRoute route = new TenantRoute(200L, "mse-test", TenantIsolationMode.INDEPENDENT_DB,
                "t_200", null, "tenant_id", null);
        when(resolver.resolveRoute(200L, "mse-test")).thenReturn(route);
        org.mockito.Mockito.doThrow(new IllegalStateException("init boom"))
                .when(lazyInit).ensureInitialized(eq("t_200"), same(route));
        TenantContext.runAs(200L, () -> runTx(manager()));
        assertThat(boundKey.get()).as("懒初始化异常按解析失败收敛内置共享库").isEqualTo("shared");
    }

    // ===== 独立库未注册：降级内置共享库（downgradeRoute 复用）=====

    @Test
    void unregisteredIndependentDowngradesToBuiltin() {
        TenantRoute route = new TenantRoute(300L, "mse-test", TenantIsolationMode.INDEPENDENT_DB,
                "t_999", null, "tenant_id", null);
        when(resolver.resolveRoute(300L, "mse-test")).thenReturn(route);
        TenantContext.runAs(300L, () -> runTx(manager()));
        assertThat(boundKey.get()).as("未注册绑定库降级内置共享库字段隔离").isEqualTo("shared");
    }

    // ===== 解析失败 / 解析器缺席 =====

    @Test
    void resolverFailureConvergesBuiltinShared() {
        when(resolver.resolveRoute(100L, "mse-test")).thenThrow(new RuntimeException("resolve boom"));
        TenantContext.runAs(100L, () -> runTx(manager()));
        assertThat(boundKey.get()).isEqualTo("shared");
    }

    @Test
    void resolverMissingFallsPrimary() {
        TenantRouteTransactionManager manager = new TenantRouteTransactionManager(
                emptyProvider(), "mse-test", providerOf(lazyInit), providerOf(registry),
                providerOf(routing), "platform");
        manager.setDataSource(routing);
        TenantContext.runAs(100L, () -> runTx(manager));
        assertThat(boundKey.get()).as("解析器未就绪按共享主库缺省（绝不缓存兜底）").isNull();
    }

    // ===== push/poll 栈平衡：doBegin 抛异常也必须 poll =====

    @Test
    void doBeginPollsStackOnFailure() {
        DynamicRoutingDataSource broken = new DynamicRoutingDataSource(java.util.List.of()) {
            @Override
            public Map<String, DataSource> getDataSources() {
                return dsMap;
            }

            @Override
            public Connection getConnection() {
                throw new IllegalStateException("connection refused");
            }
        };
        TenantRouteTransactionManager manager = new TenantRouteTransactionManager(
                providerOf(resolver), "mse-test", providerOf(lazyInit), providerOf(registry),
                providerOf(broken), "platform");
        manager.setDataSource(broken);
        TenantRoute route = new TenantRoute(200L, "mse-test", TenantIsolationMode.INDEPENDENT_DB,
                "t_200", null, "tenant_id", null);
        when(resolver.resolveRoute(200L, "mse-test")).thenReturn(route);
        assertThrows(Exception.class, () -> TenantContext.runAs(200L,
                () -> manager.getTransaction(new DefaultTransactionDefinition())));
        assertThat(DynamicDataSourceContextHolder.peek()).as("异常路径栈必须归零").isNull();
    }

    // ===== 桩 =====

    private static DataSource h2() {
        return new SimpleDriverDataSource(new org.h2.Driver(), URL, "sa", "");
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

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return new ObjectProvider<>() {
            @Override
            public T getIfAvailable() {
                return null;
            }
        };
    }
}
