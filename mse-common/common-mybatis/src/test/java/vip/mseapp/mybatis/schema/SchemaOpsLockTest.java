package vip.mseapp.mybatis.schema;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 统一结构操作锁：无 Redis 降级（返回 token 视为获得，释放空操作——进程内串行由调用方兜底）
 * 与锁 key 生成（同 URL 同 key、异 URL 异 key）。Redis 路径（token 写入 + Lua 比对删除）依赖
 * 真实 Redis，经部署环境回归，不在此单测。
 */
class SchemaOpsLockTest {

    @Test
    void noRedisDegradesToAcquired() throws Exception {
        String key = SchemaOpsLock.lockKeyFor("jdbc:mysql://localhost:3306/mse");
        String token = SchemaOpsLock.tryAcquire(null, key, Duration.ofSeconds(1));
        assertNotNull(token);
        assertDoesNotThrow(() -> SchemaOpsLock.release(null, key, token));
        assertNotNull(SchemaOpsLock.acquire(null, key, Duration.ofSeconds(1), 10, Duration.ofMillis(100)));
    }

    @Test
    void lockKeyStablePerUrlAndDistinctAcrossUrls() {
        assertEquals(SchemaOpsLock.lockKeyFor("jdbc:mysql://a:3306/db"), SchemaOpsLock.lockKeyFor("jdbc:mysql://a:3306/db"));
        assertNotEquals(SchemaOpsLock.lockKeyFor("jdbc:mysql://a:3306/db"), SchemaOpsLock.lockKeyFor("jdbc:mysql://b:3306/db"));
        assertEquals("mse:lock:schema-migrate:", SchemaOpsLock.lockKeyFor("x").substring(0, SchemaOpsLock.LOCK_PREFIX.length()));
    }
}
