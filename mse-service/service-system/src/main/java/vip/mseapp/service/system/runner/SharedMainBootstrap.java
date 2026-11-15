package vip.mseapp.service.system.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import vip.mseapp.mybatis.config.MseTenantProperties;
import vip.mseapp.mybatis.schema.MigrateOptions;
import vip.mseapp.mybatis.schema.MigrationTarget;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 内置共享库引导（0.1.0 v2，mse.tenant.shared-main.* 每服务自declare）：注册归
 * starter-mybatis 通用装载（各服务自读自配、键默认=本服务应用名、显式 key:* =通用），
 * 本 Runner 仅按 mse-system 自己的坐标建库（MySQL）+ eager 建 TENANT 身份表——保证车队
 * BASE 重放不被 identityReady 跳过（MASTER 业务表由各服务懒初始化随首流量收敛）。
 *
 * <p>未配置坐标=不动作（纯独立库部署）；standalone 单库形态跳过。排序
 * MasterSchemaAutoMigration（+1000）之后、BaseTenantFleetRunner（+1002）之前。
 */
public class SharedMainBootstrap implements ApplicationRunner, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SharedMainBootstrap.class);

    private final Environment environment;

    private final TenantSchemaMigrator migrator;

    private final MseTenantProperties tenantProperties;

    public SharedMainBootstrap(Environment environment, TenantSchemaMigrator migrator,
                               MseTenantProperties tenantProperties) {
        this.environment = environment;
        this.migrator = migrator;
        this.tenantProperties = tenantProperties;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1001;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (Boolean.parseBoolean(environment.getProperty("mse.standalone.enabled", "false"))) {
            return;
        }
        String url = environment.getProperty("mse.tenant.shared-main.url", "").trim();
        String username = environment.getProperty("mse.tenant.shared-main.username", "").trim();
        String password = environment.getProperty("mse.tenant.shared-main.password", "");
        String dbType = environment.getProperty("mse.tenant.shared-main.db-type", "mysql").trim();
        if (url.isEmpty() || username.isEmpty()) {
            log.info("[mse-shared-main] 未配置 mse.tenant.shared-main.*（纯独立库部署），内置共享库引导跳过");
            return;
        }
        if ("mysql".equalsIgnoreCase(dbType)) {
            createDatabaseIfAbsent(url, username, password);
        }
        if (!tenantProperties.getSchema().isSharedMainEagerInit()) {
            return;
        }
        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            TenantSchemaMigrator.Result result = migrator.migrate(conn, dbType,
                    MigrateOptions.structure(MigrationTarget.TENANT, true));
            if (result.applied() > 0) {
                log.info("[mse-shared-main] 内置共享库身份表直建: {} → {}（应用 {}、失败 {}）",
                        result.before(), result.after(), result.applied(), result.failed());
            }
            if (result.failed() > 0) {
                log.warn("[mse-shared-main] 内置共享库身份表直建存在失败（懒初始化兜底）: {}", result.firstError());
            }
        } catch (Exception e) {
            log.warn("[mse-shared-main] 内置共享库身份表直建跳过（连接/建表失败，懒初始化兜底）: {}", e.getMessage());
        }
    }

    /** MySQL 建库（幂等；dm/kingbase 沿「手动建库」既有约定） */
    private void createDatabaseIfAbsent(String url, String username, String password) {
        int slash = url.indexOf('/', url.indexOf("//") + 2);
        if (slash < 0) {
            return;
        }
        int dot = url.indexOf('?', slash);
        String dbName = url.substring(slash + 1, dot > 0 ? dot : url.length()).trim();
        String serverUrl = url.substring(0, slash) + "/?useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(serverUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS `" + dbName
                    + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
        } catch (SQLException e) {
            log.warn("[mse-shared-main] 内置共享库建库跳过（已存在或权限受限时无害）: {}", e.getMessage());
        }
    }
}
