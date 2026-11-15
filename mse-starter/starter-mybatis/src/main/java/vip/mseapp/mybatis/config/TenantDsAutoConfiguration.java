package vip.mseapp.mybatis.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.beans.factory.SmartInitializingSingleton;
import vip.mseapp.mybatis.datasource.LocalTenantDsMetadataProvider;
import vip.mseapp.mybatis.datasource.TenantDsLazyInitializer;
import vip.mseapp.mybatis.datasource.TenantDsMetadataProvider;
import vip.mseapp.mybatis.datasource.TenantDsRegistry;
import vip.mseapp.mybatis.datasource.TenantServiceModuleRegistry;

import java.nio.charset.StandardCharsets;

/**
 * 动态租户数据源自动装配（租户库界面化维护）。
 *
 * <p>加载 {@code sys_tenant_datasource}（平台库全局表，「平台管理 → 租户数据源」维护）到
 * dynamic-datasource 运行时注册表：启动全量注册（lazy 建池），管理端变更经 Redis topic
 * {@link TenantDsRegistry#TOPIC} 广播、各进程经元数据通道回查后本地应用（无 Redis 时仅本进程生效）。
 *
 * <p>元数据双通道：业务微服务主库无平台表，由 starter-bridge 的 RPC 通道顶替
 * （本装配排在 RpcBridgeAutoConfiguration 之后，本地通道经 @ConditionalOnMissingBean 让位）；
 * 单体/mse-tenant 用本地 JDBC 通道。开关：{@code mse.tenant.dynamic-ds.enabled}。
 */
@AutoConfiguration(afterName = {
        "com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration",
        "com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceCreatorAutoConfiguration",
        "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration",
        "vip.mseapp.rpc.bridge.config.RpcBridgeAutoConfiguration"})
@ConditionalOnClass(DynamicRoutingDataSource.class)
@ConditionalOnProperty(prefix = "mse.tenant.dynamic-ds", name = "enabled", matchIfMissing = true)
public class TenantDsAutoConfiguration {

    /** 元数据本地 JDBC 通道（兜底；RPC 通道存在时经 @ConditionalOnMissingBean 让位） */
    @Bean
    @ConditionalOnMissingBean(TenantDsMetadataProvider.class)
    public LocalTenantDsMetadataProvider localTenantDsMetadataProvider(
            ObjectProvider<DynamicRoutingDataSource> routingProvider,
            @Value("${spring.datasource.dynamic.primary:master}") String primaryDsKey,
            MseTenantProperties tenantProperties) {
        return new LocalTenantDsMetadataProvider(routingProvider, primaryDsKey,
                tenantProperties.getSchema().getPlatformDs());
    }

    @Bean
    @ConditionalOnMissingBean(TenantDsRegistry.class)
    public TenantDsRegistry tenantDsRegistry(ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                             ObjectProvider<DefaultDataSourceCreator> creatorProvider,
                                             MseTenantProperties tenantProperties,
                                             @Value("${spring.application.name:}") String applicationName,
                                             TenantDsMetadataProvider metadataProvider,
                                             TenantServiceModuleRegistry moduleRegistry,
                                             ObjectProvider<RedisConnectionFactory> connectionFactoryProvider) {
        String serviceName = MseMybatisAutoConfiguration.resolveServiceName(tenantProperties, applicationName);
        return new TenantDsRegistry(routingProvider, creatorProvider, tenantProperties.getDynamicDs(),
                serviceName, metadataProvider, connectionFactoryProvider, moduleRegistry,
                tenantProperties.getSharedMain());
    }

    /** 租户库懒初始化（动态开通链路第 3 级；mse.tenant.dynamic-ds.lazy-init 可关） */
    @Bean
    @ConditionalOnMissingBean(TenantDsLazyInitializer.class)
    @ConditionalOnProperty(prefix = "mse.tenant.dynamic-ds", name = "lazy-init", matchIfMissing = true)
    public TenantDsLazyInitializer tenantDsLazyInitializer(TenantDsMetadataProvider metadataProvider,
                                                           ObjectProvider<DynamicRoutingDataSource> routingProvider,
                                                           MseTenantProperties tenantProperties,
                                                           @Value("${spring.datasource.dynamic.primary:master}") String primaryDsKey,
                                                           ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplateProvider,
                                                           ObjectProvider<vip.mseapp.mybatis.schema.TenantSchemaMigrator> schemaMigratorProvider,
                                                           ObjectProvider<vip.mseapp.mybatis.datasource.CatalogChangeSource> catalogChangeSourceProvider,
                                                           ObjectProvider<TenantDsRegistry> registryProvider) {
        return new TenantDsLazyInitializer(metadataProvider, routingProvider, primaryDsKey,
                tenantProperties, redisTemplateProvider, schemaMigratorProvider, catalogChangeSourceProvider,
                registryProvider);
    }

    /** raw JDBC 租户路由（业务模块绕过 MyBatis 直取连接的批量通道，防静默落平台库） */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.mybatis.datasource.TenantRawConnectionRouter.class)
    public vip.mseapp.mybatis.datasource.TenantRawConnectionRouter tenantRawConnectionRouter(
            ObjectProvider<vip.mseapp.core.datasource.TenantRouteResolver> resolverProvider,
            ObjectProvider<TenantDsRegistry> registryProvider,
            ObjectProvider<DynamicRoutingDataSource> routingProvider,
            ObjectProvider<TenantDsLazyInitializer> lazyInitProvider,
            TenantServiceModuleRegistry moduleRegistry,
            MseTenantProperties tenantProperties) {
        return new vip.mseapp.mybatis.datasource.TenantRawConnectionRouter(resolverProvider, registryProvider,
                routingProvider, lazyInitProvider, moduleRegistry, tenantProperties.getSchema().getPlatformDs());
    }

    /**
     * 启动全量注册 + 订阅变更广播。SmartInitializingSingleton 时机保证路由器与
     * 监听容器均已就绪；容器尚未 start 时注册的监听会在 start 阶段统一订阅。
     */
    @Bean
    @ConditionalOnBean(TenantDsRegistry.class)
    public SmartInitializingSingleton tenantDsStartupLoader(TenantDsRegistry registry,
                                                            ObjectProvider<RedisMessageListenerContainer> containerProvider) {
        return () -> {
            registry.registerAll();
            // 内置共享库（mse.tenant.shared-main.* 自declare）：登记行之外的本服务专属池，不广播
            //（各服务自读自配）；键默认=本服务应用名、显式 '*'=通用
            registry.registerBuiltin();
            containerProvider.ifAvailable(container -> container.addMessageListener(
                    (message, pattern) -> {
                        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
                        int index = payload.indexOf(':');
                        if (index > 0) {
                            registry.onRemoteChange(payload.substring(0, index), payload.substring(index + 1));
                        }
                    },
                    new ChannelTopic(TenantDsRegistry.TOPIC)));
        };
    }

    /**
     * 变更广播监听容器：复用 Boot 的 RedisConnectionFactory（无 Redis 部署整类缺省，
     * 变更仅本进程生效）。类级条件保护方法签名中 spring-data-redis 类型的解析。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisMessageListenerContainer.class)
    static class TenantDsRedisConfiguration {

        @Bean
        @ConditionalOnBean(RedisConnectionFactory.class)
        @ConditionalOnMissingBean(RedisMessageListenerContainer.class)
        public RedisMessageListenerContainer tenantDsRedisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(connectionFactory);
            return container;
        }
    }
}
