package vip.mseapp.service.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.api.tenant.dto.TablePolicyDTO;
import vip.mseapp.api.tenant.dto.TenantRouteDTO;
import vip.mseapp.core.datasource.TablePolicy;
import vip.mseapp.core.datasource.TenantIsolationMode;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteResolver;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.mybatis.datasource.TenantDbCloner;
import vip.mseapp.mybatis.datasource.TenantDatasourceDefinition;
import vip.mseapp.mybatis.datasource.TenantDsRegistry;
import vip.mseapp.mybatis.datasource.TenantDsUrlBuilder;
import vip.mseapp.mybatis.datasource.TenantServiceModuleRegistry;
import vip.mseapp.redis.TenantCache;
import vip.mseapp.service.platform.entity.SysTenant;
import vip.mseapp.service.platform.entity.SysTenantDatasource;
import vip.mseapp.service.platform.entity.SysTenantServiceConfig;
import vip.mseapp.service.platform.entity.SysTenantServiceTablePolicy;
import vip.mseapp.service.platform.mapper.SysTenantDatasourceMapper;
import vip.mseapp.service.platform.mapper.SysTenantMapper;
import vip.mseapp.service.platform.mapper.SysTenantServiceConfigMapper;
import vip.mseapp.service.platform.mapper.SysTenantServiceTablePolicyMapper;
import vip.mseapp.service.platform.service.SysTenantDatasourceService;
import vip.mseapp.service.platform.service.SysTenantRouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

// ==================== !!!控制面服务边界（勿删类级注解）!!! ====================
/**
 * 控制面服务：读写仅平台库存在的表（sys_tenant_service_config/sys_tenant/sys_tenant_datasource 等）。
 *
 * <p>类级 {@code NOT_SUPPORTED} 是防错库堤坝，必须整类生效：控制面任何 SQL 都不允许混入调用方
 * 可能持有的租户路由事务——事务在 doBegin 已把物理连接钉死在租户库，控制面查询跟随绑定连接即报
 * 「Table '租户库.sys_tenant_service_config' doesn't exist」并误降级共享缺省路由（see c60b444）。
 * 挂起后控制面查询按平台实体钉库执行（与无事务路径同通道）；新增公开方法自动受类级注解覆盖，
 * 内部 this. 自调用由入口方法的挂起语义兜住。
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class SysTenantRouteServiceImpl extends CrudRepository<SysTenantServiceConfigMapper, SysTenantServiceConfig>
        implements SysTenantRouteService {

    private static final Logger log = LoggerFactory.getLogger(SysTenantRouteServiceImpl.class);

    private static final long TTL_MILLIS = 30_000;

    private final SysTenantServiceTablePolicyMapper policyMapper;

    private final SysTenantMapper tenantMapper;

    /** 租户数据源表：已知服务名的第三路来源（已有配置中出现的服务） */
    private final SysTenantDatasourceMapper datasourceMapper;

    private final ObjectProvider<TenantDsRegistry> registryProvider;

    /** 模块服务身份收集器（@TenantServiceModule）：已知服务名的第一路来源 */
    private final ObjectProvider<TenantServiceModuleRegistry> moduleRegistryProvider;

    private final ObjectProvider<SysTenantDatasourceService> datasourceServiceProvider;


    private final ObjectProvider<vip.mseapp.mybatis.config.MseTenantProperties> tenantPropertiesProvider;

    /** 租户控制面 Redis 缓存（L2，跨实例共享；缺席时退化为仅进程内 L1 + DB） */
    private final ObjectProvider<TenantCache> tenantCacheProvider;

    /** 配置文件/Nacos 默认主库的数据源 key（spring.datasource.dynamic.primary，缺省 master）：绑定它=共享字段隔离落本服务主库 */

    /** 路由 L1（TTL 30s + 容量上限 + 周期清扫——过期不访问的键不再驻留） */
    private final vip.mseapp.core.concurrent.TtlBoundedCache<CacheKey, TenantRoute> cache =
            new vip.mseapp.core.concurrent.TtlBoundedCache<>(TTL_MILLIS, 4096, "mse-platform-route");

    /** 租户事件发送器（路由/策略变更广播 ROUTE_CHANGED 给数据面；无 mq-starter 时=本进程 Spring 事件） */
    private final ObjectProvider<vip.mseapp.mq.TenantEventSender> eventSenderProvider;

    private record CacheKey(Long tenantId, String serviceName) {
    }

    public SysTenantRouteServiceImpl(SysTenantServiceTablePolicyMapper policyMapper,
                                     SysTenantMapper tenantMapper,
                                     SysTenantDatasourceMapper datasourceMapper,
                                     ObjectProvider<TenantServiceModuleRegistry> moduleRegistryProvider,
                                     ObjectProvider<TenantDsRegistry> registryProvider,
                                     ObjectProvider<SysTenantDatasourceService> datasourceServiceProvider,
                                     ObjectProvider<vip.mseapp.mybatis.config.MseTenantProperties> tenantPropertiesProvider,
                                     ObjectProvider<TenantCache> tenantCacheProvider,
                                     ObjectProvider<vip.mseapp.mq.TenantEventSender> eventSenderProvider) {
        this.policyMapper = policyMapper;
        this.tenantMapper = tenantMapper;
        this.datasourceMapper = datasourceMapper;
        this.moduleRegistryProvider = moduleRegistryProvider;
        this.registryProvider = registryProvider;
        this.datasourceServiceProvider = datasourceServiceProvider;
        this.tenantPropertiesProvider = tenantPropertiesProvider;
        this.tenantCacheProvider = tenantCacheProvider;
        this.eventSenderProvider = eventSenderProvider;
    }

    /** 释放缓存清扫线程（daemon 不阻断退出，显式关闭仅为整洁；@Bean 注册自动推断 destroy） */
    public void close() {
        cache.close();
    }

    // ===== 运行时路由解析（TenantRouteResolver SPI，30s L1 + Redis L2；挂起语义见类级注解）=====

    @Override
    public TenantRoute resolveRoute(Long tenantId, String serviceName) {
        String service = normalizeService(serviceName);
        CacheKey key = new CacheKey(tenantId, service);
        TenantRoute route = cache.get(key);
        if (route == null) {
            route = loadWithCache(tenantId, service);
            cache.put(key, route);
        }
        return route;
    }

    /**
     * L2（Redis 共享缓存，key 带版本号 + TTL 失效，多实例任一回填其余命中）→ L3（DB 三表解析）。
     * 解析失败不阻断 SQL：退回字段隔离缺省快照（与历史语义一致）——降级快照只进 L1 30s，
     * 绝不进 L2，防短暂 DB 故障把缺省路由毒化进共享缓存。
     */
    private TenantRoute loadWithCache(Long tenantId, String service) {
        TenantCache tenantCache = tenantCacheProvider.getIfAvailable();
        try {
            if (tenantCache != null) {
                // L2 载体为 TenantRouteDTO（TenantRoute 不可变类无默认构造，不能直接 JSON 反序列化）
                return fromRouteDto(tenantCache.getRoute(tenantId, service, TenantRouteDTO.class,
                        () -> toRouteDto(loadStrict(tenantId, service))));
            }
            return loadStrict(tenantId, service);
        } catch (Exception e) {
            // 解析失败退回共享缺省快照（dsKey=null=各服务本地内置共享库，字段隔离）——
            // 平台侧不感知各服务内置坐标，null 由数据面拦截器本地解析
            log.warn("[mse-tenant] 解析租户路由失败，退回共享缺省: tenantId={} service={}", tenantId, service, e);
            return TenantRoute.sharedDefault(tenantId, service, null);
        }
    }

    @Override
    public void evict(Long tenantId) {
        cache.removeIf(key -> key.tenantId().equals(tenantId));
        // 跨实例失效：INCR 租户级版本，其他实例 L1 过期（≤30s）后按新版本 key 回源
        TenantCache tenantCache = tenantCacheProvider.getIfAvailable();
        if (tenantCache != null) {
            tenantCache.evictTenant(tenantId);
        }
    }

    /**
     * 路由变更广播：本进程/Redis L1/L2 失效之外，再向数据面发 ROUTE_CHANGED 事件——
     * 微服务形态下数据面各业务实例的进程内快照经 MQ（或版本号节流校验）秒级失效，
     * 不再依赖「事件无人消费 + 快照无过期」的最长永久不生效路径。发送失败仅告警
     * （数据面另有版本校验/TTL 兜底收敛）。
     */
    private void publishRouteChanged(Long tenantId, String serviceName) {
        vip.mseapp.mq.TenantEventSender sender = eventSenderProvider.getIfAvailable();
        if (sender == null) {
            return;
        }
        try {
            sender.sendTenant(vip.mseapp.mq.TenantEvent.TOPIC, tenantId,
                    vip.mseapp.mq.TenantEvent.Type.ROUTE_CHANGED, serviceName == null ? "" : serviceName);
        } catch (Exception e) {
            log.warn("[mse-tenant] 路由变更事件广播失败（数据面经版本校验/TTL 兜底收敛）: tenant={} {}",
                    tenantId, e.getMessage());
        }
    }

    @Override
    public void evictAll() {
        cache.clear();
        TenantCache tenantCache = tenantCacheProvider.getIfAvailable();
        if (tenantCache != null) {
            tenantCache.evictGlobal();
        }
    }

    @Override
    public List<TenantRouteDTO> listRoutes(Long tenantId) {
        // 服务清单 = 服务配置表出现过的 serviceName ∪ "*" 全局缺省（tenant.dsKey 语义在 resolveRoute 内收敛）
        Set<String> services = new java.util.LinkedHashSet<>();
        services.add("*");
        lambdaQuery().eq(SysTenantServiceConfig::getTenantId, tenantId)
                .list().forEach(config -> services.add(config.getServiceName()));
        return services.stream()
                .map(service -> toRouteDto(resolveRoute(tenantId, service)))
                .toList();
    }

    private TenantRouteDTO toRouteDto(TenantRoute route) {
        TenantRouteDTO dto = new TenantRouteDTO();
        dto.setTenantId(route.getTenantId());
        dto.setServiceName(route.getServiceName());
        dto.setMode(route.getMode().name());
        dto.setDsKey(route.getDsKey());
        dto.setTableSuffix(route.getTableSuffix());
        dto.setTenantColumn(route.getTenantColumn());
        dto.setTablePolicies(route.getTablePolicies().entrySet().stream()
                .map(entry -> {
                    TablePolicyDTO policy = new TablePolicyDTO();
                    policy.setTableName(entry.getKey());
                    policy.setMode(entry.getValue().getMode().name());
                    policy.setTableSuffix(entry.getValue().getTableSuffix());
                    return policy;
                })
                .toList());
        return dto;
    }

    /** L2 载体（TenantRouteDTO）→ 路由快照重建（valueOf 非法值由 loadWithCache 外层 catch 兜底） */
    private TenantRoute fromRouteDto(TenantRouteDTO dto) {
        Map<String, TablePolicy> policies = new LinkedHashMap<>();
        if (dto.getTablePolicies() != null) {
            for (TablePolicyDTO policy : dto.getTablePolicies()) {
                policies.put(policy.getTableName().toLowerCase(),
                        new TablePolicy(TablePolicy.Mode.valueOf(policy.getMode()), policy.getTableSuffix()));
            }
        }
        return new TenantRoute(dto.getTenantId(), dto.getServiceName(),
                TenantIsolationMode.valueOf(dto.getMode()), dto.getDsKey(),
                dto.getTableSuffix(), dto.getTenantColumn(), policies);
    }

    /** 三表解析（无缓存；失败向上抛，由 loadWithCache 统一退缺省快照） */
    private TenantRoute loadStrict(Long tenantId, String service) {
        SysTenantServiceConfig config = lambdaQuery()
                .eq(SysTenantServiceConfig::getTenantId, tenantId)
                .eq(SysTenantServiceConfig::getServiceName, service)
                .one();
        if (config == null) {
            // 全局默认兜底：语义由绑定数据源的库模式决定——
            // shared 共享库 = SHARED_FIELD + 共享池路由（多租户同库，tenant_id 行级隔离）；
            // dedicated 独立库（含数据源记录已删除）= INDEPENDENT_DB（存量兼容）
            SysTenant tenant = tenantMapper.selectById(tenantId);
            String dsKey = tenant == null ? null : tenant.getDsKey();
            if (dsKey != null && !dsKey.isBlank()) {
                if (isSharedDatasource(dsKey)) {
                    return new TenantRoute(tenantId, service, TenantIsolationMode.SHARED_FIELD, dsKey,
                            null, "tenant_id", null);
                }
                return new TenantRoute(tenantId, service, TenantIsolationMode.INDEPENDENT_DB, dsKey,
                        null, "tenant_id", null);
            }
            // 未绑定数据源：共享缺省快照（dsKey=null=各服务本地内置共享库，字段隔离）
            return TenantRoute.sharedDefault(tenantId, service, null);
        }
        Map<String, TablePolicy> policies = new LinkedHashMap<>();
        for (SysTenantServiceTablePolicy policy : listPolicies(tenantId, service)) {
            policies.put(policy.getTableName().toLowerCase(),
                    new TablePolicy(TablePolicy.Mode.valueOf(policy.getTableMode()), policy.getTableSuffix()));
        }
        return new TenantRoute(tenantId, service,
                TenantIsolationMode.valueOf(config.getIsolationMode()),
                config.getDsKey(), config.getTableSuffix(), config.getTenantColumn(), policies);
    }

    // ===== 管理端 CRUD =====

    @Override
    public List<SysTenantServiceConfig> listByTenant(Long tenantId) {
        return lambdaQuery()
                .eq(SysTenantServiceConfig::getTenantId, tenantId)
                .orderByAsc(SysTenantServiceConfig::getServiceName)
                .list();
    }

    @Override
    public SysTenantServiceConfig getConfig(Long tenantId, String serviceName) {
        return lambdaQuery()
                .eq(SysTenantServiceConfig::getTenantId, tenantId)
                .eq(SysTenantServiceConfig::getServiceName, normalizeService(serviceName))
                .one();
    }

    @Override
    public void saveConfig(SysTenantServiceConfig config) {
        if (config.getTenantId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "tenantId 不能为空");
        }
        String service = normalizeService(config.getServiceName());
        if ("*".equals(service)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "service_name 不能为 '*'（'*' 仅用于通用数据源）");
        }
        config.setServiceName(service);
        validateMode(config);
        validateDatasource(config);
        config.setTenantColumn(normalizeColumn(config.getTenantColumn()));
        SysTenantServiceConfig existing = getConfig(config.getTenantId(), service);
        if (existing == null) {
            config.setId(null);
            save(config);
        } else {
            config.setId(existing.getId());
            updateById(config);
        }
        evict(config.getTenantId());
        publishRouteChanged(config.getTenantId(), service);
        // 分表模式：保存即补建缺失的后缀表（幂等，失败不阻断配置保存——首次路由/手动生成可重试）
        if (TenantIsolationMode.SHARED_TABLE_SPLIT.name().equals(config.getIsolationMode())) {
            try {
                String summary = generateSplitTables(config.getTenantId(), service);
                log.info("[mse-tenant] 分表策略已保存，补建分表完成: {} → {}", key(config.getTenantId(), service), summary);
            } catch (Exception e) {
                log.warn("[mse-tenant] 分表补建失败（可稍后在隔离策略页重试）: {} → {}",
                        key(config.getTenantId(), service), e.getMessage());
            }
        }
    }

    @Override
    public void deleteConfig(Long id) {
        SysTenantServiceConfig config = getById(id);
        if (config == null) {
            return;
        }
        removeById(id);
        policyMapper.delete(new LambdaQueryWrapper<SysTenantServiceTablePolicy>()
                .eq(SysTenantServiceTablePolicy::getTenantId, config.getTenantId())
                .eq(SysTenantServiceTablePolicy::getServiceName, config.getServiceName()));
        evict(config.getTenantId());
        publishRouteChanged(config.getTenantId(), config.getServiceName());
    }

    @Override
    public List<SysTenantServiceTablePolicy> listPolicies(Long tenantId, String serviceName) {
        return policyMapper.selectList(new LambdaQueryWrapper<SysTenantServiceTablePolicy>()
                .eq(SysTenantServiceTablePolicy::getTenantId, tenantId)
                .eq(SysTenantServiceTablePolicy::getServiceName, normalizeService(serviceName))
                .orderByAsc(SysTenantServiceTablePolicy::getTableName));
    }

    @Override
    public void savePolicy(SysTenantServiceTablePolicy policy) {
        if (policy.getTenantId() == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "tenantId 不能为空");
        }
        String service = normalizeService(policy.getServiceName());
        if (!hasText(policy.getTableName())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "表名不能为空");
        }
        policy.setServiceName(service);
        policy.setTableName(policy.getTableName().trim().toLowerCase());
        if (!"SPLIT".equals(policy.getTableMode()) && !"FIELD".equals(policy.getTableMode()) && !"GLOBAL".equals(policy.getTableMode())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "表级模式非法（SPLIT/FIELD/GLOBAL）");
        }
        if ("SPLIT".equals(policy.getTableMode()) && !hasText(policy.getTableSuffix())
                && !hasText(serviceSuffix(policy.getTenantId(), service))) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "SPLIT 策略需要表级或服务级分表后缀");
        }
        policy.setTableSuffix(blankToNull(policy.getTableSuffix()));
        SysTenantServiceTablePolicy existing = policyMapper.selectOne(new LambdaQueryWrapper<SysTenantServiceTablePolicy>()
                .eq(SysTenantServiceTablePolicy::getTenantId, policy.getTenantId())
                .eq(SysTenantServiceTablePolicy::getServiceName, service)
                .eq(SysTenantServiceTablePolicy::getTableName, policy.getTableName()));
        if (existing == null) {
            policy.setId(null);
            policyMapper.insert(policy);
        } else {
            policy.setId(existing.getId());
            policyMapper.updateById(policy);
        }
        evict(policy.getTenantId());
        publishRouteChanged(policy.getTenantId(), service);
    }

    @Override
    public void deletePolicy(Long id) {
        SysTenantServiceTablePolicy policy = policyMapper.selectById(id);
        if (policy == null) {
            return;
        }
        policyMapper.deleteById(id);
        evict(policy.getTenantId());
        publishRouteChanged(policy.getTenantId(), policy.getServiceName());
    }

    @Override
    public void validateDefaultBinding(String dsKey, Long excludeTenantId) {
        if (!hasText(dsKey)) {
            return;
        }
        SysTenantDatasource ds = datasourceServiceProvider.getObject()
                .lambdaQuery().eq(SysTenantDatasource::getDsKey, dsKey.trim()).one();
        if (ds == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "租户数据源不存在：" + dsKey + "，请先在「平台管理 → 租户数据源」维护");
        }
        if (Integer.valueOf(1).equals(ds.getStatus())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "租户数据源已停用：" + dsKey);
        }
        // 全局默认绑定仅接受 '*' 通用数据源：服务专属数据源只在该服务进程注册连接池，
        // 其他服务路由命中后只能降级主库字段隔离（绑定前拦住，避免部署形态退化）
        String scope = !hasText(ds.getServiceName()) ? "*" : ds.getServiceName().trim();
        if (!"*".equals(scope)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "服务专属数据源（" + scope + "）不能作为租户全局默认绑定：请使用 '*' 通用数据源，"
                            + "或改在该服务的隔离配置中绑定");
        }
        // 独占校验仅针对独立库（一库一租户）：其他租户的全局默认 / 任意服务配置不得占用同一 dedicated 数据源；
        // 共享库允许多租户绑定（tenant_id 行级隔离）
        if (SysTenantDatasource.MODE_DEDICATED.equals(ds.getDsMode())) {
            Long used = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                    .eq(SysTenant::getDsKey, dsKey.trim())
                    .ne(excludeTenantId != null, SysTenant::getId, excludeTenantId));
            if (used != null && used > 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "独立库已被其他租户绑定（一库一租户）");
            }
            Long usedInConfig = lambdaQuery()
                    .eq(SysTenantServiceConfig::getDsKey, dsKey.trim())
                    .ne(excludeTenantId != null, SysTenantServiceConfig::getTenantId, excludeTenantId)
                    .count();
            if (usedInConfig != null && usedInConfig > 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "独立库已被其他租户的服务配置占用（一库一租户）");
            }
        }
    }

    @Override
    public List<String> listServiceNames() {
        Set<String> names = new TreeSet<>();
        TenantServiceModuleRegistry registry = moduleRegistryProvider.getIfAvailable();
        if (registry != null) {
            names.addAll(registry.serviceNames());
        }
        vip.mseapp.mybatis.config.MseTenantProperties properties = tenantPropertiesProvider.getIfAvailable();
        if (properties != null) {
            names.addAll(properties.getServiceMappings().keySet());
        }
        for (SysTenantServiceConfig config : lambdaQuery().select(SysTenantServiceConfig::getServiceName).list()) {
            addServiceName(names, config.getServiceName());
        }
        for (SysTenantDatasource datasource : datasourceMapper.selectList(
                new LambdaQueryWrapper<SysTenantDatasource>().select(SysTenantDatasource::getServiceName))) {
            addServiceName(names, datasource.getServiceName());
        }
        return new ArrayList<>(names);
    }

    private void addServiceName(Set<String> names, String serviceName) {
        if (hasText(serviceName) && !"*".equals(serviceName)) {
            names.add(serviceName);
        }
    }

    // ===== 内部 =====

    /** 全局默认绑定的数据源是否共享库（shared）；dedicated 或数据源记录缺失均按独立库存量语义处理 */
    private boolean isSharedDatasource(String dsKey) {
        SysTenantDatasource ds = datasourceServiceProvider.getObject()
                .lambdaQuery().eq(SysTenantDatasource::getDsKey, dsKey).one();
        return ds != null && SysTenantDatasource.MODE_SHARED.equals(ds.getDsMode());
    }

    private void validateMode(SysTenantServiceConfig config) {
        try {
            TenantIsolationMode.valueOf(config.getIsolationMode());
        } catch (Exception e) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "隔离模式非法（INDEPENDENT_DB / SHARED_TABLE_SPLIT / SHARED_FIELD）");
        }
    }

    private void validateDatasource(SysTenantServiceConfig config) {
        if (!hasText(config.getDsKey())) {
            config.setDsKey(null);
            return;
        }
        SysTenantDatasource ds = datasourceServiceProvider.getObject()
                .lambdaQuery().eq(SysTenantDatasource::getDsKey, config.getDsKey().trim()).one();
        if (ds == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "租户数据源不存在：" + config.getDsKey() + "，请先在「平台管理 → 租户数据源」维护");
        }
        if (Integer.valueOf(1).equals(ds.getStatus())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "租户数据源已停用：" + config.getDsKey());
        }
        // 服务级绑定作用域：'*' 通用数据源或与本服务一致（专属数据源仅归属服务注册连接池）
        String scope = !hasText(ds.getServiceName()) ? "*" : ds.getServiceName().trim();
        String configService = config.getServiceName() == null ? "" : config.getServiceName().trim();
        if (!"*".equals(scope) && !scope.equals(configService)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "数据源 " + config.getDsKey() + " 为 " + scope + " 服务专属，不能绑定到 "
                            + configService + " 服务的隔离配置");
        }
        config.setDsKey(config.getDsKey().trim());
        if (SysTenantDatasource.MODE_DEDICATED.equals(ds.getDsMode())) {
            // 独立库独占：同 ds_key 的其他 (租户, 服务) 配置不允许存在
            Long usedInConfig = lambdaQuery()
                    .eq(SysTenantServiceConfig::getDsKey, config.getDsKey())
                    .ne(config.getTenantId() != null, SysTenantServiceConfig::getTenantId, config.getTenantId())
                    .count();
            if (usedInConfig != null && usedInConfig > 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        "独立库已被其他租户/服务配置占用（一库一租户）");
            }
        }
    }

    private String serviceSuffix(Long tenantId, String service) {
        SysTenantServiceConfig config = getConfig(tenantId, service);
        return config == null ? null : config.getTableSuffix();
    }

    @Override
    public String generateSplitTables(Long tenantId, String serviceName) {
        String service = normalizeService(serviceName);
        SysTenantServiceConfig config = getConfig(tenantId, service);
        if (config == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "请先保存该租户在 " + service + " 服务的隔离配置");
        }
        // 分表源库 = 内置共享库（本服务 mse.tenant.shared-main.* 自declare；primary 是平台库不可作源）
        vip.mseapp.mybatis.config.MseTenantProperties.SharedMain sharedDs =
                tenantPropertiesProvider.getIfAvailable(vip.mseapp.mybatis.config.MseTenantProperties::new)
                        .getSharedMain();
        if (sharedDs == null || sharedDs.getUrl() == null || sharedDs.getUrl().isBlank()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "本服务未配置内置共享库（mse.tenant.shared-main.*），无法生成分表");
        }
        String sharedPassword = sharedDs.getPassword();
        TenantRoute route = resolveRoute(tenantId, service);
        Set<String> ignoreTables = TenantDbCloner.ignoreTableNames(tenantPropertiesProvider
                .getIfAvailable(vip.mseapp.mybatis.config.MseTenantProperties::new));
        try (Connection source = DriverManager.getConnection(sharedDs.getUrl(), sharedDs.getUsername(), sharedPassword)) {
            List<String> tables = TenantDbCloner.tenantTables(source, ignoreTables);
            // 两遍扫描：① 候选 rename；② 排除「本身已是其他表的分表目标」的键——源库清单含已生成的
            // 物理分表（demo_order_0001），不排除则被再当逻辑表叠加后缀（demo_order_0001_0001），
            // 每次调用多一层永不收敛
            Map<String, String> candidates = new LinkedHashMap<>();
            for (String table : tables) {
                if (route.effectiveMode(table) == TablePolicy.Mode.SPLIT) {
                    String suffix = route.effectiveSuffix(table);
                    if (hasText(suffix)) {
                        candidates.put(table, table + suffix);
                    }
                }
            }
            java.util.Set<String> targets = new java.util.HashSet<>(candidates.values());
            Map<String, String> renames = new LinkedHashMap<>();
            candidates.forEach((table, target) -> {
                if (!targets.contains(table)) {
                    renames.put(table, target);
                }
            });
            if (renames.isEmpty()) {
                return "当前配置未启用任何 SPLIT 分表策略，无需生成分表";
            }
            TenantDbCloner.CloneResult result;
            if (hasText(config.getDsKey())) {
                SysTenantDatasource ds = datasourceServiceProvider.getObject()
                        .lambdaQuery().eq(SysTenantDatasource::getDsKey, config.getDsKey()).one();
                if (ds == null) {
                    throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "路由数据源不存在：" + config.getDsKey());
                }
                String password = decrypt(ds.getPassword());
                try (Connection target = DriverManager.getConnection(
                        TenantDsUrlBuilder.build(toDefinition(ds)), ds.getUsername(), password)) {
                    result = TenantDbCloner.cloneMissing(source, target, new ArrayList<>(renames.keySet()), renames);
                }
            } else {
                // 未配置专属绑定：分表与主表同在缺省共享库
                result = TenantDbCloner.cloneMissing(source, source, new ArrayList<>(renames.keySet()), renames);
            }
            String summary = "分表 " + renames.size() + " 张：本次新建 " + result.created() + "、已存在 " + result.existed();
            log.info("[mse-tenant] 生成分表完成: {} → {}", key(tenantId, service), summary);
            return summary;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "生成分表失败：" + e.getMessage());
        }
    }

    private TenantDatasourceDefinition toDefinition(SysTenantDatasource ds) {
        TenantDatasourceDefinition def = new TenantDatasourceDefinition();
        def.setId(ds.getId());
        def.setDsKey(ds.getDsKey());
        def.setDsMode(ds.getDsMode());
        def.setServiceName(ds.getServiceName());
        def.setDbType(ds.getDbType());
        def.setHost(ds.getHost());
        def.setPort(ds.getPort());
        def.setDbName(ds.getDbName());
        def.setParams(ds.getParams());
        def.setUsername(ds.getUsername());
        def.setPassword(ds.getPassword());
        def.setStatus(ds.getStatus());
        return def;
    }

    private String decrypt(String stored) {
        TenantDsRegistry registry = registryProvider.getIfAvailable();
        if (registry != null) {
            return registry.decrypt(stored);
        }
        throw new ServiceException(GlobalErrorCode.INTERNAL_ERROR.getCode(), "TenantDsRegistry 未装配，无法解密数据源密码");
    }

    private String key(Long tenantId, String service) {
        return "tenant=" + tenantId + ",service=" + service;
    }

    private String normalizeService(String serviceName) {
        if (!hasText(serviceName)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "service_name 不能为空");
        }
        return serviceName.trim();
    }

    private String normalizeColumn(String column) {
        return hasText(column) ? column.trim() : null;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
