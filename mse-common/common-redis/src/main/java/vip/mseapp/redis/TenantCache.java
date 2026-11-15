package vip.mseapp.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 租户控制面数据的 Redis 共享缓存（租户×服务路由快照 + 租户行快照）。
 *
 * <p>使用方：service-platform（SysTenantRouteServiceImpl 路由解析、RemoteTenantServiceImpl
 * 租户查询）与 tenant-starter（StandaloneTenantApi 本租户行——standalone 默认仅一条自身租户缓存）。
 * 各使用方自带进程内 30s L1；本类为跨实例 L2，任一实例回填其余实例命中。
 *
 * <p>失效策略与 {@link PermissionCache} 同构：key 携带版本号（租户级/全局级），写侧 INCR 后
 * 旧 key 随 TTL 自然淘汰；条目值为带默认类型信息的 DTO（@class），读回类型不符按未命中处理
 * （脏条目删除后回源覆盖）。
 *
 * <p>可用性优先：Redis 读写任何异常一律降级回源（loader）不阻断租户路由/登录链路；
 * 版本失效（evict*）失败仅告警——写侧管理操作不因缓存故障失败，收敛兜底交给条目 TTL。
 */
public class TenantCache {

    private static final Logger log = LoggerFactory.getLogger(TenantCache.class);

    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    public TenantCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 读取租户×服务路由快照，未命中时加载并回填（双版本 key：全局.租户）。
     * 加载期间版本被并发递增则丢弃本次结果不回填（数据可能已过期，下次读走新版本 key）。
     */
    public <T> T getRoute(long tenantId, String serviceName, Class<T> type, Supplier<T> loader) {
        long globalVer;
        long tenantVer;
        String key;
        try {
            globalVer = readVersion(MseTenantCacheKeys.globalVer());
            tenantVer = readVersion(MseTenantCacheKeys.tenantVer(tenantId));
            key = MseTenantCacheKeys.route(tenantId, serviceName, globalVer, tenantVer);
            T cached = readEntry(key, type);
            if (cached != null) {
                return cached;
            }
        } catch (Exception ex) {
            // 连版本号都读不出 = Redis 整体不可用：直接回源，不回填
            log.warn("[mse-tenant-cache] 路由缓存读取失败，降级回源 DB: tenant={} service={}: {}",
                    tenantId, serviceName, ex.getMessage());
            return loader.get();
        }
        T value = loader.get();
        if (value != null) {
            backfill(key, value, () -> globalVer == readVersion(MseTenantCacheKeys.globalVer())
                    && tenantVer == readVersion(MseTenantCacheKeys.tenantVer(tenantId)));
        }
        return value;
    }

    /** 读取租户行快照，未命中时加载并回填（仅租户级版本；loader 返回 null 不缓存，防探测性负缓存） */
    public <T> T getTenantInfo(long tenantId, Class<T> type, Supplier<T> loader) {
        long tenantVer;
        String key;
        try {
            tenantVer = readVersion(MseTenantCacheKeys.tenantVer(tenantId));
            key = MseTenantCacheKeys.tenantInfo(tenantId, tenantVer);
            T cached = readEntry(key, type);
            if (cached != null) {
                return cached;
            }
        } catch (Exception ex) {
            log.warn("[mse-tenant-cache] 租户缓存读取失败，降级回源 DB: tenant={}: {}", tenantId, ex.getMessage());
            return loader.get();
        }
        T value = loader.get();
        if (value != null) {
            backfill(key, value, () -> tenantVer == readVersion(MseTenantCacheKeys.tenantVer(tenantId)));
        }
        return value;
    }

    /** 该租户的路由配置/租户行变更后调用（INCR 租户级版本，跨实例 L1 过期后按新版本 key 回源） */
    public void evictTenant(long tenantId) {
        try {
            redisTemplate.opsForValue().increment(MseTenantCacheKeys.tenantVer(tenantId));
        } catch (Exception ex) {
            log.warn("[mse-tenant-cache] 租户版本失效失败（由条目 TTL 兜底收敛）: tenant={}: {}",
                    tenantId, ex.getMessage());
        }
    }

    /**
     * 读租户级版本号（与 {@link #evictTenant} 同一版本源）：数据面快照节流校验用——控制面失效
     * INCR 后，数据面比对本地快照携带的版本即可判定是否需要重建。Redis 异常返回 null
     * （调用方跳过本次校验，交由快照 TTL 兜底收敛）。
     */
    public Long readTenantVersion(long tenantId) {
        try {
            return readVersion(MseTenantCacheKeys.tenantVer(tenantId));
        } catch (Exception ex) {
            log.debug("[mse-tenant-cache] 租户版本号读取失败（跳过本次校验）: tenant={}: {}",
                    tenantId, ex.getMessage());
            return null;
        }
    }

    /** 租户数据源 CRUD 后调用（INCR 全局版本，所有租户的路由条目随之失效） */
    public void evictGlobal() {
        try {
            redisTemplate.opsForValue().increment(MseTenantCacheKeys.globalVer());
        } catch (Exception ex) {
            log.warn("[mse-tenant-cache] 全局版本失效失败（由条目 TTL 兜底收敛）: {}", ex.getMessage());
        }
    }

    /**
     * 读条目：读取异常（反序列化失败等条目级问题）与类型不符（历史脏数据/格式演进）均按未命中
     * 处理——顺手删除脏 key，让本次回源结果直接覆盖。
     */
    private <T> T readEntry(String key, Class<T> type) {
        Object cached;
        try {
            cached = redisTemplate.opsForValue().get(key);
        } catch (Exception ex) {
            deleteQuietly(key);
            log.warn("[mse-tenant-cache] 缓存条目读取异常（按未命中处理）: key={}: {}", key, ex.getMessage());
            return null;
        }
        if (cached == null) {
            return null;
        }
        if (!type.isInstance(cached)) {
            deleteQuietly(key);
            log.warn("[mse-tenant-cache] 缓存条目类型不符（已删除待回填）: key={} 期望={} 实际={}",
                    key, type.getName(), cached.getClass().getName());
            return null;
        }
        return type.cast(cached);
    }

    /** 回填：版本未变才写（加载期间发生失效则丢弃，下次读走新版本 key）；失败仅告警 */
    private void backfill(String key, Object value, Supplier<Boolean> versionUnchanged) {
        try {
            if (Boolean.TRUE.equals(versionUnchanged.get())) {
                redisTemplate.opsForValue().set(key, value, TTL);
            }
        } catch (Exception ex) {
            log.warn("[mse-tenant-cache] 缓存回填失败（忽略）: key={}: {}", key, ex.getMessage());
        }
    }

    private void deleteQuietly(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
            // Redis 整体不可用时删除同样失败，忽略
        }
    }

    private long readVersion(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value.toString());
    }
}
