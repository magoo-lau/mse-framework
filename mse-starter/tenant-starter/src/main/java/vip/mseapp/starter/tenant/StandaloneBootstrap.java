package vip.mseapp.starter.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.mybatis.schema.MigrateOptions;
import vip.mseapp.mybatis.schema.MigrationTarget;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;
import vip.mseapp.redis.TenantCache;
import vip.mseapp.service.system.security.LicenseHolder;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * standalone 启动自足化：脱离管理平台的租户单体，首次启动对租户库（master）补齐全部运行结构：
 * <ol>
 *   <li>TENANT + MASTER 通道脚本直建（身份表 + 业务表，幂等账本续跑）；</li>
 *   <li>BASE 通道脚本直建（菜单/权限点/子应用注册——各积木 db/base 注册数据，租户库本地副本）；</li>
 *   <li>sys_tenant 简版表 + 本租户自身行（登录校验数据源，平台库不存在时的本地权威记录）；</li>
 *   <li>sys_config 平台行 → 本租户行复制（登录安全/多端登录等参数租户可读可覆盖）；</li>
 *   <li>每租户种子脚本（${tenant_id}=本租户）。</li>
 * </ol>
 * 完成后租户库自足：登录/菜单/权限/业务数据全部本地闭环，仅权限域真源由 license 凭证驱动。
 */
public class StandaloneBootstrap implements ApplicationRunner, org.springframework.core.Ordered {

    /**
     * 先于 MasterSchemaAutoMigration（该 Runner 在 HIGHEST_PRECEDENCE+1000，见其注释）：
     * 全新库必须先直建身份表（TENANT 通道），master/base 通道脚本可能 JOIN sys_user 等
     * 身份表，顺序颠倒会在空库上迁移失败阻断启动。
     */
    @Override
    public int getOrder() {
        return org.springframework.core.Ordered.HIGHEST_PRECEDENCE;
    }

    private static final Logger log = LoggerFactory.getLogger(StandaloneBootstrap.class);

    private final JdbcTemplate jdbcTemplate;
    private final TenantSchemaMigrator migrator;
    private final LicenseHolder licenseHolder;
    private final ObjectProvider<TenantCache> tenantCacheProvider;
    private final long tenantId;
    private final String tenantCode;
    private final String tenantName;

    public StandaloneBootstrap(JdbcTemplate jdbcTemplate, TenantSchemaMigrator migrator,
                               LicenseHolder licenseHolder,
                               ObjectProvider<TenantCache> tenantCacheProvider,
                               @Value("${mse.standalone.tenant-id:1}") long tenantId,
                               @Value("${mse.standalone.tenant-code:tenant}") String tenantCode,
                               @Value("${mse.standalone.tenant-name:租户私有部署}") String tenantName) {
        this.jdbcTemplate = jdbcTemplate;
        this.migrator = migrator;
        this.licenseHolder = licenseHolder;
        this.tenantCacheProvider = tenantCacheProvider;
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            String dbType = TenantSchemaMigrator.detectDbType(conn);
            // ①/② 结构与基座数据直建（BASE 关自动基线：幂等脚本收敛，BASELINE 语义不适用全新副本）
            migrator.migrate(conn, dbType, MigrateOptions.structure(MigrationTarget.TENANT, true));
            migrator.migrate(conn, dbType, MigrateOptions.structure(MigrationTarget.MASTER, true));
            migrator.migrate(conn, dbType, MigrateOptions.structure(MigrationTarget.BASE, false));
            // ③ 本租户权威行（登录校验数据源；平台库不存在时的本地记录）
            ensureTenantRow(conn, dbType);
            // ④ 平台参数行 → 本租户行（幂等复制）
            replicateConfigs(conn, dbType);
            // ⑤ 每租户种子
            migrator.migrate(conn, dbType,
                    MigrateOptions.forTenant(tenantId, Map.of()));
            // ⑥ 调度管理面（表 + 租户端菜单/权限——仅 standalone 播种，SaaS 租户侧无此入口）
            ensureScheduleAdmin(conn, dbType);
        }
        // 本租户行缓存失效一次：重启后首次访问回源最新行（运行期由 TTL + 导入失效兜底）
        TenantCache tenantCache = tenantCacheProvider.getIfAvailable();
        if (tenantCache != null) {
            tenantCache.evictTenant(tenantId);
        }
        LicenseHolder.State state = licenseHolder.state();
        if (state == LicenseHolder.State.MISSING) {
            log.warn("[mse-standalone] 授权凭证缺失：菜单/权限域为空（登录将被锁定），请配置 mse.license.* 导入平台签发的凭证");
        } else if (state == LicenseHolder.State.GRACE) {
            log.warn("[mse-standalone] {}", licenseHolder.graceMessage());
        } else {
            log.info("[mse-standalone] 启动自足化完成: tenant={}({}) license={}",
                    tenantCode, tenantId, state);
        }
    }

    /** sys_tenant 简版表 + 本租户行（幂等；仅 standalone 本地权威记录，平台侧结构以 service-platform 为准） */
    private void ensureTenantRow(Connection conn, String dbType) {
        String ddl = switch (dbType) {
            case "dm" -> """
                    CREATE TABLE IF NOT EXISTS sys_tenant (
                        id BIGINT NOT NULL,
                        code VARCHAR(30) NULL,
                        name VARCHAR(60) NULL,
                        package_id BIGINT NULL,
                        ds_key VARCHAR(60) NULL,
                        domain VARCHAR(128) NULL,
                        status TINYINT DEFAULT 0 NOT NULL,
                        expire_time TIMESTAMP NULL,
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        deleted TINYINT DEFAULT 0 NOT NULL,
                        CONSTRAINT pk_standalone_tenant PRIMARY KEY (id)
                    )""";
            case "kingbase" -> """
                    CREATE TABLE IF NOT EXISTS sys_tenant (
                        id BIGINT NOT NULL,
                        code VARCHAR(30) NULL,
                        name VARCHAR(60) NULL,
                        package_id BIGINT NULL,
                        ds_key VARCHAR(60) NULL,
                        domain VARCHAR(128) NULL,
                        status SMALLINT DEFAULT 0 NOT NULL,
                        expire_time TIMESTAMP NULL,
                        create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                        deleted SMALLINT DEFAULT 0 NOT NULL,
                        CONSTRAINT pk_standalone_tenant PRIMARY KEY (id)
                    )""";
            default -> """
                    CREATE TABLE IF NOT EXISTS sys_tenant (
                        id BIGINT NOT NULL,
                        code VARCHAR(30) NULL,
                        name VARCHAR(60) NULL,
                        package_id BIGINT NULL,
                        ds_key VARCHAR(60) NULL,
                        domain VARCHAR(128) NULL,
                        status TINYINT NOT NULL DEFAULT 0,
                        expire_time DATETIME NULL,
                        create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        deleted TINYINT NOT NULL DEFAULT 0,
                        PRIMARY KEY (id)
                    ) ENGINE = InnoDB COMMENT ='租户（standalone 本地权威行）'""";
        };
        jdbcTemplate.execute(ddl);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_tenant WHERE id = ?", Integer.class, tenantId);
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO sys_tenant (id, code, name, ds_key, status, deleted) VALUES (?, ?, ?, 'standalone', 0, 0)",
                    tenantId, tenantCode, tenantName);
            log.info("[mse-standalone] 本租户权威行已写入: id={} code={}", tenantId, tenantCode);
        }
    }

    /**
     * sys_config 平台行（tenant_id=0）→ 本租户行（同 config_key 不存在时复制；租户可再覆盖）。
     * id 一律重新分配（当前 max(id) + 窗口号）：平台行原 id 与 BASE 通道种子/导入数据的既有行撞主键
     * （如 Duplicate entry '4' for sys_config.PRIMARY），不能复用源行 id。
     */
    private void replicateConfigs(Connection conn, String dbType) {
        Integer copied = jdbcTemplate.update("""
                INSERT INTO sys_config (id, tenant_id, config_name, config_key, config_value, config_type, deleted)
                SELECT (SELECT COALESCE(MAX(t.id), 0) FROM sys_config t) + ROW_NUMBER() OVER (ORDER BY c.id),
                       ?, c.config_name, c.config_key, c.config_value, c.config_type, 0
                  FROM sys_config c
                 WHERE c.tenant_id = 0
                   AND NOT EXISTS (SELECT 1 FROM sys_config tt
                                    WHERE tt.tenant_id = ? AND tt.config_key = c.config_key AND tt.deleted = 0)
                """, tenantId, tenantId);
        if (copied != null && copied > 0) {
            log.info("[mse-standalone] 平台参数副本已生成本租户行: {} 条", copied);
        }
    }

    /** 供登录链路（AuthController 经 RemoteTenantService 桩）使用的本地租户状态 */
    record LocalTenant(long id, String code, String name, int status, java.time.LocalDateTime expireTime) {
    }

    /**
     * 调度管理面自足化（幂等）：sys_schedule_task / sys_schedule_task_log 直建
     * （与 service-platform 的 V20261001_06/_07 同构，standalone 单库落主库——离线部署
     * 租户库存在任务调度相关表）。租户端菜单 195 与权限点 1911-1914 不在此播种：
     * 由 service-system 的 base 通道脚本 V20261001_05 随 BASE 直建提供（单一来源，
     * 可见性由套餐圈定——离线导出签发凭证时经 extra-menu-ids 默认并入）。
     */
    private void ensureScheduleAdmin(Connection conn, String dbType) {
        String tinyint = "kingbase".equals(dbType) || "dm".equals(dbType) ? "SMALLINT" : "TINYINT";
        String datetime = "mysql".equals(dbType) ? "DATETIME" : "TIMESTAMP";
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_schedule_task (
                    id BIGINT NOT NULL,
                    task_key VARCHAR(60) NOT NULL,
                    owner_service VARCHAR(60) NULL,
                    scheduler_mode VARCHAR(20) NULL,
                    task_name VARCHAR(100) NULL,
                    description VARCHAR(255) NULL,
                    task_type VARCHAR(20) NOT NULL,
                    task_category VARCHAR(20) NOT NULL DEFAULT 'biz',
                    schedule_expr VARCHAR(120) NULL,
                    expr_override VARCHAR(120) NULL,
                    remark VARCHAR(1000) NULL,
                    enabled %s NOT NULL DEFAULT 1,
                    trigger_pending %s NOT NULL DEFAULT 0,
                    last_run_time %s NULL,
                    last_duration_ms BIGINT NOT NULL DEFAULT 0,
                    last_result VARCHAR(20) NULL,
                    last_error VARCHAR(1000) NULL,
                    run_count BIGINT NOT NULL DEFAULT 0,
                    error_count BIGINT NOT NULL DEFAULT 0,
                    report_time %s NULL,
                    create_by BIGINT NULL,
                    create_time %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_by BIGINT NULL,
                    update_time %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    deleted %s NOT NULL DEFAULT 0,
                    PRIMARY KEY (id),
                    CONSTRAINT uk_standalone_schedule_task UNIQUE (task_key)
                )""".formatted(tinyint, tinyint, datetime, datetime, datetime, datetime, tinyint));
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_schedule_task_log (
                    id VARCHAR(80) NOT NULL,
                    task_key VARCHAR(60) NOT NULL,
                    owner_service VARCHAR(60) NULL,
                    start_time %s NOT NULL,
                    duration_ms BIGINT NOT NULL DEFAULT 0,
                    result VARCHAR(20) NOT NULL,
                    error_msg VARCHAR(1000) NULL,
                    create_time %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id)
                )""".formatted(datetime, datetime));
        // 调度器设置（「调度器」页签：租户并发闸门上限热更；与 service-platform 的 V20261003_01 同构）
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_schedule_setting (
                    id BIGINT NOT NULL,
                    setting_key VARCHAR(60) NOT NULL,
                    setting_value VARCHAR(500) NULL,
                    remark VARCHAR(255) NULL,
                    create_by BIGINT NULL,
                    create_time %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    update_by BIGINT NULL,
                    update_time %s NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    deleted %s NOT NULL DEFAULT 0,
                    PRIMARY KEY (id),
                    CONSTRAINT uk_standalone_schedule_setting UNIQUE (setting_key)
                )""".formatted(datetime, datetime, tinyint));
        // MySQL 无 CREATE INDEX IF NOT EXISTS：以重复索引异常吞掉实现幂等重启
        try {
            jdbcTemplate.update(
                    "CREATE INDEX idx_standalone_schedule_log_task ON sys_schedule_task_log (task_key, start_time)");
        } catch (Exception ignored) {
            // 索引已存在
        }
        // 旧版直建表补「任务分类」列（CREATE IF NOT EXISTS 对已存在的表不加列）：重复列异常吞掉幂等
        try {
            jdbcTemplate.update(
                    "ALTER TABLE sys_schedule_task ADD COLUMN task_category VARCHAR(20) NOT NULL DEFAULT 'biz'");
        } catch (Exception ignored) {
            // 列已存在
        }
        // 退役遗留的 schedule-state-sync 行：该同步任务已改为独立监控线程（不注册进调度中心），
        // 历史版本关闭任务时可能把它一并停用，残留行会成为管理页上的僵尸条目
        jdbcTemplate.update("DELETE FROM sys_schedule_task WHERE task_key = 'schedule-state-sync'");
        log.info("[mse-standalone] 调度管理面就绪：sys_schedule_task / sys_schedule_task_log（菜单 195 由 base 通道播种）");
    }

    /** 查本租户行（JDBC；standalone 仅一租户） */
    static LocalTenant loadLocalTenant(JdbcTemplate template, long id) {
        List<Map<String, Object>> rows = template.queryForList(
                "SELECT id, code, name, status, expire_time FROM sys_tenant WHERE id = ? AND deleted = 0", id);
        if (rows.isEmpty()) {
            throw new ServiceException(GlobalErrorCode.TENANT_NOT_EXISTS);
        }
        Map<String, Object> row = rows.get(0);
        Object expire = row.get("expire_time");
        return new LocalTenant(id, String.valueOf(row.get("code")), String.valueOf(row.get("name")),
                ((Number) row.get("status")).intValue(),
                expire == null ? null : java.time.LocalDateTime.parse(String.valueOf(expire).replace(' ', 'T')));
    }
}
