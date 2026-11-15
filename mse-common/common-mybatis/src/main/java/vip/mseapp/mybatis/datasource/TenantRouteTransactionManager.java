package vip.mseapp.mybatis.datasource;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionDefinition;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteResolver;
import vip.mseapp.core.log.LogContext;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * 租户路由版 JDBC 事务管理器：修复「本地事务连接固定落主库」缺陷。
 *
 * <p>背景：租户切库发生在 {@link TenantDsExecutorInterceptor}（MyBatis Executor 级，
 * 每条语句 push/poll），而 Spring 事务在 {@code doBegin} 即从 dynamic-datasource 取物理连接
 * ——此刻尚无任何语句、路由栈为空，连接被钉死在 primary 主库。独立库租户在
 * {@code @Transactional} 内的全部读写因此落错库（读不到本租户数据、写进共享主库）。
 *
 * <p>修复：{@code doBegin} 前按当前租户上下文解析路由并 push 数据源 key，事务连接绑到
 * 绑定库后 poll；语句级拦截器照常叠加工作（事务内连接已绑定，重复 push 无副作用）。
 * 平台会话（tenantId=0）路由平台控制面库，无租户上下文/runIgnore 保持原语义。
 *
 * <p>语义不变量（与拦截器同一限制，见其类注释）：一次本地事务仍只绑一个库，
 * 首次绑定取「方法所属服务」的缺省路由（不区分方法内逐 mapper 的服务名），
 * 跨库写操作必须走 Seata（AGENTS.md 事务注解规范既有约定）。绑定库经
 * {@link #BOUND_DS_RESOURCE_KEY} 记录到事务资源（doBegin 绑、cleanup 解绑），
 * {@link TenantDsExecutorInterceptor} 据此校验事务内语句目标库一致——不一致默认快速报错
 * （mse.tenant.tx-ds-mismatch=warn 降级告警），不再静默落错库。
 */
public class TenantRouteTransactionManager extends JdbcTransactionManager {

    private static final Logger log = LoggerFactory.getLogger(TenantRouteTransactionManager.class);

    /** 事务绑定库记录的 resource key（doBegin 绑定、语句级拦截器读取校验、doCleanup 解绑） */
    static final Object BOUND_DS_RESOURCE_KEY = new Object();

    /** 事务绑定库（一次本地事务只绑一个库护栏的载体：绑定 dsKey + 解析所用服务名，供报错定位） */
    record BoundDs(String dsKey, String serviceName) {
    }

    private final ObjectProvider<TenantRouteResolver> routeResolverProvider;

    private final String defaultServiceName;

    private final ObjectProvider<TenantDsLazyInitializer> lazyInitProvider;

    private final ObjectProvider<TenantDsRegistry> registryProvider;

    private final ObjectProvider<DynamicRoutingDataSource> routingProvider;

    private final String platformDsKey;

    public TenantRouteTransactionManager(ObjectProvider<TenantRouteResolver> routeResolverProvider,
                                         String defaultServiceName,
                                         ObjectProvider<TenantDsLazyInitializer> lazyInitProvider,
                                         ObjectProvider<TenantDsRegistry> registryProvider,
                                         ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                         String platformDsKey) {
        this.routeResolverProvider = routeResolverProvider;
        this.defaultServiceName = defaultServiceName == null || defaultServiceName.isBlank()
                ? "*" : defaultServiceName.trim();
        this.lazyInitProvider = lazyInitProvider;
        this.registryProvider = registryProvider;
        this.routingProvider = routingProvider;
        this.platformDsKey = platformDsKey == null || platformDsKey.isBlank() ? "platform" : platformDsKey.trim();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        String dsKey = resolveCurrentDsKey();
        if (dsKey == null) {
            super.doBegin(transaction, definition);
            return;
        }
        LogContext.setDs(dsKey);
        DynamicDataSourceContextHolder.push(dsKey);
        try {
            super.doBegin(transaction, definition);
            // 绑定库记录（super.doBegin 成功后；已绑定时不再覆盖——REQUIRES_NEW 挂起恢复等边界，
            // 最外层绑定即事务实际连接）：供语句级拦截器校验「事务内语句目标库一致」
            if (org.springframework.transaction.support.TransactionSynchronizationManager
                    .getResource(BOUND_DS_RESOURCE_KEY) == null) {
                org.springframework.transaction.support.TransactionSynchronizationManager
                        .bindResource(BOUND_DS_RESOURCE_KEY, new BoundDs(dsKey, defaultServiceName));
            }
        } finally {
            DynamicDataSourceContextHolder.poll();
            LogContext.removeDs();
        }
    }

    @Override
    protected void doCleanupAfterCompletion(Object transaction) {
        try {
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .unbindResource(BOUND_DS_RESOURCE_KEY);
        } catch (IllegalStateException ignored) {
            // 本事务未绑定（resolveCurrentDsKey=null 未路由等）：无物可解
        }
        super.doCleanupAfterCompletion(transaction);
    }

    /**
     * 当前上下文应绑定的数据源 key；null = 不干预（保持 primary 原语义）。
     * 与 {@link TenantDsExecutorInterceptor} 同一解析链，但不区分方法内逐 mapper 的服务名
     * （事务层面只有缺省服务名可用）。
     */
    private String resolveCurrentDsKey() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || TenantContext.isIgnoreTenant()) {
            return null;
        }
        if (tenantId == 0L) {
            // 平台会话：控制面数据源已注册才切，否则落 primary（与拦截器一致）
            DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
            return routing != null && routing.getDataSources().containsKey(platformDsKey) ? platformDsKey : null;
        }
        try {
            TenantRouteResolver resolver = routeResolver();
            if (resolver == null) {
                // 未就绪：本次按共享主库缺省（字段隔离，安全方向），下次事务重试获取（绝不缓存兜底）
                return null;
            }
            TenantRoute route = resolver.resolveRoute(tenantId, defaultServiceName);
            if (route == null) {
                return null;
            }
            TenantDsRegistry registry = registry();
            if (registry != null) {
                route = TenantDsExecutorInterceptor.downgradeRoute(registry, tenantId, defaultServiceName, route);
            }
            if (!route.routeDatasource()) {
                // dsKey=null 的共享路由 = 本服务内置共享库（0.1.0 v2 语义）
                if (route.getMode() == vip.mseapp.core.datasource.TenantIsolationMode.SHARED_FIELD) {
                    TenantDsRegistry reg = registry();
                    String builtinKey = reg == null ? null : reg.builtinKey();
                    if (builtinKey != null) {
                        return builtinKey;
                    }
                    log.warn("[mse-tenant-ds] 共享事务需要内置共享库但本服务未配置 mse.tenant.shared-main.*，"
                            + "事务落 primary（租户表缺表即报错可见）: tenant={}", tenantId);
                }
                return null;
            }
            String dsKey = route.getDsKey();
            // 动态开通链路第 3 级：事务连接是首连，未注册的绑定库必须先补齐，否则 strict=false 静默回落主库
            TenantRoute bound = route;
            lazyInitProvider.ifAvailable(initializer -> initializer.ensureInitialized(dsKey, bound));
            return dsKey;
        } catch (Exception e) {
            // 解析失败收敛缺省共享库（字段隔离，安全方向），语句级拦截器仍会按语句重试路由；
            // 无缺省共享库时按无路由处理（落 primary=平台库，租户表缺表即报错可见）
            TenantDsRegistry fallback = registry();
            String builtinKey = fallback == null ? null : fallback.builtinKey();
            if (builtinKey != null) {
                log.warn("[mse-tenant-ds] 事务路由解析失败，本次事务收敛内置共享库: tenant={} service={} err={}",
                        tenantId, defaultServiceName, e.getMessage());
                return builtinKey;
            }
            log.warn("[mse-tenant-ds] 事务路由解析失败且本服务未配置内置共享库，本次事务落 primary: tenant={} service={} err={}",
                    tenantId, defaultServiceName, e.getMessage());
            return null;
        }
    }

    /** 解析器懒获取：ObjectProvider 每次解析，Bean 未就绪时返回 null 由调用方按缺省处理（绝不缓存兜底） */
    private TenantRouteResolver routeResolver() {
        return routeResolverProvider.getIfAvailable();
    }

    /** 注册器懒获取：同上，缺席时跳过降级判定（注册器只影响「绑定库不可本地执行」的降级方向） */
    private TenantDsRegistry registry() {
        return registryProvider.getIfAvailable();
    }
}
