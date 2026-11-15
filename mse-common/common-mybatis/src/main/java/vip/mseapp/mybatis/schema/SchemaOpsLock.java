package vip.mseapp.mybatis.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * 库结构操作统一分布式锁：启动自动迁移、每租户数据迁移、租户库懒初始化、管理端手动初始化/
 * 结构同步/临时 SQL 等全部结构入口按<b>目标库</b>（JDBC URL 摘要）互斥——同一物理库上任何两个
 * 结构操作跨实例串行，避免各入口各持一把锁形同互不设防（如懒初始化与手动初始化并发跑脚本）。
 *
 * <p><b>持有者语义</b>：随机 UUID token 标识持有者，释放走 Lua「比对 token 再删」原子操作——
 * 杜绝固定值锁（值恒 "1"，任何人都像持有者）与直接 delete（误删已过 TTL、被他实例接手的锁）。
 *
 * <p><b>降级</b>：无 Redis / Redis 异常时视为获得（返回 token，释放空操作）——进程内串行由
 * 调用方自持的 synchronized 兜底；多实例部署必须配 Redis。
 */
public final class SchemaOpsLock {

    private static final Logger log = LoggerFactory.getLogger(SchemaOpsLock.class);

    /** 统一锁 key 前缀（按目标库 JDBC URL 摘要互斥；沿用既有格式，滚动升级期新旧代码共用同 key） */
    public static final String LOCK_PREFIX = "mse:lock:schema-migrate:";

    /** 释放脚本：持有者 token 一致才删（原子比对删除，非持有者不误删） */
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private SchemaOpsLock() {
    }

    /** 锁 key：按目标库 JDBC URL 摘要（共享同库的多个服务/实例互斥） */
    public static String lockKeyFor(String jdbcUrl) {
        return LOCK_PREFIX + sha256(jdbcUrl);
    }

    /** 锁 key：按目标库连接元数据（URL）摘要 */
    public static String lockKeyFor(Connection conn) throws SQLException {
        return lockKeyFor(conn.getMetaData().getURL());
    }

    /**
     * 单次尝试获取（tryEnter 语义）：返回持有者 token（无 Redis 降级时同样返回 token，进程内
     * 兜底由调用方保证）；{@code null} = 其他实例持有中，调用方应跳过本轮（下次自愈）。
     */
    public static String tryAcquire(StringRedisTemplate template, String lockKey, Duration ttl) {
        String token = UUID.randomUUID().toString();
        if (template == null) {
            return token;
        }
        try {
            Boolean ok = template.opsForValue().setIfAbsent(lockKey, token, ttl);
            return Boolean.TRUE.equals(ok) ? token : null;
        } catch (Exception e) {
            log.debug("[mse-schema] Redis 锁不可用（进程内串行兜底）: {}", e.getMessage());
            return token;
        }
    }

    /**
     * 阻塞获取（轮询等待，启动通道 fail-fast 用）：其他实例持有时每 {@code pollIntervalMs}
     * 轮询，超 {@code waitTimeout} 抛 {@link IllegalStateException}；无 Redis / Redis 异常时
     * 直接视为获得（进程内兜底由调用方保证）。
     */
    public static String acquire(StringRedisTemplate template, String lockKey, Duration ttl,
                                 long pollIntervalMs, Duration waitTimeout) throws InterruptedException {
        String token = UUID.randomUUID().toString();
        if (template == null) {
            return token;
        }
        long deadline = System.currentTimeMillis() + waitTimeout.toMillis();
        while (true) {
            try {
                Boolean ok = template.opsForValue().setIfAbsent(lockKey, token, ttl);
                if (Boolean.TRUE.equals(ok)) {
                    return token;
                }
            } catch (Exception e) {
                log.warn("[mse-schema] Redis 锁不可用（进程内串行兜底）: {}", e.getMessage());
                return token;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new IllegalStateException("等待其他实例完成库结构操作超时（"
                        + waitTimeout.toSeconds() + "s）: " + lockKey);
            }
            Thread.sleep(pollIntervalMs);
        }
    }

    /** 释放：Lua 比对持有者 token 再删（原子）；非持有者 / 无 Redis / 异常静默（TTL 到期自动释放） */
    public static void release(StringRedisTemplate template, String lockKey, String token) {
        if (template == null || token == null) {
            return;
        }
        try {
            template.execute(RELEASE_SCRIPT, List.of(lockKey), token);
        } catch (Exception e) {
            log.debug("[mse-schema] Redis 锁释放失败（TTL 到期自动释放）: {}", e.getMessage());
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
