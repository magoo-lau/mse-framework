package vip.mseapp.service.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.datasource.TenantRouteResolver;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.mybatis.datasource.TenantDsRegistry;
import vip.mseapp.mybatis.datasource.TenantDsUrlBuilder;
import vip.mseapp.service.platform.entity.SysTenant;
import vip.mseapp.service.platform.entity.SysTenantDatasource;
import vip.mseapp.service.platform.entity.SysTenantServiceConfig;
import vip.mseapp.service.platform.mapper.SysTenantDatasourceMapper;
import vip.mseapp.service.platform.mapper.SysTenantMapper;
import vip.mseapp.service.platform.mapper.SysTenantServiceConfigMapper;
import vip.mseapp.service.platform.service.SysTenantDatasourceService;
import vip.mseapp.service.platform.service.support.TenantDatasourceBatchSyncService;
import vip.mseapp.service.platform.service.support.TenantDatasourceConnectionTester;
import vip.mseapp.service.platform.service.support.TenantDatasourceInitializer;
import vip.mseapp.service.platform.service.support.TenantDatasourceSupport;
import vip.mseapp.service.platform.service.support.TenantSchemaOpsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;

/**
 * 租户数据源管理门面：CRUD/校验/视图在本类，重活委托同包级协作者（service.support）——
 * 连接测试 {@link TenantDatasourceConnectionTester}、初始化 {@link TenantDatasourceInitializer}、
 * 结构演进（迁移/同步/预演/状态/临时 SQL）{@link TenantSchemaOpsService}、批量同步
 * {@link TenantDatasourceBatchSyncService}，共享小工具 {@link TenantDatasourceSupport}。
 * 协作者经 TenantServiceAutoConfiguration @Bean 注册（内部实现，不带 ConditionalOnMissingBean）。
 */
// 控制面服务（sys_tenant_datasource 等仅平台库存在）：类级 NOT_SUPPORTED 防错库堤坝，勿删
//（范式详见 SysTenantRouteServiceImpl 类注释）。
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class SysTenantDatasourceServiceImpl extends CrudRepository<SysTenantDatasourceMapper, SysTenantDatasource>
        implements SysTenantDatasourceService {

    private static final Logger log = LoggerFactory.getLogger(SysTenantDatasourceServiceImpl.class);

    /** 内置主库哨兵 id：0=通用行，负数=微服务形态各服务行；正数=物理行 */
    private static final long BUILTIN_ID_UNIVERSAL = 0L;

    private final SysTenantMapper tenantMapper;
    private final SysTenantServiceConfigMapper configMapper;
    private final ObjectProvider<TenantDsRegistry> registryProvider;
    /** 主数据源（dynamic-datasource 路由器，无上下文时 getConnection() 即平台库） */
    private final ObjectProvider<DataSource> masterDataSourceProvider;

    /** 路由解析器（数据源变更失效路由缓存；与 SysTenantRouteServiceImpl 互引，Provider 延迟解析） */
    private final ObjectProvider<TenantRouteResolver> routeResolverProvider;

    private final TenantDatasourceSupport support;
    private final TenantDatasourceConnectionTester connectionTester;
    private final TenantDatasourceInitializer initializer;
    private final TenantSchemaOpsService schemaOps;
    private final TenantDatasourceBatchSyncService batchSync;

    public SysTenantDatasourceServiceImpl(SysTenantMapper tenantMapper,
                                          SysTenantServiceConfigMapper configMapper,
                                          ObjectProvider<TenantDsRegistry> registryProvider,
                                          ObjectProvider<DataSource> masterDataSourceProvider,
                                          ObjectProvider<TenantRouteResolver> routeResolverProvider,
                                          TenantDatasourceSupport support,
                                          TenantDatasourceConnectionTester connectionTester,
                                          TenantDatasourceInitializer initializer,
                                          TenantSchemaOpsService schemaOps,
                                          TenantDatasourceBatchSyncService batchSync) {
        this.tenantMapper = tenantMapper;
        this.configMapper = configMapper;
        this.registryProvider = registryProvider;
        this.masterDataSourceProvider = masterDataSourceProvider;
        this.routeResolverProvider = routeResolverProvider;
        this.support = support;
        this.connectionTester = connectionTester;
        this.initializer = initializer;
        this.schemaOps = schemaOps;
        this.batchSync = batchSync;
    }

    @Override
    public PageResult<DatasourceView> pageDatasources(long pageNum, long pageSize, String dsKey, String dsMode,
                                                      String serviceName, Integer status) {
        List<DatasourceView> builtin = List.of(); // 内置主库虚拟行已退役：缺省共享库是登记表物理行
        Page<SysTenantDatasource> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysTenantDatasource>()
                        .like(TenantDatasourceSupport.hasText(dsKey), SysTenantDatasource::getDsKey, dsKey)
                        .eq(TenantDatasourceSupport.hasText(dsMode), SysTenantDatasource::getDsMode, dsMode)
                        .eq(TenantDatasourceSupport.hasText(serviceName), SysTenantDatasource::getServiceName, serviceName)
                        .eq(status != null, SysTenantDatasource::getStatus, status)
                        .orderByDesc(SysTenantDatasource::getId));
        List<DatasourceView> views = new ArrayList<>(page.getRecords().stream().map(this::toView).toList());
        // 内置行第 1 页置顶（与库模式/key/服务/状态筛选同口径），总数随之增加
        if (pageNum == 1 && !builtin.isEmpty()) {
            builtin.addAll(views);
            views = builtin.size() > (int) pageSize ? new ArrayList<>(builtin.subList(0, (int) pageSize)) : builtin;
        }
        return PageResult.of(page.getTotal() + builtin.size(), pageNum, pageSize, views);
    }

    @Override
    public List<DatasourceView> listOptions() {
        List<DatasourceView> options = new ArrayList<>();
        lambdaQuery()
                .eq(SysTenantDatasource::getStatus, 0)
                .orderByDesc(SysTenantDatasource::getId)
                .list().stream().map(this::toView).forEach(options::add);
        return options;
    }

    @Override
    public DatasourceView getDatasource(Long id) {
        requireMutableId(id);
        SysTenantDatasource ds = getById(id);
        return ds == null ? null : toView(ds);
    }

    @Override
    public Long createDatasource(SysTenantDatasource datasource) {
        validateForm(datasource);
        Long count = lambdaQuery().eq(SysTenantDatasource::getDsKey, datasource.getDsKey()).count();
        if (count != null && count > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "数据源 key 已存在：" + datasource.getDsKey());
        }
        if (!TenantDatasourceSupport.hasText(datasource.getPassword())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "连接密码不能为空");
        }
        datasource.setId(null);
        datasource.setStatus(0);
        datasource.setPassword(support.encrypt(datasource.getPassword()));
        save(datasource);
        applyRuntime(datasource.getDsKey());
        evictRouteCache();
        return datasource.getId();
    }

    @Override
    public void updateDatasource(SysTenantDatasource payload) {
        requireMutableId(payload.getId());
        SysTenantDatasource old = getById(payload.getId());
        if (old == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        String newKey = payload.getDsKey();
        if (!TenantDatasourceSupport.hasText(newKey)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "数据源 key 不能为空");
        }
        if (!old.getDsKey().equals(newKey.trim())) {
            long bound = countBound(old.getDsKey(), null);
            if (bound > 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        "数据源已被 " + bound + " 个租户绑定，不可修改 key；请先解绑相关租户");
            }
        }
        validateScopeChange(old, payload);
        validateForm(payload);
        if (!TenantDatasourceSupport.hasText(payload.getPassword())) {
            payload.setPassword(old.getPassword());
        } else {
            payload.setPassword(support.encrypt(payload.getPassword()));
        }
        updateById(payload);
        applyRuntime(old.getDsKey());
        if (!old.getDsKey().equals(payload.getDsKey())) {
            applyRuntime(payload.getDsKey());
        }
        evictRouteCache();
    }

    @Override
    public void deleteDatasource(Long id) {
        requireMutableId(id);
        SysTenantDatasource ds = getById(id);
        if (ds == null) {
            return;
        }
        long bound = countBound(ds.getDsKey(), null);
        if (bound > 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "数据源已被 " + bound + " 个租户绑定，不可删除；请先解绑相关租户");
        }
        removeById(id);
        applyRuntime(ds.getDsKey());
        evictRouteCache();
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        requireMutableId(id);
        SysTenantDatasource ds = getById(id);
        if (ds == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        if (Integer.valueOf(1).equals(status)) {
            long bound = countBound(ds.getDsKey(), null);
            if (bound > 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        "数据源已被 " + bound + " 个租户绑定，不可停用；停用会导致这些租户路由回落平台库");
            }
        }
        SysTenantDatasource update = new SysTenantDatasource();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
        applyRuntime(ds.getDsKey());
        evictRouteCache();
    }

    @Override
    public long testConnection(Long id, SysTenantDatasource form) {
        requireMutableId(id);
        // 密码是否取自库中密文：表单新输入的是明文（新增页/编辑页未保存的修改），直接试连不 decrypt
        boolean storedPassword = false;
        if (id != null) {
            SysTenantDatasource stored = getById(id);
            if (stored == null) {
                throw new ServiceException(GlobalErrorCode.NOT_FOUND);
            }
            // 表单携带了连接信息（编辑页未保存的修改）优先表单，密码留空用库中密文；否则按存量行测试
            if (form == null || !TenantDatasourceSupport.hasText(form.getHost())) {
                form = stored;
                storedPassword = true;
            } else if (!TenantDatasourceSupport.hasText(form.getPassword())) {
                form.setPassword(stored.getPassword());
                storedPassword = true;
            }
        }
        if (form == null || !TenantDatasourceSupport.hasText(form.getHost())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "缺少连接信息");
        }
        if (!TenantDatasourceSupport.hasText(form.getPassword())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "连接密码不能为空");
        }
        return connectionTester.test(form, storedPassword);
    }

    @Override
    public String initDatabase(Long id) {
        requireMutableId(id);
        SysTenantDatasource ds = getById(id);
        if (ds == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return initializer.initDatabase(ds);
    }

    @Override
    public InitStatusView initStatus(Long id) {
        requireMutableId(id);
        SysTenantDatasource ds = getById(id);
        if (ds == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return new InitStatusView(ds.getInitStatus(), ds.getInitMessage());
    }

    // ===== 结构演进：版本化迁移 + 结构比对同步（委托 TenantSchemaOpsService） =====

    @Override
    public String migrate(Long id) {
        requireMutableId(id);
        return schemaOps.migrate(requireDs(id));
    }

    @Override
    public String syncSchema(Long id) {
        requireMutableId(id);
        return schemaOps.syncSchema(id, requireDs(id));
    }

    @Override
    public SchemaPlanView planSchema(Long id) {
        requireMutableId(id);
        return schemaOps.planSchema(requireDs(id));
    }

    @Override
    public BatchSyncStatusView batchSyncStatus() {
        return batchSync.status();
    }

    @Override
    public SchemaStatusView schemaStatus(Long id) {
        requireMutableId(id);
        return schemaOps.schemaStatus(requireDs(id));
    }

    @Override
    public String executeAdhoc(Long id, String sql, String description) {
        if (sql == null || sql.isBlank()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "SQL 不能为空");
        }
        if (sql.length() > 200_000) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "SQL 过大（上限 20 万字符），请拆分执行");
        }
        requireMutableId(id);
        return schemaOps.executeAdhoc(requireDs(id), sql, description);
    }

    @Override
    public String batchSync() {
        List<DatasourceView> targets = listOptions().stream().filter(t -> !t.builtIn()).toList();
        List<Long> ids = targets.stream().map(DatasourceView::id).toList();
        return batchSync.submit(ids, targets);
    }

    // ===== 内部 =====

    private SysTenantDatasource requireDs(Long id) {
        SysTenantDatasource ds = getById(id);
        if (ds == null) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND);
        }
        return ds;
    }

    /** 管理面变更应用到运行时：本进程注册表即时生效，Redis 广播由 TenantDsRegistry 内部完成 */
    private void applyRuntime(String dsKey) {
        TenantDsRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            log.warn("[mse-tenant-ds] TenantDsRegistry 未装配（动态注册不可用），变更仅落库: key={}", dsKey);
            return;
        }
        try {
            SysTenantDatasource ds = lambdaQuery().eq(SysTenantDatasource::getDsKey, dsKey).one();
            if (ds == null || Integer.valueOf(1).equals(ds.getStatus())) {
                registry.unregister(dsKey);
            } else {
                registry.register(TenantDatasourceSupport.toDefinition(ds));
            }
        } catch (Exception e) {
            // lazy 池建连在首次路由时才发生，此处失败多为定义问题：不回滚管理操作，记录即可
            log.warn("[mse-tenant-ds] 应用运行时变更失败: key={}", dsKey, e);
        }
    }

    /**
     * 数据源变更影响路由解析（shared/dedicated 判定、key 变更、停用可用性）：全量失效路由缓存——
     * 本进程 L1 即时清 + INCR 全局版本（跨实例 L1 过期 ≤30s 后按新版本 key 回源）。低频管理操作，全量失效可接受。
     */
    private void evictRouteCache() {
        TenantRouteResolver routeResolver = routeResolverProvider.getIfAvailable();
        if (routeResolver != null) {
            routeResolver.evictAll();
        }
    }

    private DatasourceView toView(SysTenantDatasource ds) {
        Map<Long, String> bound = boundTenants(ds.getDsKey(), null);
        boolean taken = SysTenantDatasource.MODE_DEDICATED.equals(ds.getDsMode()) && !bound.isEmpty();
        Boolean registered = registryProvider.getIfAvailable() != null
                && registryProvider.getObject().isRegistered(ds.getDsKey());
        return new DatasourceView(ds.getId(), ds.getDsKey(), ds.getDsMode(), ds.getServiceName(), ds.getDbType(),
                ds.getHost(), ds.getPort(), ds.getDbName(), ds.getParams(), ds.getUsername(), ds.getStatus(),
                ds.getRemark(), ds.getCreateTime(), bound.size(),
                String.join("、", bound.values()), taken, registered, ds.getInitStatus(), ds.getInitMessage(),
                ds.getSyncStatus(), ds.getSyncMessage(), ds.getSyncTime(), false);
    }

    /** 哨兵 id（0/负数）防御：历史内置行时代的 id 段，物理行 id 恒为正——界面写操作一律拒绝 */
    private void requireMutableId(Long id) {
        if (id != null && id <= BUILTIN_ID_UNIVERSAL) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "内置数据源行不可在界面修改");
        }
    }

    /** 绑定来源（全局默认 sys_tenant.ds_key + 服务级 sys_tenant_service_config.ds_key；serviceName 非 null 时仅统计该服务配置） */
    private Map<Long, String> boundTenants(String dsKey, String serviceName) {
        Map<Long, String> bound = new java.util.LinkedHashMap<>();
        for (SysTenant tenant : tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getDsKey, dsKey)
                .select(SysTenant::getId, SysTenant::getName))) {
            bound.put(tenant.getId(), tenant.getName());
        }
        LambdaQueryWrapper<SysTenantServiceConfig> wrapper = new LambdaQueryWrapper<SysTenantServiceConfig>()
                .eq(SysTenantServiceConfig::getDsKey, dsKey)
                .select(SysTenantServiceConfig::getTenantId);
        if (serviceName != null) {
            wrapper.eq(SysTenantServiceConfig::getServiceName, serviceName);
        }
        List<Long> configTenantIds = configMapper.selectList(wrapper)
                .stream().map(SysTenantServiceConfig::getTenantId).toList();
        if (!configTenantIds.isEmpty()) {
            for (SysTenant tenant : tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                    .in(SysTenant::getId, configTenantIds)
                    .select(SysTenant::getId, SysTenant::getName))) {
                bound.putIfAbsent(tenant.getId(), tenant.getName());
            }
        }
        return bound;
    }

    /** 绑定引用数：全局默认绑定（sys_tenant.ds_key）+ 服务级配置（sys_tenant_service_config.ds_key） */
    private long countBound(String dsKey, Long excludeTenantId) {
        Long tenantCount = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getDsKey, dsKey)
                .ne(excludeTenantId != null, SysTenant::getId, excludeTenantId));
        Long configCount = configMapper.selectCount(new LambdaQueryWrapper<SysTenantServiceConfig>()
                .eq(SysTenantServiceConfig::getDsKey, dsKey));
        long total = (tenantCount == null ? 0 : tenantCount) + (configCount == null ? 0 : configCount);
        return total;
    }

    /**
     * 作用域变更兼容校验：数据源 service_name 变更后，现存绑定必须仍适配新作用域——
     * 全局默认绑定（sys_tenant.ds_key）仅接受 '*' 通用数据源；服务级配置（sys_tenant_service_config）
     * 要求 '*' 或与本服务一致。否则其他服务路由命中该 key 但本地未注册连接池，会降级主库执行。
     */
    private void validateScopeChange(SysTenantDatasource old, SysTenantDatasource payload) {
        String oldScope = TenantDatasourceSupport.hasText(old.getServiceName()) ? old.getServiceName().trim() : "*";
        String newScope = TenantDatasourceSupport.hasText(payload.getServiceName()) ? payload.getServiceName().trim() : "*";
        if (newScope.equals(oldScope)) {
            return;
        }
        if (!"*".equals(newScope)) {
            Long defaults = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                    .eq(SysTenant::getDsKey, payload.getDsKey()));
            if (defaults != null && defaults > 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        "数据源已被 " + defaults + " 个租户绑定为全局默认，作用域仅能为 '*'；请先解绑相关租户");
            }
        }
        List<SysTenantServiceConfig> configs = configMapper.selectList(new LambdaQueryWrapper<SysTenantServiceConfig>()
                .eq(SysTenantServiceConfig::getDsKey, payload.getDsKey()));
        for (SysTenantServiceConfig config : configs) {
            String configService = TenantDatasourceSupport.hasText(config.getServiceName()) ? config.getServiceName().trim() : "";
            if (!"*".equals(newScope) && !newScope.equals(configService)) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        "数据源已被租户 " + config.getTenantId() + " 的 " + configService + " 服务配置绑定，"
                                + "作用域不可变更为 " + newScope + "；请先调整相关隔离配置");
            }
        }
    }

    private void validateForm(SysTenantDatasource ds) {
        if (!TenantDatasourceSupport.hasText(ds.getDsKey())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "数据源 key 不能为空");
        }
        if (!SysTenantDatasource.MODE_SHARED.equals(ds.getDsMode())
                && !SysTenantDatasource.MODE_DEDICATED.equals(ds.getDsMode())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "库模式非法（shared/dedicated）");
        }
        try {
            TenantDsUrlBuilder.build(TenantDatasourceSupport.toDefinition(ds));
        } catch (IllegalArgumentException e) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), e.getMessage());
        }
        validateNotPlatformDb(ds);
    }

    /**
     * 防撞校验：租户数据源不得指向平台控制面库（同实例同名库即视为撞库）。
     * 控制面库承载 sys_tenant 系平台元数据，被注册为租户库会破坏控制面/数据面库边界。
     */
    private void validateNotPlatformDb(SysTenantDatasource ds) {
        String platform = platformDbCoordinates();
        if (platform == null) {
            return;
        }
        String target = (TenantDatasourceSupport.hasText(ds.getHost()) ? ds.getHost().trim().toLowerCase() : "")
                + ":" + ds.getPort() + "/" + (TenantDatasourceSupport.hasText(ds.getDbName()) ? ds.getDbName().trim().toLowerCase() : "");
        if (platform.equals(target)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "租户数据源不得指向平台库（" + platform + "）：平台库承载控制面元数据，请为租户库使用独立 database");
        }
    }

    /** 平台库坐标 host:port/db（解析本服务主库 JDBC URL；解析失败返回 null 跳过校验） */
    private String platformDbCoordinates() {
        String url = primaryJdbcUrl();
        if (url == null) {
            return null;
        }
        java.util.regex.Matcher matcher = JDBC_DB.matcher(url);
        return matcher.find() ? matcher.group(1).toLowerCase() + ":" + matcher.group(2)
                + "/" + matcher.group(3).toLowerCase() : null;
    }

    /** 本服务 primary JDBC URL（连接元数据读取；primary=平台库，作平台库坐标判定） */
    private String primaryJdbcUrl() {
        DataSource master = masterDataSourceProvider.getIfAvailable();
        if (!(master instanceof DynamicRoutingDataSource routing)) {
            return null;
        }
        try (Connection conn = routing.getConnection()) {
            return conn.getMetaData().getURL();
        } catch (SQLException e) {
            return null;
        }
    }

    /** jdbc:xxx://host:port/db?... 形态的坐标提取（dm 的 schema 参数形态不匹配时跳过校验） */
    private static final Pattern JDBC_DB =
            Pattern.compile("jdbc:\\w+://([^/:?]+):(\\d+)/([^?]+)", Pattern.CASE_INSENSITIVE);
}
