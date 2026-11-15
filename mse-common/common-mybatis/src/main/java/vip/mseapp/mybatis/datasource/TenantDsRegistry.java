package vip.mseapp.mybatis.datasource;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import vip.mseapp.core.crypto.AesCipher;
import vip.mseapp.mybatis.config.MseTenantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 动态租户数据源注册器：sys_tenant_datasource（界面维护的租户库）⇄ dynamic-datasource 运行时注册。
 *
 * <p>职责：
 * <ul>
 *   <li>启动全量注册（{@link #registerAll}）：经元数据通道（本地 JDBC / RPC，见
 *       {@link TenantDsMetadataProvider}）读取本服务相关行建池注册（lazy，不启动即连）；
 *       表缺失/通道不可用仅告警跳过，单条失败不阻断启动；</li>
 *   <li>运行时增删（{@link #register}/{@link #unregister}）：同 key 覆盖 yaml 静态条目并关闭旧池；
 *       管理端（service-tenant）变更后调用，并经 Redis 广播通知其他进程；</li>
 *   <li>远端变更应用（{@link #onRemoteChange}）：收到广播后经元数据通道回查再本地应用，
 *       消息不含凭据；</li>
 *   <li>连通性测试（{@link #testConnection}）。</li>
 * </ul>
 *
 * <p>平台库（primary）仍由静态配置承载；yaml 中静态声明的租户数据源条目继续兼容（双轨），
 * 同 key 时以动态注册为准。所有依赖经 ObjectProvider 延迟解析（首次使用时才要求存在），
 * 消除自动装配顺序敏感性。
 *
 * <p>内置缺省共享库（0.1.0 v2）：每服务从自己的配置（{@code mse.tenant.shared-main.*}）自declare
 * 一个内置池——键默认=本服务应用名（服务私有共享库），显式 {@code key:'*'}=通用。启动装载
 * {@link #registerBuiltin()} 注册（不广播：各服务自读自配）；路由兜底（dsKey=null 语义=落本服务
 * 内置共享库）与同库判定基准共用该定义。未配置坐标的服务无内置池——共享租户语句快速失败
 * （防 strict=false 静默落 primary=平台库）。
 *
 * <p>同库绑定：数据源记录目标与内置共享库物理同库时不建独立池（{@link #isMainDbAlias}），
 * 路由命中收敛到内置池按字段隔离执行——同库物理上承载多租户数据，绝不能按独立库免过滤执行，
 * 也不能再建第二个同库池。
 */
public class TenantDsRegistry {

    private static final Logger log = LoggerFactory.getLogger(TenantDsRegistry.class);

    /** 变更广播 topic（消息体：action:dsKey，action ∈ upsert/remove；凭据不进消息，各进程回查） */
    public static final String TOPIC = "mse:topic:tenant-ds";

    private final ObjectProvider<DynamicRoutingDataSource> routingProvider;

    private final ObjectProvider<com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator> creatorProvider;

    private final MseTenantProperties.DynamicDs dynamicDs;

    private final TenantDsMetadataProvider metadataProvider;

    /** 本服务名（元数据拉取过滤 service_name ∈ (本服务, '*')；'*'=全量，单体语义） */
    private final String serviceName;

    /** 可空：无 Redis（或未装配）时变更仅本进程生效 */
    private final ObjectProvider<RedisConnectionFactory> connectionFactoryProvider;

    /** 已应用定义签名（dsKey → target|username|password密文|status）：同签名重复注册不重建池、不广播（打断广播回声回环） */
    private final java.util.Map<String, String> appliedSignatures = new java.util.concurrent.ConcurrentHashMap<>();

    /** 与缺省共享库同库的绑定 key：不建池，路由命中时收敛到缺省共享池字段隔离（见类注释） */
    private final java.util.Set<String> mainDbAliasKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** 内置共享库定义（配置自declare；启动装载注册，进程内不变） */
    private volatile TenantDatasourceDefinition builtinSharedMain;


    /** 可空：模块服务身份收集器（@TenantServiceModule）——单体聚合按模块服务名并集加载数据源 */
    private final TenantServiceModuleRegistry moduleRegistry;

    public TenantDsRegistry(ObjectProvider<DynamicRoutingDataSource> routingProvider,
                            ObjectProvider<com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator> creatorProvider,
                            MseTenantProperties.DynamicDs dynamicDs,
                            String serviceName,
                            TenantDsMetadataProvider metadataProvider,
                            ObjectProvider<RedisConnectionFactory> connectionFactoryProvider,
                            TenantServiceModuleRegistry moduleRegistry) {
        this(routingProvider, creatorProvider, dynamicDs, serviceName, metadataProvider,
                connectionFactoryProvider, moduleRegistry, null);
    }

    public TenantDsRegistry(ObjectProvider<DynamicRoutingDataSource> routingProvider,
                            ObjectProvider<com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator> creatorProvider,
                            MseTenantProperties.DynamicDs dynamicDs,
                            String serviceName,
                            TenantDsMetadataProvider metadataProvider,
                            ObjectProvider<RedisConnectionFactory> connectionFactoryProvider,
                            TenantServiceModuleRegistry moduleRegistry,
                            MseTenantProperties.SharedMain sharedMain) {
        this.routingProvider = routingProvider;
        this.creatorProvider = creatorProvider;
        this.dynamicDs = dynamicDs;
        this.serviceName = serviceName == null || serviceName.isBlank() ? "*" : serviceName.trim();
        this.metadataProvider = metadataProvider;
        this.connectionFactoryProvider = connectionFactoryProvider;
        this.moduleRegistry = moduleRegistry;
        if (sharedMain != null && sharedMain.getUrl() != null && !sharedMain.getUrl().isBlank()
                && sharedMain.getUsername() != null && !sharedMain.getUsername().isBlank()) {
            String key = sharedMain.getKey() == null || sharedMain.getKey().isBlank()
                    ? this.serviceName : sharedMain.getKey().trim();
            TenantDatasourceDefinition def = new TenantDatasourceDefinition();
            def.setDsKey(key);
            def.setDsMode("shared");
            def.setServiceName(this.serviceName);
            def.setDbType(sharedMain.getDbType() == null || sharedMain.getDbType().isBlank()
                    ? "mysql" : sharedMain.getDbType().trim());
            String[] coords = parseCoords(sharedMain.getUrl());
            def.setHost(coords[0]);
            def.setPort(Integer.valueOf(coords[1]));
            def.setDbName(coords[2]);
            def.setUsername(sharedMain.getUsername());
            // 配置明文加密一次（AES-GCM 随机 IV：进程内只此一份密文，签名去重稳定）
            def.setPassword(AesCipher.encrypt(sharedMain.getPassword(),
                    AesCipher.parseKey(dynamicDs.getEncryptKey())));
            def.setStatus(0);
            builtinSharedMain = def;
        }
    }

    /** JDBC URL → [host, port, dbName]（内置共享库坐标解析） */
    private static String[] parseCoords(String url) {
        int scheme = url.indexOf("//") + 2;
        int slash = url.indexOf('/', scheme);
        String hostPort = url.substring(scheme, slash > 0 ? slash : url.length());
        int colon = hostPort.indexOf(':');
        String host = colon > 0 ? hostPort.substring(0, colon) : hostPort;
        String port = colon > 0 ? hostPort.substring(colon + 1) : "3306";
        int dot = url.indexOf('?', slash);
        String dbName = slash > 0 ? url.substring(slash + 1, dot > 0 ? dot : url.length()) : "";
        return new String[]{host, port, dbName};
    }

    /** 启动装载内置共享库池（幂等：已注册/未配置跳过；不广播——各服务自读自配） */
    public void registerBuiltin() {
        if (builtinSharedMain == null) {
            return;
        }
        try {
            register(builtinSharedMain, routing(), false);
        } catch (Exception e) {
            log.warn("[mse-tenant-ds] 内置共享库注册失败（语句路由时可见）: key={} {}", builtinKey(), e.getMessage());
        }
    }

    /** 内置共享库键（配置 key 或本服务应用名）；未配置内置库返回 null */
    public String builtinKey() {
        return builtinSharedMain == null ? null : builtinSharedMain.getDsKey();
    }

    /** 内置共享库定义（未配置返回 null） */
    public TenantDatasourceDefinition builtinSharedMain() {
        return builtinSharedMain;
    }

    /** 启动全量注册：通道/路由器不可用或表缺失仅告警（存量库未升级），单条失败不阻断启动 */
    public void registerAll() {
        List<TenantDatasourceDefinition> definitions;
        try {
            definitions = metadataProvider.listDefinitions(loadServiceNames());
        } catch (Exception e) {
            log.warn("[mse-tenant-ds] 元数据读取失败，跳过动态租户数据源加载: {}", e.getMessage());
            return;
        }
        DynamicRoutingDataSource routing;
        try {
            routing = routing();
        } catch (IllegalStateException e) {
            log.warn("[mse-tenant-ds] {}，跳过动态租户数据源加载", e.getMessage());
            return;
        }
        int ok = 0;
        for (TenantDatasourceDefinition def : definitions) {
            try {
                register(def, routing, false);
                ok++;
            } catch (Exception e) {
                log.warn("[mse-tenant-ds] 注册租户数据源失败（该库暂以平台库兜底，strict=false 语义）: key={} target={}",
                        def.getDsKey(), def.target(), e);
            }
        }
        log.info("[mse-tenant-ds] 动态租户数据源注册完成: {}/{}（keys={}）", ok, definitions.size(), registeredKeys());
    }

    /** 元数据加载服务名集合：本服务名 ∪ 模块注解服务名（'*' 由查询自身兜底包含，无需显式加入） */
    private java.util.Collection<String> loadServiceNames() {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        names.add(serviceName);
        if (moduleRegistry != null) {
            names.addAll(moduleRegistry.serviceNames());
        }
        return names;
    }

    /** 运行时注册（幂等）：建 lazy 池 → addDataSource（同 key 覆盖静态/旧池并关闭）→ 广播 upsert */
    public void register(TenantDatasourceDefinition def) {
        register(def, routing(), true);
    }

    private void register(TenantDatasourceDefinition def, DynamicRoutingDataSource routing, boolean broadcast) {
        String dsKey = def.getDsKey();
        if (dsKey == null || dsKey.isBlank()) {
            throw new IllegalArgumentException("dsKey 不能为空");
        }

        String signature = def.target() + "|" + def.getUsername() + "|" + def.getPassword() + "|" + def.getStatus();
        // 内置共享库同库绑定：不建独立池，仅记别名标记（路由侧收敛到内置池字段隔离，
        // 绝不能按独立库免过滤执行，也不能对同一物理库建第二个池）——内置行自身除外
        TenantDatasourceDefinition shared = builtinSharedMain;
        if (shared != null && !shared.getDsKey().equals(dsKey)
                && TenantDsUrlBuilder.sameTarget(def, TenantDsUrlBuilder.build(shared))) {
            if (signature.equals(appliedSignatures.get(dsKey))) {
                return;
            }
            mainDbAliasKeys.add(dsKey);
            appliedSignatures.put(dsKey, signature);
            removePool(routing, dsKey);
            log.info("[mse-tenant-ds] 租户数据源与主库同库，按共享库语义执行（不建独立池）: key={} service={} target={}",
                    dsKey, def.getServiceName(), def.target());
            if (broadcast) {
                publish("upsert", dsKey);
            }
            return;
        }
        mainDbAliasKeys.remove(dsKey);
        if (signature.equals(appliedSignatures.get(dsKey)) && routing.getDataSources().containsKey(dsKey)) {
            // 定义未变化的重复注册（广播回声/重复加载）：不重建池、不广播，避免关池抖动与消息回环
            log.debug("[mse-tenant-ds] 租户数据源定义未变化，跳过重复注册: key={}", dsKey);
            return;
        }
        DataSourceProperty property = new DataSourceProperty();
        property.setPoolName("mse-tenant-ds-" + dsKey);
        property.setUrl(TenantDsUrlBuilder.build(def));
        property.setUsername(def.getUsername());
        property.setPassword(decrypt(def.getPassword()));
        // 租户库按需建连：启动不触碰各租户库，首次路由时才建池
        property.setLazy(Boolean.TRUE);
        // 每库小池（防总连接数爆炸）：Hikari 显式上限；Druid 同参数语义映射
        com.baomidou.dynamic.datasource.creator.hikaricp.HikariCpConfig hikari =
                new com.baomidou.dynamic.datasource.creator.hikaricp.HikariCpConfig();
        hikari.setMaxPoolSize(dynamicDs.getMaxPoolSize());
        hikari.setMinIdle(dynamicDs.getMinIdle());
        hikari.setIdleTimeout(dynamicDs.getIdleTimeoutMs());
        hikari.setMaxLifetime(dynamicDs.getMaxLifetimeMs());
        hikari.setConnectionTimeout(dynamicDs.getConnectionTimeoutMs());
        property.setHikari(hikari);
        DataSource created = creator().createDataSource(property);
        DataSource old = routing.getDataSources().get(dsKey);
        routing.addDataSource(dsKey, created);
        closeQuietly(old);
        appliedSignatures.put(dsKey, signature);
        log.info("[mse-tenant-ds] 租户数据源已注册: key={} service={} target={} mode={}",
                dsKey, def.getServiceName(), def.target(), def.getDsMode());
        if (broadcast) {
            publish("upsert", dsKey);
        }
    }

    /** 运行时注销：移除并关闭连接池 → 广播 remove（路由侧 strict=false 时该 key 回落平台库） */
    public void unregister(String dsKey) {
        unregister(dsKey, true);
    }

    private void unregister(String dsKey, boolean broadcast) {
        DataSource removed = routing().getDataSources().remove(dsKey);
        appliedSignatures.remove(dsKey);
        mainDbAliasKeys.remove(dsKey);
        if (removed == null) {
            return;
        }
        closeQuietly(removed);
        log.info("[mse-tenant-ds] 租户数据源已注销: key={}", dsKey);
        if (broadcast) {
            publish("remove", dsKey);
        }
    }

    /**
     * 远端变更应用：upsert → 经元数据通道回查该行（启用则注册，停用/已删则注销）；remove → 注销。
     * 回查式设计幂等且凭据不进消息。应用动作本身不再广播（消息已来自总线，再广播即回声回环）。
     */
    public void onRemoteChange(String action, String dsKey) {
        try {
            if ("remove".equals(action)) {
                unregister(dsKey, false);
                return;
            }
            TenantDatasourceDefinition def = metadataProvider.getDefinition(dsKey);
            if (def == null || Integer.valueOf(1).equals(def.getStatus())) {
                unregister(dsKey, false);
            } else {
                register(def, routing(), false);
            }
        } catch (Exception e) {
            log.warn("[mse-tenant-ds] 应用远端变更失败: action={} key={}", action, dsKey, e);
        }
    }

    /** 按 dsKey 经元数据通道回查定义（管理端展示/校验用）；不存在返回 null */
    public TenantDatasourceDefinition loadDefinition(String dsKey) {
        return metadataProvider.getDefinition(dsKey);
    }

    /** 是否已在本地 dynamic-datasource 注册（含 yaml 静态条目）；路由器未装配返回 false */
    public boolean isRegistered(String dsKey) {
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        return routing != null && dsKey != null && routing.getDataSources().containsKey(dsKey);
    }

    /** 该 key 是否绑定到与主库相同的数据库（不建池；路由侧须降级共享库字段隔离语义） */
    public boolean isMainDbAlias(String dsKey) {
        return dsKey != null && mainDbAliasKeys.contains(dsKey);
    }

    /** 移除并关闭既有真实池（主库同库别名化时收编） */
    private void removePool(DynamicRoutingDataSource routing, String dsKey) {
        DataSource old = routing.getDataSources().remove(dsKey);
        if (old != null) {
            closeQuietly(old);
            log.info("[mse-tenant-ds] 已移除同库数据源的独立连接池: key={}", dsKey);
        }
    }

    /** 已注册 keys（路由器未装配返回空集） */
    public Set<String> registeredKeys() {
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        return routing == null ? new LinkedHashSet<>() : new LinkedHashSet<>(routing.getDataSources().keySet());
    }

    /** 连通性测试：短连接 SELECT 1（5s 登录超时），返回耗时毫秒；失败抛 IllegalStateException */
    public long testConnection(TenantDatasourceDefinition def) {
        String url = TenantDsUrlBuilder.build(def);
        int old = DriverManager.getLoginTimeout();
        DriverManager.setLoginTimeout(5);
        try (Connection conn = DriverManager.getConnection(url, def.getUsername(), decrypt(def.getPassword()));
             Statement stmt = conn.createStatement()) {
            long start = System.currentTimeMillis();
            stmt.execute("SELECT 1");
            return System.currentTimeMillis() - start;
        } catch (SQLException e) {
            throw new IllegalStateException("连接失败: " + e.getMessage(), e);
        } finally {
            DriverManager.setLoginTimeout(old);
        }
    }

    /** 路由器延迟解析：不存在给出明确文案（装配顺序不敏感的关键） */
    private DynamicRoutingDataSource routing() {
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("dynamic-datasource 路由器未装配（无数据源配置的服务不可用动态租户数据源）");
        }
        return routing;
    }

    private com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator creator() {
        com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator creator = creatorProvider.getIfAvailable();
        if (creator == null) {
            creator = new com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator();
        }
        return creator;
    }

    public String decrypt(String stored) {
        return AesCipher.decrypt(stored, AesCipher.parseKey(dynamicDs.getEncryptKey()));
    }

    private void publish(String action, String dsKey) {
        RedisConnectionFactory factory = connectionFactoryProvider.getIfAvailable();
        if (factory == null) {
            return;
        }
        String payload = action + ":" + dsKey;
        try (var connection = factory.getConnection()) {
            connection.publish(TOPIC.getBytes(StandardCharsets.UTF_8), payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("[mse-tenant-ds] 变更广播失败（不影响本进程）: action={} key={}", action, dsKey, e);
        }
    }

    private void closeQuietly(DataSource dataSource) {
        if (dataSource instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.debug("[mse-tenant-ds] 关闭旧连接池失败（忽略）", e);
            }
        }
    }
}
