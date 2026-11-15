package vip.mseapp.rpc.bridge;

import vip.mseapp.api.system.RemoteCatalogRegistry;
import vip.mseapp.api.system.dto.BaseScriptDescriptor;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.mybatis.datasource.CatalogChangeSource;
import vip.mseapp.mybatis.schema.BaseScope;
import vip.mseapp.mybatis.schema.MigrationTarget;
import vip.mseapp.mybatis.schema.SchemaMigration;

import java.util.ArrayList;
import java.util.List;

/**
 * 目录变更批次来源的 RPC 通道（消费 mse-system 的 {@link RemoteCatalogRegistry}）：
 * 新租户库懒初始化追平平台端目录编辑用。
 *
 * <p>双形态与基座桥接一致：单体 local 直调容器内唯一实现、微服务 dubbo 远调
 * （@RpcReference 注入，与 RpcTenantDsMetadataProvider 同范式）。
 */
public class RpcCatalogChangeSource implements CatalogChangeSource {

    @RpcReference
    private RemoteCatalogRegistry remoteCatalogRegistry;

    @Override
    public List<SchemaMigration> changesSince(long sinceId, int limit) {
        List<BaseScriptDescriptor> changes = remoteCatalogRegistry.listChanges(sinceId, limit);
        List<SchemaMigration> migrations = new ArrayList<>();
        if (changes == null) {
            return migrations;
        }
        for (BaseScriptDescriptor change : changes) {
            if (change == null || change.getSql() == null || change.getSql().isBlank()) {
                continue;
            }
            String baseScript = change.getScript() == null ? "catalog/change" : change.getScript().trim();
            for (String dialect : List.of("mysql", "dm", "kingbase")) {
                // 与 CatalogChangeDispatcher 同键格式（catalog/change/<方言>/<id>），懒初始化按目标库
                // 方言过滤后与车队扇出共享同一账本键，互为幂等
                migrations.add(new SchemaMigration(
                        change.getBiz() == null ? "catalog" : change.getBiz().trim(),
                        change.getVersion(), "目录变更批次", SchemaMigration.TYPE_CLASSPATH,
                        MigrationTarget.BASE, false, baseScript + "/" + dialect + "/" + change.getVersion(),
                        change.getSql(), change.getChecksum(), BaseScope.of(change.getBaseScope())));
            }
        }
        return migrations;
    }
}
