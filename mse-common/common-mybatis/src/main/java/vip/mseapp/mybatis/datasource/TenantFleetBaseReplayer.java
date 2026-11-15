package vip.mseapp.mybatis.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import vip.mseapp.core.crypto.AesCipher;
import vip.mseapp.mybatis.config.MseTenantProperties;
import vip.mseapp.mybatis.schema.MigrateOptions;
import vip.mseapp.mybatis.schema.MigrationTarget;
import vip.mseapp.mybatis.schema.SchemaMigration;
import vip.mseapp.mybatis.schema.SchemaOpsLock;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户库车队 BASE 通道重放器：把 base 脚本（子应用注册/页面菜单/权限点，含平台域菜单种子）全量
 * 重放到「缺省共享主库 + 每个独立租户库」，各库经 mse_&lt;biz&gt;_schema_history 分册账本幂等收敛——
 * 租户会话的菜单/权限/子应用读取随租户路由到各自绑定库（租户库自持副本），不再依赖基座库单点；
 * 平台库/基座库不再积攒各 biz 的 base 注册数据与账本。
 *
 * <p><b>目标库枚举</b>：内置共享库（builtin 通道，mse.tenant.shared-main.* 自 declare，未配置则无）
 * ∪ 元数据通道（sys_tenant_datasource，通道未装配则跳过）登记的启用数据源（service_name='*'
 * 通用行——通用共享库与各独立租户库）双路枚举，按物理目标（方言|主机|端口|库名）去重；
 * primary（平台库）不参与——平台库目录经 BaseDataRegistryServiceImpl
 * 摄入通道维护，不经车队。未初始化的租户库（无身份表 sys_user）跳过——懒初始化或
 * SharedMainBootstrap eager 通道会在首次路由/启动时直建（TENANT → MASTER → BASE），不打半成品。
 *
 * <p><b>幂等与并发</b>：脚本幂等（IF NOT EXISTS / NOT EXISTS）+ 分册账本按文件名去重；Redis 锁按
 * 目标库 URL 互斥（多实例/推送周期重叠安全，tryEnter 抢不到即跳过计入 skipped——摘要「跳过」字样
 * 触发推送端退避重试），进程内 per-库串行兜底。单库失败不阻断其他库，失败/冲突明细计入 errors，
 * 由调用方上抛使启动推送按可重试失败处理（成功即停语义下若当成功吞掉，失败的库将无人补跑）。
 */
public class TenantFleetBaseReplayer {

    private static final Logger log = LoggerFactory.getLogger(TenantFleetBaseReplayer.class);

    private static final Duration LOCK_TTL = Duration.ofSeconds(300);

    /** 一次性车队重放汇总：目标库数、应用/失败/锁跳过脚本数、失败明细 */
    public record FleetResult(int targets, int applied, int failed, int skipped, List<String> errors) {

        public static final FleetResult EMPTY = new FleetResult(0, 0, 0, 0, List.of());
    }

    /** 重放目标：label 供日志/锁 key；def 经 JDBC 直连 */
    private record Target(String label, String dsKey, TenantDatasourceDefinition def) {
    }

    private final TenantDsMetadataProvider metadataProvider;

    /** 内置共享库定义提供者（mse.tenant.shared-main.* 自declare；未配置返回 null） */
    private final java.util.function.Supplier<TenantDatasourceDefinition> builtinSharedMainProvider;

    private final MseTenantProperties.DynamicDs dynamicDs;

    private final TenantSchemaMigrator migrator;

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    /** 进程内串行（与 Redis 锁叠加；无 Redis 时兜底） */
    private static final Object PROCESS_LOCK = new Object();

    private static final ConcurrentHashMap<String, Object> DB_LOCKS = new ConcurrentHashMap<>();

    public TenantFleetBaseReplayer(TenantDsMetadataProvider metadataProvider,
                                   java.util.function.Supplier<TenantDatasourceDefinition> builtinSharedMainProvider,
                                   MseTenantProperties.DynamicDs dynamicDs,
                                   TenantSchemaMigrator migrator,
                                   ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.metadataProvider = metadataProvider;
        this.builtinSharedMainProvider = builtinSharedMainProvider;
        this.dynamicDs = dynamicDs;
        this.migrator = migrator;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /**
     * 全量重放 base 脚本到租户库车队（每租户通道脚本被忽略——那由开通/升级通道按 (script, tenant)
     * 记账执行）。返回汇总；元数据不可用（平台库未就绪）时返回 EMPTY 由调用方择机重试。
     */
    public FleetResult replayAll(List<SchemaMigration> baseScripts) {
        List<SchemaMigration> scripts = baseScripts == null ? List.of() : baseScripts.stream()
                .filter(m -> m.target() == MigrationTarget.BASE && !m.perTenant())
                .toList();
        if (scripts.isEmpty()) {
            return FleetResult.EMPTY;
        }
        List<Target> targets;
        try {
            targets = resolveTargets();
        } catch (Exception e) {
            log.warn("[mse-base-fleet] 租户库目标枚举失败（平台库未就绪？）: {}", e.getMessage());
            return FleetResult.EMPTY;
        }
        if (targets.isEmpty()) {
            log.warn("[mse-base-fleet] 车队目标为空（无内置共享库、无启用登记数据源或均未就绪），"
                    + "base 脚本无处重放——请检查 mse.tenant.shared-main 配置与「租户数据源」登记");
            return FleetResult.EMPTY;
        }
        int applied = 0;
        int failed = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        synchronized (PROCESS_LOCK) {
            for (Target target : targets) {
                String lockKey = "mse:lock:base-fleet:" + sha256(target.label());
                String token = SchemaOpsLock.tryAcquire(redisTemplateProvider.getIfAvailable(), lockKey, LOCK_TTL);
                if (token == null) {
                    skipped++;
                    log.info("[mse-base-fleet] 目标库正在被其他实例重放，本次跳过: {}", target.label());
                    continue;
                }
                try {
                    TenantSchemaMigrator.Result result = replayOn(target, scripts);
                    applied += result.applied();
                    failed += result.failed();
                    if (result.failed() > 0) {
                        errors.add(target.label() + ": " + result.firstError());
                    } else if (result.conflicts() > 0) {
                        errors.add(target.label() + ": " + result.firstConflict());
                    } else if (result.applied() > 0) {
                        log.info("[mse-base-fleet] 目标库 base 重放: {}（应用 {}）", target.label(), result.applied());
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add(target.label() + ": " + e.getMessage());
                    log.warn("[mse-base-fleet] 目标库 base 重放失败: {}", target.label(), e);
                } finally {
                    SchemaOpsLock.release(redisTemplateProvider.getIfAvailable(), lockKey, token);
                }
            }
        }
        return new FleetResult(targets.size(), applied, failed, skipped, List.copyOf(errors));
    }

    /**
     * 单库重放：结构通道、关闭自动基线（脚本幂等收敛，账本只作已执行登记）；按目标库方言筛取
     * 脚本（车队里混合方言库各取各的：db/mysql/… 与 db/dm/… 同版本并存，账本键即脚本路径互不冲突）。
     */
    private TenantSchemaMigrator.Result replayOn(Target target, List<SchemaMigration> scripts) throws Exception {
        try (Connection conn = connect(target)) {
            String dbType = TenantSchemaMigrator.detectDbType(conn);
            List<SchemaMigration> dialectScripts = scripts.stream()
                    .filter(m -> m.script() == null || m.script().contains("/" + dbType + "/"))
                    .toList();
            if (dialectScripts.isEmpty()) {
                return new TenantSchemaMigrator.Result(null, null, 0, 0, 0, List.of());
            }
            return migrator.migrate(conn, dbType, dialectScripts,
                    MigrateOptions.structure(MigrationTarget.BASE, false));
        }
    }

    /** 目标枚举：元数据登记的启用数据源 + 本服务内置共享库（mse.tenant.shared-main.*），按物理目标去重，未初始化库剔除 */
    private List<Target> resolveTargets() throws SQLException {
        Map<String, Target> targets = new LinkedHashMap<>();
        // 内置共享库（本服务配置自declare；mse-system 侧 Bootstrap 已保 identity 就绪）
        TenantDatasourceDefinition builtin = builtinSharedMainProvider == null ? null : builtinSharedMainProvider.get();
        if (builtin != null && (builtin.getStatus() == null || builtin.getStatus() == 0)) {
            targets.put("builtin:" + builtin.getDsKey(),
                    new Target("builtin:" + builtin.getDsKey() + "@" + builtin.target(), builtin.getDsKey(), builtin));
        }
        for (TenantDatasourceDefinition def : metadataProvider == null
                ? List.<TenantDatasourceDefinition>of() : metadataProvider.listDefinitions("*")) {
            if (def.getStatus() != null && def.getStatus() != 0) {
                continue;
            }
            String physical = (def.getDbType() + "|" + def.getHost() + "|" + def.getPort() + "|" + def.getDbName())
                    .toLowerCase();
            targets.putIfAbsent(physical, new Target("ds:" + def.getDsKey() + "@" + def.target(), def.getDsKey(), def));
        }
        List<Target> ready = new ArrayList<>();
        for (Target target : targets.values()) {
            try (Connection conn = connect(target)) {
                if (identityReady(conn)) {
                    ready.add(target);
                } else {
                    log.info("[mse-base-fleet] 跳过未初始化租户库（无身份表，等懒初始化全量直建）: {}", target.label());
                }
            } catch (Exception e) {
                log.warn("[mse-base-fleet] 目标库连接失败，跳过（下轮自愈）: {} - {}", target.label(), e.getMessage());
            }
        }
        return ready;
    }

    /** 目标库连接：登记数据源经 JDBC 直连（与懒初始化同源） */
    private Connection connect(Target target) throws SQLException {
        TenantDatasourceDefinition def = target.def();
        String password = AesCipher.decrypt(def.getPassword(), AesCipher.parseKey(dynamicDs.getEncryptKey()));
        return DriverManager.getConnection(TenantDsUrlBuilder.build(def), def.getUsername(), password);
    }

    /** 身份结构就绪判定（sys_user 可查）：未初始化库交给懒初始化全量直建，不打半成品 */
    private boolean identityReady(Connection conn) {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(
                "SELECT 1 FROM sys_user WHERE 1 = 0")) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
