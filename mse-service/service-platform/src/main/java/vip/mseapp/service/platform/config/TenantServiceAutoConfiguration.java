package vip.mseapp.service.platform.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.core.datasource.TenantRouteResolver;
import vip.mseapp.mybatis.config.MseTenantProperties;
import vip.mseapp.mybatis.datasource.TenantDsRegistry;
import vip.mseapp.mybatis.datasource.TenantServiceModuleRegistry;
import vip.mseapp.redis.DomainBrandingCache;
import vip.mseapp.redis.TenantCache;
import vip.mseapp.service.platform.mapper.SysTenantDatasourceMapper;
import vip.mseapp.service.platform.mapper.SysTenantLicenseMapper;
import vip.mseapp.service.platform.mapper.SysTenantMapper;
import vip.mseapp.service.platform.mapper.SysTenantServiceConfigMapper;
import vip.mseapp.service.platform.mapper.SysTenantServiceTablePolicyMapper;
import vip.mseapp.service.platform.rpc.RemoteTenantServiceImpl;
import vip.mseapp.service.platform.service.DomainAssetStorage;
import vip.mseapp.service.platform.service.FsDomainAssetStorage;
import vip.mseapp.service.platform.service.LocalDomainAssetStorage;
import vip.mseapp.service.platform.service.SysDomainConfigService;
import vip.mseapp.service.platform.service.SysScheduleTaskService;
import vip.mseapp.service.platform.service.SysTenantDatasourceService;
import vip.mseapp.service.platform.service.SysTenantPackageService;
import vip.mseapp.service.platform.service.SysTenantRouteService;
import vip.mseapp.service.platform.service.SysTenantService;
import vip.mseapp.service.platform.service.TenantProvisioner;
import vip.mseapp.service.platform.runner.DomainBrandingCacheRunner;
import vip.mseapp.service.platform.service.impl.SysDomainConfigServiceImpl;
import vip.mseapp.service.platform.service.impl.SysTenantDatasourceServiceImpl;
import vip.mseapp.service.platform.service.impl.SysTenantPackageServiceImpl;
import vip.mseapp.service.platform.service.impl.SysTenantRouteServiceImpl;
import vip.mseapp.service.platform.service.impl.SysTenantServiceImpl;
import vip.mseapp.service.platform.service.support.TenantDatasourceBatchSyncService;
import vip.mseapp.service.platform.service.support.TenantDatasourceConnectionTester;
import vip.mseapp.service.platform.service.support.TenantDatasourceInitializer;
import vip.mseapp.service.platform.service.support.TenantDatasourceSupport;
import vip.mseapp.service.platform.service.support.TenantSchemaOpsService;

import javax.sql.DataSource;

/**
 * 租户管理服务自动装配（Spring Boot SPI）。
 *
 * <p>本模块的所有 Service / RPC 实现都由此注册，接入方仅需引入 jar 依赖，
 * 无需 component-scan 基座包名，也无需 @MapperScan——基座 mapper 由本类统一扫描注册。
 * 控制器由 {@link TenantControllerAutoConfiguration} 装配。
 *
 * <p><b>可覆盖性</b>：每个默认实现都带 {@code @ConditionalOnMissingBean(契约类型)}，
 * 接入方声明自己的同契约 Bean 即可整体替换默认实现（如自定义
 * {@link TenantRouteResolver} 数据源路由、自定义 {@link SysTenantService}），
 * 基座代码零改动。
 *
 * <p><b>循环依赖</b>：{@link SysTenantRouteServiceImpl} 与 {@link SysTenantServiceImpl}
 * 互引（租户变更失效路由缓存），前者经 ObjectProvider 延迟解析数据源服务，不构成构造期循环；
 * {@link SysTenantDatasourceServiceImpl} 数据源变更失效路由缓存（反向引用）同样经
 * ObjectProvider 延迟解析。
 */
@AutoConfiguration(afterName = "vip.mseapp.fs.autoconfigure.FsServiceAutoConfiguration")
@MapperScan(basePackages = "vip.mseapp.service.platform.mapper", annotationClass = Mapper.class)
public class TenantServiceAutoConfiguration {

    /**
     * 租户×服务路由解析（common-core TenantRouteResolver SPI 的 base 本地实现）；
     * 微服务形态下由业务启动器提供 RPC 版实现（声明同契约 Bean 即可覆盖）。
     * 进程内 30s L1 之外经 TenantCache 走 Redis L2（多实例共享，版本号 + TTL 失效）。
     */
    @Bean
    @ConditionalOnMissingBean(TenantRouteResolver.class)
    public SysTenantRouteService sysTenantRouteServiceImpl(SysTenantServiceTablePolicyMapper policyMapper,
                                                           SysTenantMapper tenantMapper,
                                                           SysTenantDatasourceMapper datasourceMapper,
                                                           ObjectProvider<TenantServiceModuleRegistry> moduleRegistryProvider,
                                                           ObjectProvider<TenantDsRegistry> registryProvider,
                                                           ObjectProvider<SysTenantDatasourceService> datasourceServiceProvider,
                                                           ObjectProvider<MseTenantProperties> tenantPropertiesProvider,
                                                           ObjectProvider<TenantCache> tenantCacheProvider,
                                                           ObjectProvider<vip.mseapp.mq.TenantEventSender> eventSenderProvider) {
        return new SysTenantRouteServiceImpl(policyMapper, tenantMapper, datasourceMapper, moduleRegistryProvider,
                registryProvider, datasourceServiceProvider, tenantPropertiesProvider,
                tenantCacheProvider, eventSenderProvider);
    }

    /** 租户异步开通器：开通/重试的耗时动作（管理员创建 + 独立库首次克隆）后台执行 */
    @Bean
    @ConditionalOnMissingBean(TenantProvisioner.class)
    public TenantProvisioner tenantProvisioner(SysTenantMapper tenantMapper,
                                               TenantRouteResolver routeResolver,
                                               org.springframework.beans.factory.ObjectProvider<vip.mseapp.mq.TenantEventSender> eventSenderProvider,
                                               org.springframework.context.ApplicationEventPublisher eventPublisher) {
        return new TenantProvisioner(tenantMapper, routeResolver, eventSenderProvider, eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean(SysTenantService.class)
    public SysTenantService sysTenantServiceImpl(TenantRouteResolver routeResolver,
                                                 RedisTemplate<String, Object> redisTemplate,
                                                 SysTenantRouteService routeService,
                                                 TenantProvisioner provisioner,
                                                 ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider,
                                                 ObjectProvider<vip.mseapp.mq.TenantEventSender> eventSenderProvider) {
        return new SysTenantServiceImpl(routeResolver, redisTemplate, routeService, provisioner,
                entitlementProvider, eventSenderProvider);
    }

    /** 租户数据源管理共享小工具（实体→连接定义/加解密/文本与结果判定；内部协作者，勿在业务工程依赖） */
    @Bean
    public TenantDatasourceSupport tenantDatasourceSupport(
            ObjectProvider<MseTenantProperties> tenantPropertiesProvider) {
        return new TenantDatasourceSupport(tenantPropertiesProvider);
    }

    /** 连接测试协作者（内部实现，替换整个 Service 时随门面一并失效） */
    @Bean
    public TenantDatasourceConnectionTester tenantDatasourceConnectionTester(TenantDatasourceSupport support) {
        return new TenantDatasourceConnectionTester(support);
    }

    /** 结构演进协作者：迁移/结构比对同步/预演/结构状态/临时 SQL + 进程内 per-dsKey 串行锁与跨实例锁 */
    @Bean
    public TenantSchemaOpsService tenantSchemaOpsService(
            SysTenantDatasourceMapper datasourceMapper,
            ObjectProvider<vip.mseapp.mybatis.schema.TenantSchemaMigrator> schemaMigratorProvider,
            ObjectProvider<DataSource> masterDataSourceProvider,
            ObjectProvider<MseTenantProperties> tenantPropertiesProvider,
            org.springframework.beans.factory.ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplateProvider,
            TenantDatasourceSupport support) {
        return new TenantSchemaOpsService(datasourceMapper, schemaMigratorProvider, masterDataSourceProvider,
                tenantPropertiesProvider, redisTemplateProvider, support);
    }

    /** 初始化协作者：建库 + 三通道脚本直建的异步登记/后台执行/状态落行 */
    @Bean
    public TenantDatasourceInitializer tenantDatasourceInitializer(
            SysTenantDatasourceMapper datasourceMapper,
            ObjectProvider<vip.mseapp.mybatis.schema.TenantSchemaMigrator> schemaMigratorProvider,
            ObjectProvider<DataSource> masterDataSourceProvider,
            org.springframework.beans.factory.ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplateProvider,
            TenantDatasourceSupport support,
            TenantSchemaOpsService schemaOps) {
        return new TenantDatasourceInitializer(datasourceMapper, schemaMigratorProvider, masterDataSourceProvider,
                redisTemplateProvider, support, schemaOps);
    }

    /** 批量同步协作者：逐库迁移+比对的批次登记/后台执行/Redis 进度双写与状态读取 */
    @Bean
    public TenantDatasourceBatchSyncService tenantDatasourceBatchSyncService(
            org.springframework.beans.factory.ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplateProvider,
            SysTenantDatasourceMapper datasourceMapper,
            TenantSchemaOpsService schemaOps) {
        return new TenantDatasourceBatchSyncService(redisTemplateProvider, datasourceMapper, schemaOps);
    }

    @Bean
    @ConditionalOnMissingBean(SysTenantDatasourceService.class)
    public SysTenantDatasourceService sysTenantDatasourceServiceImpl(
            SysTenantMapper tenantMapper,
            SysTenantServiceConfigMapper configMapper,
            ObjectProvider<TenantDsRegistry> registryProvider,
            ObjectProvider<DataSource> masterDataSourceProvider,
            ObjectProvider<TenantRouteResolver> routeResolverProvider,
            TenantDatasourceSupport support,
            TenantDatasourceConnectionTester connectionTester,
            TenantDatasourceInitializer initializer,
            TenantSchemaOpsService schemaOps,
            TenantDatasourceBatchSyncService batchSync) {
        return new SysTenantDatasourceServiceImpl(tenantMapper, configMapper, registryProvider,
                masterDataSourceProvider, routeResolverProvider, support, connectionTester, initializer,
                schemaOps, batchSync);
    }

    /** 悬挂初始化任务对账恢复（进程重启丢任务自愈）：超时仍「初始化中」的行自动重新提交（幂等） */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.service.platform.runner.TenantDsInitRecoveryRunner.class)
    public vip.mseapp.service.platform.runner.TenantDsInitRecoveryRunner tenantDsInitRecoveryRunner(
            vip.mseapp.service.platform.mapper.SysTenantDatasourceMapper datasourceMapper,
            SysTenantDatasourceService datasourceService) {
        return new vip.mseapp.service.platform.runner.TenantDsInitRecoveryRunner(datasourceMapper, datasourceService);
    }

    @Bean
    @ConditionalOnMissingBean(SysTenantPackageService.class)
    public SysTenantPackageService sysTenantPackageServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                                                ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider) {
        return new SysTenantPackageServiceImpl(redisTemplate, entitlementProvider);
    }

    /** 租户换库搬迁（租户换绑数据源后搬数据），管理端点触发 */
    @Bean
    public vip.mseapp.service.platform.service.TenantDataMigrationService tenantDataMigrationService(
            com.baomidou.dynamic.datasource.DynamicRoutingDataSource routingDataSource,
            SysTenantMapper tenantMapper,
            TenantDsRegistry dsRegistry,
            org.springframework.beans.factory.ObjectProvider<vip.mseapp.mybatis.datasource.TenantDsLazyInitializer> lazyInitializerProvider,
            TenantRouteResolver routeResolver,
            org.springframework.beans.factory.ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisTemplateProvider,
            @Value("${spring.datasource.dynamic.primary:master}") String primaryDsKey) {
        return new vip.mseapp.service.platform.service.TenantDataMigrationService(
                routingDataSource, tenantMapper, dsRegistry, lazyInitializerProvider,
                routeResolver, redisTemplateProvider, primaryDsKey);
    }

    @Bean
    @ConditionalOnMissingBean(SysDomainConfigService.class)
    public SysDomainConfigService sysDomainConfigServiceImpl(ObjectProvider<DomainBrandingCache> cacheProvider) {
        return new SysDomainConfigServiceImpl(cacheProvider);
    }

    /** 平台域品牌缓存启动重建 Runner（多实例分布式锁只做一份，失败不阻断启动） */
    @Bean
    @ConditionalOnMissingBean(DomainBrandingCacheRunner.class)
    public DomainBrandingCacheRunner domainBrandingCacheRunner(
            SysDomainConfigService domainConfigService,
            ObjectProvider<DomainBrandingCache> cacheProvider,
            ObjectProvider<vip.mseapp.redis.config.MseRedisAutoConfiguration.LockOps> lockOpsProvider) {
        return new DomainBrandingCacheRunner(domainConfigService, cacheProvider, lockOpsProvider);
    }

    /** 授权凭证签发（私有部署授权；mse.license.private-key 未配置时签发端点明确报错） */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.service.platform.service.TenantLicenseService.class)
    public vip.mseapp.service.platform.service.TenantLicenseService tenantLicenseService(
            SysTenantLicenseMapper licenseMapper,
            SysTenantMapper tenantMapper,
            vip.mseapp.service.platform.mapper.SysTenantPackageMapper packageMapper,
            @org.springframework.beans.factory.annotation.Value("${mse.license.private-key:}") String privateKey,
            ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider) {
        return new vip.mseapp.service.platform.service.TenantLicenseService(
                licenseMapper, tenantMapper, packageMapper, privateKey, entitlementProvider);
    }

    /**
     * 交付附件签发门面（私有部署支撑，ADR-9 职责归位：平台只签发附件包，最终交付包由
     * deliver-studio「套件+附件」重打包产出）。归档目录/单表行数上限/并发任务上限/下载签名密钥
     * 与有效期/直推 studio 配置可经 mse.deliver.* 调整；私钥与 license 签发同源。
     */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.service.platform.service.deliver.DeliverPackageService.class)
    public vip.mseapp.service.platform.service.deliver.DeliverPackageService deliverPackageService(
            vip.mseapp.service.platform.mapper.SysDeliverPackageMapper packageMapper,
            SysTenantMapper tenantMapper,
            SysTenantDatasourceMapper datasourceMapper,
            ObjectProvider<DataSource> masterDataSourceProvider,
            ObjectProvider<MseTenantProperties> tenantPropertiesProvider,
            @org.springframework.beans.factory.annotation.Value("${mse.license.private-key:}") String privateKey,
            @org.springframework.beans.factory.annotation.Value("${mse.deliver.archive-dir:./data/deliver}") String archiveDir,
            @org.springframework.beans.factory.annotation.Value("${mse.deliver.platform-version:}") String platformVersion,
            @org.springframework.beans.factory.annotation.Value("${mse.deliver.export.max-table-rows:0}") long maxTableRows,
            @org.springframework.beans.factory.annotation.Value("${mse.deliver.export.max-active-tasks:2}") int maxActiveTasks,
            @org.springframework.beans.factory.annotation.Value("${mse.deliver.download-sign-key:}") String downloadSignKey,
            @org.springframework.beans.factory.annotation.Value("${mse.deliver.download-url-ttl-seconds:1800}") long downloadUrlTtlSeconds,
            @org.springframework.beans.factory.annotation.Value("${mse.deliver.studio.url:}") String studioUrl,
            @org.springframework.beans.factory.annotation.Value("${mse.deliver.studio.user:}") String studioUser,
            @org.springframework.beans.factory.annotation.Value("${mse.deliver.studio.password:}") String studioPassword) {
        return new vip.mseapp.service.platform.service.deliver.DeliverPackageService(
                packageMapper, tenantMapper, datasourceMapper, masterDataSourceProvider,
                tenantPropertiesProvider, privateKey, archiveDir, platformVersion, maxTableRows,
                maxActiveTasks, downloadSignKey, downloadUrlTtlSeconds,
                vip.mseapp.service.platform.service.deliver.StudioPushClient.of(studioUrl, studioUser, studioPassword));
    }

    // ===== 品牌素材存储（登录页 Logo/背景图）：mse-fs 可用时优先公开素材通道，本地磁盘兜底 =====
    // 两侧条件互斥（不可依赖嵌套类声明顺序：javac 编译出的 InnerClasses 顺序不保序，
    // Local 先注册时 Fs 的 @ConditionalOnMissingBean 会让位，素材将静默落本地）；
    // 声明同契约 Bean 仍可整体替换（如 OSS/MinIO）。

    @Configuration(proxyBeanMethods = false)
    static class FsDomainAssetStorageConfiguration {

        @Bean
        @Conditional(FsAvailableCondition.class)
        @ConditionalOnMissingBean(DomainAssetStorage.class)
        public DomainAssetStorage fsDomainAssetStorage() {
            return new FsDomainAssetStorage();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LocalDomainAssetStorageConfiguration {

        @Bean
        @Conditional(FsUnavailableCondition.class)
        @ConditionalOnMissingBean(DomainAssetStorage.class)
        public DomainAssetStorage domainAssetStorage(
                @Value("${mse.tenant.branding-dir:./branding-assets}") String brandingDir) {
            return new LocalDomainAssetStorage(brandingDir);
        }
    }

    // ===== RPC 契约实现（实现类标 @RpcService：dubbo 模式自动导出，单体本地直调）=====

    /** 内置任务调度登记（平台库 sys_schedule_task）：各服务调度中心周期上报聚合 + 平台端「任务调度」页数据源 */
    @Bean
    @ConditionalOnMissingBean(SysScheduleTaskService.class)
    public SysScheduleTaskService sysScheduleTaskService(
            vip.mseapp.service.platform.mapper.SysScheduleTaskMapper taskMapper,
            vip.mseapp.service.platform.mapper.SysScheduleTaskLogMapper logMapper,
            org.springframework.beans.factory.ObjectProvider<vip.mseapp.service.platform.mapper.SysScheduleSettingMapper> settingMapperProvider,
            org.springframework.beans.factory.ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisProvider,
            vip.mseapp.service.platform.schedule.ScheduleControlRefresher controlRefresher) {
        return new SysScheduleTaskService(taskMapper, logMapper, settingMapperProvider.getIfAvailable(),
                redisProvider, controlRefresher);
    }

    /**
     * 调度控制缓存重建器（事件驱动、零周期 SQL）：就绪后按平台库重建一次；管理操作后
     * 静默去抖（mse.scheduling.control-rebuild-debounce，缺省 60s）重建一次——对账自愈
     * 写透失败/Redis 被清空/并发竞态（含闸门参数键 mse:schedule:gates）。
     */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.service.platform.schedule.ScheduleControlRefresher.class)
    public vip.mseapp.service.platform.schedule.ScheduleControlRefresher scheduleControlRefresher(
            org.springframework.core.env.Environment environment,
            vip.mseapp.service.platform.mapper.SysScheduleTaskMapper taskMapper,
            org.springframework.beans.factory.ObjectProvider<vip.mseapp.service.platform.mapper.SysScheduleSettingMapper> settingMapperProvider,
            org.springframework.beans.factory.ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisProvider) {
        long debounceMs = org.springframework.boot.convert.DurationStyle.detectAndParse(
                environment.getProperty("mse.scheduling.control-rebuild-debounce", "60s")).toMillis();
        return new vip.mseapp.service.platform.schedule.ScheduleControlRefresher(taskMapper,
                settingMapperProvider, redisProvider, debounceMs);
    }

    /** 调度状态契约实现：starter-bridge 状态同步器消费（local 直调 / dubbo 远调） */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.api.system.RemoteScheduleStateApi.class)
    public vip.mseapp.service.platform.rpc.RemoteScheduleStateApiImpl remoteScheduleStateApiImpl(
            SysScheduleTaskService scheduleTaskService) {
        return new vip.mseapp.service.platform.rpc.RemoteScheduleStateApiImpl(scheduleTaskService);
    }

    @Bean
    @ConditionalOnMissingBean(RemoteTenantService.class)
    public RemoteTenantService remoteTenantServiceImpl(SysTenantService tenantService,
                                                       SysTenantPackageService packageService,
                                                       SysDomainConfigService domainConfigService,
                                                       SysTenantRouteService routeService,
                                                       SysTenantDatasourceService datasourceService,
                                                       ObjectProvider<DomainBrandingCache> cacheProvider,
                                                       ObjectProvider<TenantCache> tenantCacheProvider,
                                                       ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider) {
        return new RemoteTenantServiceImpl(tenantService, packageService, domainConfigService,
                routeService, datasourceService, cacheProvider, tenantCacheProvider, entitlementProvider);
    }

    /** 权益对账任务（0.1.0：默认关，仅日志对账） */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.service.platform.service.impl.EntitlementResyncTask.class)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "mse.entitlement", name = "resync-enabled", havingValue = "true")
    public vip.mseapp.service.platform.service.impl.EntitlementResyncTask entitlementResyncTask(
            vip.mseapp.service.platform.service.EntitlementService entitlementService) {
        return new vip.mseapp.service.platform.service.impl.EntitlementResyncTask(entitlementService);
    }

    /** 权益计算（0.1.0）：控制面权威口径；快照/契约经此取值，数据面走缓存/快照本地消费 */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.service.platform.service.EntitlementService.class)
    public vip.mseapp.service.platform.service.EntitlementService entitlementServiceImpl(
            vip.mseapp.service.platform.mapper.SysEntitlementItemMapper itemMapper,
            vip.mseapp.service.platform.mapper.SysPackageItemMapper packageItemMapper,
            vip.mseapp.service.platform.mapper.SysAddonMapper addonMapper,
            vip.mseapp.service.platform.mapper.SysAddonItemMapper addonItemMapper,
            vip.mseapp.service.platform.mapper.SysTenantAddonMapper tenantAddonMapper,
            vip.mseapp.service.platform.mapper.SysTenantEntitlementOverrideMapper overrideMapper,
            SysTenantPackageService packageService,
            vip.mseapp.service.platform.mapper.SysTenantMapper tenantMapper,
            org.springframework.beans.factory.ObjectProvider<org.springframework.data.redis.core.RedisTemplate<String, Object>> redisProvider,
            org.springframework.beans.factory.ObjectProvider<vip.mseapp.mq.TenantEventSender> eventSenderProvider) {
        return new vip.mseapp.service.platform.service.impl.EntitlementServiceImpl(itemMapper,
                packageItemMapper, addonMapper, addonItemMapper, tenantAddonMapper, overrideMapper,
                packageService, tenantMapper, redisProvider, eventSenderProvider);
    }
}
