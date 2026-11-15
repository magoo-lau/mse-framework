package vip.mseapp.core.datasource;

/**
 * 租户×服务路由解析 SPI（base 侧组装：service_config + 表级策略 + ds_key 全局默认）。
 *
 * <p>实现随部署形态替换：service-tenant 本地实现（单体/mse-tenant）、starter-bridge RPC
 * 实现（业务微服务）。实现内部自带 30s 缓存；策略/配置变更时按租户失效。
 */
public interface TenantRouteResolver {

    /**
     * 解析租户在指定服务下的路由快照。
     *
     * @param tenantId    租户编号
     * @param serviceName 服务名（spring.application.name）
     * @return 路由快照，永不为 null（未配置返回 SHARED_FIELD 缺省快照）
     */
    TenantRoute resolveRoute(Long tenantId, String serviceName);

    /** 服务级配置/绑定变更时失效该租户的解析缓存（默认空实现） */
    default void evict(Long tenantId) {
    }

    /** 全量失效（策略批量变更） */
    default void evictAll() {
    }
}
