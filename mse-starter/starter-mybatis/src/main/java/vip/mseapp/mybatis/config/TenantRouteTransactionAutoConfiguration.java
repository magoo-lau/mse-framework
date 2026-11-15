package vip.mseapp.mybatis.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import vip.mseapp.mybatis.datasource.TenantDsLazyInitializer;
import vip.mseapp.mybatis.datasource.TenantDsRegistry;
import vip.mseapp.mybatis.datasource.TenantRouteTransactionManager;
import vip.mseapp.core.datasource.TenantRouteResolver;

import javax.sql.DataSource;

/**
 * 租户路由事务管理器自动装配：{@link TenantRouteTransactionManager} 在 {@code doBegin}
 * 时按租户上下文切库，修复「本地事务连接固定落主库」缺陷（详见其类注释）。
 *
 * <p>排在 Boot 的 DataSourceTransactionManagerAutoConfiguration 之前处理，
 * 以 {@code @ConditionalOnMissingBean(PlatformTransactionManager)} 让位接入方自定义事务管理器；
 * 无数据源部署（如 mse-auth 纯消费端）经 ObjectProvider 判空返回 null，零装配。
 * 开关：{@code mse.tenant.route-tx.enabled=false} 可退回 Boot 默认事务管理器。
 */
@AutoConfiguration(beforeName = "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration")
@ConditionalOnClass({JdbcTransactionManager.class, DynamicRoutingDataSource.class})
@EnableConfigurationProperties(MseTenantProperties.class)
public class TenantRouteTransactionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PlatformTransactionManager.class)
    @ConditionalOnProperty(prefix = "mse.tenant.route-tx", name = "enabled", matchIfMissing = true)
    public TenantRouteTransactionManager tenantRouteTransactionManager(
            ObjectProvider<DataSource> dataSourceProvider,
            ObjectProvider<TenantRouteResolver> routeResolverProvider,
            ObjectProvider<TenantDsLazyInitializer> lazyInitProvider,
            ObjectProvider<TenantDsRegistry> registryProvider,
            ObjectProvider<DynamicRoutingDataSource> routingProvider,
            MseTenantProperties tenantProperties,
            @Value("${spring.application.name:}") String applicationName) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            // 无数据源部署（纯消费端）：返回 null 即不注册，Boot 默认装配按缺失处理
            return null;
        }
        TenantRouteTransactionManager manager = new TenantRouteTransactionManager(routeResolverProvider,
                MseMybatisAutoConfiguration.resolveServiceName(tenantProperties, applicationName),
                lazyInitProvider, registryProvider, routingProvider,
                tenantProperties.getSchema().getPlatformDs());
        // 必须与 SqlSessionFactory 同一 DataSource 实例：事务连接按 DataSource 身份同步，
        // MyBatis 经 DataSourceUtils 复用同一事务连接（换包装实例会令语句全部脱离事务）
        manager.setDataSource(dataSource);
        return manager;
    }
}
