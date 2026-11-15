package vip.mseapp.mybatis.datasource;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.datasource.TenantIsolationMode;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteResolver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * raw JDBC 租户路由器：业务模块直接 {@code dataSource.getConnection()} 批量读写
 * （同步引擎/质量引擎等绕过 MyBatis 的通道）时，语句级路由拦截器
 * {@link TenantDsExecutorInterceptor} 覆盖不到——不路由即静默落 primary（=平台库）。
 *
 * <p>本路由器按拦截器同款语义解析「租户域业务表」的落库（resolveRoute → 绑定库不可本地
 * 执行时降级内置共享库 → 独立库取专属池、共享池取内置共享库），供 biz 侧统一替换
 * 裸 {@code getConnection()}：
 * <ul>
 *   <li>平台会话（tenant=0）→ 平台库；</li>
 *   <li>独立库租户 → 租户专属库（先经 {@link TenantDsLazyInitializer} 补齐结构）；</li>
 *   <li>共享租户 → 内置共享库（mse.tenant.shared-main.*；未配置快速失败，绝不静默落平台库）；</li>
 *   <li>runIgnore → 内置共享库（跨租户扫描语义，行归属由调用方 SQL 自定）；</li>
 *   <li>无租户上下文 → 快速失败（raw JDBC 不存在安全缺省落点）。</li>
 * </ul>
 * 服务名按调用方类名经 {@link TenantServiceModuleRegistry} 按 @TenantServiceModule 包前缀解析
 * （与语句拦截器 resolveServiceName 同口径），业务侧零配置。
 */
public class TenantRawConnectionRouter {

    private final org.springframework.beans.factory.ObjectProvider<TenantRouteResolver> resolverProvider;
    private final org.springframework.beans.factory.ObjectProvider<TenantDsRegistry> registryProvider;
    private final org.springframework.beans.factory.ObjectProvider<DynamicRoutingDataSource> routingProvider;
    private final org.springframework.beans.factory.ObjectProvider<TenantDsLazyInitializer> lazyInitProvider;
    private final TenantServiceModuleRegistry moduleRegistry;
    private final String platformDsKey;

    public TenantRawConnectionRouter(
            org.springframework.beans.factory.ObjectProvider<TenantRouteResolver> resolverProvider,
            org.springframework.beans.factory.ObjectProvider<TenantDsRegistry> registryProvider,
            org.springframework.beans.factory.ObjectProvider<DynamicRoutingDataSource> routingProvider,
            org.springframework.beans.factory.ObjectProvider<TenantDsLazyInitializer> lazyInitProvider,
            TenantServiceModuleRegistry moduleRegistry,
            String platformDsKey) {
        this.resolverProvider = resolverProvider;
        this.registryProvider = registryProvider;
        this.routingProvider = routingProvider;
        this.lazyInitProvider = lazyInitProvider;
        this.moduleRegistry = moduleRegistry;
        this.platformDsKey = platformDsKey == null || platformDsKey.isBlank() ? "platform" : platformDsKey.trim();
    }

    /** 按当前租户上下文打开「租户域业务表」语义的连接（调用方负责关闭）。 */
    public Connection open(Class<?> caller) throws SQLException {
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("动态数据源未装配，raw JDBC 租户路由不可用");
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && tenantId == MseConstants.PLATFORM_TENANT_ID && !TenantContext.isIgnoreTenant()) {
            // 平台会话：平台库即专属库（未注册平台数据源的环境回落 primary，与语句拦截器一致）
            return routing.getDataSources().containsKey(platformDsKey)
                    ? routing.getDataSource(platformDsKey).getConnection()
                    : routing.getConnection();
        }
        String builtinKey = builtinKey();
        if (tenantId == null || TenantContext.isIgnoreTenant()) {
            if (builtinKey == null) {
                throw new IllegalStateException("raw JDBC 需要内置共享库落点：请配置 mse.tenant.shared-main.*"
                        + "（caller=" + caller.getName() + " tenant=" + tenantId + "）");
            }
            return routing.getDataSource(builtinKey).getConnection();
        }
        String serviceName = resolveServiceName(caller.getName());
        TenantRouteResolver resolver = resolverProvider.getIfAvailable();
        if (resolver == null) {
            throw new IllegalStateException("租户路由解析器未装配，raw JDBC 租户路由不可用");
        }
        TenantRoute resolved = resolver.resolveRoute(tenantId, serviceName);
        TenantDsRegistry registry = registryProvider.getIfAvailable();
        if (registry != null) {
            resolved = TenantDsExecutorInterceptor.downgradeRoute(registry, tenantId, serviceName, resolved);
        }
        final TenantRoute route = resolved;
        final String targetKey;
        if (!route.routeDatasource()) {
            if (route.getMode() != TenantIsolationMode.SHARED_FIELD) {
                throw new IllegalStateException("raw JDBC 不支持的路由模式：" + route.getMode()
                        + "（tenant=" + tenantId + " service=" + serviceName + "）");
            }
            if (builtinKey == null) {
                throw new IllegalStateException("共享租户语句需要内置共享库：请配置 mse.tenant.shared-main.*"
                        + "（tenant=" + tenantId + " service=" + serviceName + "）");
            }
            targetKey = builtinKey;
        } else {
            targetKey = route.getDsKey();
        }
        if (!Objects.equals(targetKey, builtinKey)) {
            // 独立库：首次路由补齐表结构（幂等），与语句拦截器同一时机
            lazyInitProvider.ifAvailable(initializer -> initializer.ensureInitialized(targetKey, route));
        }
        return routing.getDataSource(targetKey).getConnection();
    }

    /** 底层动态路由数据源（需要按显式 dsKey 取专属池的调用方用；未装配快速失败） */
    public DynamicRoutingDataSource routing() {
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("动态数据源未装配，raw JDBC 租户路由不可用");
        }
        return routing;
    }

    private String builtinKey() {
        TenantDsRegistry registry = registryProvider.getIfAvailable();
        return registry == null ? null : registry.builtinKey();
    }

    /** 调用方类名 → 服务名：@TenantServiceModule 包前缀最长匹配，未命中回落 *（与语句拦截器同口径） */
    private String resolveServiceName(String className) {
        if (moduleRegistry != null) {
            String annotated = moduleRegistry.resolveServiceName(className);
            if (annotated != null && !annotated.isBlank()) {
                return annotated;
            }
        }
        return "*";
    }
}
