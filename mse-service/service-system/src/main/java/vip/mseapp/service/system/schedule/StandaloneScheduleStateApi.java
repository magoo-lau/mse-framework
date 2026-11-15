package vip.mseapp.service.system.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.support.CronExpression;
import vip.mseapp.api.system.RemoteScheduleStateApi;
import vip.mseapp.api.system.dto.ScheduleSyncResultDTO;
import vip.mseapp.api.system.dto.ScheduleTaskAdminDTO;
import vip.mseapp.api.system.dto.ScheduleTaskLogDTO;
import vip.mseapp.api.system.dto.ScheduleTaskReportDTO;
import vip.mseapp.api.system.dto.ScheduleTaskStateDTO;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.schedule.ScheduleControlState;
import vip.mseapp.core.schedule.TaskDefinition;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 调度状态契约的 standalone 回落实现（{@code mse.standalone.enabled=true} 时装配）：
 * 离线私有部署单库自足——无平台库/无 service-platform，本实现以 JdbcTemplate 直写
 * 本库 sys_schedule_task（表由 StandaloneBootstrap 直建，照 sys_tenant 简版表先例，
 * 不依赖 service-platform 的实体/mapper）。语义与 service-platform 版一致（事件驱动上报、
 * 无周期心跳）：上报 upsert（管理面字段 enabled/expr_override/remark/trigger_pending 永不覆盖，
 * 全字段比对无变化跳过写）、应答启停/覆盖/触发抢占（触发落库后写踢键促实例即时 sync）；
 * 租户端「任务调度」页（菜单 195，仅 standalone 播种）经本实现读写。
 *
 * <p>启停/编辑落库成功后两段式维护 Redis 控制哈希 {@link ScheduleControlState#CONTROL_HASH_KEY}
 * （下行快通道）：①立即写透单字段（纯 Redis 写、零 SQL，5s 级收敛）；②静默 60s 去抖后全量重建
 * 对账（连续操作只执行一次 SQL，自愈写透失败/Redis 被清空/并发竞态）。重建失败由本进程实例
 * 监控线程的 sync 通道兜底（sync 尾部补排重建），Redis 恢复后自愈。
 */
public class StandaloneScheduleStateApi implements RemoteScheduleStateApi, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(StandaloneScheduleStateApi.class);

    private static final long MIN_OVERRIDE_DELAY_MS = 1000;

    /** 管理操作后去抖全量重建的静默窗口（对账合并：连续操作只跑一次 SQL） */
    private static final long REBUILD_DEBOUNCE_MS = 60_000;

    private final JdbcTemplate jdbcTemplate;

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    /** 去抖执行器（懒创建守护线程，无操作时零开销）与待执行任务 */
    private volatile ScheduledExecutorService debouncer;

    private ScheduledFuture<?> pendingRebuild;

    public StandaloneScheduleStateApi(JdbcTemplate jdbcTemplate,
                                      ObjectProvider<StringRedisTemplate> redisProvider) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisProvider = redisProvider;
    }

    /** 管理操作后安排去抖全量重建：静默 60s 后执行，期间有新操作则重新计时 */
    private synchronized void scheduleRebuild() {
        if (debouncer == null) {
            debouncer = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "mse-standalone-schedule-rebuild");
                thread.setDaemon(true);
                return thread;
            });
        }
        if (pendingRebuild != null) {
            pendingRebuild.cancel(false);
        }
        pendingRebuild = debouncer.schedule(() -> {
            try {
                rebuildControlHash();
            } catch (Exception e) {
                log.warn("[mse-schedule] 控制缓存去抖重建失败（下次管理操作再自愈）: {}", e.getMessage());
            }
        }, REBUILD_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    /** 至多补排一次去抖重建（已有待执行不重排）：供 sync 通道自愈用，避免周期 sync 反复重计时导致永不执行 */
    private synchronized void ensureRebuildScheduled() {
        if (pendingRebuild == null || pendingRebuild.isDone()) {
            scheduleRebuild();
        }
    }

    @Override
    public void destroy() {
        ScheduledExecutorService current = debouncer;
        if (current != null) {
            current.shutdownNow();
        }
    }

    @Override
    public ScheduleSyncResultDTO sync(ScheduleTaskReportDTO report) {
        for (ScheduleTaskStateDTO state : report.getTasks()) {
            upsert(report, state);
        }
        ScheduleSyncResultDTO result = new ScheduleSyncResultDTO();
        if (report.getTasks().isEmpty()) {
            return result;
        }
        String placeholders = String.join(",", report.getTasks().stream()
                .map(t -> "?").toList());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, task_key, enabled, trigger_pending, expr_override FROM sys_schedule_task"
                        + " WHERE task_key IN (" + placeholders + ")",
                report.getTasks().stream().map(ScheduleTaskStateDTO::getTaskId).toArray());
        Map<String, Boolean> enabledByTask = new HashMap<>();
        Map<String, String> overrideByTask = new HashMap<>();
        List<String> triggered = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String taskKey = String.valueOf(row.get("task_key"));
            enabledByTask.put(taskKey, !Integer.valueOf(0).equals(toInt(row.get("enabled"))));
            Object override = row.get("expr_override");
            overrideByTask.put(taskKey, override == null || override.toString().isBlank() ? null : override.toString());
            if (Integer.valueOf(1).equals(toInt(row.get("trigger_pending")))) {
                int claimed = jdbcTemplate.update(
                        "UPDATE sys_schedule_task SET trigger_pending = 0 WHERE id = ? AND trigger_pending = 1",
                        row.get("id"));
                if (claimed > 0) {
                    triggered.add(taskKey);
                }
            }
        }
        result.setEnabledByTask(enabledByTask);
        result.setExprOverrideByTask(overrideByTask);
        result.setTriggeredTaskIds(triggered);
        // 尾部对账：补排一次去抖重建（不重排已待执行的）——降级期间 60s 内补回控制哈希自愈
        ensureRebuildScheduled();
        return result;
    }

    /**
     * upsert：定义 + 运行态刷新，管理面字段（enabled/expr_override/remark/trigger_pending）不动；
     * 全字段比对无变化跳过写（同步本身不产生写流量）。新任务首报落行后即时写透控制哈希，
     * 新装环境无需管理操作/重启，快通道即刻可用。
     */
    private void upsert(ScheduleTaskReportDTO report, ScheduleTaskStateDTO state) {
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT * FROM sys_schedule_task WHERE task_key = ?", state.getTaskId());
        if (!existing.isEmpty() && reportUnchanged(existing.get(0), report, state)) {
            return;
        }
        String sql = existing.isEmpty()
                ? """
                INSERT INTO sys_schedule_task (task_key, owner_service, scheduler_mode, task_name, description,
                    task_type, task_category, schedule_expr, last_run_time, last_duration_ms, last_result, last_error,
                    run_count, error_count, report_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)"""
                : """
                UPDATE sys_schedule_task SET owner_service = ?, scheduler_mode = ?, task_name = ?, description = ?,
                    task_type = ?, task_category = ?, schedule_expr = ?, last_run_time = ?, last_duration_ms = ?,
                    last_result = ?, last_error = ?, run_count = ?, error_count = ?, report_time = CURRENT_TIMESTAMP
                WHERE task_key = ?""";
        Object[] args = existing.isEmpty()
                ? new Object[]{state.getTaskId(), report.getServiceName(), report.getSchedulerMode(),
                        state.getName(), state.getDescription(), state.getType(), normalizedCategory(state),
                        state.getExpression(),
                        toTimestamp(state.getLastRunStartMs()), state.getLastDurationMs(), state.getLastResult(),
                        truncate(state.getLastError()), state.getRunCount(), state.getErrorCount()}
                : new Object[]{report.getServiceName(), report.getSchedulerMode(),
                        state.getName(), state.getDescription(), state.getType(), normalizedCategory(state),
                        state.getExpression(),
                        toTimestamp(state.getLastRunStartMs()), state.getLastDurationMs(), state.getLastResult(),
                        truncate(state.getLastError()), state.getRunCount(), state.getErrorCount(),
                        state.getTaskId()};
        jdbcTemplate.update(sql, args);
        if (existing.isEmpty()) {
            pushControl(state.getTaskId(), true, null);
            scheduleRebuild();
        }
    }

    /** 上报快照与库中行全字段比对：定义与运行态一致 = 无需写（report_time 不单独刷，判活已下线） */
    private static boolean reportUnchanged(Map<String, Object> row, ScheduleTaskReportDTO report,
                                           ScheduleTaskStateDTO state) {
        return Objects.equals(text(row.get("owner_service")), report.getServiceName())
                && Objects.equals(text(row.get("scheduler_mode")), report.getSchedulerMode())
                && Objects.equals(text(row.get("task_name")), state.getName())
                && Objects.equals(text(row.get("description")), state.getDescription())
                && Objects.equals(text(row.get("task_type")), state.getType())
                && Objects.equals(text(row.get("task_category")), normalizedCategory(state))
                && Objects.equals(text(row.get("schedule_expr")), state.getExpression())
                && Objects.equals(row.get("last_run_time"), toTimestamp(state.getLastRunStartMs()))
                && Objects.equals(toLong(row.get("last_duration_ms")), state.getLastDurationMs())
                && Objects.equals(text(row.get("last_result")), state.getLastResult())
                && Objects.equals(text(row.get("last_error")), truncate(state.getLastError()))
                && Objects.equals(toLong(row.get("run_count")), state.getRunCount())
                && Objects.equals(toLong(row.get("error_count")), state.getErrorCount());
    }

    @Override
    public PageResult<ScheduleTaskAdminDTO> listTasks(long pageNum, long pageSize, String keyword,
                                                      String serviceName, String category) {
        StringBuilder where = new StringBuilder(" WHERE deleted = 0");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (task_key LIKE ? OR task_name LIKE ? OR description LIKE ? OR remark LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (serviceName != null && !serviceName.isBlank()) {
            where.append(" AND owner_service = ?");
            args.add(serviceName.trim());
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND task_category = ?");
            args.add(category.trim());
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_schedule_task" + where, Long.class, args.toArray());
        long size = Math.max(1, pageSize);
        String pageSql = "SELECT * FROM sys_schedule_task" + where
                + " ORDER BY task_key LIMIT " + size + " OFFSET " + (Math.max(1, pageNum) - 1) * size;
        List<ScheduleTaskAdminDTO> records = jdbcTemplate.queryForList(pageSql, args.toArray())
                .stream().map(StandaloneScheduleStateApi::toAdminDto).toList();
        return PageResult.of(total == null ? 0 : total, pageNum, size, records);
    }

    @Override
    public boolean toggle(String taskKey, boolean enabled) {
        boolean updated = jdbcTemplate.update("UPDATE sys_schedule_task SET enabled = ? WHERE task_key = ?",
                enabled ? 1 : 0, taskKey) > 0;
        if (updated) {
            String override = jdbcTemplate.queryForObject(
                    "SELECT expr_override FROM sys_schedule_task WHERE task_key = ?",
                    String.class, taskKey);
            pushControl(taskKey, enabled, override == null || override.isBlank() ? null : override);
            scheduleRebuild();
        }
        return updated;
    }

    @Override
    public boolean trigger(String taskKey) {
        boolean updated = jdbcTemplate.update("UPDATE sys_schedule_task SET trigger_pending = 1 WHERE task_key = ?",
                taskKey) > 0;
        if (updated) {
            kickSync();
        }
        return updated;
    }

    /**
     * 写触发踢键（值=毫秒时间戳）：实例控制节拍读到新值即发起全量 sync 抢占 trigger_pending
     * （与 service-platform 版一致；无 TTL——重启首拍读到旧值也能接管遗留 pending）。
     */
    private void kickSync() {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().set(ScheduleControlState.KICK_KEY, String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            log.warn("[mse-schedule] 触发踢键写入失败（降级兜底节拍照常按 DB 收敛）: {}", e.getMessage());
        }
    }

    @Override
    public int appendLogs(List<ScheduleTaskLogDTO> logs) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        java.util.Set<String> touched = new java.util.LinkedHashSet<>();
        int appended = 0;
        for (ScheduleTaskLogDTO dto : logs) {
            if (dto.getId() == null || dto.getId().isBlank() || dto.getTaskKey() == null) {
                continue;
            }
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                    "SELECT id FROM sys_schedule_task_log WHERE id = ?", dto.getId());
            if (!existing.isEmpty()) {
                continue; // 重启重报：幂等跳过
            }
            jdbcTemplate.update("""
                    INSERT INTO sys_schedule_task_log (id, task_key, owner_service, start_time, duration_ms, result, error_msg)
                    VALUES (?, ?, ?, ?, ?, ?, ?)""",
                    dto.getId(), dto.getTaskKey(), dto.getServiceName(),
                    dto.getStartTime() == null ? java.time.LocalDateTime.now() : dto.getStartTime(),
                    dto.getDurationMs() == null ? 0 : dto.getDurationMs(),
                    "FAILED".equalsIgnoreCase(dto.getResult()) ? "FAILED" : "SUCCESS",
                    truncate(dto.getError()));
            appended++;
            touched.add(dto.getTaskKey());
        }
        for (String taskKey : touched) {
            jdbcTemplate.update("""
                    DELETE FROM sys_schedule_task_log WHERE task_key = ? AND start_time < (
                        SELECT t.start_time FROM (SELECT start_time FROM sys_schedule_task_log
                            WHERE task_key = ? ORDER BY start_time DESC LIMIT 1 OFFSET 200) t)""",
                    taskKey, taskKey);
        }
        return appended;
    }

    @Override
    public PageResult<ScheduleTaskLogDTO> listLogs(String taskKey, long pageNum, long pageSize, String result) {
        String resultFilter = "SUCCESS".equalsIgnoreCase(result) || "FAILED".equalsIgnoreCase(result)
                ? result.toUpperCase() : null;
        Object[] countArgs = resultFilter == null ? new Object[]{taskKey} : new Object[]{taskKey, resultFilter};
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_schedule_task_log WHERE task_key = ?"
                        + (resultFilter == null ? "" : " AND result = ?"), Long.class, countArgs);
        String pageSql = "SELECT * FROM sys_schedule_task_log WHERE task_key = ?"
                + (resultFilter == null ? "" : " AND result = ?")
                + " ORDER BY start_time DESC LIMIT " + Math.max(1, pageSize) + " OFFSET " + (Math.max(1, pageNum) - 1) * Math.max(1, pageSize);
        List<ScheduleTaskLogDTO> records = jdbcTemplate.queryForList(pageSql, countArgs)
                .stream().map(StandaloneScheduleStateApi::toLogDto).toList();
        return PageResult.of(total == null ? 0 : total, pageNum, pageSize, records);
    }

    // ==================== 调度器设置（本库 sys_schedule_setting 读写 + 闸门键写透） ====================

    @Override
    public Map<String, String> getScheduleSettings() {
        Map<String, String> out = new java.util.LinkedHashMap<>();
        try {
            for (Map<String, Object> row : jdbcTemplate.queryForList(
                    "SELECT setting_key, setting_value FROM sys_schedule_setting WHERE deleted = 0 ORDER BY setting_key")) {
                out.put(text(row.get("setting_key")), text(row.get("setting_value")));
            }
        } catch (Exception e) {
            log.debug("[mse-schedule] sys_schedule_setting 未就绪（首次建表前），设置读取为空: {}", e.getMessage());
        }
        return out;
    }

    @Override
    public boolean updateScheduleSettings(Map<String, Integer> gateLimits) {
        if (gateLimits == null || gateLimits.isEmpty()) {
            return false;
        }
        Map<String, Integer> normalized = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : gateLimits.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            Integer limit = entry.getValue();
            if (!key.matches("[\\w.-]+") || limit == null || limit < 1) {
                return false;
            }
            normalized.put(key, limit);
        }
        for (Map.Entry<String, Integer> entry : normalized.entrySet()) {
            String settingKey = "gates." + entry.getKey();
            String value = String.valueOf(entry.getValue());
            List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                    "SELECT id FROM sys_schedule_setting WHERE setting_key = ? AND deleted = 0", settingKey);
            if (existing.isEmpty()) {
                jdbcTemplate.update("""
                        INSERT INTO sys_schedule_setting (id, setting_key, setting_value, remark, create_time, update_time)
                        VALUES (?, ?, ?, '租户并发闸门上限（热更）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)""",
                        nextId(), settingKey, value);
            } else {
                jdbcTemplate.update(
                        "UPDATE sys_schedule_setting SET setting_value = ?, update_time = CURRENT_TIMESTAMP WHERE setting_key = ?",
                        value, settingKey);
            }
        }
        pushGatesFromDb();
        scheduleRebuild();
        return true;
    }

    /** 按本库 gates.* 行写透 Redis 闸门参数键（与控制哈希同通道；失败由去抖重建自愈） */
    private void pushGatesFromDb() {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            Map<String, Integer> limits = new java.util.LinkedHashMap<>();
            for (Map<String, Object> row : jdbcTemplate.queryForList(
                    "SELECT setting_key, setting_value FROM sys_schedule_setting WHERE deleted = 0")) {
                String key = text(row.get("setting_key"));
                if (key != null && key.startsWith("gates.")) {
                    try {
                        limits.put(key.substring(5), Integer.parseInt(text(row.get("setting_value")).trim()));
                    } catch (NumberFormatException ignored) {
                        // 非法行跳过
                    }
                }
            }
            String encoded = ScheduleControlState.encodeGates(limits);
            if (encoded.isEmpty()) {
                redis.delete(ScheduleControlState.GATES_KEY);
            } else {
                redis.opsForValue().set(ScheduleControlState.GATES_KEY, encoded);
            }
        } catch (Exception e) {
            log.warn("[mse-schedule] 闸门参数键写透失败（去抖重建自愈）: {}", e.getMessage());
        }
    }

    /** 设置行主键（standalone 单实例：时间戳 + 序号即够） */
    private static Long nextId() {
        return System.currentTimeMillis() * 1000 + (idSeq.incrementAndGet() & 0xFFFF);
    }

    private static final java.util.concurrent.atomic.AtomicInteger idSeq = new java.util.concurrent.atomic.AtomicInteger();

    private static ScheduleTaskLogDTO toLogDto(Map<String, Object> row) {
        ScheduleTaskLogDTO dto = new ScheduleTaskLogDTO();
        dto.setId(text(row.get("id")));
        dto.setTaskKey(text(row.get("task_key")));
        dto.setServiceName(text(row.get("owner_service")));
        Object startTime = row.get("start_time");
        dto.setStartTime(startTime == null ? null : Timestamp.valueOf(startTime.toString()).toLocalDateTime());
        dto.setDurationMs(toLong(row.get("duration_ms")));
        dto.setResult(text(row.get("result")));
        dto.setError(text(row.get("error_msg")));
        return dto;
    }

    @Override
    public boolean updateTask(String taskKey, String exprOverride, String remark) {
        String override = normalizeOverride(exprOverride);
        if (override == null) {
            return false;
        }
        List<Object> args = new ArrayList<>();
        args.add(override);
        String sql = "UPDATE sys_schedule_task SET expr_override = ?";
        if (remark != null) {
            sql += ", remark = ?";
            args.add(remark.length() > 1000 ? remark.substring(0, 1000) : remark);
        }
        sql += " WHERE task_key = ?";
        args.add(taskKey);
        boolean updated = jdbcTemplate.update(sql, args.toArray()) > 0;
        if (updated) {
            Integer enabled = jdbcTemplate.queryForObject(
                    "SELECT enabled FROM sys_schedule_task WHERE task_key = ?", Integer.class, taskKey);
            pushControl(taskKey, enabled == null || enabled != 0,
                    override.isBlank() ? null : override);
            scheduleRebuild();
        }
        return updated;
    }

    /** 管理面落库后写透控制哈希单字段（纯 Redis 写、零 SQL；失败仅告警，去抖重建对账自愈） */
    private void pushControl(String taskKey, boolean enabled, String exprOverride) {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForHash().put(ScheduleControlState.CONTROL_HASH_KEY, taskKey,
                    ScheduleControlState.encode(new ScheduleControlState(
                            taskKey, enabled, exprOverride, System.currentTimeMillis())));
        } catch (Exception e) {
            log.warn("[mse-schedule] 控制缓存写透失败（去抖重建自愈）: {}", e.getMessage());
        }
    }

    /** 去抖重建回调：全量重建 Redis 控制哈希（standalone 单实例表极小；失败仅告警，sync 通道兜底） */
    private void rebuildControlHash() {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT task_key, enabled, expr_override FROM sys_schedule_task WHERE deleted = 0");
            Map<String, String> fields = new HashMap<>(rows.size() * 2);
            for (Map<String, Object> row : rows) {
                String key = String.valueOf(row.get("task_key"));
                Object override = row.get("expr_override");
                fields.put(key, ScheduleControlState.encode(new ScheduleControlState(
                        key, !Integer.valueOf(0).equals(toInt(row.get("enabled"))),
                        override == null || override.toString().isBlank() ? null : override.toString(),
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
            // 闸门参数键随控制哈希同批重建（gates.* 设置行 → 键值；对账自愈）
            pushGatesFromDb();
        } catch (Exception e) {
            log.warn("[mse-schedule] 控制缓存重建失败（sync 通道兜底）: {}", e.getMessage());
        }
    }

    /** 覆盖表达式规范化：空 = ""（恢复代码默认）；纯数字 ≥1000 毫秒 或合法 cron；非法 = null 哨兵 */
    private String normalizeOverride(String exprOverride) {
        String trimmed = exprOverride == null ? "" : exprOverride.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            if (Long.parseLong(trimmed) < MIN_OVERRIDE_DELAY_MS) {
                log.warn("[mse-schedule] 覆盖间隔低于 {}ms 被拒绝: {}", MIN_OVERRIDE_DELAY_MS, trimmed);
                return null;
            }
            return trimmed;
        }
        try {
            CronExpression.parse(trimmed);
            return trimmed;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ScheduleTaskAdminDTO toAdminDto(Map<String, Object> row) {
        ScheduleTaskAdminDTO dto = new ScheduleTaskAdminDTO();
        dto.setId(toLong(row.get("id")));
        dto.setTaskKey(text(row.get("task_key")));
        dto.setOwnerService(text(row.get("owner_service")));
        dto.setSchedulerMode(text(row.get("scheduler_mode")));
        dto.setTaskName(text(row.get("task_name")));
        dto.setDescription(text(row.get("description")));
        dto.setTaskType(text(row.get("task_type")));
        dto.setTaskCategory(text(row.get("task_category")));
        dto.setScheduleExpr(text(row.get("schedule_expr")));
        dto.setEnabled(toInt(row.get("enabled")));
        Object override = row.get("expr_override");
        dto.setExprOverride(override == null || override.toString().isBlank() ? null : override.toString());
        dto.setRemark(text(row.get("remark")));
        Object lastRun = row.get("last_run_time");
        dto.setLastRunTime(lastRun == null ? null : Timestamp.valueOf(lastRun.toString()).toLocalDateTime());
        dto.setLastDurationMs(toLong(row.get("last_duration_ms")));
        dto.setLastResult(text(row.get("last_result")));
        dto.setLastError(text(row.get("last_error")));
        dto.setRunCount(toLong(row.get("run_count")));
        dto.setErrorCount(toLong(row.get("error_count")));
        Object reportTime = row.get("report_time");
        dto.setReportTime(reportTime == null ? null : Timestamp.valueOf(reportTime.toString()).toLocalDateTime());
        return dto;
    }

    private static Timestamp toTimestamp(long epochMs) {
        return epochMs <= 0 ? null : Timestamp.from(Instant.ofEpochMilli(epochMs));
    }

    /** 分类归一（空回落业务域缺省 biz；与 TaskDefinition 常量同口径） */
    private static String normalizedCategory(ScheduleTaskStateDTO state) {
        return state.getCategory() == null || state.getCategory().isBlank()
                ? TaskDefinition.CATEGORY_BIZ : state.getCategory();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= 1000) {
            return value;
        }
        return value.substring(0, 1000);
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static Integer toInt(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }
}
