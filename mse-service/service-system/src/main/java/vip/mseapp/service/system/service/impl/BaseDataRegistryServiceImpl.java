package vip.mseapp.service.system.service.impl;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import vip.mseapp.api.system.dto.BaseScriptDescriptor;
import vip.mseapp.mybatis.datasource.TenantFleetBaseReplayer;
import vip.mseapp.mybatis.schema.BaseScope;
import vip.mseapp.mybatis.schema.MigrateOptions;
import vip.mseapp.mybatis.schema.MigrationTarget;
import vip.mseapp.mybatis.schema.SchemaMigration;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;
import vip.mseapp.redis.MsePermCacheKeys;
import vip.mseapp.service.system.service.BaseDataRegistryService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * base 脚本注册实现：按本服务数据库方言筛取推送脚本（target=base），按落库域分流——
 * <ul>
 *   <li><b>平台域</b>（mse-base-scope: platform，如基座身份/平台菜单/平台侧结构）：应用到基座库
 *       （注册了 platform 数据源=单体双库落平台库，否则 primary 即基座库），经
 *       {@link TenantSchemaMigrator} 按 biz 分册入账本；</li>
 *   <li><b>租户域</b>（缺省，各 biz 子应用注册/页面菜单/权限点）双路分发：
 *       ①<b>摄入控制面目录</b>——同步应用到平台库（定义目录真源，平台管理端只读写自己的库，
 *       不再经 runIgnore 并集读数据面；脚本 WHERE NOT EXISTS 与平台库既有种子行天然去重，
 *       平台库自动生长 {@code mse_<biz>_schema_history} 分册账本）；②交
 *       {@link TenantFleetBaseReplayer} 全量重放到租户库车队（共享主库 + 各独立库，各库自持副本
 *       与分册账本），租户会话的菜单/权限/子应用读取随租户路由到自持库。</li>
 * </ul>
 * 基座库通道自动基线必须关闭——基座库表结构可能来自旧版 docker init 而非脚本清单，「历史空+有表→
 * 打基线」会误判脚本已包含而整批跳过；幂等收敛靠脚本自身（NOT EXISTS / IF NOT EXISTS / 条件 UPDATE）
 * 保证，账本仅作已执行登记与审计。车队重放同样幂等（各租户库独立账本）。
 *
 * <p><b>并发控制</b>：平台库通道（平台域 + 目录摄入共用同一落点）进程内串行 + Redis setIfAbsent
 * 互斥（多实例 mse-system 收到并发推送），锁采用短轮询重试（约 30s）——推送端已改为启动一次 +
 * 成功前退避重试，不再有周期重推兜底，接收端持锁冲突时自行等待而非把责任推给「下个周期」；
 * 车队重放由重放器按目标库逐库加锁。
 */
public class BaseDataRegistryServiceImpl implements BaseDataRegistryService {

    private static final Logger log = LoggerFactory.getLogger(BaseDataRegistryServiceImpl.class);

    private static final Duration LOCK_TTL = Duration.ofSeconds(120);

    private static final String LOCK_KEY = "mse:lock:base-data-migrate";

    /** Redis 锁短轮询轮数/间隔：约 30s 内等待其他实例释放（推送端启动重试语义下不再依赖周期重推） */
    private static final int LOCK_RETRY_ROUNDS = 15;

    private static final long LOCK_RETRY_INTERVAL_MS = 2_000L;

    /** 进程内串行（与 Redis 锁叠加；无 Redis 时兜底） */
    private static final Object PROCESS_LOCK = new Object();

    private final ObjectProvider<DynamicRoutingDataSource> routingProvider;

    private final String primaryDsKey;

    /** 平台控制面库数据源 key（mse.tenant.schema.platform-ds，缺省 platform；注册即单体双库） */
    private final String platformDsKey;

    private final TenantSchemaMigrator migrator;

    private final TenantFleetBaseReplayer fleetReplayer;

    private final ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplateProvider;

    public BaseDataRegistryServiceImpl(ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                       String primaryDsKey,
                                       String platformDsKey,
                                       TenantSchemaMigrator migrator,
                                       TenantFleetBaseReplayer fleetReplayer,
                                       ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplateProvider) {
        this.routingProvider = routingProvider;
        this.primaryDsKey = primaryDsKey == null || primaryDsKey.isBlank() ? "master" : primaryDsKey.trim();
        this.platformDsKey = platformDsKey == null || platformDsKey.isBlank() ? "platform" : platformDsKey.trim();
        this.migrator = migrator;
        this.fleetReplayer = fleetReplayer;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Override
    public String register(List<BaseScriptDescriptor> scripts) {
        if (scripts == null || scripts.isEmpty()) {
            return "无脚本";
        }
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("dynamic-datasource 未装配，无法应用基座库脚本");
        }
        String dbType;
        try (Connection probe = baseConnection(routing)) {
            dbType = TenantSchemaMigrator.detectDbType(probe);
        } catch (SQLException e) {
            throw new IllegalStateException("基座库连接失败: " + e.getMessage(), e);
        }
        List<SchemaMigration> platformScoped = new ArrayList<>();
        List<SchemaMigration> catalogScoped = new ArrayList<>();
        List<SchemaMigration> tenantScoped = new ArrayList<>();
        for (BaseScriptDescriptor script : scripts) {
            if (script == null || isBlank(script.getDialect())) {
                continue;
            }
            if (BaseScope.PLATFORM == BaseScope.of(script.getBaseScope())) {
                // 平台域只落基座库：按基座库方言筛取
                SchemaMigration migration = toMigration(script, dbType);
                if (migration != null) {
                    platformScoped.add(migration);
                }
            } else {
                // 租户域双路：①按平台库方言筛取一份摄入控制面目录（真源）；②全方言透传交车队
                // 按各目标库方言筛取（db/<方言>/ 段即账本键，互不冲突）
                SchemaMigration catalogMigration = toMigration(script, dbType);
                if (catalogMigration != null) {
                    catalogScoped.add(catalogMigration);
                }
                SchemaMigration migration = toMigration(script, null);
                if (migration != null) {
                    tenantScoped.add(migration);
                }
            }
        }
        if (platformScoped.isEmpty() && tenantScoped.isEmpty()) {
            return "基座库(" + dbType + "）推送清单中无本方言脚本";
        }
        StringBuilder summary = new StringBuilder();
        if (!platformScoped.isEmpty()) {
            if (!summary.isEmpty()) {
                summary.append("；");
            }
            summary.append(applyToPlatformDb(routing, dbType, platformScoped, "基座库"));
        }
        if (!catalogScoped.isEmpty()) {
            if (!summary.isEmpty()) {
                summary.append("；");
            }
            summary.append(applyToPlatformDb(routing, dbType, catalogScoped, "目录"));
        }
        if (!tenantScoped.isEmpty()) {
            if (!summary.isEmpty()) {
                summary.append("；");
            }
            summary.append(replayToFleet(tenantScoped));
        }
        return summary.toString();
    }

    /**
     * 脚本应用到平台库（平台域基座数据 / 租户域目录摄入共用落点；自动基线关闭，幂等收敛；
     * 失败明细在返回摘要中）。锁短轮询重试，长持锁冲突时返回跳过提示（推送端启动重试语义下会重推）。
     */
    private String applyToPlatformDb(DynamicRoutingDataSource routing, String dbType,
                                     List<SchemaMigration> migrations, String label) {
        try (Connection base = baseConnection(routing)) {
            synchronized (PROCESS_LOCK) {
                String token = UUID.randomUUID().toString();
                if (!awaitRedisLock(token)) {
                    return label + "(" + dbType + "）其他实例长时间持锁，本次跳过（推送端将重试）";
                }
                try {
                    TenantSchemaMigrator.Result result = migrator.migrate(base, dbType, migrations,
                            MigrateOptions.structure(MigrationTarget.BASE, false));
                    if (result.failed() > 0) {
                        log.error("[mse-base-data] {}脚本应用存在失败: {}", label, result.firstError());
                    } else if (result.applied() > 0) {
                        log.info("[mse-base-data] {}脚本应用: {}", label, result.summary().replace("\n", "；"));
                    }
                    if (result.applied() > 0) {
                        evictPermissionCache();
                    }
                    return label + "(" + dbType + "）" + result.summary();
                } finally {
                    releaseRedisLock(token);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(label + "脚本应用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 租户域脚本重放到租户库车队（共享主库 + 各独立库）。部分失败/checksum 冲突以异常上抛、
     * 锁跳过在摘要中以「跳过」字样返回——推送端分别按「异常重试」「含跳过重试」处理：
     * 启动推送为成功即停语义，若把车队部分失败当成功吞掉，失败的租户库将没有任何后续补跑通道
     * （生产事故回归：一次死锁让租户库 base 副本半途而废，平台库有菜单而租户库没有）。
     */
    private String replayToFleet(List<SchemaMigration> migrations) {
        TenantFleetBaseReplayer.FleetResult result = fleetReplayer.replayAll(migrations);
        StringBuilder summary = new StringBuilder("租户库车队重放: 目标 ").append(result.targets())
                .append(" 个、应用 ").append(result.applied()).append("、失败 ").append(result.failed());
        if (result.skipped() > 0) {
            summary.append("、跳过 ").append(result.skipped());
        }
        for (String error : result.errors()) {
            summary.append("\n- ").append(error);
        }
        if (result.failed() > 0 || !result.errors().isEmpty()) {
            log.error("[mse-base-data] 租户库车队重放存在失败: {}", summary);
        } else if (result.applied() > 0) {
            log.info("[mse-base-data] 租户库车队重放: 目标 {} 个、应用 {} 条{}", result.targets(),
                    result.applied(), result.skipped() > 0 ? "、跳过 " + result.skipped() : "");
        }
        if (result.applied() > 0) {
            evictPermissionCache();
        }
        if (result.failed() > 0 || !result.errors().isEmpty()) {
            throw new IllegalStateException(summary.toString());
        }
        return summary.toString();
    }

    /**
     * 权限模型缓存全局版本递增：base 脚本可能落菜单/权限点/子应用定义，
     * 与管理端手工编辑（SysMenuController 等）同理主动失效两端鉴权缓存，
     * 不然新权限点最长 30 分钟 TTL 内对已登录用户不生效。
     */
    private void evictPermissionCache() {
        org.springframework.data.redis.core.StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().increment(MsePermCacheKeys.globalVer());
        } catch (Exception e) {
            log.warn("[mse-base-data] 权限缓存版本递增失败（留待 TTL 兜底）: {}", e.getMessage());
        }
    }

    /**
     * 基座库连接：注册了 platform 数据源用之（单体双库 / mse-system 主库切位共享租户库后的平台库落点）；
     * 否则回退 primary——仅兼容「primary 即基座库」旧拓扑（告警提示，主库切位后平台域脚本会落错库）。
     */
    private Connection baseConnection(DynamicRoutingDataSource routing) throws SQLException {
        DataSource platform = routing.getDataSources().get(platformDsKey);
        if (platform != null) {
            return platform.getConnection();
        }
        log.warn("[mse-base-data] 未注册 platform 数据源（{}），平台域脚本回退落到 primary——"
                + "primary 应为平台库（旧拓扑）；主库已切位共享租户库的部署必须显式配置 platform 数据源", platformDsKey);
        DataSource primary = routing.getDataSources().get(primaryDsKey);
        return (primary != null ? primary : routing).getConnection();
    }

    /** 有效性校验（无效条目跳过并告警，不拖垮整批）；dbTypeFilter 非空时按方言筛取，null=全方言透传 */
    private SchemaMigration toMigration(BaseScriptDescriptor script, String dbTypeFilter) {
        if (dbTypeFilter != null
                && (script.getDialect() == null || !dbTypeFilter.equalsIgnoreCase(script.getDialect().trim()))) {
            return null;
        }
        if (isBlank(script.getVersion()) || isBlank(script.getScript()) || isBlank(script.getSql())) {
            log.warn("[mse-base-data] 跳过无效脚本条目: version={}, script={}",
                    script.getVersion(), script.getScript());
            return null;
        }
        return new SchemaMigration(script.getBiz() == null ? "adhoc" : script.getBiz().trim(),
                script.getVersion().trim(), description(script.getScript()), SchemaMigration.TYPE_CLASSPATH,
                MigrationTarget.BASE, false, script.getScript().trim(), script.getSql(), script.getChecksum(),
                BaseScope.of(script.getBaseScope()));
    }

    /** 描述取文件名 {@code __} 后段（去 .sql），如 V20260922_01__mp_base.sql → mp_base */
    private static String description(String scriptPath) {
        String name = scriptPath;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        if (name.endsWith(".sql")) {
            name = name.substring(0, name.length() - 4);
        }
        int marker = name.indexOf("__");
        return marker >= 0 ? name.substring(marker + 2) : name;
    }

    /**
     * true=获得锁；false=其他实例长时间持锁（调用方返回跳过提示，推送端启动重试语义下会重推）；
     * 无 Redis=true（进程内串行兜底）。短轮询等待（约 30s）而非单次让位——推送端不再周期重推，
     * 偶发并发冲突在接收端自行消化。
     */
    private boolean awaitRedisLock(String token) {
        var template = redisTemplateProvider.getIfAvailable();
        if (template == null) {
            return true;
        }
        for (int round = 0; round < LOCK_RETRY_ROUNDS; round++) {
            try {
                if (Boolean.TRUE.equals(template.opsForValue().setIfAbsent(LOCK_KEY, token, LOCK_TTL))) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("[mse-base-data] Redis 锁不可用（进程内串行兜底）: {}", e.getMessage());
                return true;
            }
            if (round < LOCK_RETRY_ROUNDS - 1) {
                try {
                    Thread.sleep(LOCK_RETRY_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private void releaseRedisLock(String token) {
        try {
            var template = redisTemplateProvider.getIfAvailable();
            if (template == null) {
                return;
            }
            if (token.equals(template.opsForValue().get(LOCK_KEY))) {
                template.delete(LOCK_KEY);
            }
        } catch (Exception ignored) {
            // 锁 TTL 到期自动释放
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
