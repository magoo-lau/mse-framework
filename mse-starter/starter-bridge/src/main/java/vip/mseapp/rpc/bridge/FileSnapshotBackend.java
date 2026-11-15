package vip.mseapp.rpc.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 快照后端·加密文件实现（离线/standalone 部署）：单文件多租户载体，行格式
 * {@code tenantId|payload|signature|version}——载荷本身已是 AES-GCM 密文 + HMAC 签名，
 * 文件即加密载体（无明文落盘）。首读整文件载入内存，mtime 变更热重载
 * （LicenseHolder 同款范式，交付包/导入链替换文件即生效）；写为全量重写（低频）。
 */
public class FileSnapshotBackend implements SnapshotBackend {

    private static final Logger log = LoggerFactory.getLogger(FileSnapshotBackend.class);

    private final Path file;

    private volatile Map<Long, String> cached = Map.of();

    private volatile long loadedMtime;

    private volatile boolean loadedOnce;

    public FileSnapshotBackend(String path) {
        this.file = Path.of(path);
    }

    @Override
    public String read(Long tenantId) {
        reloadIfChanged();
        return cached.get(tenantId);
    }

    @Override
    public void write(Long tenantId, String payload, String signature, long version) {
        reloadIfChanged();
        Map<Long, String> next = new ConcurrentHashMap<>(cached);
        next.put(tenantId, payload + "|" + signature + "|" + version);
        try {
            Files.createDirectories(file.getParent());
            StringBuilder content = new StringBuilder();
            next.forEach((tenant, line) -> content.append(tenant).append('|').append(line).append('\n'));
            Files.writeString(file, content.toString(), StandardCharsets.UTF_8);
            cached = next;
            loadedMtime = mtime();
            loadedOnce = true;
        } catch (IOException e) {
            log.warn("[mse-snapshot] 快照文件写入失败（内存缓存仍生效）: {} {}", file, e.getMessage());
        }
    }

    /** mtime 变更热重载；文件缺失按空集处理 */
    private void reloadIfChanged() {
        long mtime = mtime();
        if (loadedOnce && mtime == loadedMtime) {
            return;
        }
        synchronized (this) {
            mtime = mtime();
            if (loadedOnce && mtime == loadedMtime) {
                return;
            }
            Map<Long, String> loaded = new ConcurrentHashMap<>();
            if (Files.isRegularFile(file)) {
                try {
                    for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                        int split = line.indexOf('|');
                        if (split <= 0) {
                            continue;
                        }
                        try {
                            loaded.put(Long.parseLong(line.substring(0, split)), line.substring(split + 1));
                        } catch (NumberFormatException ignored) {
                            // 脏行跳过
                        }
                    }
                } catch (IOException e) {
                    log.warn("[mse-snapshot] 快照文件读取失败（沿用内存副本）: {} {}", file, e.getMessage());
                    return;
                }
            }
            cached = loaded;
            loadedMtime = mtime;
            loadedOnce = true;
        }
    }

    private long mtime() {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }
}
