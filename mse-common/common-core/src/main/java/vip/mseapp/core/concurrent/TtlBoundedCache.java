package vip.mseapp.core.concurrent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 带 TTL 与容量上限的进程内缓存：过期条目惰性淘汰（读时判定）+ 后台守护线程周期清扫
 * （只写不读的键不再永久驻留）+ 容量驱逐（超上限按最旧写入淘汰；写入即刷新位置，近似 LRU）。
 * 解决裸 ConcurrentHashMap「过期不访问的键永久残留」「无容量上限随租户数无界增长」两类问题。
 *
 * <p>线程安全：读写锁护 LinkedHashMap（典型用途为租户路由/快照缓存——键量为租户数，千级以下，
 * 读多写少锁竞争可忽略）。{@code ttlMillis <= 0} 表示永不过期（不启动清扫线程），
 * {@code maxEntries <= 0} 表示不限容量。实现 {@link AutoCloseable}，宿主 Bean 销毁时调
 * {@link #close()} 停止清扫线程（守护线程不阻断 JVM 退出，显式关闭仅为整洁）。
 */
public final class TtlBoundedCache<K, V> implements AutoCloseable {

    private final long ttlMillis;

    private final int maxEntries;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** 插入序 LinkedHashMap（写入前先 remove 保证「最近写入在尾」的驱逐语义） */
    private final LinkedHashMap<K, Entry<V>> map;

    private final ScheduledExecutorService sweeper;

    private record Entry<V>(V value, long expireAt) {

        boolean expired(long now) {
            return expireAt > 0 && now >= expireAt;
        }
    }

    public TtlBoundedCache(long ttlMillis, int maxEntries, String sweeperName) {
        this.ttlMillis = ttlMillis;
        this.maxEntries = maxEntries;
        this.map = new LinkedHashMap<>(16, 0.75f, false);
        if (ttlMillis > 0) {
            long period = Math.min(Math.max(ttlMillis / 2, 5_000), 60_000);
            this.sweeper = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, sweeperName + "-ttl-sweeper");
                thread.setDaemon(true);
                return thread;
            });
            this.sweeper.scheduleWithFixedDelay(this::sweep, period, period, TimeUnit.MILLISECONDS);
        } else {
            this.sweeper = null;
        }
    }

    /** 读取（惰性过期：命中已过期条目视未命中并顺手清除）；无此键或已过期返回 null */
    public V get(K key) {
        long now = System.currentTimeMillis();
        lock.readLock().lock();
        Entry<V> entry;
        try {
            entry = map.get(key);
        } finally {
            lock.readLock().unlock();
        }
        if (entry == null) {
            return null;
        }
        if (!entry.expired(now)) {
            return entry.value();
        }
        lock.writeLock().lock();
        try {
            map.remove(key, entry);
        } finally {
            lock.writeLock().unlock();
        }
        return null;
    }

    /** 写入（返回旧值）；超容量时淘汰最旧写入条目 */
    public V put(K key, V value) {
        long expireAt = ttlMillis > 0 ? System.currentTimeMillis() + ttlMillis : 0L;
        lock.writeLock().lock();
        try {
            V old = removePositional(key);
            map.put(key, new Entry<>(value, expireAt));
            if (maxEntries > 0 && map.size() > maxEntries) {
                Iterator<K> it = map.keySet().iterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
            return old;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** 移除并返回旧值 */
    public V remove(K key) {
        lock.writeLock().lock();
        try {
            return removePositional(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** 按键条件批量移除（租户维度失效用），返回移除条数 */
    public int removeIf(java.util.function.Predicate<K> keyFilter) {
        lock.writeLock().lock();
        try {
            int[] removed = {0};
            map.keySet().removeIf(key -> {
                if (keyFilter.test(key)) {
                    removed[0]++;
                    return true;
                }
                return false;
            });
            return removed[0];
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            map.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** 当前条目数（含未清扫的已过期条目，观测用） */
    public int size() {
        lock.readLock().lock();
        try {
            return map.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /** 周期清扫已过期条目（只写不读的键由本通道回收） */
    private void sweep() {
        long now = System.currentTimeMillis();
        lock.writeLock().lock();
        try {
            map.values().removeIf(entry -> entry.expired(now));
        } finally {
            lock.writeLock().unlock();
        }
    }

    private V removePositional(K key) {
        Entry<V> old = map.remove(key);
        return old == null ? null : old.value();
    }

    @Override
    public void close() {
        if (sweeper != null) {
            sweeper.shutdownNow();
        }
    }
}
