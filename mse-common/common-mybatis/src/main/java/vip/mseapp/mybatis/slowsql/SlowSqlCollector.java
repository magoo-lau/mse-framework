package vip.mseapp.mybatis.slowsql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 慢 SQL 收集器：内存环形缓存 + 累计计数，仅记录超阈值语句。
 *
 * <p>纯内存有界结构，无落库、无网络调用；SQL 只记静态文本（? 占位参数值不捕获）。
 * 装配开关在 starter-mybatis（mse.mybatis.slow-sql.enabled，默认 false）：未开启时
 * 本类与拦截器都不会注册，零开销；开启后每条语句仅一次 nanoTime 计时，超阈值才进入本类。
 */
public class SlowSqlCollector {

    private static final Logger log = LoggerFactory.getLogger(SlowSqlCollector.class);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 端点返回的 SQL 文本上限（字符），避免超长 SQL 占满缓存 */
    private static final int MAX_SQL_LENGTH = 1000;

    private final boolean enabled;
    private final long thresholdMs;
    private final int capacity;
    private final Deque<SlowSqlRecord> recent = new ArrayDeque<>();
    private long totalSlow;

    public SlowSqlCollector(boolean enabled, long thresholdMs, int capacity) {
        this.enabled = enabled;
        this.thresholdMs = thresholdMs;
        this.capacity = Math.max(1, capacity);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getThresholdMs() {
        return thresholdMs;
    }

    /** 是否达到记录门槛（未启用直接 false，拦截器据此短路） */
    public boolean exceeds(long elapsedMs) {
        return enabled && elapsedMs >= thresholdMs;
    }

    public void record(String mapperId, String sql, long elapsedMs) {
        SlowSqlRecord item = new SlowSqlRecord(LocalDateTime.now().format(TIME_FORMAT), elapsedMs,
                mapperId, abbreviate(sql));
        synchronized (recent) {
            while (recent.size() >= capacity) {
                recent.removeFirst();
            }
            recent.addLast(item);
            totalSlow++;
        }
        log.warn("慢SQL {}ms [{}] {}", elapsedMs, mapperId, item.sql());
    }

    /** 端点快照：开关、阈值、累计慢 SQL 数与最近记录（拷贝，避免并发遍历） */
    public Snapshot snapshot() {
        synchronized (recent) {
            return new Snapshot(enabled, thresholdMs, totalSlow, new ArrayList<>(recent));
        }
    }

    public record Snapshot(boolean enabled, long thresholdMs, long totalSlow, List<SlowSqlRecord> recent) {
    }

    private static String abbreviate(String sql) {
        if (sql == null) {
            return "";
        }
        String normalized = sql.replaceAll("\\s+", " ").trim();
        return normalized.length() <= MAX_SQL_LENGTH ? normalized : normalized.substring(0, MAX_SQL_LENGTH) + "...";
    }
}
