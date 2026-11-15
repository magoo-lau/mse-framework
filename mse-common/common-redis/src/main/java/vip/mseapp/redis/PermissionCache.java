package vip.mseapp.redis;

import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 用户权限/角色集合的 Redis 缓存（Sa-Token 鉴权数据源加速）。
 *
 * <p>基座 service-system（本地实现）与 starter-bridge（RPC 消费端）共用同一缓存：
 * key 携带「全局版本.租户版本」，任一侧写入后另一侧同样命中，版本失效对两端同时生效。
 *
 * <p>失效策略：版本号 + TTL 双保险。定义类变更（菜单/权限点/套餐）自增全局版本，
 * 授权类变更（角色授权/用户角色）自增租户版本，旧版本 key 随 TTL（30 分钟）自然淘汰，
 * 避免 KEYS 模式扫描。
 */
public class PermissionCache {

    /** 权限码集合 */
    public static final String KIND_PERMISSIONS = "codes";

    /** 角色 code 集合 */
    public static final String KIND_ROLES = "roles";

    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    public PermissionCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 读取缓存的字符串集合，未命中时加载并回填。
     * 加载期间版本被并发递增则丢弃本次结果不回填（数据可能已过期，下次读走新版本 key）。
     */
    @SuppressWarnings("unchecked")
    public Set<String> getOrLoad(long tenantId, long userId, String kind, Supplier<Set<String>> loader) {
        long globalVer = readVersion(MsePermCacheKeys.globalVer());
        long tenantVer = readVersion(MsePermCacheKeys.tenantVer(tenantId));
        String key = MsePermCacheKeys.userPerm(tenantId, userId, kind, globalVer, tenantVer);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof Collection<?> collection) {
            return new HashSet<>((Collection<String>) collection);
        }
        Set<String> value = loader.get();
        if (value == null) {
            value = Set.of();
        }
        if (globalVer == readVersion(MsePermCacheKeys.globalVer())
                && tenantVer == readVersion(MsePermCacheKeys.tenantVer(tenantId))) {
            // 必须存 HashSet：加载器可能返回 Set.of 等 ImmutableCollections（final 类），
            // 默认类型序列化（NON_FINAL）对 final 类型不写 @class，读回时无法还原导致反序列化失败
            redisTemplate.opsForValue().set(key, new HashSet<>(value), TTL);
        }
        return value;
    }

    /** 租户内授权关系变更（角色授权/用户角色调整）后调用 */
    public void evictTenant(long tenantId) {
        redisTemplate.opsForValue().increment(MsePermCacheKeys.tenantVer(tenantId));
    }

    /** 全局定义变更（菜单/权限点/套餐定义）后调用 */
    public void evictGlobal() {
        redisTemplate.opsForValue().increment(MsePermCacheKeys.globalVer());
    }

    private long readVersion(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value.toString());
    }
}
