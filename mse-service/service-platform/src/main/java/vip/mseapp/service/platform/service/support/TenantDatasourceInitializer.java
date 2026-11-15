package vip.mseapp.service.platform.service.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.mybatis.datasource.TenantDsUrlBuilder;
import vip.mseapp.mybatis.schema.MigrateOptions;
import vip.mseapp.mybatis.schema.MigrationTarget;
import vip.mseapp.mybatis.schema.SchemaOpsLock;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;
import vip.mseapp.service.platform.entity.SysTenantDatasource;
import vip.mseapp.service.platform.mapper.SysTenantDatasourceMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 租户库初始化（自 SysTenantDatasourceServiceImpl 拆出的内部协作者，业务工程勿依赖）：
 * 建库 + 三通道脚本直建的异步登记与后台执行、状态落行、提交期执行期锁探测。
 */
// 控制面协作者：类级 NOT_SUPPORTED 防错库堤坝，勿删（范式见 SysTenantRouteServiceImpl 类注释）
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class TenantDatasourceInitializer {

    private static final Logger log = LoggerFactory.getLogger(TenantDatasourceInitializer.class);

    /** 初始化失败原因落库截断长度（init_message VARCHAR(500)） */
    private static final int INIT_ERROR_MAX_LENGTH = 500;

    private final SysTenantDatasourceMapper datasourceMapper;
    private final ObjectProvider<TenantSchemaMigrator> schemaMigratorProvider;
    private final ObjectProvider<DataSource> masterDataSourceProvider;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final TenantDatasourceSupport support;
    private final TenantSchemaOpsService schemaOps;

    /** 进程内 per-dsKey 互斥：同数据源初始化任务进行中拒绝重复提交 */
    private final Set<String> initInFlight = ConcurrentHashMap.newKeySet();

    /** 串行执行即可：初始化是低频管理动作；有界队列防积压（满则提交失败标「失败」，跨实例互斥由统一锁兜底） */
    private final ExecutorService initExecutor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, java.util.concurrent.TimeUnit.MILLISECONDS,
            new java.util.concurrent.ArrayBlockingQueue<>(16),
            r -> {
                Thread thread = new Thread(r, "tenant-ds-init");
                thread.setDaemon(true);
                return thread;
            },
            new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());

    public TenantDatasourceInitializer(SysTenantDatasourceMapper datasourceMapper,
                                       ObjectProvider<TenantSchemaMigrator> schemaMigratorProvider,
                                       ObjectProvider<DataSource> masterDataSourceProvider,
                                       ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                       TenantDatasourceSupport support,
                                       TenantSchemaOpsService schemaOps) {
        this.datasourceMapper = datasourceMapper;
        this.schemaMigratorProvider = schemaMigratorProvider;
        this.masterDataSourceProvider = masterDataSourceProvider;
        this.redisTemplateProvider = redisTemplateProvider;
        this.support = support;
        this.schemaOps = schemaOps;
    }

    /**
     * 建库 + 脚本直建租户库结构（异步）：全新库要执行「TENANT 身份表 + MASTER 各积木业务表 +
     * BASE base 副本」三通道全量脚本，耗时随业务积木增长且不可控，同步执行必撞前端/RPC 超时——
     * 本方法只做登记（快速校验 + 行状态落「初始化中」+ 进程内互斥 + Redis 执行期锁探测防重复
     * 提交）即返回，耗时动作在后台单线程执行（同 service 包 TenantProvisioner 异步开通器先例），
     * 状态经 initStatus 轮询或列表 init_status 展示。幂等可重跑：
     * 脚本 IF NOT EXISTS + 账本续跑，失败可重新初始化；进程重启丢失的排队任务由
     * TenantDsInitRecoveryRunner 启动对账自动重新提交。
     *
     * <p>锁时机：提交期仅「探测」执行期锁 key（存在即拒绝，防重复点击/跨实例重复提交，
     * 非原子但窗口极小）；真正的跨实例互斥在任务<b>开始执行时</b>经 {@link SchemaOpsLock}
     * 取得（按目标库 URL，与懒初始化/启动迁移/管理端结构操作同一把锁）——排队期不空占锁，
     * 长队列等待不再白白耗掉 15min 锁 TTL。
     */
    public String initDatabase(SysTenantDatasource ds) {
        if (!"mysql".equalsIgnoreCase(ds.getDbType())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "自动建库初始化当前仅支持 MySQL；达梦/金仓请按对应方言手动建库后使用「迁移」应用版本化脚本");
        }
        if (!initInFlight.add(ds.getDsKey())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "该数据源已有初始化任务在执行，请稍后刷新查看状态");
        }
        try {
            if (!initSubmitProbe(ds)) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        "其他服务实例正在对该库执行结构操作（初始化/迁移/同步），请稍后刷新查看状态");
            }
            markInitStatus(ds.getId(), 0, null);
        } catch (RuntimeException e) {
            initInFlight.remove(ds.getDsKey());
            throw e;
        }
        try {
            initExecutor.execute(() -> runInitTask(ds.getId(), ds));
        } catch (RuntimeException e) {
            markInitStatus(ds.getId(), 2, "任务提交失败（队列已满或执行器已关闭，请稍后重试）：" + e.getMessage());
            initInFlight.remove(ds.getDsKey());
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR.getCode(), "初始化任务提交失败，请稍后重试");
        }
        return "初始化任务已提交：建库（若不存在）+ 全量版本化脚本直建在后台执行（全新库约 1~2 分钟），结果经状态轮询或「结构状态」查看";
    }

    /** 后台任务体：执行期取统一锁（token + Lua 释放；拿不到=他实例在跑，标失败等人工/恢复重提） */
    private void runInitTask(Long id, SysTenantDatasource ds) {
        String lockKey = SchemaOpsLock.lockKeyFor(targetUrl(ds));
        String token = SchemaOpsLock.tryAcquire(redisTemplateProvider.getIfAvailable(), lockKey,
                TenantSchemaOpsService.SCHEMA_OPS_LOCK_TTL);
        if (token == null) {
            markInitStatus(id, 2, "其他实例正在对该库执行结构操作，本次初始化未执行（请稍后重新发起）");
            initInFlight.remove(ds.getDsKey());
            return;
        }
        try {
            String summary;
            synchronized (schemaOps.schemaLock(ds.getDsKey())) {
                summary = doInitDatabase(ds);
            }
            markInitStatus(id, 1, summary);
            log.info("[mse-tenant-ds] 租户库后台初始化完成: key={} → {}", ds.getDsKey(), summary);
        } catch (Exception e) {
            String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            markInitStatus(id, 2, error.length() > INIT_ERROR_MAX_LENGTH
                    ? error.substring(0, INIT_ERROR_MAX_LENGTH) : error);
            log.warn("[mse-tenant-ds] 租户库后台初始化失败（可修复后重新初始化）: key={}", ds.getDsKey(), e);
        } finally {
            SchemaOpsLock.release(redisTemplateProvider.getIfAvailable(), lockKey, token);
            initInFlight.remove(ds.getDsKey());
        }
    }

    /** 提交期探测：执行期锁 key 存在即认为有实例在跑（快速拒绝；探测与执行间的竞态由执行期锁兜底） */
    private boolean initSubmitProbe(SysTenantDatasource ds) {
        StringRedisTemplate template = redisTemplateProvider.getIfAvailable();
        if (template == null) {
            return true;
        }
        try {
            Boolean exists = template.hasKey(SchemaOpsLock.lockKeyFor(targetUrl(ds)));
            return !Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.debug("[mse-tenant-ds] Redis 探测不可用（进程内互斥兜底）: {}", e.getMessage());
            return true;
        }
    }

    /** 目标库 JDBC URL（锁 key 与连接构建同源） */
    private String targetUrl(SysTenantDatasource ds) {
        return TenantDsUrlBuilder.build(TenantDatasourceSupport.toDefinition(ds));
    }

    /**
     * 初始化动作本体（幂等，账本续跑）：无库名连接 CREATE DATABASE → 对目标库依次执行
     * TENANT 通道（基座身份表）+ MASTER 通道（各积木业务表，按 biz 基线自主库账本）+
     * BASE 通道（base 副本从零自持，探针自判存量）。
     */
    private String doInitDatabase(SysTenantDatasource ds) {
        String dbName = TenantDatasourceSupport.identifier(ds.getDbName(), "库名");
        String password = support.decrypt(ds.getPassword());
        DataSource master = masterDataSourceProvider.getIfAvailable();
        if (master == null) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR.getCode(), "主数据源不可用");
        }
        String urlNoDb = mysqlUrl(ds, null);
        try (Connection noDb = DriverManager.getConnection(urlNoDb, ds.getUsername(), password);
             Statement stmt = noDb.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS `" + dbName
                    + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
        } catch (SQLException e) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "建库失败：" + e.getMessage());
        }

        int applied;
        try (Connection masterConn = master.getConnection();
             Connection target = DriverManager.getConnection(TenantDsUrlBuilder.build(TenantDatasourceSupport.toDefinition(ds)), ds.getUsername(), password)) {
            TenantSchemaMigrator migrator = requireMigrator();
            Map<String, String> bizBaselines = migrator.resolveBizBaselines(masterConn, ds.getDbType());
            TenantSchemaMigrator.Result identity = migrator.migrate(target, ds.getDbType(),
                    MigrateOptions.structure(MigrationTarget.TENANT, true).withBizBaselines(bizBaselines));
            TenantSchemaMigrator.Result structure = migrator.migrate(target, ds.getDbType(),
                    MigrateOptions.structure(MigrationTarget.MASTER, true).withBizBaselines(bizBaselines));
            // BASE 通道关自动基线：base 副本幂等自持，探针空集回退全库业务表判定会把②步刚建完表的
            // 全新库误判存量、误基线整组跳过 base 脚本（迁移器对 BASE 通道本就不写/不看基线）
            TenantSchemaMigrator.Result base = migrator.migrate(target, ds.getDbType(),
                    MigrateOptions.structure(MigrationTarget.BASE, false));
            String problem = TenantDatasourceSupport.firstProblem(identity, structure, base);
            if (problem != null) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "脚本直建失败：" + problem);
            }
            applied = identity.applied() + structure.applied() + base.applied();
        } catch (SQLException | IllegalArgumentException e) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "脚本直建失败：" + e.getMessage());
        }
        return "已建库 " + dbName + "；版本化脚本直建完成（身份表 + 业务表 + base 副本）：本次应用 " + applied
                + " 条（幂等：账本续跑，已应用脚本自动跳过）";
    }

    /** 初始化任务状态落库：message 允许置 null（成功覆盖历史失败原因）；置「初始化中」时记录发起时间（启动对账恢复用） */
    private void markInitStatus(Long id, Integer status, String message) {
        LambdaUpdateWrapper<SysTenantDatasource> wrapper = new LambdaUpdateWrapper<SysTenantDatasource>()
                .eq(SysTenantDatasource::getId, id)
                .set(SysTenantDatasource::getInitStatus, status)
                .set(SysTenantDatasource::getInitMessage, message);
        if (Integer.valueOf(0).equals(status)) {
            wrapper.set(SysTenantDatasource::getInitStartedAt, java.time.LocalDateTime.now());
        }
        datasourceMapper.update(null, wrapper);
    }

    private TenantSchemaMigrator requireMigrator() {
        TenantSchemaMigrator migrator = schemaMigratorProvider.getIfAvailable();
        if (migrator == null) {
            throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR.getCode(),
                    "结构迁移器未装配（请确认 starter-mybatis 在依赖中且 TenantSchemaAutoConfiguration 生效）");
        }
        return migrator;
    }

    /** MySQL 连接串；dbName 为 null 时不选库（建库阶段） */
    private String mysqlUrl(SysTenantDatasource ds, String dbName) {
        String base = "jdbc:mysql://" + ds.getHost().trim() + ":" + ds.getPort() + "/"
                + (dbName == null ? "" : dbName)
                + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
                + "&allowPublicKeyRetrieval=true&useInformationSchema=true";
        return TenantDatasourceSupport.hasText(ds.getParams()) ? base + "&" + ds.getParams().trim() : base;
    }

    @PreDestroy
    public void shutdownInitExecutor() {
        initExecutor.shutdownNow();
    }
}
