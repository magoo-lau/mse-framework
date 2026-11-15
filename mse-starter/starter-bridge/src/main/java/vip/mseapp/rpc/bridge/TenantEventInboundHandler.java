package vip.mseapp.rpc.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import vip.mseapp.mq.TenantEvent;
import vip.mseapp.mybatis.schema.TenantDataMigrator;

/**
 * 数据面租户事件的统一处理端：进程内 Spring 事件（{@link TenantDataUpgradeListener}，单体聚合
 * 形态直达）与 MQ 消息（{@link TenantEventMqListener}，微服务形态经 mse-tenant-events 主题）
 * 双通道汇入本类，保证两条通道行为一致。
 *
 * <ul>
 *   <li>任意事件先失效本地快照（tenantId=null 表示全局变更——如路由默认值调整，全量失效）；</li>
 *   <li>tenant.created / tenant.data-upgrade / tenant.activated：为该租户补跑 pending 每租户脚本
 *       （共享库=主库 INSERT + ${tenant_id}；独立库租户由懒初始化通道覆盖，幂等跳过）；</li>
 *   <li>其余事件类型（冻结/功能/配额/路由变更/注销）仅失效快照。</li>
 * </ul>
 *
 * <p>事件双通道均缺席时（微服务 + 未配置 MQ），数据面靠快照版本节流校验（秒级）+ L1 TTL（60s）
 * + 每租户种子周期补偿收敛（见 TenantSnapshotStore / TenantDataCompensator）。
 */
public class TenantEventInboundHandler {

    private static final Logger log = LoggerFactory.getLogger(TenantEventInboundHandler.class);

    private final ObjectProvider<TenantDataMigrator> migratorProvider;

    private final ObjectProvider<TenantSnapshotStore> snapshotStoreProvider;

    /** 每租户种子补偿器（优先汇入：异步 + 独立库过滤；compensate-enabled=false 时缺席走直连） */
    private final ObjectProvider<TenantDataCompensator> compensatorProvider;

    public TenantEventInboundHandler(ObjectProvider<TenantDataMigrator> migratorProvider,
                                     ObjectProvider<TenantSnapshotStore> snapshotStoreProvider,
                                     ObjectProvider<TenantDataCompensator> compensatorProvider) {
        this.migratorProvider = migratorProvider;
        this.snapshotStoreProvider = snapshotStoreProvider;
        this.compensatorProvider = compensatorProvider;
    }

    /** 统一入口（Spring 事件与 MQ 消息共用） */
    public void handle(TenantEvent event) {
        if (event == null) {
            return;
        }
        // 任何租户事件都失效本地快照（冻结/功能/配额/路由变更 → 下次读取按 版本校验→后端→拉取 重建）；
        // tenantId=null 表示全局变更（如通用数据源/全局默认路由），全量失效
        if (event.tenantId() == null) {
            snapshotStoreProvider.ifAvailable(TenantSnapshotStore::invalidateAll);
            return;
        }
        snapshotStoreProvider.ifAvailable(store -> store.invalidate(event.tenantId()));
        String type = event.eventType();
        if (!TenantEvent.Type.CREATED.equals(type) && !TenantEvent.Type.DATA_UPGRADE.equals(type)
                && !TenantEvent.Type.ACTIVATED.equals(type)) {
            return;
        }
        migrateTenant(event.tenantId(), type);
    }

    /** 为租户补跑 pending 每租户脚本（优先汇入补偿器获得独立库过滤；缺席时直连同步执行） */
    private void migrateTenant(Long tenantId, String type) {
        TenantDataCompensator compensator = compensatorProvider.getIfAvailable();
        if (compensator != null) {
            compensator.enqueue(tenantId, "event:" + type);
            return;
        }
        TenantDataMigrator migrator = migratorProvider.getIfAvailable();
        if (migrator == null) {
            return;
        }
        try {
            var result = migrator.migrateTenant(tenantId);
            if (result.applied() > 0 || result.failed() > 0 || result.conflicts() > 0) {
                log.info("[mse-bridge] 租户事件触发每租户数据迁移: tenant={} type={} 应用 {}、失败 {}、冲突 {}",
                        tenantId, type, result.applied(), result.failed(), result.conflicts());
            }
        } catch (Exception e) {
            log.warn("[mse-bridge] 租户事件触发每租户数据迁移失败（可在管理端手动重试）: tenant={} {}",
                    tenantId, e.getMessage());
        }
    }
}
