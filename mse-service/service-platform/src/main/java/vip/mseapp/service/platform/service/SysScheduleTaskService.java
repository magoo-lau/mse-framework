package vip.mseapp.service.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.support.CronExpression;
import vip.mseapp.api.system.dto.ScheduleSyncResultDTO;
import vip.mseapp.api.system.dto.ScheduleTaskAdminDTO;
import vip.mseapp.api.system.dto.ScheduleTaskLogDTO;
import vip.mseapp.api.system.dto.ScheduleTaskReportDTO;
import vip.mseapp.api.system.dto.ScheduleTaskStateDTO;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.schedule.ScheduleControlState;
import vip.mseapp.core.schedule.TaskDefinition;
import vip.mseapp.service.platform.entity.SysScheduleSetting;
import vip.mseapp.service.platform.entity.SysScheduleTask;
import vip.mseapp.service.platform.entity.SysScheduleTaskLog;
import vip.mseapp.service.platform.mapper.SysScheduleSettingMapper;
import vip.mseapp.service.platform.mapper.SysScheduleTaskLogMapper;
import vip.mseapp.service.platform.mapper.SysScheduleTaskMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 内置任务调度登记服务（平台库 sys_schedule_task）：
 * <ul>
 *   <li><b>sync</b>（消费契约 {@code RemoteScheduleStateApi}，事件驱动——启动一次/触发踢/
 *       运行事件/降级兜底，无周期心跳）：上报 upsert——定义与运行态全字段比对，无变化跳过写
 *       （不因同步本身产生写流量），有变化才刷新（任务改名/改周期随代码自然收敛），管理面字段
 *       （enabled/exprOverride/remark/trigger_pending）永不覆盖；随后应答启停状态与调度覆盖，
 *       并对待触发行做乐观锁抢占（恰好一个实例抢到）；尾部补排控制哈希去抖重建（降级自愈）；</li>
 *   <li><b>管理面</b>：列表 / 启停 / 触发 / 编辑覆盖与说明（平台端与 standalone 租户端页面，
 *       启停/编辑经 Redis 快通道 5s 级收敛，触发经踢键触发即时 sync 抢占）。</li>
 * </ul>
 *
 * <p>启停/编辑落库成功后写透 Redis 控制哈希 {@link ScheduleControlState#CONTROL_HASH_KEY}
 * （下行快通道，实例监控线程 5s 级收敛）；写透失败不阻断管理操作（去抖重建按 DB 自愈），
 * 触发落库后写踢键 {@link ScheduleControlState#KICK_KEY} 促使实例监控线程立即 sync 抢占
 * trigger_pending（乐观锁，恰好一个实例执行）。
 */
public class SysScheduleTaskService {

    private static final Logger log = LoggerFactory.getLogger(SysScheduleTaskService.class);

    /** 覆盖间隔下限（毫秒），与调度器侧约束一致 */
    private static final long MIN_OVERRIDE_DELAY_MS = 1000;

    private final SysScheduleTaskMapper taskMapper;

    private final SysScheduleTaskLogMapper logMapper;

    private final SysScheduleSettingMapper settingMapper;

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    private final vip.mseapp.service.platform.schedule.ScheduleControlRefresher controlRefresher;

    public SysScheduleTaskService(SysScheduleTaskMapper taskMapper, SysScheduleTaskLogMapper logMapper,
                                  ObjectProvider<StringRedisTemplate> redisProvider,
                                  vip.mseapp.service.platform.schedule.ScheduleControlRefresher controlRefresher) {
        this(taskMapper, logMapper, null, redisProvider, controlRefresher);
    }

    public SysScheduleTaskService(SysScheduleTaskMapper taskMapper, SysScheduleTaskLogMapper logMapper,
                                  SysScheduleSettingMapper settingMapper,
                                  ObjectProvider<StringRedisTemplate> redisProvider,
                                  vip.mseapp.service.platform.schedule.ScheduleControlRefresher controlRefresher) {
        this.taskMapper = taskMapper;
        this.logMapper = logMapper;
        this.settingMapper = settingMapper;
        this.redisProvider = redisProvider;
        this.controlRefresher = controlRefresher;
    }

    /** 状态同步（单次往返完成上行 upsert + 下行启停/覆盖/触发抢占） */
    public ScheduleSyncResultDTO sync(ScheduleTaskReportDTO report) {
        for (ScheduleTaskStateDTO state : report.getTasks()) {
            upsert(report, state);
        }
        ScheduleSyncResultDTO result = new ScheduleSyncResultDTO();
        if (report.getTasks().isEmpty()) {
            return result;
        }
        List<String> taskIds = report.getTasks().stream().map(ScheduleTaskStateDTO::getTaskId).toList();
        List<SysScheduleTask> rows = taskMapper.selectList(new LambdaQueryWrapper<SysScheduleTask>()
                .in(SysScheduleTask::getTaskKey, taskIds));
        Map<String, Boolean> enabledByTask = new HashMap<>();
        Map<String, String> overrideByTask = new HashMap<>();
        List<String> triggered = new ArrayList<>();
        for (SysScheduleTask row : rows) {
            enabledByTask.put(row.getTaskKey(), row.getEnabled() == null || row.getEnabled() == 1);
            if (row.getExprOverride() != null && !row.getExprOverride().isBlank()) {
                overrideByTask.put(row.getTaskKey(), row.getExprOverride());
            } else {
                overrideByTask.put(row.getTaskKey(), null);
            }
            if (row.getTriggerPending() != null && row.getTriggerPending() == 1) {
                int claimed = taskMapper.update(null, new LambdaUpdateWrapper<SysScheduleTask>()
                        .eq(SysScheduleTask::getId, row.getId())
                        .eq(SysScheduleTask::getTriggerPending, 1)
                        .set(SysScheduleTask::getTriggerPending, 0));
                if (claimed > 0) {
                    triggered.add(row.getTaskKey());
                }
            }
        }
        result.setEnabledByTask(enabledByTask);
        result.setExprOverrideByTask(overrideByTask);
        result.setTriggeredTaskIds(triggered);
        // 尾部对账：补排一次去抖重建（不重排已待执行的）——降级期间（Redis 被清空等）60s 内补回
        // 控制哈希，快通道恢复后实例侧自动退出降级兜底
        controlRefresher.ensureRebuildScheduled();
        return result;
    }

    /**
     * upsert：定义 + 运行态刷新，管理面字段（enabled/exprOverride/remark/trigger_pending）不动；
     * 全字段比对无变化跳过写（事件驱动语义——同步本身不产生写流量，report_time 只随真实变化刷新）。
     * 新任务首报落行后即时写透控制哈希：新装环境无需管理操作/重启，快通道即刻可用。
     */
    private void upsert(ScheduleTaskReportDTO report, ScheduleTaskStateDTO state) {
        LocalDateTime now = LocalDateTime.now();
        SysScheduleTask row = taskMapper.selectOne(new LambdaQueryWrapper<SysScheduleTask>()
                .eq(SysScheduleTask::getTaskKey, state.getTaskId()));
        boolean insert = row == null;
        if (!insert && reportUnchanged(row, report, state)) {
            return;
        }
        if (insert) {
            row = new SysScheduleTask();
            row.setTaskKey(state.getTaskId());
            row.setEnabled(1);
            row.setTriggerPending(0);
        }
        row.setOwnerService(report.getServiceName());
        row.setSchedulerMode(report.getSchedulerMode());
        row.setTaskName(state.getName());
        row.setDescription(state.getDescription());
        row.setTaskType(state.getType());
        row.setTaskCategory(normalizedCategory(state.getCategory()));
        row.setScheduleExpr(state.getExpression());
        row.setLastRunTime(toLocalDateTime(state.getLastRunStartMs()));
        row.setLastDurationMs(state.getLastDurationMs());
        row.setLastResult(state.getLastResult());
        row.setLastError(state.getLastError());
        row.setRunCount(state.getRunCount());
        row.setErrorCount(state.getErrorCount());
        row.setReportTime(now);
        if (insert) {
            taskMapper.insert(row);
            pushControl(state.getTaskId(), true, null);
            controlRefresher.scheduleRebuild();
        } else {
            taskMapper.updateById(row);
        }
    }

    /** 上报快照与库中行全字段比对：定义与运行态一致 = 无需写（report_time 不单独刷，判活已下线） */
    private static boolean reportUnchanged(SysScheduleTask row, ScheduleTaskReportDTO report,
                                           ScheduleTaskStateDTO state) {
        return Objects.equals(row.getOwnerService(), report.getServiceName())
                && Objects.equals(row.getSchedulerMode(), report.getSchedulerMode())
                && Objects.equals(row.getTaskName(), state.getName())
                && Objects.equals(row.getDescription(), state.getDescription())
                && Objects.equals(row.getTaskType(), state.getType())
                && Objects.equals(row.getTaskCategory(), normalizedCategory(state.getCategory()))
                && Objects.equals(row.getScheduleExpr(), state.getExpression())
                && Objects.equals(row.getLastRunTime(), toLocalDateTime(state.getLastRunStartMs()))
                && Objects.equals(row.getLastDurationMs(), state.getLastDurationMs())
                && Objects.equals(row.getLastResult(), state.getLastResult())
                && Objects.equals(row.getLastError(), state.getLastError())
                && Objects.equals(row.getRunCount(), state.getRunCount())
                && Objects.equals(row.getErrorCount(), state.getErrorCount());
    }

    /** 管理端：任务列表分页（keyword 模糊匹配编号/名称/说明；serviceName 按服务过滤；category 分域过滤） */
    public PageResult<ScheduleTaskAdminDTO> listTasks(long pageNum, long pageSize, String keyword,
                                                      String serviceName, String category) {
        LambdaQueryWrapper<SysScheduleTask> query = new LambdaQueryWrapper<SysScheduleTask>()
                .orderByAsc(SysScheduleTask::getTaskKey);
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + keyword.trim() + "%";
            query.and(q -> q.like(SysScheduleTask::getTaskKey, like)
                    .or().like(SysScheduleTask::getTaskName, like)
                    .or().like(SysScheduleTask::getDescription, like)
                    .or().like(SysScheduleTask::getRemark, like));
        }
        if (serviceName != null && !serviceName.isBlank()) {
            query.eq(SysScheduleTask::getOwnerService, serviceName.trim());
        }
        if (category != null && !category.isBlank()) {
            query.eq(SysScheduleTask::getTaskCategory, category.trim());
        }
        Page<SysScheduleTask> page = taskMapper.selectPage(Page.of(pageNum, pageSize), query);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                page.getRecords().stream().map(SysScheduleTaskService::toAdminDto).toList());
    }

    /** 分类归一（空回落业务域缺省 biz；与 TaskDefinition 常量同口径） */
    private static String normalizedCategory(String category) {
        return category == null || category.isBlank() ? TaskDefinition.CATEGORY_BIZ : category;
    }

    /** 状态同步器：增量上行执行日志（log_key 幂等去重；按任务剪枝保留最近 200 条） */
    public int appendLogs(List<ScheduleTaskLogDTO> logs) {
        if (logs == null || logs.isEmpty()) {
            return 0;
        }
        Set<String> touchedTasks = new LinkedHashSet<>();
        int appended = 0;
        for (ScheduleTaskLogDTO dto : logs) {
            if (dto.getId() == null || dto.getId().isBlank() || dto.getTaskKey() == null) {
                continue;
            }
            if (logMapper.selectIdByLogKey(dto.getId()) != null) {
                continue; // 重启重报：幂等跳过
            }
            SysScheduleTaskLog row = new SysScheduleTaskLog();
            row.setLogKey(dto.getId());
            row.setTaskKey(dto.getTaskKey());
            row.setOwnerService(dto.getServiceName());
            row.setStartTime(dto.getStartTime() == null ? LocalDateTime.now() : dto.getStartTime());
            row.setDurationMs(dto.getDurationMs() == null ? 0 : dto.getDurationMs());
            row.setResult("FAILED".equalsIgnoreCase(dto.getResult()) ? "FAILED" : "SUCCESS");
            row.setErrorMsg(dto.getError());
            logMapper.insert(row);
            appended++;
            touchedTasks.add(dto.getTaskKey());
        }
        touchedTasks.forEach(logMapper::pruneKeepingRecent);
        return appended;
    }

    /** 管理端：执行日志分页（result：SUCCESS/FAILED/null=全部，按开始时间倒序） */
    public PageResult<ScheduleTaskLogDTO> listLogs(String taskKey, long pageNum, long pageSize, String result) {
        LambdaQueryWrapper<SysScheduleTaskLog> query = new LambdaQueryWrapper<SysScheduleTaskLog>()
                .eq(SysScheduleTaskLog::getTaskKey, taskKey)
                .orderByDesc(SysScheduleTaskLog::getStartTime);
        if ("SUCCESS".equalsIgnoreCase(result) || "FAILED".equalsIgnoreCase(result)) {
            query.eq(SysScheduleTaskLog::getResult, result.toUpperCase());
        }
        Page<SysScheduleTaskLog> page = logMapper.selectPage(Page.of(pageNum, pageSize), query);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                page.getRecords().stream().map(SysScheduleTaskService::toLogDto).toList());
    }

    private static ScheduleTaskLogDTO toLogDto(SysScheduleTaskLog row) {
        ScheduleTaskLogDTO dto = new ScheduleTaskLogDTO();
        dto.setId(row.getLogKey());
        dto.setTaskKey(row.getTaskKey());
        dto.setServiceName(row.getOwnerService());
        dto.setStartTime(row.getStartTime());
        dto.setDurationMs(row.getDurationMs());
        dto.setResult(row.getResult());
        dto.setError(row.getErrorMsg());
        return dto;
    }

    /** 管理端：启停（落库 + 写透控制缓存，全域实例 5s 级收敛；sync 通道兜底） */
    public boolean toggle(String taskKey, boolean enabled) {
        boolean updated = taskMapper.update(null, new LambdaUpdateWrapper<SysScheduleTask>()
                .eq(SysScheduleTask::getTaskKey, taskKey)
                .set(SysScheduleTask::getEnabled, enabled ? 1 : 0)) > 0;
        if (updated) {
            SysScheduleTask row = taskMapper.selectOne(new LambdaQueryWrapper<SysScheduleTask>()
                    .eq(SysScheduleTask::getTaskKey, taskKey));
            pushControl(taskKey, enabled, row == null ? null : blankToNull(row.getExprOverride()));
            controlRefresher.scheduleRebuild();
        }
        return updated;
    }

    /** 管理端：下发触发（置 pending + 写踢键，实例监控线程 ≤5s sync 抢占执行；任务不存在返回 false） */
    public boolean trigger(String taskKey) {
        boolean updated = taskMapper.update(null, new LambdaUpdateWrapper<SysScheduleTask>()
                .eq(SysScheduleTask::getTaskKey, taskKey)
                .set(SysScheduleTask::getTriggerPending, 1)) > 0;
        if (updated) {
            kickSync();
        }
        return updated;
    }

    /**
     * 管理端：编辑调度覆盖与详细说明。remark 传 null 不变、空串清空；
     * 覆盖非法（非数字非 cron / 数字 < 1000ms）返回 false。
     */
    public boolean updateTask(String taskKey, String exprOverride, String remark) {
        String override = normalizeOverride(exprOverride);
        if (override == null) {
            return false; // 非法表达式（normalize 的 null 哨兵）
        }
        LambdaUpdateWrapper<SysScheduleTask> update = new LambdaUpdateWrapper<SysScheduleTask>()
                .eq(SysScheduleTask::getTaskKey, taskKey)
                .set(SysScheduleTask::getExprOverride, override);
        if (remark != null) {
            update.set(SysScheduleTask::getRemark, remark.length() > 1000 ? remark.substring(0, 1000) : remark);
        }
        boolean updated = taskMapper.update(null, update) > 0;
        if (updated) {
            SysScheduleTask row = taskMapper.selectOne(new LambdaQueryWrapper<SysScheduleTask>()
                    .eq(SysScheduleTask::getTaskKey, taskKey));
            pushControl(taskKey, row != null && (row.getEnabled() == null || row.getEnabled() == 1),
                    blankToNull(override));
            controlRefresher.scheduleRebuild();
        }
        return updated;
    }

    /** 管理面落库后写透 Redis 控制哈希单字段（纯 Redis 写、零 SQL；失败仅告警，去抖重建对账自愈） */
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
            log.warn("[mse-schedule] 控制缓存写透失败（重建线程自愈）: {}", e.getMessage());
        }
    }

    /**
     * 写触发踢键（值=毫秒时间戳）：实例侧控制节拍读到新值即发起全量 sync 抢占 trigger_pending。
     * 无 TTL——实例重启首拍读到旧值也能接管停机期间遗留的 pending 触发；Redis 缺席时不写
     * （实例侧处于降级兜底，照常按 DB 收敛，慢而不断流）。
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** 覆盖表达式规范化：trim；空 = null（恢复代码默认）；非法 = 哨兵 NULL_TEXT 不会被返回——见调用处 */
    private String normalizeOverride(String exprOverride) {
        String trimmed = exprOverride == null ? "" : exprOverride.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.chars().allMatch(Character::isDigit)) {
            long delayMs = Long.parseLong(trimmed);
            if (delayMs < MIN_OVERRIDE_DELAY_MS) {
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

    private static ScheduleTaskAdminDTO toAdminDto(SysScheduleTask row) {
        ScheduleTaskAdminDTO dto = new ScheduleTaskAdminDTO();
        dto.setId(row.getId());
        dto.setTaskKey(row.getTaskKey());
        dto.setOwnerService(row.getOwnerService());
        dto.setSchedulerMode(row.getSchedulerMode());
        dto.setTaskName(row.getTaskName());
        dto.setDescription(row.getDescription());
        dto.setTaskType(row.getTaskType());
        dto.setTaskCategory(row.getTaskCategory());
        dto.setScheduleExpr(row.getScheduleExpr());
        dto.setEnabled(row.getEnabled());
        dto.setExprOverride(row.getExprOverride());
        dto.setRemark(row.getRemark());
        dto.setLastRunTime(row.getLastRunTime());
        dto.setLastDurationMs(row.getLastDurationMs());
        dto.setLastResult(row.getLastResult());
        dto.setLastError(row.getLastError());
        dto.setRunCount(row.getRunCount());
        dto.setErrorCount(row.getErrorCount());
        dto.setReportTime(row.getReportTime());
        return dto;
    }

    private static LocalDateTime toLocalDateTime(long epochMs) {
        return epochMs <= 0 ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
    }

    // ==================== 调度器设置（「调度器」页签：租户并发闸门热更） ====================

    /** 闸门参数键前缀（设置行 setting_key = "gates." + TenantTaskGates.key()） */
    private static final String GATES_PREFIX = "gates.";

    /** 读全部调度器设置（key→value；空 Map=未配置，调用方回落代码默认） */
    public Map<String, String> getSettings() {
        Map<String, String> out = new LinkedHashMap<>();
        if (settingMapper == null) {
            return out;
        }
        for (SysScheduleSetting row : settingMapper.selectList(new LambdaQueryWrapper<SysScheduleSetting>()
                .orderByAsc(SysScheduleSetting::getSettingKey))) {
            out.put(row.getSettingKey(), row.getSettingValue());
        }
        return out;
    }

    /**
     * 读闸门定义目录（key→remark，「调度器」页签动态渲染用）：gates.* 行的 remark 承载
     * 中文名称/说明（约定「名称（每租户）｜说明」），第三方业务积木经自有平台域种子脚本
     * 自注册即出现在页面，无需改前端。返回含尚未配置的键（仅有定义行、值回落代码默认）。
     */
    public Map<String, String> getGateDefs() {
        Map<String, String> out = new LinkedHashMap<>();
        if (settingMapper == null) {
            return out;
        }
        for (SysScheduleSetting row : settingMapper.selectList(new LambdaQueryWrapper<SysScheduleSetting>()
                .likeRight(SysScheduleSetting::getSettingKey, GATES_PREFIX)
                .orderByAsc(SysScheduleSetting::getSettingKey))) {
            out.put(row.getSettingKey().substring(GATES_PREFIX.length()),
                    row.getRemark() == null ? "" : row.getRemark());
        }
        return out;
    }

    /**
     * 更新租户并发闸门上限：gates.* 设置行 upsert + 写透 Redis 闸门参数键
     * （实例监控线程控制节拍差量应用，5s 级收敛）+ 安排去抖重建对账。
     */
    public boolean updateScheduleSettings(Map<String, Integer> gateLimits) {
        if (gateLimits == null || gateLimits.isEmpty() || settingMapper == null) {
            return false;
        }
        Map<String, Integer> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : gateLimits.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            Integer limit = entry.getValue();
            if (!key.matches("[\\w.-]+") || limit == null || limit < 1) {
                return false; // 非法键（防注入 encodeGates 行格式）或非法上限
            }
            normalized.put(key, limit);
        }
        for (Map.Entry<String, Integer> entry : normalized.entrySet()) {
            String settingKey = GATES_PREFIX + entry.getKey();
            SysScheduleSetting existing = settingMapper.selectOne(new LambdaQueryWrapper<SysScheduleSetting>()
                    .eq(SysScheduleSetting::getSettingKey, settingKey));
            String value = String.valueOf(entry.getValue());
            if (existing == null) {
                SysScheduleSetting row = new SysScheduleSetting();
                row.setSettingKey(settingKey);
                row.setSettingValue(value);
                // remark 兼作「调度器」页签闸门目录的名称（第三方经自有种子脚本写「名称（每租户）｜说明」）
                row.setRemark(entry.getKey() + "（每租户）");
                settingMapper.insert(row);
            } else if (!value.equals(existing.getSettingValue())) {
                existing.setSettingValue(value);
                settingMapper.updateById(existing);
            }
        }
        pushGates();
        controlRefresher.scheduleRebuild();
        return true;
    }

    /** 按当前库中 gates.* 行写透 Redis 闸门参数键（失败仅告警，去抖重建自愈） */
    private void pushGates() {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        Map<String, Integer> limits = new LinkedHashMap<>();
        for (SysScheduleSetting row : settingMapper.selectList(new LambdaQueryWrapper<SysScheduleSetting>()
                .likeRight(SysScheduleSetting::getSettingKey, GATES_PREFIX))) {
            try {
                limits.put(row.getSettingKey().substring(GATES_PREFIX.length()),
                        Integer.parseInt(row.getSettingValue().trim()));
            } catch (NumberFormatException ignored) {
                // 非法行跳过
            }
        }
        String encoded = ScheduleControlState.encodeGates(limits);
        try {
            if (encoded.isEmpty()) {
                redis.delete(ScheduleControlState.GATES_KEY);
            } else {
                redis.opsForValue().set(ScheduleControlState.GATES_KEY, encoded);
            }
        } catch (Exception e) {
            log.warn("[mse-schedule] 闸门参数键写透失败（去抖重建自愈）: {}", e.getMessage());
        }
    }
}
