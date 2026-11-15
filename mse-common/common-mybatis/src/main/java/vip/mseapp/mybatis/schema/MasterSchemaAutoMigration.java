package vip.mseapp.mybatis.schema;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.util.ClassUtils;
import vip.mseapp.mybatis.config.MseTenantProperties;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 启动自动迁移（mse.tenant.schema.auto-migrate=true 时装配）：应用就绪前把 classpath 各积木
 * 自带的 {@code db/<方言>/V*.sql} 与 Java 迁移应用到目标库，使「启动即建表」，免手工执行 init SQL。
 *
 * <p><b>目标路由</b>（脚本头指令 mse-target）：
 * <ul>
 *   <li>tenant / master（租户库身份结构与业务表）：<b>不在启动期执行</b>——租户库全部动态生成
 *       （缺省共享库由 SharedMainBootstrap 种登记行并 eager 建 TENANT 身份表；MASTER 业务表与
 *       各独立租户库结构由 TenantDsLazyInitializer 随首路由懒直建），启动零租户库连接；</li>
 *   <li>base（基座共享库）：仅平台域脚本（mse-base-scope: platform）——单体/基座应用（classpath 含
 *       service-system）本地执行：注册了 platform 数据源落在平台库（平台身份/平台菜单/mon_* 平台侧
 *       数据所在库）；纯业务微服务跳过——由 starter-bridge 推送 mse-system 应用。租户域 base 脚本
 *       双路分发（平台库目录摄入 + 租户库车队重放 TenantFleetBaseReplayer）；</li>
 *   <li>platform（平台控制面库）：注册了 platform 数据源用之，否则用 primary
 *       （mse-platform 自身 primary 即平台库）；无平台脚本的纯业务服务自动空跑。</li>
 * </ul>
 * 每租户通道不在此执行——开通事件 + {@link TenantDataMigrator} + 租户库懒初始化负责。
 *
 * <p><b>并发控制</b>（多实例同时启动）：统一分布式锁 {@link SchemaOpsLock} 按<b>目标库</b>互斥
 * （key 取 JDBC URL 摘要——多个微服务共享同一库时同样互斥；全部结构操作入口共用同一把锁），
 * 抢不到锁的实例阻塞轮询等持锁实例完成后自行再检查
 * （fail-fast 语义要求每个实例验证过 schema 才对外；迁移按账本续跑，重复检查零成本）。
 * 无 Redis 时退化为进程内串行（多实例部署必须配 Redis）。脚本幂等（IF NOT EXISTS / NOT EXISTS）
 * + {@code mse_<biz>_schema_history} 账本双保险。
 *
 * <p><b>失败策略</b>：迁移失败或已应用脚本 checksum 冲突（脚本内容被改动）均抛异常阻断启动
 * （fail-fast——应用可能带着新代码连旧结构库）；失败明细已入账本，管理端与下次启动可见可重试。
 */
public class MasterSchemaAutoMigration implements ApplicationRunner, org.springframework.core.Ordered {

    /**
     * 先于一切业务 ApplicationRunner（内置模板同步等启动期数据动作都依赖表已就绪）；
     * 为 standalone 自足化直建（StandaloneBootstrap，HIGHEST_PRECEDENCE）让位：全新库先经
     * TENANT 通道建身份表，本 Runner 的 base 平台域脚本（可能 JOIN 身份表）在其后执行
     * （非 standalone 部署无 StandaloneBootstrap，本 Runner 仍是首个结构动作）。
     */
    @Override
    public int getOrder() {
        return org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1000;
    }

    private static final Logger log = LoggerFactory.getLogger(MasterSchemaAutoMigration.class);

    private static final Duration LOCK_TTL = Duration.ofSeconds(600);

    /** 结构迁移就绪标记（JVM 级）：run() 全通道成功后置位；auto-migrate 关闭时由装配的兜底 Runner 置位 */
    private static final java.util.concurrent.atomic.AtomicBoolean READY =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * 启动结构迁移是否已完成。自建调度线程池等在 {@code @PostConstruct} 即启动的后台扫描任务
     * （首次触发早于 ApplicationRunner）以此做首轮门禁，避免全新库首启对尚未直建的表做无谓查询
     * 并产生失败告警噪音（下一周期自愈，此处直接跳过）。迁移失败阻断启动，标记不会误置位。
     */
    public static boolean isSchemaReady() {
        return READY.get();
    }

    /** 兜底置位：{@code mse.tenant.schema.auto-migrate=false} 或使用方整体替换执行器时由装配调用 */
    public static void markSchemaReady() {
        READY.set(true);
    }

    private static final long POLL_INTERVAL_MS = 2000;

    /** service-system 装配类：classpath 出现即视为「基座本地」（单体 / mse-system 应用） */
    private static final String SYSTEM_LOCAL_MARKER =
            "vip.mseapp.service.system.config.SystemServiceAutoConfiguration";

    private final ObjectProvider<DynamicRoutingDataSource> routingProvider;

    private final String primaryDsKey;

    private final ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplateProvider;

    private final TenantSchemaMigrator migrator;

    private final TenantMigrationScanner scanner;

    private final MseTenantProperties.Schema schema;

    /** JVM 内 per-DB 串行（无 Redis 兜底 + 与 Redis 锁叠加的双保险） */
    private static final ConcurrentHashMap<String, Object> DB_LOCKS = new ConcurrentHashMap<>();

    public MasterSchemaAutoMigration(ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                     String primaryDsKey,
                                     ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplateProvider,
                                     TenantSchemaMigrator migrator,
                                     TenantMigrationScanner scanner,
                                     MseTenantProperties.Schema schema) {
        this.routingProvider = routingProvider;
        this.primaryDsKey = primaryDsKey == null || primaryDsKey.isBlank() ? "master" : primaryDsKey.trim();
        this.redisTemplateProvider = redisTemplateProvider;
        this.migrator = migrator;
        this.scanner = scanner;
        this.schema = schema;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("dynamic-datasource 未装配，无法执行启动自动迁移");
        }
        DataSource primary = routing.getDataSources().get(primaryDsKey);
        try (Connection platformConn = (primary != null ? primary : routing).getConnection()) {
            migrator.warnLegacyLedger(platformConn);
            // 1) base：基座共享库（仅单体/基座应用本地执行；业务微服务走推送通道）——只应用平台域
            //      脚本（mse-base-scope: platform：基座身份与平台菜单种子、平台侧结构）。租户域 base
            //      脚本双路分发：平台库目录摄入（BaseDataRegistryServiceImpl）+ 租户库车队重放。
            //      primary 即平台库（本形态 primary=platform 或 mse-platform 单源），TENANT/MASTER
            //      通道不再于启动期对任何库直建——租户库动态生成（Seeder eager + 懒初始化）
            if (ClassUtils.isPresent(SYSTEM_LOCAL_MARKER, MasterSchemaAutoMigration.class.getClassLoader())) {
                DataSource baseDs = routing.getDataSources().get(schema.getPlatformDs());
                if (baseDs != null) {
                    try (Connection base = baseDs.getConnection()) {
                        migrateBase(base, "base");
                    }
                } else {
                    log.warn("[mse-schema] 未注册 platform 数据源（{}），平台域 base 脚本回退落到 primary——"
                            + "部署必须显式配置 platform 数据源，否则平台域脚本会落错库", schema.getPlatformDs());
                    migrateBase(platformConn, "base");
                }
            } else {
                log.debug("[mse-schema] 非基座应用，base 通道脚本交由推送通道（mse-system）应用");
            }
        }
        // 4) platform：平台控制面库（注册了 platform 数据源用之，否则 primary 即平台库的应用承担）。
        //    autoBaseline=false：控制面/平台域脚本全幂等（IF NOT EXISTS + 种子 NOT EXISTS），存量库靠
        //    幂等收敛即可；biz=platform 的建表全是 SYS_* 前缀（探针被扣成空集 → 退回「库内有业务表即
        //    存量」判定），同一次启动 base 通道（如 monitor 平台侧 mon_*）先建的表会把全新平台库
        //    误判成存量库、控制面建表脚本被基线整体跳过（实测回归），故不启用自动基线
        if (hasTarget(MigrationTarget.PLATFORM)) {
            DataSource platform = routing.getDataSources().get(schema.getPlatformDs());
            if (platform != null) {
                try (Connection conn = platform.getConnection()) {
                    migrateOn(conn, schema.getPlatformDs(), MigrateOptions.structure(MigrationTarget.PLATFORM, false));
                }
            } else {
                try (Connection conn = (primary != null ? primary : routing).getConnection()) {
                    migrateOn(conn, primaryDsKey, MigrateOptions.structure(MigrationTarget.PLATFORM, false));
                }
            }
        }
        // 全通道成功，放行后台扫描类任务的首轮门禁（见 isSchemaReady）
        READY.set(true);
    }

    /** 预演清单日志：待执行脚本逐条 INFO（超长截断，完整清单经管理端查看）、冲突 WARN、基线/账本判定说明 INFO */
    private static void logPlan(String label, TenantSchemaMigrator.Plan plan) {
        List<TenantSchemaMigrator.PlanStep> pending = plan.pending();
        if (!pending.isEmpty()) {
            int shown = Math.min(pending.size(), 30);
            log.info("[mse-schema] {}库待应用迁移脚本 {} 条（预演清单，与实际执行同序）:", label, pending.size());
            for (int i = 0; i < shown; i++) {
                log.info("[mse-schema]   {}. {}", i + 1, pending.get(i));
            }
            if (pending.size() > shown) {
                log.info("[mse-schema]   ……其余 {} 条略（完整清单经管理端租户库「结构状态」查看）", pending.size() - shown);
            }
        }
        for (String conflict : plan.conflicts()) {
            log.warn("[mse-schema] {}库 checksum 冲突（已应用脚本内容被改动，实际执行将阻断启动）: {}", label, conflict);
        }
        for (String note : plan.notes()) {
            log.info("[mse-schema] {}库基线/账本判定: {}", label, note);
        }
    }

    /** 方言识别（protected 便于内嵌库测试覆写） */
    protected String detectDbType(Connection conn) throws SQLException {
        return TenantSchemaMigrator.detectDbType(conn);
    }

    /** 是否存在指定目标的待应用脚本（无脚本的目标不建连接空跑） */
    private boolean hasTarget(MigrationTarget target) {
        for (String dialect : List.of("mysql", "dm", "kingbase")) {
            for (SchemaMigration migration : scanner.scan(dialect)) {
                if (migration.target() == target) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 基座库 base 通道：仅平台域脚本（mse-base-scope: platform）；租户域脚本走租户库车队重放 */
    private void migrateBase(Connection conn, String label) throws Exception {
        String dbType = detectDbType(conn);
        List<SchemaMigration> platformScoped = scanner.scan(dbType).stream()
                .filter(m -> m.target() == MigrationTarget.BASE && m.baseScope() == BaseScope.PLATFORM)
                .toList();
        if (platformScoped.isEmpty()) {
            log.debug("[mse-schema] {}库无平台域 base 脚本，跳过", label);
            return;
        }
        migrateOn(conn, label, MigrateOptions.structure(MigrationTarget.BASE, false), platformScoped);
    }

    /** 单库迁移：Redis 锁按目标库 URL 互斥 + 进程内串行兜底，失败阻断启动 */
    private void migrateOn(Connection conn, String label, MigrateOptions options) throws Exception {
        migrateOn(conn, label, options, null);
    }

    /** 单库迁移（可指定脚本清单，缺省按通道全量扫描）：统一锁按目标库 URL 互斥 + 进程内串行兜底，失败/冲突阻断启动 */
    private void migrateOn(Connection conn, String label, MigrateOptions options,
                           List<SchemaMigration> scripts) throws Exception {
        long start = System.currentTimeMillis();
        String dbType = detectDbType(conn);
        String lockKey = SchemaOpsLock.lockKeyFor(conn);
        Object dbLock = DB_LOCKS.computeIfAbsent(lockKey, k -> new Object());
        synchronized (dbLock) {
            String token = SchemaOpsLock.acquire(redisTemplateProvider.getIfAvailable(), lockKey,
                    LOCK_TTL, POLL_INTERVAL_MS, LOCK_TTL);
            try {
                // 预演清单先行（只读，与实际执行同序同口径）：启动可见「将要应用什么」，
                // 一次变更影响多服务/多库/多方言时无须真正执行即可核对
                TenantSchemaMigrator.Plan plan = scripts != null
                        ? migrator.plan(conn, dbType, scripts, options)
                        : migrator.plan(conn, dbType, options);
                logPlan(label, plan);
                TenantSchemaMigrator.Result result = scripts != null
                        ? migrator.migrate(conn, dbType, scripts, options)
                        : migrator.migrate(conn, dbType, options);
                if (result.applied() > 0 || result.failed() > 0 || result.conflicts() > 0) {
                    log.info("[mse-schema] {}库自动迁移({}): {} → {}（应用 {}、失败 {}、冲突 {}、耗时 {}ms）",
                            label, dbType, result.before(), result.after(), result.applied(), result.failed(),
                            result.conflicts(), System.currentTimeMillis() - start);
                } else {
                    log.debug("[mse-schema] {}库结构已是最新({})，耗时 {}ms", label, result.after(),
                            System.currentTimeMillis() - start);
                }
                if (result.failed() > 0 || result.conflicts() > 0) {
                    throw new IllegalStateException(label + "库结构迁移"
                            + (result.failed() > 0
                            ? "失败（mse.tenant.schema.auto-migrate=true，失败阻断启动；失败明细见 mse_<biz>_schema_history）: "
                            + result.firstError()
                            : "存在 checksum 冲突（已应用脚本内容被修改，auto-migrate 阻断启动；"
                            + "请恢复脚本原文或人工核对账本 mse_<biz>_schema_history 后重启）: "
                            + result.firstConflict()));
                }
            } finally {
                SchemaOpsLock.release(redisTemplateProvider.getIfAvailable(), lockKey, token);
            }
        }
    }
}
