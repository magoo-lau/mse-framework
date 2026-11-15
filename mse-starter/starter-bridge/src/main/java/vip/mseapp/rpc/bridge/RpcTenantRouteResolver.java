package vip.mseapp.rpc.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.api.tenant.dto.TablePolicyDTO;
import vip.mseapp.api.tenant.dto.TenantRouteDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.concurrent.TtlBoundedCache;
import vip.mseapp.core.datasource.TablePolicy;
import vip.mseapp.core.datasource.TenantIsolationMode;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteResolver;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 租户×服务路由解析器的 RPC 实现（快照优先、平台 RPC 兜底）。
 *
 * <p>面向未装配 service-platform 的业务微服务。语义：优先读本地快照
 * （{@link TenantSnapshotStore}，数据面本地自治、运行期零调平台），快照未就绪时回退
 * 平台 RPC（{@link RemoteTenantService#getTenantRoute}），再退回共享库缺省（不阻断 SQL）。
 * 30s 本地缓存（租户+服务维度，TTL + 容量上限 + 周期清扫——过期不访问的键不再驻留内存）。
 * 平台本地实现在 classpath 时本类不装配。
 */
public class RpcTenantRouteResolver implements TenantRouteResolver {

    private static final Logger log = LoggerFactory.getLogger(RpcTenantRouteResolver.class);
    private static final long TTL_MILLIS = 30_000;
    private static final int MAX_ENTRIES = 4096;

    @RpcReference
    private RemoteTenantService remoteTenantService;

    private final org.springframework.beans.factory.ObjectProvider<TenantSnapshotStore> snapshotStoreProvider;

    public RpcTenantRouteResolver(org.springframework.beans.factory.ObjectProvider<TenantSnapshotStore> snapshotStoreProvider) {
        this.snapshotStoreProvider = snapshotStoreProvider;
    }

    private record CacheKey(Long tenantId, String serviceName) {
    }

    private final TtlBoundedCache<CacheKey, TenantRoute> cache =
            new TtlBoundedCache<>(TTL_MILLIS, MAX_ENTRIES, "mse-route-resolver");

    @Override
    public TenantRoute resolveRoute(Long tenantId, String serviceName) {
        String service = serviceName == null || serviceName.isBlank() ? "*" : serviceName.trim();
        CacheKey key = new CacheKey(tenantId, service);
        TenantRoute route = cache.get(key);
        if (route == null) {
            route = load(tenantId, service);
            cache.put(key, route);
        }
        return route;
    }

    /** 租户/配置变更时调用，防止最长 30s 的路由延迟 */
    @Override
    public void evict(Long tenantId) {
        cache.removeIf(key -> key.tenantId().equals(tenantId));
        log.info("[mse-bridge] 失效租户路由缓存: tenantId={}", tenantId);
    }

    @Override
    public void evictAll() {
        cache.clear();
    }

    private TenantRoute load(Long tenantId, String service) {
        // 快照优先：数据面本地自治（平台故障不影响已开通租户）
        TenantSnapshotStore store = snapshotStoreProvider.getIfAvailable();
        if (store != null) {
            Map<String, TenantRoute> routes = store.routes(tenantId);
            if (routes != null) {
                TenantRoute route = routes.get(service);
                if (route == null) {
                    route = routes.get("*");
                }
                if (route != null) {
                    return route;
                }
            }
        }
        try {
            TenantRouteDTO dto = remoteTenantService.getTenantRoute(tenantId, service);
            if (dto == null) {
                throw new IllegalStateException("租户不存在或平台无路由记录（拒绝静默落库）: tenant=" + tenantId);
            }
            Map<String, TablePolicy> policies = new LinkedHashMap<>();
            if (dto.getTablePolicies() != null) {
                for (TablePolicyDTO policy : dto.getTablePolicies()) {
                    policies.put(policy.getTableName().toLowerCase(),
                            new TablePolicy(TablePolicy.Mode.valueOf(policy.getMode()), policy.getTableSuffix()));
                }
            }
            return new TenantRoute(tenantId, service,
                    dto.getMode() == null ? TenantIsolationMode.SHARED_FIELD
                            : TenantIsolationMode.valueOf(dto.getMode()),
                    dto.getDsKey(), dto.getTableSuffix(), dto.getTenantColumn(), policies);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // 平台不可达且本地无快照：无安全落点（primary=平台库），快速失败——
            // 已缓存租户不受影响（快照自治），未缓存租户待平台恢复后自愈
            throw new IllegalStateException("租户路由解析失败（平台不可达且本地无快照）: tenant=" + tenantId
                    + " service=" + service + ": " + e.getMessage(), e);
        }
    }
}
