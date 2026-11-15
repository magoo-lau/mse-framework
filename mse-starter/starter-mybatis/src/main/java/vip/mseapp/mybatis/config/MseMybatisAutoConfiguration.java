package vip.mseapp.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteResolver;
import vip.mseapp.mybatis.datascope.DataScopeInterceptor;
import vip.mseapp.mybatis.datascope.DataScopeParamResolver;
import vip.mseapp.mybatis.datasource.TenantDsExecutorInterceptor;
import vip.mseapp.mybatis.datasource.TenantDsLazyInitializer;
import vip.mseapp.mybatis.datasource.TenantDsRegistry;
import vip.mseapp.mybatis.datasource.TenantServiceModuleRegistry;
import vip.mseapp.mybatis.datasource.TenantSplitTableInterceptor;
import vip.mseapp.mybatis.handler.MseMetaObjectHandler;
import vip.mseapp.mybatis.handler.MseTenantLineHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import vip.mseapp.mybatis.slowsql.SlowSqlCollector;
import vip.mseapp.mybatis.slowsql.SlowSqlInterceptor;

import java.util.Locale;

/**
 * MyBatis-Plus 自动装配。
 *
 * <p>拦截器顺序（官方要求：租户插件必须先于分页，否则分页 COUNT 丢失租户条件）：
 * 租户行级插件（字段隔离判定按逻辑表名）→ 分表改写（SPLIT 后缀）→ 数据权限 → 分页。
 *
 * <p>分页方言由 mse.mybatis.db-type 决定：默认 auto，按数据源 JDBC URL 自动识别
 * （MySQL/达梦/人大金仓等），多数据源路由时逐语句识别；也可显式指定 DbType 枚举名。
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
@EnableConfigurationProperties({MseTenantProperties.class, MseMybatisProperties.class})
public class MseMybatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MybatisPlusInterceptor.class)
    public MybatisPlusInterceptor mybatisPlusInterceptor(MseTenantProperties tenantProperties,
                                                         MseMybatisProperties mybatisProperties,
                                                         ObjectProvider<DataScopeInterceptor> dataScopeProvider) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        if (tenantProperties.isEnabled()) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new MseTenantLineHandler(tenantProperties)));
            // 共享库分表：按路由快照给 SPLIT 表追加租户后缀（无快照/非 SPLIT 表原样返回）
            interceptor.addInnerInterceptor(new TenantSplitTableInterceptor());
        }
        dataScopeProvider.ifAvailable(interceptor::addInnerInterceptor);
        interceptor.addInnerInterceptor(paginationInterceptor(mybatisProperties));
        return interceptor;
    }

    /**
     * 全局表白名单自动注册：实体继承 GlobalBaseEntity 的 Mapper 装配时自动豁免租户改写
     * （基类即声明，业务模块零白名单配置）。静态 Bean 免于早期实例化警告。
     */
    @Bean
    public static MseTenantTableAutoRegistrar mseTenantTableAutoRegistrar(MseTenantProperties tenantProperties) {
        return new MseTenantTableAutoRegistrar(tenantProperties);
    }

    private PaginationInnerInterceptor paginationInterceptor(MseMybatisProperties properties) {
        String dbType = properties.getDbType() == null ? "auto" : properties.getDbType().trim();
        if ("auto".equalsIgnoreCase(dbType)) {
            // 无参构造：执行期按当前连接 URL 自动识别方言，dynamic-datasource 跨库路由时各自生效
            return new PaginationInnerInterceptor();
        }
        try {
            return new PaginationInnerInterceptor(DbType.valueOf(dbType.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("mse.mybatis.db-type 取值非法：" + dbType
                    + "（应为 auto 或 MyBatis-Plus DbType 枚举名，如 mysql / dm / kingbase_es）", ex);
        }
    }

    /**
     * 数据范围解析器由持有角色/部门数据的模块提供（service-system）；
     * 未提供（如 mse-auth 无业务库）则不注册数据权限插件，查询原样执行。
     *
     * <p>解析器经 ObjectProvider 延迟获取：拦截器在 SqlSessionFactory 构建期装配，
     * 若此刻实例化解析器（其实现通常注入 Mapper）会形成
     * 拦截器 → 解析器 → Mapper → SqlSessionFactory → 拦截器 的循环依赖。
     */
    @Bean
    @ConditionalOnBean(DataScopeParamResolver.class)
    public DataScopeInterceptor dataScopeInterceptor(ObjectProvider<DataScopeParamResolver> resolverProvider) {
        return new DataScopeInterceptor(() -> resolverProvider.getObject().resolve());
    }

    @Bean
    @ConditionalOnMissingBean(MseMetaObjectHandler.class)
    public MseMetaObjectHandler mseMetaObjectHandler() {
        return new MseMetaObjectHandler();
    }

    /**
     * 慢 SQL 收集器（内存环形缓存，供 /actuator/slowsql 端点读取快照）。
     * enabled=false 时仅作端点的状态载体，无任何记录动作。
     */
    @Bean
    public SlowSqlCollector slowSqlCollector(MseMybatisProperties properties) {
        MseMybatisProperties.SlowSql slowSql = properties.getSlowSql();
        return new SlowSqlCollector(slowSql.isEnabled(), slowSql.getThresholdMs(), slowSql.getCapacity());
    }

    /**
     * 慢 SQL 拦截器：默认关闭（mse.mybatis.slow-sql.enabled，缺省不注册，MyBatis 工厂
     * 不含本插件，零开销）；显式开启后每条语句一次计时，超阈值才记录。
     */
    @Bean
    @ConditionalOnProperty(prefix = "mse.mybatis.slow-sql", name = "enabled", havingValue = "true")
    public SlowSqlInterceptor slowSqlInterceptor(SlowSqlCollector collector) {
        return new SlowSqlInterceptor(collector);
    }

    /**
     * 模块服务身份收集器（@TenantServiceModule）：懒扫描容器内标注了服务身份注解的
     * 自动装配类，供 SQL 侧服务名解析与租户数据源注册按模块服务名扩展加载。
     * 无人消费时不产生任何扫描开销（懒触发）。
     */
    @Bean
    @ConditionalOnMissingBean(TenantServiceModuleRegistry.class)
    public TenantServiceModuleRegistry tenantServiceModuleRegistry() {
        return new TenantServiceModuleRegistry();
    }

    /**
     * 多模式租户路由：classpath 存在 dynamic-datasource 时装配。按 (租户, 服务) 路由快照
     * 决定数据源 push，并向行级插件/分表改写提供同一快照（TenantRouteContextHolder）。
     * 无 TenantRouteResolver Bean（如未接入租户服务的部署）时退化为恒共享主库。
     */
    @Bean
    @ConditionalOnClass(name = "com.baomidou.dynamic.datasource.DynamicRoutingDataSource")
    public TenantDsExecutorInterceptor tenantDsExecutorInterceptor(
            ObjectProvider<TenantRouteResolver> routeResolverProvider,
            ObjectProvider<TenantDsLazyInitializer> lazyInitProvider,
            ObjectProvider<TenantDsRegistry> registryProvider,
            org.springframework.beans.factory.ObjectProvider<com.baomidou.dynamic.datasource.DynamicRoutingDataSource> routingProvider,
            MseTenantProperties tenantProperties,
            TenantServiceModuleRegistry moduleRegistry,
            @Value("${spring.application.name:}") String applicationName) {
        // 路由解析器不在工厂方法急切解析：base/RPC 实现依赖 Mapper → SqlSessionFactory，
        // MP 自动装配收集插件期 sqlSessionFactory 正在创建中，急切解析会构成循环（拦截器内懒获取）
        return new TenantDsExecutorInterceptor(routeResolverProvider,
                resolveServiceName(tenantProperties, applicationName), tenantProperties.getServiceMappings(),
                moduleRegistry, lazyInitProvider, registryProvider, routingProvider,
                tenantProperties.getSchema().getPlatformDs(), tenantProperties.getTxDsMismatch());
    }

    /** 服务名解析：mse.tenant.service-name 显式配置优先，缺省 spring.application.name，皆空 = '*' */
    static String resolveServiceName(MseTenantProperties tenantProperties, String applicationName) {
        if (tenantProperties.getServiceName() != null && !tenantProperties.getServiceName().isBlank()) {
            return tenantProperties.getServiceName().trim();
        }
        if (applicationName != null && !applicationName.isBlank()) {
            return applicationName.trim();
        }
        return "*";
    }
}
