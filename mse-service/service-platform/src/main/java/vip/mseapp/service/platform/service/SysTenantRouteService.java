package vip.mseapp.service.platform.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteResolver;
import vip.mseapp.service.platform.entity.SysTenantServiceConfig;
import vip.mseapp.service.platform.entity.SysTenantServiceTablePolicy;

import java.util.List;

/**
 * 租户×服务隔离配置（多模式：独立库 / 共享库分表 / 共享库字段隔离 + 表级策略）。
 *
 * <p>同时是 {@link TenantRouteResolver} 的 base 本地实现：运行时路由解析（30s 缓存）与
 * 管理端 CRUD 同域收敛，配置变更即失效缓存并广播。
 */
public interface SysTenantRouteService extends IRepository<SysTenantServiceConfig>, TenantRouteResolver {

    /**
     * 租户的逐服务路由快照（含 "*" 全局缺省项）：租户快照下发用。
     */
    java.util.List<vip.mseapp.api.tenant.dto.TenantRouteDTO> listRoutes(Long tenantId);

    /** 管理端：按租户列配置（含表级策略数） */
    List<SysTenantServiceConfig> listByTenant(Long tenantId);

    /** 管理端：按 (租户, 服务) 取配置，无则返回 null */
    SysTenantServiceConfig getConfig(Long tenantId, String serviceName);

    /** 管理端：保存（按 tenant+service upsert；校验模式/数据源/独立库独占） */
    void saveConfig(SysTenantServiceConfig config);

    /** 管理端：删除配置并级联删除表级策略 */
    void deleteConfig(Long id);

    /** 管理端：表级策略列表 */
    List<SysTenantServiceTablePolicy> listPolicies(Long tenantId, String serviceName);

    /** 管理端：保存表级策略（按 tenant+service+table upsert） */
    void savePolicy(SysTenantServiceTablePolicy policy);

    /** 管理端：删除表级策略 */
    void deletePolicy(Long id);

    /**
     * 生成分表：按当前配置与表级策略，对 SPLIT 表在目标库（ds_key 指向的库；未配置则本库）
     * 克隆加后缀的表结构（幂等，仅补缺失）。返回执行摘要。
     */
    String generateSplitTables(Long tenantId, String serviceName);

    /** 全局默认绑定校验（sys_tenant.ds_key）：数据源存在且启用、独立库未被其他租户/服务配置占用 */
    void validateDefaultBinding(String dsKey, Long excludeTenantId);

    /**
     * 管理端：已知服务名清单（隔离策略与租户数据源两个管理页的「服务名」下拉共用）。
     * 三路合并去重排序：容器内 @TenantServiceModule 身份收集（单体聚合=全部业务模块，
     * 微服务=本服务）+ yaml mse.tenant.service-mappings 的 key + 两张配置表
     * （sys_tenant_service_config / sys_tenant_datasource）已出现的公共服务名（排除 '*'）。
     */
    List<String> listServiceNames();
}
