package vip.mseapp.mybatis.datasource;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.datasource.TenantIsolationMode;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteResolver;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 租户路由事务四世界矩阵（真 MySQL，单容器三库）：验证 {@link TenantRouteTransactionManager}
 * 在 doBegin 把本地事务物理连接绑到正确世界——平台（0）→平台库、共享字段隔离→内置共享库、
 * 独立库→专属库；提交/回滚语义；以及 AGENTS 红线「事务开启前必须已处于目标世界」：
 * 事务内 runAs 其他世界不重绑连接（仍落入口世界）。
 *
 * <p>与真实链路的差异：语句级拦截器（MyBatis Executor 路由）不在本测试范围——事务连接一经
 * doBegin 绑定即覆盖整个事务生命周期，这正是被验证的不变量；行级 tenant_id 注入由
 * MseTenantLineHandlerTest 覆盖。无 Docker 环境整套跳过（disabledWithoutDocker）。
 */
@Testcontainers(disabledWithoutDocker = true)
class TenantRouteTxIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withUsername("root")
            .withPassword("test");

    private static final String TABLE = "route_probe";

    private static DataSource platformDs;
    private static DataSource sharedDs;
    private static DataSource tenantDs;
    private static DynamicRoutingDataSource routing;
    private static TransactionTemplate tx;
    private static JdbcTemplate platform;
    private static JdbcTemplate shared;
    private static JdbcTemplate tenant;
    private static JdbcTemplate routed;

    @BeforeAll
    static void initWorlds() throws SQLException {
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker 不可用，跳过真库矩阵");
        try (Connection c = MYSQL.createConnection(""); Statement st = c.createStatement()) {
            for (String db : new String[]{"it_platform", "it_shared", "it_tenant"}) {
                st.execute("CREATE DATABASE " + db);
            }
        }
        platformDs = ds("it_platform");
        sharedDs = ds("it_shared");
        tenantDs = ds("it_tenant");

        Map<String, DataSource> dsMap = new HashMap<>();
        dsMap.put("master", platformDs);       // primary=平台库（0.1.0 语义：无上下文/未路由落平台）
        dsMap.put("platform", platformDs);
        dsMap.put("shared", sharedDs);         // 内置共享库
        dsMap.put("t_200", tenantDs);          // 租户 200 独立库
        routing = new DynamicRoutingDataSource(java.util.List.of()) {
            @Override
            public Map<String, DataSource> getDataSources() {
                return dsMap;
            }

            @Override
            public Connection getConnection() throws SQLException {
                String key = DynamicDataSourceContextHolder.peek();
                return (key == null ? platformDs : dsMap.getOrDefault(key, platformDs)).getConnection();
            }
        };

        // 解析器：100=共享字段隔离（落内置共享库）、200=独立库 t_200
        TenantRouteResolver resolver = mock(TenantRouteResolver.class);
        when(resolver.resolveRoute(100L, "mse-it"))
                .thenReturn(TenantRoute.sharedDefault(100L, "mse-it", null));
        when(resolver.resolveRoute(200L, "mse-it"))
                .thenReturn(new TenantRoute(200L, "mse-it", TenantIsolationMode.INDEPENDENT_DB,
                        "t_200", null, "tenant_id", null));
        TenantDsRegistry registry = mock(TenantDsRegistry.class);
        when(registry.builtinKey()).thenReturn("shared");
        when(registry.isMainDbAlias(anyString())).thenReturn(false);
        when(registry.isRegistered(anyString())).thenReturn(false);
        when(registry.isRegistered("t_200")).thenReturn(true);

        TenantRouteTransactionManager manager = new TenantRouteTransactionManager(
                providerOf(resolver), "mse-it", emptyProvider(), providerOf(registry),
                providerOf(routing), "platform");
        manager.setDataSource(routing);
        tx = new TransactionTemplate(manager);

        platform = new JdbcTemplate(platformDs);
        shared = new JdbcTemplate(sharedDs);
        tenant = new JdbcTemplate(tenantDs);
        routed = new JdbcTemplate(routing);
        for (JdbcTemplate jdbc : new JdbcTemplate[]{platform, shared, tenant}) {
            jdbc.execute("CREATE TABLE " + TABLE + " (id BIGINT AUTO_INCREMENT PRIMARY KEY, tag VARCHAR(64))");
        }
    }

    @BeforeEach
    void cleanProbes() {
        for (JdbcTemplate jdbc : new JdbcTemplate[]{platform, shared, tenant}) {
            jdbc.update("DELETE FROM " + TABLE);
        }
    }

    @AfterEach
    void cleanContext() {
        TenantContext.clear();
        while (DynamicDataSourceContextHolder.peek() != null) {
            DynamicDataSourceContextHolder.poll();
        }
    }

    private static int count(JdbcTemplate jdbc) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + TABLE, Integer.class);
        return n == null ? 0 : n;
    }

    // ===== 四世界绑定：事务提交后行只落绑定库 =====

    @Test
    void platformTxLandsPlatformDb() {
        TenantContext.runAsPlatform(() -> tx.executeWithoutResult(
                s -> routed.update("INSERT INTO " + TABLE + " (tag) VALUES ('platform')")));
        assertThat(count(platform)).as("平台事务落平台库").isEqualTo(1);
        assertThat(count(shared)).isZero();
        assertThat(count(tenant)).isZero();
    }

    @Test
    void sharedFieldTxLandsSharedDb() {
        TenantContext.runAs(100L, () -> tx.executeWithoutResult(
                s -> routed.update("INSERT INTO " + TABLE + " (tag) VALUES ('shared')")));
        assertThat(count(shared)).as("共享字段隔离事务落内置共享库").isEqualTo(1);
        assertThat(count(platform)).isZero();
        assertThat(count(tenant)).isZero();
    }

    @Test
    void independentTxLandsTenantDb() {
        TenantContext.runAs(200L, () -> tx.executeWithoutResult(
                s -> routed.update("INSERT INTO " + TABLE + " (tag) VALUES ('tenant')")));
        assertThat(count(tenant)).as("独立库事务落租户专属库").isEqualTo(1);
        assertThat(count(platform)).isZero();
        assertThat(count(shared)).isZero();
    }

    @Test
    void noContextFallsPrimaryPlatform() {
        tx.executeWithoutResult(s -> routed.update("INSERT INTO " + TABLE + " (tag) VALUES ('primary')"));
        assertThat(count(platform)).as("无上下文事务落 primary=平台库").isEqualTo(1);
    }

    // ===== 回滚语义：绑定库零残留 =====

    @Test
    void rollbackLeavesBoundDbClean() {
        try {
            TenantContext.runAs(200L, () -> tx.executeWithoutResult(s -> {
                routed.update("INSERT INTO " + TABLE + " (tag) VALUES ('rollback')");
                throw new IllegalStateException("boom");
            }));
        } catch (IllegalStateException expected) {
            // 事务模板回滚并重抛
        }
        assertThat(count(tenant)).as("回滚后绑定库零残留").isZero();
    }

    // ===== 红线：事务开启前必须已处于目标世界——doBegin 已钉死连接，事务内 runAs 不重绑 =====

    @Test
    void runAsInsideTxKeepsEntryWorld() {
        TenantContext.runAs(200L, () -> tx.executeWithoutResult(s -> {
            // 入口世界=租户200独立库；事务内切到共享世界写——连接不重绑，仍落入口世界
            TenantContext.runAs(100L, () ->
                    routed.update("INSERT INTO " + TABLE + " (tag) VALUES ('in-tx-switch')"));
        }));
        assertThat(count(tenant)).as("事务内 runAs 切世界：行仍落入口世界（连接钉死）").isEqualTo(1);
        assertThat(count(shared)).as("共享库不得收到事务内切世界的写入").isZero();
    }

    @Test
    void runAsBeforeTemplateBindsTargetWorld() {
        // 正确姿势：先 runAs 锚定世界，再开事务——连接绑到目标世界
        TenantContext.runAs(100L, () -> tx.executeWithoutResult(
                s -> routed.update("INSERT INTO " + TABLE + " (tag) VALUES ('pre-anchored')")));
        assertThat(count(shared)).as("先 runAs 后开事务：绑目标世界").isEqualTo(1);
    }

    // ===== 桩 =====

    private static DataSource ds(String db) {
        String url = MYSQL.getJdbcUrl().replaceAll("/[^/]+$", "/" + db);
        return new org.springframework.jdbc.datasource.DriverManagerDataSource(
                url, MYSQL.getUsername(), MYSQL.getPassword());
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
