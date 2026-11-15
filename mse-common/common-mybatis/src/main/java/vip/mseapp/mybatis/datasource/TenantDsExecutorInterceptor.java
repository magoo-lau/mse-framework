package vip.mseapp.mybatis.datasource;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.toolkit.DynamicDataSourceContextHolder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.datasource.TenantIsolationMode;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteContextHolder;
import vip.mseapp.core.datasource.TenantRouteResolver;
import vip.mseapp.core.log.LogContext;
import vip.mseapp.mybatis.data.GlobalBaseEntity;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.ExceptionUtil;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多模式租户数据源路由拦截器（Executor 级，整次执行外围 push/poll，线程栈严格平衡）。
 *
 * <p>同时承担「路由快照装配」：把 TenantRouteResolver 的解析结果放入
 * {@link TenantRouteContextHolder}，供租户行级插件（字段隔离判定）与分表改写
 * （StatementHandler 期）读取同一视图，执行完恢复。
 *
 * <p>路由规则（按 (租户, 服务) 路由快照）：
 * <ul>
 *   <li>绑定 ds_key（shared 共享池 / dedicated 独立库）：全部租户域表路由到绑定库——
 *       独立库整库私有（身份表随租户入库，行级插件整表免过滤），共享池靠 tenant_id 行级隔离；
 *       租户域全局表（sys_menu/sys_permission/sys_micro_app 租户库自持副本）同规则随租户；</li>
 *   <li>平台控制面表（PlatformBaseEntity）只存平台库，不随租户路由；</li>
 *   <li>未绑定 ds_key：不路由（落本服务业务主库），仅做字段/分表改写。</li>
 * </ul>
 *
 * <p>降级保护（绑定库不可本地执行时一律降级共享主库 + 字段隔离，绝不裸奔主库）：
 * <ul>
 *   <li>绑定数据源与主库同库（{@link TenantDsRegistry#isMainDbAlias}）：同库物理上承载
 *       多租户与平台数据，降级为共享库语义（不切库、tenant_id 行级过滤照常生效）；</li>
 *   <li>绑定数据源未在本服务注册（服务作用域外/未同步）：降级为共享库语义并告警，
 *       杜绝 dynamic-datasource strict=false 静默回落主库后叠加免过滤。</li>
 * </ul>
 *
 * <p>限制（重要）：本地事务内首条语句绑定连接后复用同一数据源，跨库操作必须走 Seata；
 * 签名必须同时覆盖 {@code query} 与 {@code queryCursor}——游标查询（Cursor）同样需要路由切库，
 * 漏签名的语句将无快照直连主库（行级插件仅能靠字段条件兜底，独立库语义下等于漏切库）。
 * 事务绑定库一致性护栏（0.2.0 起）：事务在 doBegin 按「应用缺省服务」钉死连接库，本拦截器
 * 按 mapper 所属服务解析语句目标库，两者不一致时默认快速报错（mse.tenant.tx-ds-mismatch=warn
 * 降级告警）——把「静默落错库」变成首条语句显式失败。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "queryCursor",
                args = {MappedStatement.class, Object.class, RowBounds.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class TenantDsExecutorInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantDsExecutorInterceptor.class);

    /** mapper 接口 → 实体路由信息（是否全局表实体） */
    private final Map<String, EntityRouteInfo> entityInfoCache = new ConcurrentHashMap<>();

    /**
     * 路由解析重入保护：{@link TenantRouteResolver} 实现通常查库（sys_tenant_service_config /
     * sys_tenant），这些语句会再进本拦截器；不挡住即 load → resolveRoute → 查库 → load 无限递归
     * （StackOverflowError）。普通 ThreadLocal 即可：严格限于 resolveRoute 同步调用期间。
     */
    private static final ThreadLocal<Boolean> RESOLVING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * 路由解析器经 ObjectProvider 延迟解析（首次语句执行才 getIfAvailable）：
     * base 本地实现/RPC 实现都依赖 Mapper → SqlSessionFactory，若在工厂方法急切解析，
     * 会撞上 MP 自动装配收集插件期的 sqlSessionFactory 创建中循环（与 DataScopeResolver 同款约束）。
     */
    private final org.springframework.beans.factory.ObjectProvider<TenantRouteResolver> routeResolverProvider;

    private volatile TenantRouteResolver routeResolver;

    private final String defaultServiceName;

    private final Map<String, String> serviceMappings;

    /** 模块服务身份收集器（@TenantServiceModule 注解推导，yaml 未命中时兜底匹配） */
    private final TenantServiceModuleRegistry moduleRegistry;

    /** 可空：租户库懒初始化（动态开通链路第 3 级），首次路由到未初始化目标库时触发 */
    private final org.springframework.beans.factory.ObjectProvider<TenantDsLazyInitializer> lazyInitProvider;

    /** 可空：租户数据源注册器（同库别名判定 / 本地注册检查），与拦截器同为懒解析避免装配顺序敏感 */
    private final org.springframework.beans.factory.ObjectProvider<TenantDsRegistry> registryProvider;

    private volatile TenantDsRegistry registry;

    /** 可空：dynamic-datasource（判定 platform 数据源是否注册，注册才切平台控制面库） */
    private final org.springframework.beans.factory.ObjectProvider<DynamicRoutingDataSource> routingProvider;

    /** 平台控制面库数据源 key（mse.tenant.schema.platform-ds，缺省 platform） */
    private final String platformDsKey;

    /** 事务绑定库不一致处置（fail=快速报错 / warn=仅告警；mse.tenant.tx-ds-mismatch） */
    private final String txDsMismatchMode;

    public TenantDsExecutorInterceptor(org.springframework.beans.factory.ObjectProvider<TenantRouteResolver> routeResolverProvider,
                                       String defaultServiceName,
                                       Map<String, String> serviceMappings,
                                       TenantServiceModuleRegistry moduleRegistry,
                                       org.springframework.beans.factory.ObjectProvider<TenantDsLazyInitializer> lazyInitProvider,
                                       org.springframework.beans.factory.ObjectProvider<TenantDsRegistry> registryProvider,
                                       org.springframework.beans.factory.ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                       String platformDsKey,
                                       String txDsMismatchMode) {
        this.routeResolverProvider = routeResolverProvider;
        this.defaultServiceName = defaultServiceName == null || defaultServiceName.isBlank()
                ? "*" : defaultServiceName.trim();
        this.serviceMappings = serviceMappings == null ? Map.of() : serviceMappings;
        this.moduleRegistry = moduleRegistry;
        this.lazyInitProvider = lazyInitProvider;
        this.registryProvider = registryProvider;
        this.routingProvider = routingProvider;
        this.platformDsKey = platformDsKey == null || platformDsKey.isBlank() ? "platform" : platformDsKey.trim();
        this.txDsMismatchMode = "warn".equalsIgnoreCase(txDsMismatchMode) ? "warn" : "fail";
    }

    /** 注册器懒获取（volatile 双检）。与 {@link #resolver()} 同理：Bean 未就绪时不缓存 null，避免状态永久化 */
    private TenantDsRegistry registry() {
        TenantDsRegistry resolved = registry;
        if (resolved == null) {
            synchronized (this) {
                resolved = registry;
                if (resolved == null) {
                    resolved = registryProvider.getIfAvailable();
                    if (resolved != null) {
                        registry = resolved;
                    }
                }
            }
        }
        return resolved;
    }

    /**
     * 解析器懒获取（volatile 双检）。<b>只在拿到真实 Bean 时缓存</b>：启动早期 SQL（如
     * Flowable 引擎初始化）可能早于 service-tenant 装配执行，此时缓存兜底实现会把
     * 「Bean 未就绪」永久化——之后整个进程路由静默失效（全部落主库 + 字段隔离），绝不缓存兜底。
     */
    private TenantRouteResolver resolver() {
        TenantRouteResolver resolved = routeResolver;
        if (resolved == null) {
            synchronized (this) {
                resolved = routeResolver;
                if (resolved == null) {
                    resolved = routeResolverProvider.getIfAvailable();
                    if (resolved == null) {
                        // 未就绪：按内置共享库缺省（字段隔离），下次语句重试获取真实解析器；
                        // 未配置内置共享库时快速失败——primary 已是平台库，静默落过去=错库写
                        return (tenantId, serviceName) -> {
                            TenantDsRegistry early = registry();
                            String builtinKey = early == null ? null : early.builtinKey();
                            if (builtinKey == null) {
                                throw new IllegalStateException("路由解析器未就绪且本服务未配置内置共享库"
                                        + "（mse.tenant.shared-main.*），拒绝执行租户语句（防落平台库错库）: tenant=" + tenantId);
                            }
                            return TenantRoute.sharedDefault(tenantId, serviceName, builtinKey);
                        };
                    }
                    routeResolver = resolved;
                }
            }
        }
        return resolved;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 平台控制面库路由：①PlatformBaseEntity 实体（任意会话，先于租户上下文判断——平台会话的
        // CRUD 也要进平台库）；②平台会话（tenantId=0）的全部语句——平台（租户 0）世界以平台库为
        // 专属库：路由快照按独立库语义装配，语句免 tenant_id 注入（库即世界私有）。
        // platform 数据源未注册（未接入平台库路由的环境）则落空、走后续正常逻辑（降级主库 + tenant_id=0
        // 字段隔离）；runIgnore 保持原语义（不路由不改写，跨租户操作自带条件自定归属库）。
        String platformDs = registeredPlatformDs();
        if (platformDs != null) {
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            EntityRouteInfo info = entityInfoCache.computeIfAbsent(mapperName(ms), this::detectEntityInfo);
            Long currentTenant = TenantContext.getTenantId();
            boolean platformSession = currentTenant != null && currentTenant == 0L
                    && !TenantContext.isIgnoreTenant();
            if (info.platform() || platformSession) {
                TenantRoute platformRoute = new TenantRoute(vip.mseapp.core.constant.MseConstants.PLATFORM_TENANT_ID,
                        "*", TenantIsolationMode.INDEPENDENT_DB, platformDs, null, "tenant_id", null);
                TenantRoute previousRoute = TenantRouteContextHolder.set(platformRoute);
                LogContext.setDs(platformDs);
                DynamicDataSourceContextHolder.push(platformDs);
                try {
                    return invocation.proceed();
                } finally {
                    DynamicDataSourceContextHolder.poll();
                    LogContext.removeDs();
                    TenantRouteContextHolder.set(platformRoute, previousRoute);
                }
            }
        }
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null || TenantContext.isIgnoreTenant()) {
            // 显式忽略隔离（runIgnore）：0.1.0 起 primary=平台库、无静态默认租户库——
            // 跨租户全局视图/全局扫描（AGENTS.md 合法场景之二）按文档语义落「缺省共享库」，
            // 语句自带显式 tenant 条件自定归属；平台登记源语句（PlatformBaseEntity 实体）
            // 已在上方平台分支路由平台库，不经此分支。未配置内置共享库保持原语义（直落 primary）。
            if (TenantContext.isIgnoreTenant() && DynamicDataSourceContextHolder.peek() == null) {
                // 已有显式 push（如 MesAndonService 按数据源遍历扫描）时尊重调用方的选择，不覆盖
                TenantDsRegistry registry = registry();
                String builtinKey = registry == null ? null : registry.builtinKey();
                if (builtinKey != null) {
                    LogContext.setDs(builtinKey);
                    DynamicDataSourceContextHolder.push(builtinKey);
                    try {
                        return invocation.proceed();
                    } finally {
                        DynamicDataSourceContextHolder.poll();
                        LogContext.removeDs();
                    }
                }
            }
            // 平台/无租户上下文：与现状一致，不路由、不改写（行级插件自行快速失败）
            return invocation.proceed();
        }
        if (RESOLVING.get()) {
            // 路由解析自身的查询：不二次路由（此时路由快照缺席，行级插件/分表改写按缺省处理）
            return invocation.proceed();
        }
        TenantRoute route;
        String serviceName;
        RESOLVING.set(Boolean.TRUE);
        try {
            serviceName = resolveServiceName(invocation);
            route = effectiveRoute(tenantId, serviceName, resolver().resolveRoute(tenantId, serviceName));
            log.debug("[mse-tenant-route] 语句路由: tenant={} service={} mapper={} mode={} ds={}",
                    tenantId, serviceName, mapperName((MappedStatement) invocation.getArgs()[0]),
                    route.getMode(), route.getDsKey());
        } finally {
            RESOLVING.remove();
        }
        TenantRoute previous = TenantRouteContextHolder.set(route);
        try {
            String dsKey = decideDsKey(invocation, route);
            if (dsKey == null) {
                return invocation.proceed();
            }
            verifyTransactionDsKey(dsKey, invocation, serviceName);
            // 动态开通链路第 3 级：首次路由到未初始化的租户库时补齐表结构（幂等，失败不阻断）
            lazyInitProvider.ifAvailable(initializer -> initializer.ensureInitialized(dsKey, route));
            // 语句级日志标注：窗口内的日志行携带 %X{ds}，精确表示这条 SQL 实际连的库
            LogContext.setDs(dsKey);
            DynamicDataSourceContextHolder.push(dsKey);
            try {
                return invocation.proceed();
            } finally {
                DynamicDataSourceContextHolder.poll();
                LogContext.removeDs();
            }
        } catch (Throwable t) {
            throw ExceptionUtil.unwrapThrowable(t);
        } finally {
            TenantRouteContextHolder.set(route, previous);
        }
    }

    /**
     * 绑定库不可本地执行时的统一降级（降级 = 内置共享库 + tenant_id 行级隔离，绝不裸奔）：
     * <ul>
     *   <li>内置共享库同库别名：同库物理上承载多租户数据，必须收敛到内置池字段隔离执行；</li>
     *   <li>未本地注册：服务作用域外或元数据未同步，dynamic-datasource strict=false 会静默
     *       回落 primary（=平台库）——先在此显式降级，保证字段隔离仍然生效。</li>
     * </ul>
     * 未配置内置共享库时快速失败——不存在安全落点。
     */
    private TenantRoute effectiveRoute(Long tenantId, String serviceName, TenantRoute route) {
        if (!route.routeDatasource()) {
            return route;
        }
        TenantDsRegistry registry = registry();
        if (registry == null) {
            return route;
        }
        return downgradeRoute(registry, tenantId, serviceName, route);
    }

    /** 降级决策（包内可测）：绑定库不可本地执行时返回内置共享库快照（字段隔离），否则原样返回；未配置内置库快速失败 */
    static TenantRoute downgradeRoute(TenantDsRegistry registry, Long tenantId, String serviceName, TenantRoute route) {
        if (!route.routeDatasource()) {
            return route;
        }
        String dsKey = route.getDsKey();
        String builtinKey = registry.builtinKey();
        if (registry.isMainDbAlias(dsKey)) {
            log.info("[mse-tenant-ds] 租户绑定数据源与内置共享库同库，收敛内置池字段隔离执行: tenant={} service={} ds={}",
                    tenantId, serviceName, dsKey);
            return TenantRoute.sharedDefault(tenantId, serviceName, builtinKey);
        }
        if (!registry.isRegistered(dsKey)) {
            if (builtinKey == null) {
                throw new IllegalStateException("租户绑定数据源未在本服务注册且本服务未配置内置共享库"
                        + "（mse.tenant.shared-main.*），拒绝静默落平台库: tenant=" + tenantId
                        + " service=" + serviceName + " ds=" + dsKey);
            }
            log.warn("[mse-tenant-ds] 租户绑定数据源未在本服务注册（服务作用域外或未同步），收敛内置共享库字段隔离: tenant={} service={} ds={}",
                    tenantId, serviceName, dsKey);
            return TenantRoute.sharedDefault(tenantId, serviceName, builtinKey);
        }
        return route;
    }

    /**
     * 事务绑定库一致性护栏：本地事务在 doBegin 已按「应用缺省服务」把物理连接钉死在某库，
     * 事务内语句按 mapper 所属服务解析的目标库若与之不同，会静默复用绑定连接落错库
     * （单体聚合多模块、应用缺省服务与语句所属服务指向不同库的场景）——不一致默认快速报错
     * （AGENTS 红线：一次本地事务只绑一个库，跨库须拆分方法/去事务化/@GlobalTransactional），
     * {@code mse.tenant.tx-ds-mismatch=warn} 可降级为仅告警（升级观察期用）。
     * 无事务 / 事务未经 TenantRouteTransactionManager 绑定（无记录）时跳过，行为不变。
     */
    private void verifyTransactionDsKey(String dsKey, Invocation invocation, String serviceName) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        Object bound = org.springframework.transaction.support.TransactionSynchronizationManager
                .getResource(TenantRouteTransactionManager.BOUND_DS_RESOURCE_KEY);
        String mapper = mapperName((MappedStatement) invocation.getArgs()[0]);
        String mismatch = transactionDsMismatch(
                bound instanceof TenantRouteTransactionManager.BoundDs boundDs ? boundDs : null,
                dsKey, serviceName, mapper);
        if (mismatch == null) {
            return;
        }
        if ("warn".equals(txDsMismatchMode)) {
            log.warn("[mse-tenant-ds] {}", mismatch);
            return;
        }
        throw new IllegalStateException(mismatch);
    }

    /**
     * 事务绑定库一致性判定（包内可测）：null=一致或不适用（事务未绑定/同库）；非空=错误消息。
     * 无事务 / 事务未经 TenantRouteTransactionManager 绑定（无记录）时跳过，行为不变。
     */
    static String transactionDsMismatch(TenantRouteTransactionManager.BoundDs bound, String statementDsKey,
                                        String statementService, String mapper) {
        if (bound == null || bound.dsKey().equals(statementDsKey)) {
            return null;
        }
        return "本地事务绑定库与语句目标库不一致（事务连接已钉死在绑定库，语句将静默落错库）："
                + "事务绑定 " + bound.dsKey() + "（服务 " + bound.serviceName() + "，doBegin 按应用缺省服务解析）"
                + " ≠ 语句目标 " + statementDsKey + "（服务 " + statementService + "，mapper " + mapper + "）"
                + "——一次本地事务只绑一个库：请拆分方法、去事务化走语句级路由，或改用 @GlobalTransactional";
    }

    private String decideDsKey(Invocation invocation, TenantRoute route) {
        if (!route.routeDatasource()) {
            // dsKey=null 的共享路由（未绑定租户/无服务配置）= 落本服务内置共享库（0.1.0 v2 语义，
            // 显式声明的「本服务主库」）：未配置内置库时快速失败（primary=平台库不得承接租户语句）
            if (route.getMode() == TenantIsolationMode.SHARED_FIELD) {
                TenantDsRegistry registry = registry();
                String builtinKey = registry == null ? null : registry.builtinKey();
                if (builtinKey == null) {
                    throw new IllegalStateException("共享租户语句需要内置共享库：请配置 mse.tenant.shared-main.*"
                            + "（tenant=" + TenantContext.getTenantId() + "）");
                }
                return builtinKey;
            }
            return null;
        }
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        EntityRouteInfo info = entityInfoCache.computeIfAbsent(mapperName(ms), this::detectEntityInfo);
        if (info.platform()) {
            // 平台控制面表（sys_tenant 系）只存平台库：租户库无副本，不随租户路由（落本服务主库；
            // 主库即平台库的服务天然正确，其余服务经路由快照缺席的 RESOLVING 通道访问）
            return null;
        }
        // 全部租户域表随租户路由到绑定库——含 sys_menu/sys_permission/sys_micro_app 租户域全局表
        // （BASE 通道车队重放的租户库自持副本）：独立库=整库私有，共享池=tenant_id 行级隔离
        return route.getDsKey();
    }

    /**
     * 语句归属服务解析（优先级递减）：yaml service-mappings 显式映射（逃生舱）→
     * {@link TenantServiceModule} 注解推导（模块自带身份，零配置接入）→ 缺省服务名
     * （service-name → spring.application.name → '*'）。两级均为最长前缀匹配。
     */
    private String resolveServiceName(Invocation invocation) {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        String mapperName = mapperName(ms);
        String best = null;
        for (Map.Entry<String, String> entry : serviceMappings.entrySet()) {
            String prefix = entry.getValue() == null ? "" : entry.getValue().trim();
            if (!prefix.isEmpty() && mapperName.startsWith(prefix)
                    && (best == null || prefix.length() > serviceMappings.get(best).length())) {
                best = entry.getKey();
            }
        }
        if (best != null) {
            return best;
        }
        if (moduleRegistry != null) {
            String annotated = moduleRegistry.resolveServiceName(mapperName);
            if (annotated != null) {
                return annotated;
            }
        }
        return defaultServiceName;
    }

    private String mapperName(MappedStatement ms) {
        String id = ms.getId();
        return id.substring(0, id.lastIndexOf('.'));
    }

    /** platform 数据源已注册时返回其 key，否则 null（懒解析，避免装配顺序敏感） */
    private String registeredPlatformDs() {
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        if (routing == null) {
            return null;
        }
        return routing.getDataSources().containsKey(platformDsKey) ? platformDsKey : null;
    }

    private EntityRouteInfo detectEntityInfo(String mapperName) {
        try {
            Class<?> mapper = Class.forName(mapperName, false, getClass().getClassLoader());
            for (Type generic : mapper.getGenericInterfaces()) {
                if (generic instanceof ParameterizedType parameterized
                        && BaseMapper.class.equals(parameterized.getRawType())) {
                    Type arg = parameterized.getActualTypeArguments()[0];
                    if (arg instanceof Class<?> entity) {
                        return new EntityRouteInfo(GlobalBaseEntity.class.isAssignableFrom(entity),
                                PlatformBaseEntity.class.isAssignableFrom(entity));
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
            // 非 BaseMapper 泛型 mapper：不参与路由
        }
        return EntityRouteInfo.NONE;
    }

    /** 实体路由信息（global=无 tenant_id 的全局表实体；platform=平台控制面实体——只存平台库，不随租户路由） */
    private record EntityRouteInfo(boolean global, boolean platform) {
        static final EntityRouteInfo NONE = new EntityRouteInfo(false, false);
    }

    @Override
    public Object plugin(Object target) {
        return target instanceof Executor ? Plugin.wrap(target, this) : target;
    }

    @Override
    public void setProperties(Properties properties) {
        // no-op
    }
}
