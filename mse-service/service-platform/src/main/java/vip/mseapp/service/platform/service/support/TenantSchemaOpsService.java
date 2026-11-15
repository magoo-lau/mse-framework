package vip.mseapp.service.platform.service.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.mybatis.config.MseTenantProperties;
import vip.mseapp.mybatis.datasource.TenantDbCloner;
import vip.mseapp.mybatis.datasource.TenantDsUrlBuilder;
import vip.mseapp.mybatis.schema.MigrateOptions;
import vip.mseapp.mybatis.schema.MigrationTarget;
import vip.mseapp.mybatis.schema.SchemaOpsLock;
import vip.mseapp.mybatis.schema.TenantSchemaDiffer;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;
import vip.mseapp.service.platform.entity.SysTenantDatasource;
import vip.mseapp.service.platform.mapper.SysTenantDatasourceMapper;
import vip.mseapp.service.platform.service.SysTenantDatasourceService.BatchSyncStatusView;
import vip.mseapp.service.platform.service.SysTenantDatasourceService.ChannelPlanView;
import vip.mseapp.service.platform.service.SysTenantDatasourceService.SchemaPlanView;
import vip.mseapp.service.platform.service.SysTenantDatasourceService.SchemaStatusView;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户库结构演进操作（自 SysTenantDatasourceServiceImpl 拆出的内部协作者，业务工程勿依赖）：
 * 版本化迁移 / 结构比对同步 / 迁移预演 / 结构状态 / 临时 SQL，以及进程内 per-dsKey 串行锁
 * （供 {@link TenantDatasourceInitializer} 复用）与跨实例 {@link SchemaOpsLock} 互斥。
 */
// 控制面协作者：类级 NOT_SUPPORTED 防错库堤坝，勿删（范式见 SysTenantRouteServiceImpl 类注释）
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class TenantSchemaOpsService {

    private static final Logger log = LoggerFactory.getLogger(TenantSchemaOpsService.class);

    /** 结构操作跨实例锁 TTL（初始化/迁移/同步共用同一把锁；兜底进程崩溃后锁自愈，正常完成即主动释放） */
    public static final Duration SCHEMA_OPS_LOCK_TTL = Duration.ofMinutes(15);

    /** 临时 SQL 大小上限（单次粘贴的维护脚本足够大） */
    private static final int ADHOC_SQL_MAX_LENGTH = 200_000;

    /** 同步结果落库截断长度（sync_message VARCHAR(500)） */
    private static final int SYNC_MESSAGE_MAX_LENGTH = 500;

    private final SysTenantDatasourceMapper datasourceMapper;
    private final ObjectProvider<TenantSchemaMigrator> schemaMigratorProvider;
    private final ObjectProvider<DataSource> masterDataSourceProvider;
    private final ObjectProvider<MseTenantProperties> tenantPropertiesProvider;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final TenantDatasourceSupport support;

    /** 结构演进操作的 per-dsKey 串行锁（界面手动触发防重复点击并发执行；初始化任务体共用） */
    private final ConcurrentHashMap<String, Object> schemaLocks = new ConcurrentHashMap<>();

    public TenantSchemaOpsService(SysTenantDatasourceMapper datasourceMapper,
                                  ObjectProvider<TenantSchemaMigrator> schemaMigratorProvider,
                                  ObjectProvider<DataSource> masterDataSourceProvider,
                                  ObjectProvider<MseTenantProperties> tenantPropertiesProvider,
                                  ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                  TenantDatasourceSupport support) {
        this.datasourceMapper = datasourceMapper;
        this.schemaMigratorProvider = schemaMigratorProvider;
        this.masterDataSourceProvider = masterDataSourceProvider;
        this.tenantPropertiesProvider = tenantPropertiesProvider;
        this.redisTemplateProvider = redisTemplateProvider;
        this.support = support;
    }

    public String migrate(SysTenantDatasource ds) {
        synchronized (schemaLock(ds.getDsKey())) {
            return withSchemaOpsLock(ds, () -> {
                try (Connection master = masterConn();
                     Connection target = openTarget(ds)) {
                    TenantSchemaMigrator migrator = requireMigrator();
                    Map<String, String> bizBaselines = migrator.resolveBizBaselines(master, ds.getDbType());
                    TenantSchemaMigrator.Result identity = migrator.migrate(target, ds.getDbType(),
                            MigrateOptions.structure(MigrationTarget.TENANT, true).withBizBaselines(bizBaselines));
                    TenantSchemaMigrator.Result result = migrator.migrate(target, ds.getDbType(),
                            MigrateOptions.structure(MigrationTarget.MASTER, true).withBizBaselines(bizBaselines));
                    // BASE 通道关自动基线：菜单/权限类脚本探针空集回退全库业务表判定，会把②步刚建完
                    // 表的全新库误判存量、误基线整组跳过 base 脚本（迁移器对 BASE 通道不写/不看基线）
                    TenantSchemaMigrator.Result base = migrator.migrate(target, ds.getDbType(),
                            MigrateOptions.structure(MigrationTarget.BASE, false));
                    String summary = "迁移完成：\n- 身份结构（TENANT）：" + identity.summary()
                            + "\n- 业务结构（MASTER）：" + result.summary()
                            + "\n- base 副本（BASE）：" + base.summary() + "\n（MASTER 按 biz 基线自主库账本；BASE 幂等自持不基线）";
                    log.info("[mse-tenant-ds] 租户库迁移: key={} → {}", ds.getDsKey(), result.after());
                    return summary;
                } catch (SQLException e) {
                    throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "迁移失败：" + e.getMessage());
                }
            });
        }
    }

    public String syncSchema(Long id, SysTenantDatasource ds) {
        synchronized (schemaLock(ds.getDsKey())) {
            try {
                String summary = withSchemaOpsLock(ds, () -> {
                    try (Connection master = masterConn();
                         Connection target = openTarget(ds)) {
                        TenantSchemaMigrator migrator = requireMigrator();
                        Map<String, String> bizBaselines = migrator.resolveBizBaselines(master, ds.getDbType());
                        TenantSchemaMigrator.Result identity = migrator.migrate(target, ds.getDbType(),
                                MigrateOptions.structure(MigrationTarget.TENANT, true).withBizBaselines(bizBaselines));
                        TenantSchemaMigrator.Result result = migrator.migrate(target, ds.getDbType(),
                                MigrateOptions.structure(MigrationTarget.MASTER, true).withBizBaselines(bizBaselines));
                        TenantSchemaMigrator.Result base = migrator.migrate(target, ds.getDbType(),
                                MigrateOptions.structure(MigrationTarget.BASE, false));
                        StringBuilder summary1 = new StringBuilder("迁移：\n- 身份结构（TENANT）：" + identity.summary()
                                + "\n- 业务结构（MASTER）：" + result.summary()
                                + "\n- base 副本（BASE）：" + base.summary());
                        if (!"mysql".equalsIgnoreCase(ds.getDbType())) {
                            summary1.append("\n结构比对：仅支持 MySQL，达梦/金仓请按方言迁移脚本升级后人工核对");
                        } else {
                            TenantSchemaDiffer.Report report = new TenantSchemaDiffer()
                                    .sync(master, target, TenantDbCloner.ignoreTableNames(
                                            tenantPropertiesProvider.getIfAvailable(MseTenantProperties::new)));
                            summary1.append('\n').append(report.summary());
                        }
                        log.info("[mse-tenant-ds] 租户库结构同步完成: key={}", ds.getDsKey());
                        return summary1.toString();
                    } catch (SQLException e) {
                        throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "结构同步失败：" + e.getMessage());
                    }
                });
                markSyncStatus(id, 1, summary);
                return summary;
            } catch (RuntimeException e) {
                markSyncStatus(id, 2, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                throw e;
            }
        }
    }

    /** 迁移预演（只读）：三通道与实际执行（migrate/syncSchema）同一口径的将执行清单，不碰任何锁、不执行语句 */
    public SchemaPlanView planSchema(SysTenantDatasource ds) {
        TenantSchemaMigrator migrator = requireMigrator();
        try (Connection master = masterConn();
             Connection target = openTarget(ds)) {
            Map<String, String> bizBaselines = migrator.resolveBizBaselines(master, ds.getDbType());
            List<ChannelPlanView> channels = List.of(
                    channelPlan(migrator, target, ds.getDbType(), "身份结构（TENANT）",
                            MigrateOptions.structure(MigrationTarget.TENANT, true).withBizBaselines(bizBaselines)),
                    channelPlan(migrator, target, ds.getDbType(), "业务结构（MASTER）",
                            MigrateOptions.structure(MigrationTarget.MASTER, true).withBizBaselines(bizBaselines)),
                    channelPlan(migrator, target, ds.getDbType(), "base 副本（BASE）",
                            MigrateOptions.structure(MigrationTarget.BASE, false)));
            return new SchemaPlanView(ds.getDbType(), channels);
        } catch (SQLException e) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "迁移预演失败：" + e.getMessage());
        }
    }

    public SchemaStatusView schemaStatus(SysTenantDatasource ds) {
        TenantSchemaMigrator migrator = requireMigrator();
        try (Connection target = openTarget(ds)) {
            // 按 biz 分册聚合（结构通道）：pending/冲突带 biz 前缀，版本取各册最大
            List<TenantSchemaMigrator.Status> statuses = migrator.statusAll(target, ds.getDbType());
            boolean ready = statuses.stream().anyMatch(TenantSchemaMigrator.Status::historyReady);
            String current = statuses.stream().map(TenantSchemaMigrator.Status::currentVersion)
                    .filter(Objects::nonNull).max(String::compareTo).orElse(null);
            List<String> pending = new ArrayList<>();
            List<String> conflicts = new ArrayList<>();
            String lastError = null;
            for (TenantSchemaMigrator.Status status : statuses) {
                status.pending().forEach(p -> pending.add("[" + status.biz() + "] " + p));
                status.conflicts().forEach(c -> conflicts.add("[" + status.biz() + "] " + c));
                if (status.lastError() != null && lastError == null) {
                    lastError = "[" + status.biz() + "] " + status.lastError();
                }
            }
            return new SchemaStatusView(ds.getDbType(), ready, current, null, pending, conflicts, lastError);
        } catch (SQLException e) {
            if (TenantDatasourceSupport.isDatabaseAbsent(e)) {
                // 数据源注册可见早于物理建库：缺库属初始化前预期态，展示待初始化状态而非报错
                log.info("[mse-tenant-ds] 读取结构状态: key={} 物理库尚未创建（未初始化）", ds.getDsKey());
                return new SchemaStatusView(ds.getDbType(), false, null, null, List.of(), List.of(),
                        "租户库尚未创建（请先执行「初始化租户库」）");
            }
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "读取结构状态失败：" + e.getMessage());
        }
    }

    public String executeAdhoc(SysTenantDatasource ds, String sql, String description) {
        synchronized (schemaLock(ds.getDsKey())) {
            return withSchemaOpsLock(ds, () -> {
                try (Connection target = openTarget(ds)) {
                    TenantSchemaMigrator.Result result = requireMigrator().applyAdhoc(target, ds.getDbType(), sql, description);
                    log.info("[mse-tenant-ds] 临时 SQL 执行: key={} {}", ds.getDsKey(), result.after());
                    return "执行成功：" + result.summary();
                } catch (SQLException e) {
                    throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "执行失败：" + e.getMessage());
                }
            });
        }
    }

    /** 迁移器装配校验（批量同步提交前 preflight 亦用） */
    public TenantSchemaMigrator requireMigrator() {
        TenantSchemaMigrator migrator = schemaMigratorProvider.getIfAvailable();
        if (migrator == null) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR.getCode(),
                    "结构迁移器未装配（请确认 starter-mybatis 在依赖中且 TenantSchemaAutoConfiguration 生效）");
        }
        return migrator;
    }

    /** 进程内 per-dsKey 串行锁（初始化任务体共用同一监视器） */
    public Object schemaLock(String dsKey) {
        return schemaLocks.computeIfAbsent(dsKey, k -> new Object());
    }

    /** 目标库 JDBC URL（锁 key 与连接构建同源） */
    private String targetUrl(SysTenantDatasource ds) {
        return TenantDsUrlBuilder.build(TenantDatasourceSupport.toDefinition(ds));
    }

    /**
     * 管理端结构操作（迁移/结构同步/临时 SQL）的跨实例互斥：与懒初始化、手动初始化、启动自动迁移
     * 共用 {@link SchemaOpsLock}（按目标库 JDBC URL）——多实例同时点各自界面对同一库跑脚本的
     * 并发由同一把锁收敛；抢不到锁快速报错（低频管理操作，不轮询等待）。
     */
    private <T> T withSchemaOpsLock(SysTenantDatasource ds, java.util.function.Supplier<T> action) {
        String lockKey = SchemaOpsLock.lockKeyFor(targetUrl(ds));
        String token = SchemaOpsLock.tryAcquire(redisTemplateProvider.getIfAvailable(), lockKey, SCHEMA_OPS_LOCK_TTL);
        if (token == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "其他实例正在对该库执行结构操作（初始化/迁移/同步/懒初始化），请稍后重试");
        }
        try {
            return action.get();
        } finally {
            SchemaOpsLock.release(redisTemplateProvider.getIfAvailable(), lockKey, token);
        }
    }

    private Connection masterConn() throws SQLException {
        DataSource master = masterDataSourceProvider.getIfAvailable();
        if (master == null) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR.getCode(), "主数据源不可用");
        }
        return master.getConnection();
    }

    /** 租户库直连（密码解密 + URL 构建，与 initDatabase/testConnection 同一通道） */
    private Connection openTarget(SysTenantDatasource ds) throws SQLException {
        return DriverManager.getConnection(TenantDsUrlBuilder.build(TenantDatasourceSupport.toDefinition(ds)),
                ds.getUsername(), support.decrypt(ds.getPassword()));
    }

    /** 结构同步状态落行（单库/批量共用）：status 0 同步中 1 成功 2 失败；落行失败不阻断同步本体 */
    private void markSyncStatus(Long id, Integer status, String message) {
        try {
            datasourceMapper.update(null, new LambdaUpdateWrapper<SysTenantDatasource>()
                    .eq(SysTenantDatasource::getId, id)
                    .set(SysTenantDatasource::getSyncStatus, status)
                    .set(SysTenantDatasource::getSyncMessage, TenantDatasourceSupport.truncate(message, SYNC_MESSAGE_MAX_LENGTH))
                    .set(SysTenantDatasource::getSyncTime, java.time.LocalDateTime.now()));
        } catch (Exception e) {
            log.debug("[mse-tenant-ds] 同步状态落行失败（不阻断同步本体）: id={} {}", id, e.getMessage());
        }
    }

    private static ChannelPlanView channelPlan(TenantSchemaMigrator migrator, Connection target, String dbType,
                                               String label, MigrateOptions options) throws SQLException {
        TenantSchemaMigrator.Plan plan = migrator.plan(target, dbType, options);
        return new ChannelPlanView(label, plan.pending().size(),
                plan.pending().stream().map(TenantSchemaMigrator.PlanStep::toString).toList(),
                plan.conflicts(), plan.notes());
    }
}
