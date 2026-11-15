package vip.mseapp.service.system.runner;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 分域体检 Runner（mse.divide.audit.enabled，默认开启）：启动时对「平台库 / 本服务租户域主库」做一次
 * 只读清点，发现平台域与租户域身份数据互串时 WARN 提示（<b>不做任何清理动作</b>——租户行出平台库走
 * 换库搬迁接口（POST /tenant/datasource/data-migrate/tenant/{tenantId}）逐租户搬移或运维清理 SQL，
 * 平台行出租户库走运维清理 SQL）：
 * <ul>
 *   <li>① 平台库 sys_user 出现 tenant_id&gt;0 行：历史共享租户数据残留（分域前主库即平台库），
 *       提示走搬移；</li>
 *   <li>② 本服务 primary（租户域主库）sys_user 出现 tenant_id=0 平台行：分域前平台账号种子经
 *       base 通道重放进租户库的遗留，提示核对清理。</li>
 * </ul>
 * 身份表不存在（全新库迁移未完成等）静默跳过。排序紧随 {@link BaseTenantFleetRunner} 之后。
 *
 * <p>口径说明：平台库<b>合法持有</b>定义目录（sys_menu 租户域行 / sys_permission / sys_micro_app
 * 经 base 脚本摄入与编辑批次维护，0.1.0 目录真源上收）——本体检只盯身份表 sys_user 的互串，
 * 目录三表不在清点范围。
 */
public class DomainDivideAuditRunner implements ApplicationRunner, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DomainDivideAuditRunner.class);

    private final ObjectProvider<DynamicRoutingDataSource> routingProvider;

    private final String platformDsKey;

    /** 内置共享库坐标（mse.tenant.shared-main.*，审计②的体检目标；未配置跳过） */
    private final vip.mseapp.mybatis.config.MseTenantProperties.SharedMain sharedMain;

    public DomainDivideAuditRunner(ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                   String platformDsKey,
                                   vip.mseapp.mybatis.config.MseTenantProperties.SharedMain sharedMain) {
        this.routingProvider = routingProvider;
        this.platformDsKey = platformDsKey == null || platformDsKey.isBlank() ? "platform" : platformDsKey.trim();
        this.sharedMain = sharedMain;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1003;
    }

    @Override
    public void run(ApplicationArguments args) {
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        if (routing == null) {
            return;
        }
        try {
            auditPlatformLib(routing);
        } catch (Exception e) {
            log.debug("[mse-divide] 平台库体检跳过: {}", e.getMessage());
        }
        try {
            auditPrimaryLib(routing);
        } catch (Exception e) {
            log.debug("[mse-divide] 租户域主库体检跳过: {}", e.getMessage());
        }
    }

    /** ① 平台库：身份表不应有租户（tenant_id&gt;0）行 */
    private void auditPlatformLib(DynamicRoutingDataSource routing) throws SQLException {
        DataSource platform = routing.getDataSources().get(platformDsKey);
        // primary=平台库（本形态同库），回退链天然成立
        DataSource target = platform != null ? platform : routing;
        if (target == null) {
            return;
        }
        try (Connection conn = target.getConnection()) {
            long count = countRows(conn, "tenant_id <> 0");
            if (count > 0) {
                log.warn("[mse-divide] 平台库身份表发现租户域行 {} 条（sys_user tenant_id>0，分域前主库即平台库的"
                        + "历史残留）——请经换库搬迁接口（/tenant/datasource/data-migrate/tenant/{tenantId}）"
                        + "逐租户搬移或运维 SQL 清理，平台库不应存租户数据", count);
            }
        }
    }

    /** ② 内置共享库（租户域数据承载）：身份表不应有平台（tenant_id=0）行；未配置跳过 */
    private void auditPrimaryLib(DynamicRoutingDataSource routing) throws SQLException {
        if (sharedMain == null || sharedMain.getUrl() == null || sharedMain.getUrl().isBlank()) {
            log.debug("[mse-divide] 未配置内置共享库（纯独立库部署），共享库体检跳过");
            return;
        }
        String url = sharedMain.getUrl();
        int scheme = url.indexOf("//") + 2;
        int slash = url.indexOf('/', scheme);
        String hostPort = url.substring(scheme, slash > 0 ? slash : url.length());
        int colon = hostPort.indexOf(':');
        String host = colon > 0 ? hostPort.substring(0, colon) : hostPort;
        int port = colon > 0 ? Integer.parseInt(hostPort.substring(colon + 1)) : 3306;
        int dot = url.indexOf('?', slash);
        String dbName = slash > 0 ? url.substring(slash + 1, dot > 0 ? dot : url.length()) : "";
        vip.mseapp.mybatis.datasource.TenantDatasourceDefinition shared =
                new vip.mseapp.mybatis.datasource.TenantDatasourceDefinition();
        shared.setDsKey("audit-shared");
        shared.setDbType(sharedMain.getDbType());
        shared.setHost(host);
        shared.setPort(port);
        shared.setDbName(dbName);
        shared.setUsername(sharedMain.getUsername());
        shared.setPassword(sharedMain.getPassword());
        try (Connection conn = java.sql.DriverManager.getConnection(url, shared.getUsername(), shared.getPassword())) {
            long count = countRows(conn, "tenant_id = 0");
            if (count > 0) {
                log.warn("[mse-divide] 缺省共享库发现平台身份行 {} 条（sys_user tenant_id=0，历史种子遗留）——"
                        + "平台账号只应存在平台库，请核对清理（运维清理 SQL）", count);
            }
        }
    }

    private long countRows(Connection conn, String tenantFilter) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM sys_user WHERE " + tenantFilter)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }
}
