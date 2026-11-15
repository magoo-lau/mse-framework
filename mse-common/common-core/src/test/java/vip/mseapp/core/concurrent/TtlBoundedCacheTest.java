package vip.mseapp.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * TTL + 容量上限缓存：惰性过期、容量驱逐（最旧写入）、按键批量移除、永不过期模式。
 */
class TtlBoundedCacheTest {

    @Test
    void entriesExpireLazily() throws InterruptedException {
        try (TtlBoundedCache<String, String> cache = new TtlBoundedCache<>(80, 0, "test-ttl")) {
            cache.put("k", "v");
            assertEquals("v", cache.get("k"));
            Thread.sleep(120);
            assertNull(cache.get("k"));
        }
    }

    @Test
    void zeroTtlNeverExpires() {
        TtlBoundedCache<String, String> cache = new TtlBoundedCache<>(0, 0, "test-nottl");
        cache.put("k", "v");
        assertEquals("v", cache.get("k"));
        cache.close();
    }

    @Test
    void overCapacityEvictsOldestWrite() {
        TtlBoundedCache<String, Integer> cache = new TtlBoundedCache<>(0, 2, "test-cap");
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        assertNull(cache.get("a"));
        assertEquals(Integer.valueOf(2), cache.get("b"));
        assertEquals(Integer.valueOf(3), cache.get("c"));
        cache.close();
    }

    @Test
    void rewriteRefreshesEvictionPosition() {
        TtlBoundedCache<String, Integer> cache = new TtlBoundedCache<>(0, 2, "test-lru");
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("a", 1); // 重写刷新位置：a 变最新，b 成最旧
        cache.put("c", 3);
        assertNull(cache.get("b"));
        assertEquals(Integer.valueOf(1), cache.get("a"));
        cache.close();
    }

    @Test
    void removeIfByKeyPredicate() {
        TtlBoundedCache<Long, String> cache = new TtlBoundedCache<>(0, 0, "test-rmif");
        cache.put(1L, "a");
        cache.put(2L, "b");
        cache.put(3L, "c");
        assertEquals(2, cache.removeIf(key -> key == 1L || key == 3L));
        assertNull(cache.get(1L));
        assertEquals("b", cache.get(2L));
        cache.close();
    }
}
