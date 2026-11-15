package vip.mseapp.mybatis.datasource;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 元数据本地 JDBC 通道：直接读平台控制面表 sys_tenant_datasource。
 *
 * <p>目标库选择：注册了 platform 数据源（单体双库）落在平台库（sys_tenant* 控制面表所在，
 * platform 通道建表），否则用 primary（mse-platform 微服务自身 primary 即平台库）。
 * 业务微服务主库无平台表时由 starter-bridge 的 RPC 通道实现顶替
 * （@ConditionalOnMissingBean 让位语义）。
 */
public class LocalTenantDsMetadataProvider implements TenantDsMetadataProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalTenantDsMetadataProvider.class);

    private static final String SELECT_BASE =
            "SELECT id, ds_key, ds_mode, service_name, db_type, host, port, db_name, params, username, password, status, remark"
                    + " FROM sys_tenant_datasource WHERE deleted = 0";

    private final ObjectProvider<DynamicRoutingDataSource> routingProvider;

    private final String primaryDsKey;

    /** 平台控制面库数据源 key（与 mse.tenant.schema.platform-ds 同源，缺省 platform） */
    private final String platformDsKey;

    public LocalTenantDsMetadataProvider(ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                         String primaryDsKey) {
        this(routingProvider, primaryDsKey, "platform");
    }

    public LocalTenantDsMetadataProvider(ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                         String primaryDsKey, String platformDsKey) {
        this.routingProvider = routingProvider;
        this.primaryDsKey = primaryDsKey == null || primaryDsKey.isBlank() ? "master" : primaryDsKey.trim();
        this.platformDsKey = platformDsKey == null || platformDsKey.isBlank()
                ? "platform" : platformDsKey.trim();
    }

    @Override
    public List<TenantDatasourceDefinition> listDefinitions(String serviceName) {
        String service = serviceName == null || serviceName.isBlank() ? "*" : serviceName.trim();
        List<TenantDatasourceDefinition> list = new ArrayList<>();
        String sql = SELECT_BASE + " AND (service_name = ? OR service_name = '*')";
        try (Connection conn = masterConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, service);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            logReadFailure(e);
        }
        return list;
    }

    /** 多服务名单条 IN 查询（单体聚合：默认服务名 + 各模块注解服务名 + '*'） */
    @Override
    public List<TenantDatasourceDefinition> listDefinitions(Collection<String> serviceNames) {
        List<String> names = new ArrayList<>();
        for (String serviceName : serviceNames) {
            String service = serviceName == null || serviceName.isBlank() ? "*" : serviceName.trim();
            if (!names.contains(service)) {
                names.add(service);
            }
        }
        List<TenantDatasourceDefinition> list = new ArrayList<>();
        if (names.isEmpty()) {
            return list;
        }
        if (names.size() == 1) {
            return listDefinitions(names.get(0));
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(names.size(), "?"));
        String sql = SELECT_BASE + " AND (service_name IN (" + placeholders + ") OR service_name = '*')";
        try (Connection conn = masterConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < names.size(); i++) {
                ps.setString(i + 1, names.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            logReadFailure(e);
        }
        return list;
    }

    @Override
    public TenantDatasourceDefinition getDefinition(String dsKey) {
        try (Connection conn = masterConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + " AND ds_key = ?")) {
            ps.setString(1, dsKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("读取租户数据源失败: " + dsKey, e);
        }
    }

    /** 表缺失（首次启动建表前属预期）降级 DEBUG 免栈噪声；其余（连不上/权限等）保持 WARN 带栈 */
    private void logReadFailure(SQLException e) {
        String state = e.getSQLState();
        String message = e.getMessage() == null ? "" : e.getMessage();
        if ("42S02".equals(state) || "42P01".equals(state)
                || message.contains("doesn't exist") || message.contains("does not exist")
                || message.contains("不存在") || message.contains("not found")) {
            log.debug("[mse-tenant-ds] sys_tenant_datasource 尚未建表（首次启动建表前属预期），跳过租户数据源加载");
            return;
        }
        log.warn("[mse-tenant-ds] 本地读取 sys_tenant_datasource 失败（平台库未初始化或暂不可用）", e);
    }

    /** 平台控制面库连接：注册了 platform 数据源用之（单体双库），否则 primary 即平台库（mse-platform 微服务） */
    private Connection masterConnection() throws SQLException {
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("dynamic-datasource 未装配，无法访问平台元数据");
        }
        javax.sql.DataSource platform = routing.getDataSources().get(platformDsKey);
        if (platform != null) {
            return platform.getConnection();
        }
        javax.sql.DataSource master = routing.getDataSources().get(primaryDsKey);
        return (master != null ? master : routing).getConnection();
    }

    private TenantDatasourceDefinition map(ResultSet rs) throws SQLException {
        TenantDatasourceDefinition def = new TenantDatasourceDefinition();
        def.setId(rs.getLong("id"));
        def.setDsKey(rs.getString("ds_key"));
        def.setDsMode(rs.getString("ds_mode"));
        def.setServiceName(rs.getString("service_name"));
        def.setDbType(rs.getString("db_type"));
        def.setHost(rs.getString("host"));
        def.setPort(rs.getInt("port"));
        def.setDbName(rs.getString("db_name"));
        def.setParams(rs.getString("params"));
        def.setUsername(rs.getString("username"));
        def.setPassword(rs.getString("password"));
        def.setStatus(rs.getInt("status"));
        def.setRemark(rs.getString("remark"));
        return def;
    }
}
