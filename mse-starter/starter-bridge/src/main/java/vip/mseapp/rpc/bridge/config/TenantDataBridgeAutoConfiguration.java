package vip.mseapp.rpc.bridge.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import vip.mseapp.mybatis.config.MseTenantProperties;
import vip.mseapp.mybatis.schema.TenantDataMigrator;
import vip.mseapp.redis.TenantCache;
import vip.mseapp.rpc.bridge.TenantDataUpgradeListener;
import vip.mseapp.rpc.bridge.TenantDataUpgradeRunner;
import vip.mseapp.rpc.bridge.TenantEventInboundHandler;
import vip.mseapp.rpc.bridge.TenantEventMqListener;

/**
 * 数据面租户事件桥接自动装配：租户事件的统一处理端（进程内 Spring 事件 + MQ 双通道汇入
 * {@link TenantEventInboundHandler}）、租户快照本地存储（TTL + 版本节流校验 + 容量上限）、
 * auto-upgrade 启动扫捕兜底。
 * 依赖的 {@link TenantDataMigrator} 由 starter-mybatis 装配；缺失时监听器空转。
 */
@AutoConfiguration
public class TenantDataBridgeAutoConfiguration {

    /** 事件统一处理端（Spring 事件与 MQ 消息共用：失效快照 + 按需补跑每租户脚本） */
    @Bean
    @ConditionalOnMissingBean
    public TenantEventInboundHandler tenantEventInboundHandler(ObjectProvider<TenantDataMigrator> migratorProvider,
            ObjectProvider<vip.mseapp.rpc.bridge.TenantSnapshotStore> snapshotStoreProvider,
            ObjectProvider<vip.mseapp.rpc.bridge.TenantDataCompensator> compensatorProvider) {
        return new TenantEventInboundHandler(migratorProvider, snapshotStoreProvider, compensatorProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantDataUpgradeListener tenantDataUpgradeListener(TenantEventInboundHandler handler) {
        return new TenantDataUpgradeListener(handler);
    }

    /**
     * 每租户种子补偿器（默认装配）：周期 tick（统一调度中心，多实例防重）+ 快照首触秒级补偿，
     * 独立库租户过滤（懒初始化直建覆盖）——共享库新建/存量租户的每租户种子不再依赖 MQ 事件
     * 到达与启动扫捕。{@code mse.tenant.schema.tenant-data.compensate-enabled=false} 关闭
     * （事件通道回落直连迁移）。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mse.tenant.schema.tenant-data", name = "compensate-enabled",
            havingValue = "true", matchIfMissing = true)
    public vip.mseapp.rpc.bridge.TenantDataCompensator tenantDataCompensator(
            ObjectProvider<TenantDataMigrator> migratorProvider,
            ObjectProvider<vip.mseapp.core.schedule.MseTaskScheduler> schedulerProvider,
            MseTenantProperties properties) {
        var tenantData = properties.getSchema().getTenantData();
        return new vip.mseapp.rpc.bridge.TenantDataCompensator(migratorProvider, schedulerProvider,
                tenantData.getCompensateIntervalMs(), tenantData.getCompensateBatchSize(),
                tenantData.getCompensateRecheckIntervalMs());
    }

    /**
     * 租户快照本地存储：内存 L1（TTL + 容量 + 版本节流校验，见 {@link vip.mseapp.rpc.bridge.TenantSnapshotStore}）
     * + 后端 L2（{@link vip.mseapp.rpc.bridge.SnapshotBackend}）+ 平台拉取自举写穿；
     * AES-GCM + HMAC 防篡改。后端选择：配置了 mse.tenant.snapshot-file 用加密文件（离线/standalone，
     * 无 Redis 依赖），否则有 Redis 用 Redis（在线，多实例共享），都没有则内存-only。
     * 版本探测接 {@link TenantCache#readTenantVersion}（与控制面失效 INCR 同源；无 Redis 时无探测，
     * 快照时效由 TTL 兜底）。首次加载回调接补偿器首触通道（补偿器缺席时无回调）。
     */
    @Bean
    @ConditionalOnMissingBean
    public vip.mseapp.rpc.bridge.TenantSnapshotStore tenantSnapshotStore(
            org.springframework.core.env.Environment environment,
            ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisProvider,
            ObjectProvider<TenantCache> tenantCacheProvider,
            ObjectProvider<vip.mseapp.rpc.bridge.TenantDataCompensator> compensatorProvider,
            MseTenantProperties properties) {
        String snapshotFile = environment.getProperty("mse.tenant.snapshot-file", "").trim();
        vip.mseapp.rpc.bridge.SnapshotBackend backend;
        if (!snapshotFile.isEmpty()) {
            backend = new vip.mseapp.rpc.bridge.FileSnapshotBackend(snapshotFile);
        } else if (redisProvider.getIfAvailable() != null) {
            backend = new vip.mseapp.rpc.bridge.RedisSnapshotBackend(redisProvider);
        } else {
            backend = null; // 内存-only：快照随进程生命周期，进程重启后回源自举
        }
        TenantCache tenantCache = tenantCacheProvider.getIfAvailable();
        java.util.function.Function<Long, Long> versionProbe =
                tenantCache == null ? null : tenantCache::readTenantVersion;
        vip.mseapp.rpc.bridge.TenantDataCompensator compensator = compensatorProvider.getIfAvailable();
        java.util.function.Consumer<Long> firstLoadHook =
                compensator == null ? null : tenantId -> compensator.enqueue(tenantId, "snapshot-first-load");
        return new vip.mseapp.rpc.bridge.TenantSnapshotStore(backend, properties.getSnapshotKey(),
                properties.getSnapshotTtlMs(), properties.getSnapshotMaxEntries(),
                properties.getSnapshotVersionCheckIntervalMs(), versionProbe, firstLoadHook);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "mse.tenant.schema.tenant-data", name = "auto-upgrade", havingValue = "true")
    public TenantDataUpgradeRunner tenantDataUpgradeRunner(TenantDataMigrator migrator) {
        return new TenantDataUpgradeRunner(migrator);
    }

    /**
     * 目录变更批次来源 RPC 通道：新租户库懒初始化经此追平平台端目录编辑
     * （消费 RemoteCatalogRegistry；单体 local 直调 / 微服务 dubbo）。缺省注册——
     * 单体下 mse-system 本地实现经 @RpcReference 直调，与 RpcTenantDsMetadataProvider 让位语义不同
     * （目录批次真源只在平台库，无需本地 JDBC 版）。
     */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.mybatis.datasource.CatalogChangeSource.class)
    public vip.mseapp.rpc.bridge.RpcCatalogChangeSource rpcCatalogChangeSource() {
        return new vip.mseapp.rpc.bridge.RpcCatalogChangeSource();
    }

    /**
     * 租户事件 MQ 消费端（微服务形态即时通道，此前全仓库无 mse-tenant-events 消费者）：
     * 宿主引入 mq-starter（rocketmq-spring 在 classpath）且配置了 rocketmq.name-server 时装配——
     * rocketmq-spring 的监听容器 BeanPostProcessor 经类级 @Import 无条件注册（不随 name-server
     * 条件跳过），name-server 缺失时任何 @RocketMQMessageListener bean 初始化即抛
     * "Property 'nameServer' is required"（守卫同 SearchIndexEventListener/OrderEventListener）；
     * 未配 MQ 环境走进程内 Spring 事件 + 补偿器兜底。嵌套配置类隔离类引用。
     */
    @ConditionalOnClass(name = "org.apache.rocketmq.spring.annotation.RocketMQMessageListener")
    @ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
    static class MqListenerConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "mse.tenant.events", name = "mq-enabled",
                havingValue = "true", matchIfMissing = true)
        public TenantEventMqListener tenantEventMqListener(TenantEventInboundHandler handler) {
            return new TenantEventMqListener(handler);
        }
    }
}
