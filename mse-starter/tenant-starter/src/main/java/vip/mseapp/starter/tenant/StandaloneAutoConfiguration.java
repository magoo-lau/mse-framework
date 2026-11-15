package vip.mseapp.starter.tenant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;
import vip.mseapp.service.system.security.LicenseHolder;

/**
 * standalone 私有部署自动装配（{@code mse.standalone.enabled=true} 启用）：
 * RemoteTenantService 本地桩（契约消费本地闭环）+ 启动自足化 + 固定租户上下文。
 * tenant-starter 专属；mono-starter（在线聚合，含 service-platform）不引本模块。
 */
@AutoConfiguration
@ConditionalOnProperty(name = "mse.standalone.enabled", havingValue = "true")
public class StandaloneAutoConfiguration {

    /** RemoteTenantService 本地桩：bridge 的 RPC 版实现经 @ConditionalOnMissingBean 让位 */
    @Bean
    @ConditionalOnMissingBean(RemoteTenantService.class)
    public StandaloneTenantApi standaloneTenantApi(JdbcTemplate jdbcTemplate,
                                                   LicenseHolder licenseHolder,
                                                   ObjectProvider<vip.mseapp.redis.TenantCache> tenantCacheProvider,
                                                   @Value("${mse.standalone.tenant-id:1}") long tenantId) {
        return new StandaloneTenantApi(jdbcTemplate, licenseHolder, tenantCacheProvider, tenantId);
    }

    /** 启动自足化：租户库直建全部结构 + 基座数据副本 + 本租户权威行（ApplicationRunner，应用就绪前执行） */
    @Bean
    public StandaloneBootstrap standaloneBootstrap(JdbcTemplate jdbcTemplate,
                                                   ObjectProvider<TenantSchemaMigrator> migratorProvider,
                                                   LicenseHolder licenseHolder,
                                                   ObjectProvider<vip.mseapp.redis.TenantCache> tenantCacheProvider,
                                                   @Value("${mse.standalone.tenant-id:1}") long tenantId,
                                                   @Value("${mse.standalone.tenant-code:tenant}") String tenantCode,
                                                   @Value("${mse.standalone.tenant-name:租户私有部署}") String tenantName) {
        TenantSchemaMigrator migrator = migratorProvider.getIfAvailable();
        if (migrator == null) {
            throw new IllegalStateException("TenantSchemaMigrator 未装配，standalone 启动自足化无法执行");
        }
        return new StandaloneBootstrap(jdbcTemplate, migrator, licenseHolder, tenantCacheProvider,
                tenantId, tenantCode, tenantName);
    }

    /** 固定租户上下文：最高优先级预置本机租户（认证后 token 权威值覆盖） */
    @Bean
    public FilterRegistrationBean<StandaloneTenantContextFilter> standaloneTenantContextFilter(
            @Value("${mse.standalone.tenant-id:1}") long tenantId) {
        FilterRegistrationBean<StandaloneTenantContextFilter> registration =
                new FilterRegistrationBean<>(new StandaloneTenantContextFilter(tenantId));
        // 早于 TenantContextFilter 与 Sa-Token 过滤器，保证登录/公开接口也有租户上下文
        registration.setOrder(-200);
        registration.addUrlPatterns("/*");
        registration.setName("standaloneTenantContextFilter");
        return registration;
    }

    /**
     * 离线交付导入服务（standalone 专属）：部署包验签/计划/应用——
     * 与 license 同一验签公钥（mse.license.public-key），数据装载走本地主库原生 JDBC。
     */
    @Bean
    @ConditionalOnMissingBean(DeliverImportService.class)
    public DeliverImportService deliverImportService(JdbcTemplate jdbcTemplate,
                                                     LicenseHolder licenseHolder,
                                                     @Value("${mse.license.public-key:}") String publicKey,
                                                     @Value("${mse.license.file:./conf/license.token}") String licenseFile,
                                                     @Value("${mse.standalone.tenant-code:tenant}") String tenantCode,
                                                     ObjectProvider<vip.mseapp.redis.TenantCache> tenantCacheProvider,
                                                     @Value("${mse.standalone.tenant-id:1}") long tenantId) {
        return new DeliverImportService(jdbcTemplate, licenseHolder, publicKey, licenseFile, tenantCode,
                tenantCacheProvider, tenantId);
    }

    /** 部署包启动自动导入（一键部署主路径）：安装器把包拷到 conf/deploy-package.zip，结构就绪后自动应用 */
    @Bean
    @ConditionalOnMissingBean(DeliverAutoImportRunner.class)
    public DeliverAutoImportRunner deliverAutoImportRunner(DeliverImportService deliverImportService,
                                                           @Value("${mse.deliver.auto-import:true}") boolean enabled,
                                                           @Value("${mse.deliver.auto-import-file:./conf/deploy-package.zip}") String autoImportFile) {
        return new DeliverAutoImportRunner(deliverImportService, enabled, autoImportFile);
    }

    /** 离线交付导入端点（仅 Servlet 环境注册；在线形态无 tenant-starter，不出现） */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(StandaloneImportController.class)
    public StandaloneImportController standaloneImportController(DeliverImportService deliverImportService) {
        return new StandaloneImportController(deliverImportService);
    }
}
