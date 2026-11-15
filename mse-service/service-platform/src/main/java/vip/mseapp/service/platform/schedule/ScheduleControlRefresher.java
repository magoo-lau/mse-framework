package vip.mseapp.service.platform.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import vip.mseapp.core.schedule.ScheduleControlState;
import vip.mseapp.service.platform.entity.SysScheduleSetting;
import vip.mseapp.service.platform.entity.SysScheduleTask;
import vip.mseapp.service.platform.mapper.SysScheduleSettingMapper;
import vip.mseapp.service.platform.mapper.SysScheduleTaskMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 调度控制缓存重建器（平台侧，控制哈希的 DB 事实源镜像）——事件驱动、零周期 SQL：
 * <ul>
 *   <li><b>启动一次</b>：ApplicationReadyEvent（晚于全部 Runner——结构迁移已完成）后同步
 *       {@link #rebuild()} 一次，保证实例延迟启动窗口内哈希就绪（首轮 pause/resume 不必等首次写透）；</li>
 *   <li><b>管理操作后去抖重建</b>：管理面启停/编辑落库成功后经 {@link #scheduleRebuild()} 安排——
 *       静默 {@code mse.scheduling.control-rebuild-debounce}（缺省 60s）后执行一次，期间有新操作
 *       则重新计时（连续点 N 次开关只执行 1 次 SQL，对账修复写透失败/Redis 被清空/并发竞态）；</li>
 *   <li><b>生效即时性不靠重建</b>：管理面落库后另有单字段 HSET 写透（纯 Redis 写、零 SQL），
 *       快通道 5s 级收敛；重建只是合并后的自愈对账。Redis 被清空后快通道静默（缺失=无指令），
 *       sync 通道照常按 DB 权威收敛，去抖重建/下次操作/重启恢复。</li>
 * </ul>
 *
 * <p>RENAME 替换窗口内管理面并发写透可能被覆盖：数据同源于 DB、下次操作的去抖重建自愈，无需加锁。
 * Redis 缺席时告警一次后待机；去抖执行器懒创建（守护线程，无操作时零开销）。
 */
public class ScheduleControlRefresher implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ScheduleControlRefresher.class);

    private final SysScheduleTaskMapper taskMapper;

    /** 调度器设置表（闸门参数键重建用；表未就绪时可选缺席——gates 键跳过重建） */
    private final org.springframework.beans.factory.ObjectProvider<SysScheduleSettingMapper> settingMapperProvider;

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    private final long debounceMs;

    /** 去抖执行器（懒创建，99% 时间空闲）与待执行任务 */
    private volatile ScheduledExecutorService debouncer;

    private ScheduledFuture<?> pendingRebuild;

    public ScheduleControlRefresher(SysScheduleTaskMapper taskMapper,
                                    ObjectProvider<StringRedisTemplate> redisProvider,
                                    long debounceMs) {
        this(taskMapper, null, redisProvider, debounceMs);
    }

    public ScheduleControlRefresher(SysScheduleTaskMapper taskMapper,
                                    org.springframework.beans.factory.ObjectProvider<SysScheduleSettingMapper> settingMapperProvider,
                                    ObjectProvider<StringRedisTemplate> redisProvider,
                                    long debounceMs) {
        this.taskMapper = taskMapper;
        this.settingMapperProvider = settingMapperProvider;
        this.redisProvider = redisProvider;
        this.debounceMs = Math.max(1_000, debounceMs);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            rebuild();
            log.info("[mse-schedule] 控制缓存已按平台库重建（启动一次，此后仅管理操作后 {}s 去抖重建）",
                    debounceMs / 1000);
        } catch (Exception e) {
            log.warn("[mse-schedule] 控制缓存启动重建失败（表可能未就绪；首次管理操作的去抖重建自愈）: {}",
                    e.getMessage());
        }
    }

    /** 管理操作后安排去抖重建：静默 debounceMs 后执行，期间有新操作则重新计时 */
    public synchronized void scheduleRebuild() {
        if (debouncer == null) {
            debouncer = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "mse-schedule-control-refresher");
                thread.setDaemon(true);
                return thread;
            });
        }
        if (pendingRebuild != null) {
            pendingRebuild.cancel(false);
        }
        pendingRebuild = debouncer.schedule(() -> {
            try {
                rebuild();
                log.info("[mse-schedule] 控制缓存已按平台库去抖重建（对账完成）");
            } catch (Exception e) {
                log.warn("[mse-schedule] 控制缓存去抖重建失败（下次管理操作再自愈）: {}", e.getMessage());
            }
        }, debounceMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 至多补排一次去抖重建（已有待执行则不重排）：供 sync 通道周期调用——若走
     * {@link #scheduleRebuild()} 会被降级兜底的连续 sync 反复重计时导致永不执行；
     * 本方法保证 60s 内必重建一次，是快通道降级（Redis 被清空等）的收敛自愈路径。
     */
    public synchronized void ensureRebuildScheduled() {
        if (pendingRebuild == null || pendingRebuild.isDone()) {
            scheduleRebuild();
        }
    }

    /** 全量重建：DB 全表 → staging 哈希 → RENAME 原子替换（空表退化为删除，等价空哈希） */
    public void rebuild() {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            log.warn("[mse-schedule] Redis 缺席，控制哈希重建暂不可用（快通道降级，sync 通道兜底）");
            return;
        }
        var rows = taskMapper.selectList(new LambdaQueryWrapper<SysScheduleTask>()
                .select(SysScheduleTask::getTaskKey, SysScheduleTask::getEnabled,
                        SysScheduleTask::getExprOverride));
        Map<String, String> fields = new HashMap<>(rows.size() * 2);
        for (SysScheduleTask row : rows) {
            String override = row.getExprOverride();
            fields.put(row.getTaskKey(), ScheduleControlState.encode(new ScheduleControlState(
                    row.getTaskKey(), row.getEnabled() == null || row.getEnabled() == 1,
                    override == null || override.isBlank() ? null : override,
                    System.currentTimeMillis())));
        }
        if (fields.isEmpty()) {
            redis.delete(ScheduleControlState.CONTROL_HASH_KEY);
            return;
        }
        String staging = ScheduleControlState.CONTROL_HASH_KEY + ":staging";
        redis.delete(staging);
        redis.opsForHash().putAll(staging, fields);
        redis.rename(staging, ScheduleControlState.CONTROL_HASH_KEY);
        rebuildGates(redis);
    }

    /** 闸门参数键重建（gates.* 设置行 → encodeGates 写键；无行=删键回落构造默认；设置表未就绪跳过） */
    private void rebuildGates(StringRedisTemplate redis) {
        if (settingMapperProvider == null) {
            return;
        }
        SysScheduleSettingMapper settingMapper = settingMapperProvider.getIfAvailable();
        if (settingMapper == null) {
            return;
        }
        try {
            Map<String, Integer> limits = new LinkedHashMap<>();
            for (SysScheduleSetting row : settingMapper.selectList(new LambdaQueryWrapper<SysScheduleSetting>()
                    .likeRight(SysScheduleSetting::getSettingKey, "gates."))) {
                String key = row.getSettingKey().substring("gates.".length());
                try {
                    limits.put(key, Integer.parseInt(row.getSettingValue().trim()));
                } catch (NumberFormatException ignored) {
                    // 非法值跳过（下次管理操作修正）
                }
            }
            String encoded = ScheduleControlState.encodeGates(limits);
            if (encoded.isEmpty()) {
                redis.delete(ScheduleControlState.GATES_KEY);
            } else {
                redis.opsForValue().set(ScheduleControlState.GATES_KEY, encoded);
            }
        } catch (Exception e) {
            log.warn("[mse-schedule] 闸门参数键重建失败（下次管理操作自愈）: {}", e.getMessage());
        }
    }

    @Override
    public void destroy() {
        ScheduledExecutorService current = debouncer;
        if (current != null) {
            current.shutdownNow();
        }
    }
}
