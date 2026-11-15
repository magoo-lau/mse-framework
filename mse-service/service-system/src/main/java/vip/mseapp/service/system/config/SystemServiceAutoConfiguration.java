package vip.mseapp.service.system.config;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import vip.mseapp.mail.MseMailSender;
import vip.mseapp.api.system.RemoteAccessService;
import vip.mseapp.api.system.RemoteAuthService;
import vip.mseapp.api.system.RemoteBaseDataRegistry;
import vip.mseapp.api.system.RemoteBrandingService;
import vip.mseapp.api.system.RemoteConfigService;
import vip.mseapp.api.system.RemoteDataScopeService;
import vip.mseapp.api.system.RemoteDeptService;
import vip.mseapp.api.system.RemoteLogService;
import vip.mseapp.api.system.RemoteMessageService;
import vip.mseapp.api.system.RemoteOrgService;
import vip.mseapp.api.system.RemoteRoleService;
import vip.mseapp.api.system.RemoteUserService;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.mybatis.config.MseMybatisAutoConfiguration;
import vip.mseapp.mybatis.config.MseTenantProperties;
import vip.mseapp.mybatis.datascope.DataScopeParamResolver;
import vip.mseapp.mybatis.datasource.TenantDsMetadataProvider;
import vip.mseapp.mybatis.datasource.TenantDsRegistry;
import vip.mseapp.mybatis.datasource.TenantFleetBaseReplayer;
import vip.mseapp.mybatis.schema.TenantMigrationScanner;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;
import vip.mseapp.redis.DomainBrandingCache;
import vip.mseapp.redis.PermissionCache;
import vip.mseapp.redis.config.MseRedisAutoConfiguration;
import vip.mseapp.service.system.datascope.DataScopeResolverImpl;
import vip.mseapp.service.system.log.ServiceOperLogRecorder;
import vip.mseapp.service.system.mapper.SysDeptMapper;
import vip.mseapp.service.system.mapper.SysMenuMapper;
import vip.mseapp.service.system.mapper.SysMicroAppMapper;
import vip.mseapp.service.system.mapper.SysOperLogMapper;
import vip.mseapp.service.system.mapper.SysPermissionMapper;
import vip.mseapp.service.system.mapper.SysRoleDeptMapper;
import vip.mseapp.service.system.mapper.SysRoleMapper;
import vip.mseapp.service.system.mapper.SysRoleMenuMapper;
import vip.mseapp.service.system.mapper.SysRolePermissionMapper;
import vip.mseapp.service.system.mapper.SysUserMapper;
import vip.mseapp.service.system.mapper.SysUserPostMapper;
import vip.mseapp.service.system.mapper.SysUserRoleMapper;
import vip.mseapp.service.system.mapper.SysDictDataMapper;
import vip.mseapp.service.system.mapper.SysDictTypeMapper;
import vip.mseapp.service.system.mapper.SysDomainRegistryMapper;
import vip.mseapp.service.system.rpc.RemoteAccessServiceImpl;
import vip.mseapp.service.system.runner.BaseTenantFleetRunner;
import vip.mseapp.service.system.runner.DomainDivideAuditRunner;
import vip.mseapp.service.system.runner.TenantBrandingCacheRunner;
import vip.mseapp.service.system.rpc.RemoteAuthServiceImpl;
import vip.mseapp.service.system.rpc.RemoteBaseDataRegistryImpl;
import vip.mseapp.service.system.rpc.RemoteBrandingServiceImpl;
import vip.mseapp.service.system.rpc.RemoteConfigServiceImpl;
import vip.mseapp.service.system.rpc.RemoteDataScopeServiceImpl;
import vip.mseapp.service.system.rpc.RemoteDeptServiceImpl;
import vip.mseapp.service.system.rpc.RemoteLogServiceImpl;
import vip.mseapp.service.system.rpc.RemoteMessageServiceImpl;
import vip.mseapp.service.system.rpc.RemoteOrgServiceImpl;
import vip.mseapp.service.system.rpc.RemoteRoleServiceImpl;
import vip.mseapp.service.system.rpc.RemoteUserServiceImpl;
import vip.mseapp.service.system.security.AccessService;
import vip.mseapp.service.system.security.AccessServiceImpl;
import vip.mseapp.service.system.security.SaPermissionImpl;
import vip.mseapp.service.system.service.SysConfigService;
import vip.mseapp.service.system.service.SysDeptService;
import vip.mseapp.service.system.service.SysDictDataService;
import vip.mseapp.service.system.service.SysDictTypeService;
import vip.mseapp.service.system.service.SysHomeTemplateService;
import vip.mseapp.service.system.service.SysHomeUserSettingService;
import vip.mseapp.service.system.service.SysLogininforService;
import vip.mseapp.service.system.service.SysMenuService;
import vip.mseapp.service.system.service.SysMessageService;
import vip.mseapp.service.system.service.SysMicroAppService;
import vip.mseapp.service.system.service.SysOnlineService;
import vip.mseapp.service.system.service.SysOperLogService;
import vip.mseapp.service.system.service.SysPermissionService;
import vip.mseapp.service.system.service.SysPostService;
import vip.mseapp.service.system.service.SysRoleService;
import vip.mseapp.service.system.service.SysTenantBrandingService;
import vip.mseapp.service.system.service.SysTenantBrandingDomainService;
import vip.mseapp.service.system.service.SysUserService;
import vip.mseapp.service.system.service.AvatarStorage;
import vip.mseapp.service.system.service.BaseDataRegistryService;
import vip.mseapp.service.system.service.BrandingAssetStorage;
import vip.mseapp.service.system.service.FsAvatarStorage;
import vip.mseapp.service.system.service.FsBrandingAssetStorage;
import vip.mseapp.service.system.service.LocalAvatarStorage;
import vip.mseapp.service.system.service.LocalBrandingAssetStorage;
import vip.mseapp.service.system.service.TenantBrandingCachePublisher;
import vip.mseapp.service.system.service.TenantScopeService;
import vip.mseapp.service.system.service.TwoFactorService;
import vip.mseapp.service.system.service.impl.SysConfigServiceImpl;
import vip.mseapp.service.system.service.impl.BaseDataRegistryServiceImpl;
import vip.mseapp.service.system.service.impl.SysDeptServiceImpl;
import vip.mseapp.service.system.service.impl.SysDictDataServiceImpl;
import vip.mseapp.service.system.service.impl.SysDictTypeServiceImpl;
import vip.mseapp.service.system.service.impl.SysHomeTemplateServiceImpl;
import vip.mseapp.service.system.service.impl.SysHomeUserSettingServiceImpl;
import vip.mseapp.service.system.service.impl.SysLogininforServiceImpl;
import vip.mseapp.service.system.service.impl.SysMenuServiceImpl;
import vip.mseapp.service.system.service.impl.SysMessageServiceImpl;
import vip.mseapp.service.system.service.impl.SysMicroAppServiceImpl;
import vip.mseapp.service.system.service.impl.SysOnlineServiceImpl;
import vip.mseapp.service.system.service.impl.SysOperLogServiceImpl;
import vip.mseapp.service.system.service.impl.SysPermissionServiceImpl;
import vip.mseapp.service.system.service.impl.SysPostServiceImpl;
import vip.mseapp.service.system.service.impl.SysRoleServiceImpl;
import vip.mseapp.service.system.service.impl.SysTenantBrandingServiceImpl;
import vip.mseapp.service.system.service.impl.SysTenantBrandingDomainServiceImpl;
import vip.mseapp.service.system.service.impl.SysUserServiceImpl;
import vip.mseapp.service.system.service.impl.TenantScopeServiceImpl;
import vip.mseapp.service.system.service.impl.TwoFactorServiceImpl;
import vip.mseapp.web.log.OperLogRecorder;

/**
 * 系统管理服务自动装配（Spring Boot SPI）。
 *
 * <p>本模块的所有 Service / 支撑组件 / RPC 实现都改为由此注册，接入方仅需引入 jar 依赖，
 * 无需 component-scan 基座包名，也无需 @MapperScan——基座 mapper 由本类统一扫描注册。
 * 控制器由 {@link SystemControllerAutoConfiguration} 装配，两者均经
 * {@code META-INF/spring/...AutoConfiguration.imports} 登记（Spring Boot SPI）。
 *
 * <p><b>可覆盖性</b>：每个默认实现都带 {@code @ConditionalOnMissingBean(契约类型)}，
 * 接入方声明自己的同契约 Bean 即可整体替换默认实现（如自定义 {@link SysUserService} 实现），
 * 基座代码零改动。这也是“积木只读、按需组装”约定的技术落点。
 *
 * <p><b>装配顺序</b>：本类必须先于 {@link MseMybatisAutoConfiguration} 处理——
 * 数据权限插件按“容器是否提供 {@link DataScopeParamResolver}”决定是否注册，
 * 默认实现 {@link DataScopeResolverImpl} 在本类注册，晚处理会导致插件被跳过。
 */
@AutoConfiguration(before = MseMybatisAutoConfiguration.class,
        afterName = "vip.mseapp.fs.autoconfigure.FsServiceAutoConfiguration")
@MapperScan(basePackages = "vip.mseapp.service.system.mapper", annotationClass = Mapper.class)
public class SystemServiceAutoConfiguration {

    /**
     * 调度状态契约的 standalone 回落实现（仅离线私有部署装配：mse.standalone.enabled=true）：
     * 单库直写 sys_schedule_task（表由 StandaloneBootstrap 直建），语义同 service-platform 版；
     * 租户端「任务调度」页（菜单 195，仅 standalone 播种）与本服务状态同步器共同消费。
     * SaaS/单体下不装配——mse-platform 持权威实现，各服务同步器走 dubbo 泛化。
     */
    @org.springframework.context.annotation.Bean
    @ConditionalOnMissingBean(vip.mseapp.api.system.RemoteScheduleStateApi.class)
    @ConditionalOnProperty(prefix = "mse.standalone", name = "enabled", havingValue = "true")
    public vip.mseapp.service.system.schedule.StandaloneScheduleStateApi standaloneScheduleStateApi(
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
            org.springframework.beans.factory.ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> redisProvider) {
        return new vip.mseapp.service.system.schedule.StandaloneScheduleStateApi(jdbcTemplate, redisProvider);
    }

    // ===== 用户头像存储（mse-fs 优先、本地磁盘兜底）=====
    // 两侧条件互斥（不可依赖嵌套类声明顺序：javac 编译出的 InnerClasses 顺序不保序，
    // Local 先注册时 Fs 的 @ConditionalOnMissingBean 会让位，文件将静默落本地）

    @Configuration(proxyBeanMethods = false)
    static class FsAvatarStorageConfiguration {

        @Bean
        @Conditional(AvatarAvailableCondition.class)
        @ConditionalOnMissingBean(AvatarStorage.class)
        public AvatarStorage fsAvatarStorage() {
            return new FsAvatarStorage();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LocalAvatarStorageConfiguration {

        @Bean
        @Conditional(AvatarUnavailableCondition.class)
        @ConditionalOnMissingBean(AvatarStorage.class)
        public AvatarStorage avatarStorage(@Value("${mse.system.avatar-dir:./avatar-assets}") String avatarDir) {
            return new LocalAvatarStorage(avatarDir);
        }
    }

    // ===== 租户品牌素材存储（mse-fs 优先、本地磁盘兜底；条件互斥理由同上）=====

    @Configuration(proxyBeanMethods = false)
    static class FsBrandingAssetStorageConfiguration {

        @Bean
        @Conditional(BrandingAvailableCondition.class)
        @ConditionalOnMissingBean(BrandingAssetStorage.class)
        public BrandingAssetStorage fsBrandingAssetStorage() {
            return new FsBrandingAssetStorage();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class LocalBrandingAssetStorageConfiguration {

        @Bean
        @Conditional(BrandingUnavailableCondition.class)
        @ConditionalOnMissingBean(BrandingAssetStorage.class)
        public BrandingAssetStorage brandingAssetStorage(
                @Value("${mse.tenant.branding-dir:./branding-assets}") String brandingDir) {
            return new LocalBrandingAssetStorage(brandingDir);
        }
    }

    // ===== 支撑组件（数据范围 / 操作日志落库）；权限缓存由 common-redis 提供 =====

    @Bean
    @ConditionalOnMissingBean(DataScopeParamResolver.class)
    public DataScopeParamResolver dataScopeResolverImpl(SysUserMapper userMapper, SysRoleMapper roleMapper,
                                                        SysDeptMapper deptMapper) {
        return new DataScopeResolverImpl(userMapper, roleMapper, deptMapper);
    }

    @Bean
    @ConditionalOnMissingBean(OperLogRecorder.class)
    public OperLogRecorder serviceOperLogRecorder(SysOperLogMapper operLogMapper) {
        return new ServiceOperLogRecorder(operLogMapper);
    }

    // ===== 基础业务服务（无内部依赖者在前）=====

    @Bean
    @ConditionalOnMissingBean(SysConfigService.class)
    public SysConfigService sysConfigServiceImpl() {
        return new SysConfigServiceImpl();
    }

    /** 在线会话管理（Sa-Token Redis 会话直查，无业务表） */
    @Bean
    @ConditionalOnMissingBean(SysOnlineService.class)
    public SysOnlineService sysOnlineServiceImpl() {
        return new SysOnlineServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(SysDictDataService.class)
    public SysDictDataService sysDictDataServiceImpl(SysDictTypeMapper dictTypeMapper) {
        return new SysDictDataServiceImpl(dictTypeMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SysDictTypeService.class)
    public SysDictTypeService sysDictTypeServiceImpl(SysDictDataMapper dictDataMapper) {
        return new SysDictTypeServiceImpl(dictDataMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SysLogininforService.class)
    public SysLogininforService sysLogininforServiceImpl() {
        return new SysLogininforServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(SysMessageService.class)
    public SysMessageService sysMessageServiceImpl() {
        return new SysMessageServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(SysOperLogService.class)
    public SysOperLogService sysOperLogServiceImpl() {
        return new SysOperLogServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(SysDeptService.class)
    public SysDeptService sysDeptServiceImpl(SysUserMapper userMapper) {
        return new SysDeptServiceImpl(userMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SysMenuService.class)
    public SysMenuService sysMenuServiceImpl(SysRoleMapper roleMapper, SysMicroAppMapper microAppMapper) {
        return new SysMenuServiceImpl(roleMapper, microAppMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SysMicroAppService.class)
    public SysMicroAppService sysMicroAppServiceImpl() {
        return new SysMicroAppServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(SysHomeTemplateService.class)
    public SysHomeTemplateService sysHomeTemplateServiceImpl() {
        return new SysHomeTemplateServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(SysHomeUserSettingService.class)
    public SysHomeUserSettingService sysHomeUserSettingServiceImpl() {
        return new SysHomeUserSettingServiceImpl();
    }

    /** 租户品牌个性化（租户世界数据，每租户一行；登录页解析经 RemoteBrandingService 契约读取） */
    @Bean
    @ConditionalOnMissingBean(SysTenantBrandingService.class)
    public SysTenantBrandingService sysTenantBrandingServiceImpl(
            ObjectProvider<TenantBrandingCachePublisher> publisherProvider) {
        return new SysTenantBrandingServiceImpl(publisherProvider);
    }

    /** 租户品牌域名绑定（租户自助；域名全局唯一——本库查重 + 平台库 sys_domain_registry 跨库判重） */
    @Bean
    @ConditionalOnMissingBean(SysTenantBrandingDomainService.class)
    public SysTenantBrandingDomainService sysTenantBrandingDomainServiceImpl(
            ObjectProvider<TenantBrandingCachePublisher> publisherProvider,
            ObjectProvider<SysDomainRegistryMapper> registryProvider) {
        return new SysTenantBrandingDomainServiceImpl(publisherProvider, registryProvider);
    }

    /**
     * 租户域品牌缓存发布器（启动全量重建 + 变更写穿 Redis 租户域 hash）。
     * 租户契约经 ObjectProvider 惰性解析：local 模式本地直调，dubbo 模式远程代理。
     */
    @Bean
    @ConditionalOnMissingBean(TenantBrandingCachePublisher.class)
    public TenantBrandingCachePublisher tenantBrandingCachePublisher(
            SysTenantBrandingService brandingService,
            SysTenantBrandingDomainService domainService,
            ObjectProvider<DomainBrandingCache> cacheProvider,
            ObjectProvider<RemoteTenantService> remoteTenantProvider,
            ObjectProvider<SysDomainRegistryMapper> registryProvider) {
        return new TenantBrandingCachePublisher(brandingService, domainService, cacheProvider,
                remoteTenantProvider, registryProvider);
    }

    /** 租户域品牌缓存启动重建 Runner（多实例分布式锁只做一份，失败不阻断启动） */
    @Bean
    @ConditionalOnMissingBean(TenantBrandingCacheRunner.class)
    public TenantBrandingCacheRunner tenantBrandingCacheRunner(TenantBrandingCachePublisher publisher,
                                                               ObjectProvider<MseRedisAutoConfiguration.LockOps> lockOpsProvider) {
        return new TenantBrandingCacheRunner(publisher, lockOpsProvider);
    }

    @Bean
    @ConditionalOnMissingBean(SysPostService.class)
    public SysPostService sysPostServiceImpl(SysUserPostMapper userPostMapper) {
        return new SysPostServiceImpl(userPostMapper);
    }

    /** 授权凭证持有器（standalone 私有部署模式专用）：平台签发 license 的验签/状态机 */
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mse.standalone.enabled", havingValue = "true")
    @Bean
    public vip.mseapp.service.system.security.LicenseHolder licenseHolder(
            @org.springframework.beans.factory.annotation.Value("${mse.license.public-key:}") String publicKey,
            @org.springframework.beans.factory.annotation.Value("${mse.license.token:}") String token,
            @org.springframework.beans.factory.annotation.Value("${mse.license.file:}") String licenseFile,
            @org.springframework.beans.factory.annotation.Value("${mse.standalone.tenant-code:}") String tenantCode,
            @org.springframework.beans.factory.annotation.Value("${mse.license.grace-days:15}") long graceDays) {
        return new vip.mseapp.service.system.security.LicenseHolder(publicKey, token, licenseFile, tenantCode, graceDays);
    }

    /** 授权凭证驱动的租户权限域（standalone）：声明在前，默认实现经 MissingBean 让位 */
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mse.standalone.enabled", havingValue = "true")
    @Bean
    public TenantScopeService licenseTenantScopeService(SysMenuMapper menuMapper,
                                                        SysPermissionMapper permissionMapper,
                                                        vip.mseapp.service.system.security.LicenseHolder licenseHolder) {
        return new vip.mseapp.service.system.security.LicenseTenantScopeService(menuMapper, permissionMapper, licenseHolder);
    }

    @Bean
    @ConditionalOnMissingBean(TenantScopeService.class)
    public TenantScopeService tenantScopeServiceImpl(SysMenuMapper menuMapper, SysPermissionMapper permissionMapper,
                                                     ObjectProvider<vip.mseapp.redis.EntitlementCache> entitlementCacheProvider) {
        return new TenantScopeServiceImpl(menuMapper, permissionMapper, entitlementCacheProvider);
    }

    @Bean
    @ConditionalOnMissingBean(SysPermissionService.class)
    public SysPermissionService sysPermissionServiceImpl(SysMenuMapper menuMapper,
                                                         SysRolePermissionMapper rolePermissionMapper,
                                                         PermissionCache permissionCache,
                                                         ObjectProvider<vip.mseapp.service.system.catalog.CatalogChangeRecorder> recorderProvider) {
        return new SysPermissionServiceImpl(menuMapper, rolePermissionMapper, permissionCache, recorderProvider);
    }

    @Bean
    @ConditionalOnMissingBean(SysRoleService.class)
    public SysRoleService sysRoleServiceImpl(SysRoleMenuMapper roleMenuMapper,
                                             SysRolePermissionMapper rolePermissionMapper,
                                             SysRoleDeptMapper roleDeptMapper,
                                             SysUserRoleMapper userRoleMapper,
                                             SysMenuService menuService,
                                             TenantScopeService tenantScopeService,
                                             PermissionCache permissionCache) {
        return new SysRoleServiceImpl(roleMenuMapper, rolePermissionMapper, roleDeptMapper, userRoleMapper,
                menuService, tenantScopeService, permissionCache);
    }

    @Bean
    @ConditionalOnMissingBean(SysUserService.class)
    public SysUserService sysUserServiceImpl(SysUserRoleMapper userRoleMapper,
                                             SysUserPostMapper userPostMapper,
                                             PermissionCache permissionCache,
                                             SysConfigService configService,
                                             ObjectProvider<vip.mseapp.service.system.security.QuotaGuard> quotaGuardProvider) {
        return new SysUserServiceImpl(userRoleMapper, userPostMapper, permissionCache, configService,
                quotaGuardProvider);
    }

    /** 配额守卫（0.1.0 权益模型·准入最小集：quota.max_users 软配额） */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.service.system.security.QuotaGuard.class)
    public vip.mseapp.service.system.security.QuotaGuard quotaGuard(TenantScopeService tenantScopeService) {
        return new vip.mseapp.service.system.security.QuotaGuard(tenantScopeService);
    }

    /** 登录双因子认证（TOTP + 邮箱动态码；MseMailSender 缺装配时邮箱通道自动不可用） */
    @Bean
    @ConditionalOnMissingBean(TwoFactorService.class)
    public TwoFactorService twoFactorServiceImpl(SysUserService userService, SysConfigService configService,
                                                 StringRedisTemplate redis,
                                                 ObjectProvider<MseMailSender> mailSender) {
        return new TwoFactorServiceImpl(userService, configService, redis, mailSender);
    }

    // ===== 鉴权装配（Sa-Token 鉴权数据源 / 访问域判定）=====

    @Bean
    @ConditionalOnMissingBean(AccessService.class)
    public AccessService accessServiceImpl(SysMenuMapper menuMapper,
                                           SysMenuService menuService,
                                           SysPermissionService permissionService,
                                           SysPermissionMapper permissionMapper,
                                           TenantScopeService tenantScopeService,
                                           SysMicroAppService microAppService) {
        return new AccessServiceImpl(menuMapper, menuService, permissionService, permissionMapper,
                tenantScopeService, microAppService);
    }

    /** Sa-Token 鉴权数据源：按 sa-token 契约 StpInterface 覆盖 */
    @Bean
    @ConditionalOnMissingBean(StpInterface.class)
    public SaPermissionImpl saPermissionImpl(SysUserService userService, AccessService accessService,
                                             PermissionCache permissionCache) {
        return new SaPermissionImpl(userService, accessService, permissionCache);
    }

    // ===== RPC 契约实现（实现类标 @RpcService：dubbo 模式自动导出，单体本地直调）=====

    @Bean
    @ConditionalOnMissingBean(RemoteUserService.class)
    public RemoteUserService remoteUserServiceImpl(SysUserService userService) {
        return new RemoteUserServiceImpl(userService);
    }

    /** 登录双因子认证契约：认证服务（service-auth）登录链路校验 TOTP/邮箱动态码 */
    @Bean
    @ConditionalOnMissingBean(RemoteAuthService.class)
    public RemoteAuthService remoteAuthServiceImpl(TwoFactorService twoFactorService) {
        return new RemoteAuthServiceImpl(twoFactorService);
    }

    @Bean
    @ConditionalOnMissingBean(RemoteLogService.class)
    public RemoteLogService remoteLogServiceImpl(SysLogininforService logininforService,
                                                 SysOperLogMapper operLogMapper) {
        return new RemoteLogServiceImpl(logininforService, operLogMapper);
    }

    /** 参数配置查询契约：认证服务等无库微服务读取 sys_config（登录策略开关等） */
    @Bean
    @ConditionalOnMissingBean(RemoteConfigService.class)
    public RemoteConfigService remoteConfigServiceImpl(SysConfigService configService) {
        return new RemoteConfigServiceImpl(configService);
    }

    /** 租户品牌查询契约：登录页按域名解析出租户后读取租户自助品牌（runAs 目标租户），兼触发租户域缓存重建 */
    @Bean
    @ConditionalOnMissingBean(RemoteBrandingService.class)
    public RemoteBrandingService remoteBrandingServiceImpl(SysTenantBrandingService brandingService,
                                                           ObjectProvider<TenantBrandingCachePublisher> publisherProvider) {
        return new RemoteBrandingServiceImpl(brandingService, publisherProvider);
    }

    /** 鉴权数据源契约：业务微服务（starter-bridge）经此拉取权限/角色码 */
    @Bean
    @ConditionalOnMissingBean(RemoteAccessService.class)
    public RemoteAccessService remoteAccessServiceImpl(SysUserService userService, AccessService accessService) {
        return new RemoteAccessServiceImpl(userService, accessService);
    }

    /** 组织架构查询契约：工作流审批人规则 / 人员部门选择器等业务场景消费 */
    @Bean
    @ConditionalOnMissingBean(RemoteOrgService.class)
    public RemoteOrgService remoteOrgServiceImpl(SysUserMapper userMapper, SysDeptMapper deptMapper,
                                                 SysRoleMapper roleMapper, SysUserRoleMapper userRoleMapper) {
        return new RemoteOrgServiceImpl(userMapper, deptMapper, roleMapper, userRoleMapper);
    }

    /** 消息服务中心契约：业务微服务（OA 工作流等）投递站内消息，双端工作台消费 */
    @Bean
    @ConditionalOnMissingBean(RemoteMessageService.class)
    public RemoteMessageService remoteMessageServiceImpl(SysMessageService messageService) {
        return new RemoteMessageServiceImpl(messageService);
    }

    // ===== 基座库脚本接收（db/base 通道：业务积木 base.sql 的版本化替代，starter-bridge 推送）=====

    /**
     * 租户库车队 base 重放器：把租户域 base 脚本（基座全局表/租户域菜单/权限点 + biz 子应用注册/
     * 页面菜单）全量重放到登记库全集（缺省共享库 shared-main + 各独立租户库，各库自持副本与分册
     * 账本）；primary（平台库）不参与——平台库目录经 {@link #baseDataRegistryService} 摄入通道维护。
     */
    @Bean
    @ConditionalOnMissingBean(TenantFleetBaseReplayer.class)
    public TenantFleetBaseReplayer tenantFleetBaseReplayer(
            ObjectProvider<TenantDsMetadataProvider> metadataProvider,
            ObjectProvider<TenantDsRegistry> registryProvider,
            MseTenantProperties tenantProperties,
            TenantSchemaMigrator migrator,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        return new TenantFleetBaseReplayer(metadataProvider.getIfAvailable(),
                registryProvider.getIfAvailable() == null ? null : registryProvider.getObject()::builtinSharedMain,
                tenantProperties.getDynamicDs(), migrator, redisTemplateProvider);
    }

    /** 车队重放启动 Runner：就绪即全量重放一轮（紧随启动迁移、先于业务 Runner），后续收敛由推送通道兜底 */
    @Bean
    @ConditionalOnMissingBean(BaseTenantFleetRunner.class)
    public BaseTenantFleetRunner baseTenantFleetRunner(TenantMigrationScanner scanner,
                                                       TenantFleetBaseReplayer fleetReplayer) {
        return new BaseTenantFleetRunner(scanner, fleetReplayer);
    }

    /**
     * 内置共享库引导（0.1.0 v2）：注册归 starter-mybatis 通用装载（各服务自declare），
     * 本 Runner 仅按 mse-system 自己的坐标建库 + eager 建 TENANT 身份表（保车队重放不被
     * identityReady 跳过）；未配置不动作，standalone 跳过。排序 +1001（车队重放前）。
     */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.service.system.runner.SharedMainBootstrap.class)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "mse.standalone.enabled", havingValue = "false", matchIfMissing = true)
    public vip.mseapp.service.system.runner.SharedMainBootstrap sharedMainBootstrap(
            org.springframework.core.env.Environment environment,
            TenantSchemaMigrator migrator,
            MseTenantProperties tenantProperties) {
        return new vip.mseapp.service.system.runner.SharedMainBootstrap(environment, migrator, tenantProperties);
    }

    /** 分域体检 Runner：平台库/租户域主库身份行互串只读清点（默认开，仅 WARN 提示不清理） */
    @Bean
    @ConditionalOnMissingBean(DomainDivideAuditRunner.class)
    @ConditionalOnProperty(name = "mse.divide.audit.enabled", havingValue = "true", matchIfMissing = true)
    public DomainDivideAuditRunner domainDivideAuditRunner(
            ObjectProvider<DynamicRoutingDataSource> routingProvider,
            MseTenantProperties tenantProperties) {
        return new DomainDivideAuditRunner(routingProvider, tenantProperties.getSchema().getPlatformDs(),
                tenantProperties.getSharedMain());
    }

    @Bean
    @ConditionalOnMissingBean(BaseDataRegistryService.class)
    public BaseDataRegistryService baseDataRegistryService(
            ObjectProvider<DynamicRoutingDataSource> routingProvider,
            @Value("${spring.datasource.dynamic.primary:master}") String primaryDsKey,
            MseTenantProperties tenantProperties,
            TenantSchemaMigrator migrator,
            TenantFleetBaseReplayer fleetReplayer,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        return new BaseDataRegistryServiceImpl(routingProvider, primaryDsKey,
                tenantProperties.getSchema().getPlatformDs(), migrator, fleetReplayer, redisTemplateProvider);
    }

    /** 基座库脚本注册契约：业务微服务（starter-bridge 推送器）推送子应用/菜单/权限点幂等脚本 */
    @Bean
    @ConditionalOnMissingBean(RemoteBaseDataRegistry.class)
    public RemoteBaseDataRegistry remoteBaseDataRegistryImpl(BaseDataRegistryService baseDataRegistryService) {
        return new RemoteBaseDataRegistryImpl(baseDataRegistryService);
    }

    /** 目录变更批次契约：新租户库懒初始化追平平台端目录编辑（批次表未建返回空） */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.api.system.RemoteCatalogRegistry.class)
    public vip.mseapp.api.system.RemoteCatalogRegistry remoteCatalogRegistryImpl(
            ObjectProvider<DynamicRoutingDataSource> routingProvider,
            @Value("${spring.datasource.dynamic.primary:master}") String primaryDsKey,
            MseTenantProperties tenantProperties) {
        return new vip.mseapp.service.system.rpc.RemoteCatalogRegistryImpl(routingProvider, primaryDsKey,
                tenantProperties.getSchema().getPlatformDs());
    }

    /**
     * 目录变更批次记录器：平台端菜单/权限点/子应用编辑落批次（standalone 不装配——
     * 单库目录自持，编辑直接生效无需扇出）。
     */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.service.system.catalog.CatalogChangeRecorder.class)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            name = "mse.standalone.enabled", havingValue = "false", matchIfMissing = true)
    public vip.mseapp.service.system.catalog.CatalogChangeRecorder catalogChangeRecorder(
            vip.mseapp.service.system.mapper.SysCatalogChangeMapper mapper) {
        return new vip.mseapp.service.system.catalog.CatalogChangeRecorder(mapper);
    }

    /**
     * 目录变更批次调谐器：周期扇出到登记库全集（mse.catalog.dispatch.enabled 可关；
     * 间隔/窗口：mse.catalog.dispatch.interval-seconds 默认 60、window-days 默认 30）。
     */
    @Bean
    @ConditionalOnMissingBean(vip.mseapp.service.system.catalog.CatalogChangeDispatcher.class)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "mse.catalog.dispatch", name = "enabled", matchIfMissing = true)
    public vip.mseapp.service.system.catalog.CatalogChangeDispatcher catalogChangeDispatcher(
            vip.mseapp.service.system.mapper.SysCatalogChangeMapper mapper,
            TenantFleetBaseReplayer fleetReplayer,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            @Value("${mse.catalog.dispatch.window-days:30}") int windowDays) {
        return new vip.mseapp.service.system.catalog.CatalogChangeDispatcher(mapper, fleetReplayer,
                redisTemplateProvider, windowDays);
    }

    /** 部门服务契约：开放平台等业务微服务消费 */
    @Bean
    @ConditionalOnMissingBean(RemoteDeptService.class)
    public RemoteDeptService remoteDeptServiceImpl(SysDeptService deptService) {
        return new RemoteDeptServiceImpl(deptService);
    }

    /** 角色服务契约：开放平台等业务微服务消费 */
    @Bean
    @ConditionalOnMissingBean(RemoteRoleService.class)
    public RemoteRoleService remoteRoleServiceImpl(SysRoleService roleService) {
        return new RemoteRoleServiceImpl(roleService);
    }

    /** 数据范围查询契约：业务微服务（搜索引擎个人数据过滤等）按 userId 解析数据范围 */
    @Bean
    @ConditionalOnMissingBean(RemoteDataScopeService.class)
    public RemoteDataScopeService remoteDataScopeServiceImpl(SysUserMapper userMapper, SysRoleMapper roleMapper,
                                                             SysDeptMapper deptMapper) {
        return new RemoteDataScopeServiceImpl(userMapper, roleMapper, deptMapper);
    }
}
