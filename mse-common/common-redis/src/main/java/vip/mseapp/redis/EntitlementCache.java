package vip.mseapp.redis;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 租户权益快照的 Redis 缓存（0.1.0 权益模型）：值类型由调用方给定
 * （service-system 传 EntitlementDTO），key 携带「全局版本.租户版本」。
 *
 * <p>失效策略与 {@link PermissionCache} 同款：版本号 + TTL（30 分钟）双保险——
 * 定义类变更（权益项/套餐项/附加项定义）INCR 全局版本，租户类变更（订购/覆盖/换绑套餐）
 * INCR 租户版本；旧版本 key 随 TTL 自然淘汰。无 Redis（装配缺席）时直连加载器。
 */
public class EntitlementCache {

    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    public EntitlementCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 读取缓存的权益快照，未命中时加载并回填；加载期间版本被并发递增则丢弃本次结果不回填 */
    public <T> T getOrLoad(long tenantId, Class<T> type, Supplier<T> loader) {
        long globalVer = readVersion(MseEntCacheKeys.globalVer());
        long tenantVer = readVersion(MseEntCacheKeys.tenantVer(tenantId));
        String key = MseEntCacheKeys.entitlement(tenantId, globalVer, tenantVer);
        Object cached = redisTemplate.opsForValue().get(key);
        if (type.isInstance(cached)) {
            return type.cast(cached);
        }
        T value = loader.get();
        if (value != null
                && globalVer == readVersion(MseEntCacheKeys.globalVer())
                && tenantVer == readVersion(MseEntCacheKeys.tenantVer(tenantId))) {
            redisTemplate.opsForValue().set(key, value, TTL);
        }
        return value;
    }

    /** 租户级权益变更（订购附加项/覆盖/换绑套餐）后调用 */
    public void evictTenant(long tenantId) {
        redisTemplate.opsForValue().increment(MseEntCacheKeys.tenantVer(tenantId));
    }

    /** 全局定义变更（权益项/套餐项/附加项定义）后调用 */
    public void evictGlobal() {
        redisTemplate.opsForValue().increment(MseEntCacheKeys.globalVer());
    }

    private long readVersion(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value.toString());
    }
}
