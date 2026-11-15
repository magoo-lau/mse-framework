package vip.mseapp.mybatis.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import vip.mseapp.mybatis.schema.MasterSchemaAutoMigration;
import vip.mseapp.mybatis.schema.MseSchemaJavaMigration;
import vip.mseapp.mybatis.schema.TenantDataMigrator;
import vip.mseapp.mybatis.schema.TenantMigrationScanner;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;

import java.util.List;

/**
 * 结构演进自动装配：版本化迁移执行器 + classpath 脚本扫描器 + 启动自动迁移 + 每租户数据迁移器。
 *
 * <p>服务对象：主库/基座库/平台库与租户库的 schema 演进（db/&lt;方言&gt;/V*.sql 脚本 + Java 迁移，
 * 账本 mse_&lt;biz&gt;_schema_history 按 biz 分册）。不依赖 dynamic-datasource（迁移连接按目标直取），
 * 随 starter-mybatis 常驻装配；管理端点在 service-platform（「租户数据源」界面），
 * 懒初始化挂钩在 {@link TenantDsLazyInitializer}。
 * 启动自动迁移默认开启（{@code mse.tenant.schema.auto-migrate=false} 关闭；失败阻断启动）；
 * base 通道的业务脚本推送端在 starter-bridge，执行端在 service-system，不走本装配。
 * 可声明同类型 Bean 整体替换。
 */
@AutoConfiguration
public class TenantSchemaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TenantMigrationScanner tenantMigrationScanner(MseTenantProperties properties) {
        return new TenantMigrationScanner(TenantMigrationScanner.DEFAULT_LOCATION_PATTERN,
                properties.getSchema().isStrictScan());
    }

    @Bean
    @ConditionalOnMissingBean
    public TenantSchemaMigrator tenantSchemaMigrator(TenantMigrationScanner scanner,
                                                     ObjectProvider<MseSchemaJavaMigration> javaMigrations) {
        // installed_by 取节点用户名（管理端操作者审计由 platform 层传入 operator 覆盖时再扩展）；
        // Java 迁移由各积木以 Spring Bean 声明，此处收集并入统一版本序
        List<MseSchemaJavaMigration> migrations = javaMigrations.stream().toList();
        return new TenantSchemaMigrator(scanner, System.getProperty("user.name", "mse"), migrations);
    }

    /**
     * 每租户数据迁移器：开通事件/管理端升级广播/auto-upgrade 扫捕的数据面执行端
     * （共享库租户在主库 INSERT + ${tenant_id} 注入，独立库租户由懒初始化同通道覆盖）。
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantDataMigrator tenantDataMigrator(
            ObjectProvider<DynamicRoutingDataSource> routingProvider,
            ObjectProvider<vip.mseapp.mybatis.datasource.TenantDsRegistry> registryProvider,
            ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplateProvider,
            TenantSchemaMigrator migrator,
            MseTenantProperties properties) {
        return new TenantDataMigrator(routingProvider, registryProvider, redisTemplateProvider, migrator,
                properties.getSchema().getPlaceholders());
    }

    /**
     * 启动自动迁移（默认开启；失败阻断启动）。多实例并发安全：
     * Redis 锁按目标库互斥 + 进程内串行兜底，详见 {@link MasterSchemaAutoMigration}。
     */
    @Bean
    @ConditionalOnMissingBean(MasterSchemaAutoMigration.class)
    @ConditionalOnProperty(prefix = "mse.tenant.schema", name = "auto-migrate",
            havingValue = "true", matchIfMissing = true)
    public MasterSchemaAutoMigration masterSchemaAutoMigration(
            ObjectProvider<DynamicRoutingDataSource> routingProvider,
            @Value("${spring.datasource.dynamic.primary:master}") String primaryDsKey,
            ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplateProvider,
            TenantSchemaMigrator migrator,
            TenantMigrationScanner scanner,
            MseTenantProperties properties) {
        return new MasterSchemaAutoMigration(routingProvider, primaryDsKey, redisTemplateProvider,
                migrator, scanner, properties.getSchema());
    }

    /**
     * 就绪标记兜底：auto-migrate 关闭（结构由部署方自行保证）或使用方以异类 Runner 整体替换执行器时，
     * 后台扫描任务的就绪门禁（{@link MasterSchemaAutoMigration#isSchemaReady}）须视为已就绪。
     * 默认执行器存在时不装配——标记由其 run() 完成后自行置位。
     */
    @Bean
    @ConditionalOnMissingBean(MasterSchemaAutoMigration.class)
    public org.springframework.boot.ApplicationRunner schemaReadyFallbackMarker() {
        return args -> MasterSchemaAutoMigration.markSchemaReady();
    }
}
