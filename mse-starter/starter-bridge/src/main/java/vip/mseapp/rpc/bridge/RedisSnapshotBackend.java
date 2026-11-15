package vip.mseapp.rpc.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * 快照后端·Redis 实现（在线部署缺省）：key {@code mse:snapshot:t:{tenantId}}，值为
 * {@code payload\nsignature\nversion}，TTL 10min 兜底（版本失效事件之外的最终收敛上限）。
 * 多实例共享同一份快照（收敛优于逐库副本）；Redis 异常降级返回 null/跳过写入——
 * 回源 RPC 通道不受影响（TenantCache 同款容错）。
 */
public class RedisSnapshotBackend implements SnapshotBackend {

    private static final Logger log = LoggerFactory.getLogger(RedisSnapshotBackend.class);

    private static final String KEY_PREFIX = "mse:snapshot:t:";

    private static final Duration TTL = Duration.ofMinutes(10);

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    public RedisSnapshotBackend(ObjectProvider<StringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
    }

    @Override
    public String read(Long tenantId) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return null;
        }
        try {
            return redis.opsForValue().get(KEY_PREFIX + tenantId);
        } catch (Exception e) {
            log.warn("[mse-snapshot] Redis 快照读取不可用（降级回源）: tenant={} {}", tenantId, e.getMessage());
            return null;
        }
    }

    @Override
    public void write(Long tenantId, String payload, String signature, long version) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(KEY_PREFIX + tenantId,
                    payload + "\n" + signature + "\n" + version, TTL);
        } catch (Exception e) {
            log.warn("[mse-snapshot] Redis 快照写入失败（内存缓存仍生效）: tenant={} {}", tenantId, e.getMessage());
        }
    }
}
