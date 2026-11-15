package vip.mseapp.mybatis.datasource;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import vip.mseapp.core.crypto.AesCipher;
import vip.mseapp.core.datasource.TablePolicy;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.mybatis.config.MseTenantProperties;
import vip.mseapp.mybatis.schema.MigrateOptions;
import vip.mseapp.mybatis.schema.MigrationTarget;
import vip.mseapp.mybatis.schema.SchemaMigration;
import vip.mseapp.mybatis.schema.SchemaOpsLock;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户库懒初始化（脚本直建）：首次路由命中某目标库时，按「自研 Flyway」方式对目标库执行
 * 版本化脚本建表——①TENANT 通道（基座身份表 + undo_log + 快照表）→ ②MASTER 通道（各积木
 * 业务表）→ ③BASE 通道（base 副本全量重放：子应用注册/页面菜单/权限点）→ ④每租户种子脚本
 * （${tenant_id}=本租户），替代原「从主库克隆表结构」机制。
 *
 * <p>直建语义：全新库（无业务表）全量脚本执行；克隆来源的存量库（有业务表、账本可能缺席）
 * 按主库对应账本当前版本打结构基线（只补 pending），与克隆时代的库自然兼容。脚本幂等
 * （IF NOT EXISTS / NOT EXISTS）+ {@code mse_<biz>_schema_history} 账本双保险。
 *
 * <p>并发控制：进程内 per-key 串行 + 统一分布式锁 {@link vip.mseapp.mybatis.schema.SchemaOpsLock}
 * 按目标库 URL 互斥（与启动迁移/管理端手动初始化/结构同步/临时 SQL 同一把锁）；无 Redis 时退化
 * 为单机串行。失败不标记完成，下次请求重试；初始化失败不阻断请求（SQL 会因缺表自然报错，问题可见）。
 */
public class TenantDsLazyInitializer {

    private static final Logger log = LoggerFactory.getLogger(TenantDsLazyInitializer.class);

    /**
     * 初始化锁 TTL：三通道全量脚本 + 目录追平 + 每租户种子耗时不可控（新库全量建表可达分钟级），
     * 与启动迁移/每租户迁移同档（600s）；其他实例持锁时本请求跳过放行（不轮询等待）。
     */
    private static final Duration LOCK_TTL = Duration.ofSeconds(600);

    private final TenantDsMetadataProvider metadataProvider;

    private final ObjectProvider<DynamicRoutingDataSource> routingProvider;

    private final String primaryDsKey;

    private final MseTenantProperties.DynamicDs dynamicDs;

    private final MseTenantProperties tenantProperties;

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private final ObjectProvider<TenantSchemaMigrator> schemaMigratorProvider;

    /** 定义目录变更批次来源（追平平台端目录编辑；实现缺席时跳过，车队扇出兜底） */
    private final ObjectProvider<CatalogChangeSource> catalogChangeSourceProvider;

    /** 租户数据源注册表（内置共享库定义兜底解析用；未装配时无兜底） */
    private final ObjectProvider<TenantDsRegistry> registryProvider;

    private final Set<String> initialized = ConcurrentHashMap.newKeySet();

    private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

    public TenantDsLazyInitializer(TenantDsMetadataProvider metadataProvider,
                                   ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                   String primaryDsKey,
                                   MseTenantProperties tenantProperties,
                                   ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                   ObjectProvider<TenantSchemaMigrator> schemaMigratorProvider) {
        this(metadataProvider, routingProvider, primaryDsKey, tenantProperties, redisTemplateProvider,
                schemaMigratorProvider, null);
    }

    public TenantDsLazyInitializer(TenantDsMetadataProvider metadataProvider,
                                   ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                   String primaryDsKey,
                                   MseTenantProperties tenantProperties,
                                   ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                   ObjectProvider<TenantSchemaMigrator> schemaMigratorProvider,
                                   ObjectProvider<CatalogChangeSource> catalogChangeSourceProvider) {
        this(metadataProvider, routingProvider, primaryDsKey, tenantProperties, redisTemplateProvider,
                schemaMigratorProvider, catalogChangeSourceProvider, null);
    }

    public TenantDsLazyInitializer(TenantDsMetadataProvider metadataProvider,
                                   ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                   String primaryDsKey,
                                   MseTenantProperties tenantProperties,
                                   ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                   ObjectProvider<TenantSchemaMigrator> schemaMigratorProvider,
                                   ObjectProvider<CatalogChangeSource> catalogChangeSourceProvider,
                                   ObjectProvider<TenantDsRegistry> registryProvider) {
        this.metadataProvider = metadataProvider;
        this.routingProvider = routingProvider;
        this.primaryDsKey = primaryDsKey == null || primaryDsKey.isBlank() ? "master" : primaryDsKey.trim();
        this.dynamicDs = tenantProperties.getDynamicDs();
        this.tenantProperties = tenantProperties;
        this.redisTemplateProvider = redisTemplateProvider;
        this.schemaMigratorProvider = schemaMigratorProvider;
        this.catalogChangeSourceProvider = catalogChangeSourceProvider;
        this.registryProvider = registryProvider;
    }

    /**
     * 确保目标库已初始化（幂等；进程内成功一次后直接返回）。
     *
     * @param dsKey 路由数据源 key（已注册进 dynamic-datasource）
     * @param route 当前路由快照（提供 SPLIT 表与后缀）
     */
    public void ensureInitialized(String dsKey, TenantRoute route) {
        if (initialized.contains(dsKey)) {
            return;
        }
        Object lock = keyLocks.computeIfAbsent(dsKey, k -> new Object());
        synchronized (lock) {
            if (initialized.contains(dsKey)) {
                return;
            }
            // 统一锁按目标库 JDBC URL 互斥（与启动迁移/管理端结构操作同一把）；定义解析失败时
            // 无 URL 可锁（不取锁直接走 doInit 抛出可读错误），URL 构建异常同理
            String lockKey = null;
            TenantDatasourceDefinition def = resolveDefinition(dsKey);
            if (def != null) {
                try {
                    lockKey = SchemaOpsLock.lockKeyFor(TenantDsUrlBuilder.build(def));
                } catch (Exception ignored) {
                    // URL 构建失败：交由 doInit 抛出可读错误
                }
            }
            String token = lockKey == null ? null
                    : SchemaOpsLock.tryAcquire(redisTemplateProvider.getIfAvailable(), lockKey, LOCK_TTL);
            if (token == null) {
                // 其他实例正在初始化：本次请求直接放行（缺表报错可见，稍后重试即成功）
                return;
            }
            try {
                doInit(dsKey, route);
                initialized.add(dsKey);
            } catch (Exception e) {
                log.warn("[mse-tenant-ds] 租户库懒初始化失败（下次请求重试）: key={}", dsKey, e);
            } finally {
                SchemaOpsLock.release(redisTemplateProvider.getIfAvailable(), lockKey, token);
            }
        }
    }

    /** 数据源定义解析（含内置共享库自declare兜底；查无返回 null） */
    private TenantDatasourceDefinition resolveDefinition(String dsKey) {
        TenantDatasourceDefinition def = metadataProvider.getDefinition(dsKey);
        if (def == null && registryProvider != null) {
            // 内置共享库（mse.tenant.shared-main.* 自declare）不落 sys_tenant_datasource 登记表，
            // 元数据通道恒查不到——按本服务内置定义直建（0.1.0 内置库语义，否则共享租户业务表永不就绪）
            TenantDsRegistry registry = registryProvider.getIfAvailable();
            if (registry != null && dsKey != null && dsKey.equals(registry.builtinKey())) {
                def = registry.builtinSharedMain();
                log.info("[mse-tenant-ds] 内置共享库按自declare定义直建: key={}", dsKey);
            }
        }
        return def;
    }

    /**
     * 可覆写扩展点：子类在直建后追加自身 schema（如 OA 的 Flowable ACT_ 引擎表脚本）。
     *
     * <p>执行序：TENANT 通道（身份结构）→ MASTER 通道（业务结构）→ BASE 通道（base 副本）→
     * SPLIT 分表改名 → 每租户种子。先身份后业务保证全新库在业务脚本执行时不会因已有业务表被
     * 误判「存量库」而错误基线（基线探针按 biz 自身建表判定，且扣除 TENANT 通道身份表）。
     */
    protected void doInit(String dsKey, TenantRoute route) throws Exception {
        TenantDatasourceDefinition def = resolveDefinition(dsKey);
        if (def == null) {
            throw new IllegalStateException("数据源定义不存在: " + dsKey);
        }
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("dynamic-datasource 未装配");
        }
        javax.sql.DataSource primary = routing.getDataSources().get(primaryDsKey);
        String password = AesCipher.decrypt(def.getPassword(), AesCipher.parseKey(dynamicDs.getEncryptKey()));
        ensureMysqlDatabaseExists(def, password);
        try (Connection master = (primary != null ? primary : routing).getConnection();
             Connection target = DriverManager.getConnection(TenantDsUrlBuilder.build(def),
                     def.getUsername(), password)) {
            migrateSchema(dsKey, master, target, def, route);
            applySplitRenames(target, def, route);
        }
    }

    /**
     * 缺库自举（仅 MySQL，仅懒初始化路径）：直连带库名的 JDBC URL 对不存在的库连接即失败
     * （Unknown database），三通道脚本永远无法执行、每次请求无限重试同一失败——先建库再走通道。
     * 不在 {@link TenantDsUrlBuilder} 全局追加 createDatabaseIfNotExist：连通性测试等调用方
     * 需要让「库名配错」显式失败，而非静默建库掩盖配置错误。dm/kingbase 建库属 DBA 侧动作，维持现状。
     */
    private void ensureMysqlDatabaseExists(TenantDatasourceDefinition def, String password) throws SQLException {
        if (!"mysql".equalsIgnoreCase(def.getDbType())) {
            return;
        }
        String serverUrl = "jdbc:mysql://" + def.getHost() + ":" + def.getPort()
                + "/?useUnicode=true&characterEncoding=utf8&useSSL=false"
                + "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        String dbName = def.getDbName().replace("`", "");
        try (Connection server = DriverManager.getConnection(serverUrl, def.getUsername(), password);
             Statement stmt = server.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS `" + dbName + "`"
                    + " DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
        }
    }

    /**
     * 脚本直建与结构收敛：①TENANT 通道——基座身份表直建/收敛；②MASTER 通道——各积木业务表
     * 直建/收敛（全新库全量执行；克隆来源存量库按主库账本基线只补 pending）；③BASE 通道——
     * base 脚本全量重放（子应用注册/页面菜单/权限点，含平台域菜单种子；与 standalone 形态同构，
     * 探针按 biz 自身建表判定，不因身份表已存在而误基线）；④每租户通道——per-tenant 种子脚本。
     * 迁移失败随整体初始化重试（脚本幂等、账本按 (script, tenant) 续跑）。
     */
    protected void migrateSchema(String dsKey, Connection master, Connection target,
                                 TenantDatasourceDefinition def, TenantRoute route) throws Exception {
        TenantSchemaMigrator migrator = schemaMigratorProvider.getIfAvailable();
        if (migrator == null) {
            log.warn("[mse-tenant-ds] TenantSchemaMigrator 未装配，跳过脚本直建: key={}", dsKey);
            return;
        }
        String dbType = def.getDbType();
        Map<String, String> baselines = migrator.resolveBizBaselines(master, dbType);
        TenantSchemaMigrator.Result identity = migrator.migrate(target, dbType,
                MigrateOptions.structure(MigrationTarget.TENANT, true).withBizBaselines(baselines));
        if (!identity.steps().isEmpty()) {
            log.info("[mse-tenant-ds] 租户库身份结构直建: key={} {} → {}（应用 {}、失败 {}）",
                    dsKey, identity.before(), identity.after(), identity.applied(), identity.failed());
        }
        TenantSchemaMigrator.Result structure = migrator.migrate(target, dbType,
                MigrateOptions.structure(MigrationTarget.MASTER, true).withBizBaselines(baselines));
        if (!structure.steps().isEmpty()) {
            log.info("[mse-tenant-ds] 租户库业务结构直建: key={} {} → {}（应用 {}、失败 {}）",
                    dsKey, structure.before(), structure.after(), structure.applied(), structure.failed());
        }
        // BASE 通道关自动基线（迁移器对 BASE 通道本就不写/不看基线）：菜单/权限类 base 脚本无建表、
        // 探针空集回退全库业务表判定，会把②MASTER 刚建完表的全新库误判存量、误基线整组跳过 base 脚本
        TenantSchemaMigrator.Result base = migrator.migrate(target, dbType,
                MigrateOptions.structure(MigrationTarget.BASE, false));
        if (!base.steps().isEmpty()) {
            log.info("[mse-tenant-ds] 租户库 base 副本直建: key={} {} → {}（应用 {}、失败 {}）",
                    dsKey, base.before(), base.after(), base.applied(), base.failed());
        }
        String channelProblem = firstProblem(identity, structure, base);
        if (channelProblem != null) {
            throw new IllegalStateException("租户库脚本直建失败: " + channelProblem);
        }
        catchUpCatalogChanges(migrator, target, dbType, dsKey);
        if (route != null && route.getTenantId() != null) {
            TenantSchemaMigrator.Result perTenant = migrator.migrate(target, dbType,
                    MigrateOptions.forTenant(route.getTenantId(), tenantProperties.getSchema().getPlaceholders()));
            if (!perTenant.steps().isEmpty()) {
                log.info("[mse-tenant-ds] 租户库每租户种子应用: key={} tenant={}（应用 {}、失败 {}）",
                        dsKey, route.getTenantId(), perTenant.applied(), perTenant.failed());
            }
            if (perTenant.failed() > 0 || perTenant.conflicts() > 0) {
                throw new IllegalStateException("租户库每租户种子应用"
                        + (perTenant.failed() > 0 ? "失败: " + perTenant.firstError()
                        : "存在 checksum 冲突: " + perTenant.firstConflict()));
            }
        }
    }

    /** 三通道首个失败/checksum 冲突描述（null=全部干净）；冲突同失败对待——结构不确定不放行流量 */
    private static String firstProblem(TenantSchemaMigrator.Result... results) {
        for (TenantSchemaMigrator.Result result : results) {
            if (result.failed() > 0) {
                return result.firstError();
            }
            if (result.conflicts() > 0) {
                return "checksum 冲突: " + result.firstConflict();
            }
        }
        return null;
    }

    /**
     * 目录变更批次追平：BASE 脚本基线之上应用平台端目录编辑的全量批次（新库账本为空→全量生效，
     * 账本按 script 键幂等）。拉取失败（平台不可达等）<b>不阻断初始化</b>——warn 后放行，
     * 由车队调谐扇出在库就绪后兜底收敛（扇出目标含已初始化库）。
     */
    private void catchUpCatalogChanges(TenantSchemaMigrator migrator, Connection target,
                                       String dbType, String dsKey) {
        if (catalogChangeSourceProvider == null) {
            return;
        }
        CatalogChangeSource source = catalogChangeSourceProvider.getIfAvailable();
        if (source == null) {
            return;
        }
        try {
            List<SchemaMigration> pending = new ArrayList<>();
            long sinceId = 0L;
            while (true) {
                List<SchemaMigration> page = source.changesSince(sinceId, 200);
                if (page == null || page.isEmpty()) {
                    break;
                }
                pending.addAll(page);
                String lastVersion = page.get(page.size() - 1).version();
                long nextId = 0L;
                try {
                    nextId = Long.parseLong(lastVersion);
                } catch (NumberFormatException ignored) {
                    // version 非 zeroPad10 数字：无法推进游标，防死循环直接收口本页
                }
                if (nextId <= sinceId || page.size() < 200) {
                    break;
                }
                sinceId = nextId;
            }
            // 方言化键过滤（catalog/change/<方言>/<id>）：只取与目标库方言匹配的变体，
            // 与车队扇出/分发器共享账本键，幂等互认
            pending = pending.stream()
                    .filter(m -> m.script() == null || m.script().contains("/" + dbType + "/"))
                    .toList();
            if (pending.isEmpty()) {
                return;
            }
            TenantSchemaMigrator.Result result = migrator.migrate(target, dbType, pending,
                    MigrateOptions.structure(MigrationTarget.BASE, false));
            if (result.applied() > 0) {
                log.info("[mse-tenant-ds] 租户库目录批次追平: key={}（应用 {}、失败 {}）",
                        dsKey, result.applied(), result.failed());
            }
            if (result.failed() > 0 || result.conflicts() > 0) {
                log.warn("[mse-tenant-ds] 租户库目录批次追平存在失败/冲突（车队扇出兜底重试）: key={} {}",
                        dsKey, result.failed() > 0 ? result.firstError() : result.firstConflict());
            }
        } catch (Exception e) {
            log.warn("[mse-tenant-ds] 目录批次追平跳过（平台侧不可达，车队扇出兜底）: key={} {}", dsKey, e.getMessage());
        }
    }

    /**
     * SPLIT 分表收敛：直建产出的是基础表名，分表模式（SHARED_TABLE_SPLIT）的租户需要后缀表——
     * 逐表检查路由策略，基础表存在而后缀表缺失时执行 RENAME（幂等，已改名的跳过）。
     */
    protected void applySplitRenames(Connection target, TenantDatasourceDefinition def, TenantRoute route)
            throws SQLException {
        if (route == null) {
            return;
        }
        Set<String> existing = TenantDbCloner.existingTables(target);
        int renamed = 0;
        for (String table : existing) {
            if (route.effectiveMode(table) != TablePolicy.Mode.SPLIT) {
                continue;
            }
            String suffix = route.effectiveSuffix(table);
            if (suffix == null || suffix.isBlank() || existing.contains(table + suffix)) {
                continue;
            }
            String dbType = def.getDbType() == null ? "mysql" : def.getDbType().trim().toLowerCase();
            String rename = "dm".equals(dbType) || "kingbase".equals(dbType)
                    ? "ALTER TABLE " + TenantDbCloner.identifier(table, "表名")
                    + " RENAME TO " + TenantDbCloner.identifier(table + suffix, "表名")
                    : "RENAME TABLE " + TenantDbCloner.identifier(table, "表名")
                    + " TO " + TenantDbCloner.identifier(table + suffix, "表名");
            try (Statement stmt = target.createStatement()) {
                stmt.execute(rename);
            }
            renamed++;
        }
        if (renamed > 0) {
            log.info("[mse-tenant-ds] SPLIT 分表改名完成: key={}（{} 张）", def.getDsKey(), renamed);
        }
    }
}
