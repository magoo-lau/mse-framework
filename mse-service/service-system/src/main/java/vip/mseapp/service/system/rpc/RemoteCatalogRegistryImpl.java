package vip.mseapp.service.system.rpc;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import vip.mseapp.api.system.RemoteCatalogRegistry;
import vip.mseapp.api.system.dto.BaseScriptDescriptor;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.mybatis.schema.TenantMigrationScanner;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 定义目录变更批次契约实现：读平台库 {@code sys_catalog_change}（直连 platform 数据源，
 * 与 BaseDataRegistryServiceImpl 同款落点解析）。
 *
 * <p>批次表未建（P2 之前的部署 / 全新库无编辑批次）返回空清单不报错——追平链按「无变更」
 * 短路。载体约定：biz=catalog、version=zeroPad10(id)、script=catalog/change/&lt;id&gt;、
 * checksum=sql 摘要（与批次扇出侧渲染一致，防账本 checksum 漂移）。
 */
@RpcService(RemoteCatalogRegistry.class)
public class RemoteCatalogRegistryImpl implements RemoteCatalogRegistry {

    private static final Logger log = LoggerFactory.getLogger(RemoteCatalogRegistryImpl.class);

    private static final String CATALOG_BIZ = "catalog";

    private final ObjectProvider<DynamicRoutingDataSource> routingProvider;

    private final String primaryDsKey;

    private final String platformDsKey;

    public RemoteCatalogRegistryImpl(ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                     String primaryDsKey, String platformDsKey) {
        this.routingProvider = routingProvider;
        this.primaryDsKey = primaryDsKey == null || primaryDsKey.isBlank() ? "master" : primaryDsKey.trim();
        this.platformDsKey = platformDsKey == null || platformDsKey.isBlank() ? "platform" : platformDsKey.trim();
    }

    @Override
    public List<BaseScriptDescriptor> listChanges(long sinceId, int limit) {
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        if (routing == null) {
            return List.of();
        }
        DataSource platform = routing.getDataSources().get(platformDsKey);
        DataSource target = platform != null ? platform : routing.getDataSources().get(primaryDsKey);
        if (target == null) {
            target = routing;
        }
        try (Connection conn = target.getConnection()) {
            return query(conn, sinceId, limit);
        } catch (SQLException e) {
            // 批次表未建等场景按无变更处理（debug 级）；真实故障由调用方降级 + 车队扇出兜底
            log.debug("[mse-catalog] 目录批次读取跳过: {}", e.getMessage());
            return List.of();
        }
    }

    private List<BaseScriptDescriptor> query(Connection conn, long sinceId, int limit) throws SQLException {
        List<BaseScriptDescriptor> result = new ArrayList<>();
        String sql = "SELECT id, sql_text FROM sys_catalog_change WHERE id > ? ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sinceId);
            ps.setMaxRows(Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong(1);
                    String sqlText = rs.getString(2);
                    BaseScriptDescriptor descriptor = new BaseScriptDescriptor();
                    descriptor.setBiz(CATALOG_BIZ);
                    descriptor.setVersion(String.format("%010d", id));
                    descriptor.setDialect(TenantSchemaMigrator.detectDbType(conn));
                    descriptor.setScript("catalog/change"); // 前缀：方言与 id 由 RpcCatalogChangeSource 展开
                    descriptor.setSql(sqlText);
                    descriptor.setChecksum(TenantMigrationScanner.checksum(sqlText));
                    descriptor.setBaseScope("TENANT");
                    result.add(descriptor);
                }
            }
        }
        return result;
    }
}
