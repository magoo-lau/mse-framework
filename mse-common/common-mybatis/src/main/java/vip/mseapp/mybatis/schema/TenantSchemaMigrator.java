package vip.mseapp.mybatis.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 结构迁移执行器（Flyway 式纪律的自研轻量实现）：按版本序应用 pending 脚本/Java 迁移并记入
 * {@code mse_<biz>_schema_history} 账本（版本/checksum/耗时/成败），提供审计与版本追踪。
 *
 * <p><b>账本路由</b>：脚本/Java 迁移按 biz 段路由到各自账本，同库多积木各记各账；执行顺序仍按
 * 版本全局排序（跨 biz 依赖——如基座表先于业务种子——靠版本号约定保证），失败记 success=0 后停止，
 * 下次执行自动重试该版本；已应用脚本 checksum 变化计 CONFLICT 不重跑（{@link Result#conflicts()}
 * 计数，启动自动迁移与懒初始化据此阻断/失败——避免新代码带旧结构库运行）。
 *
 * <p><b>通道</b>：结构通道（tenant_id=0 记账：DDL + 平台数据，目标库级一次）与每租户通道
 * （tenant_id=实际租户记账：种子数据，每租户一次，占位符 ${tenant_id} 注入）互不干扰；
 * 结构基线只约束结构通道——每租户通道按 (script, tenant) 独立记账，每个新/存量租户独立补跑，
 * 这是「新增种子脚本后存量租户增量升级」的基础。
 *
 * <p><b>基线语义</b>（与克隆式建库自洽的关键）：结构通道账本为空且目标库已有<b>该 biz 自身</b>业务表
 * 时，视为「存量库接入」，按基线版本写一条 BASELINE 流水（≤基线的结构脚本视为已包含，不再执行）。
 * 「自身业务表」按 biz 建表脚本解析的探针表判定（本通道建表 − TENANT 通道身份表 − 共享基建表）——
 * 多积木共库（平台库/聚合库）不因他 biz 的表而误判，框架自建表（sys_/mse_ 前缀、ACT_/FLW_ 引擎表、
 * undo_log）同样不计入——含业务脚本自携的 undo_log（Seata 启动即抢建，业务表未建时它先在，会误判存量）；探针解析不出建表的 biz 退回全库业务表判定的保守旧行为。账本已有基线但探针表全部
 * 缺失时视为误判基线（旧版粗粒度探针遗留），删除记账按全新库补齐（自愈）。新克隆租户库基线取
 * 主库对应账本当前版本（{@link #resolveBizBaselines}），克隆即主库结构天然正确。
 */
public class TenantSchemaMigrator {

    private static final Logger log = LoggerFactory.getLogger(TenantSchemaMigrator.class);

    /** 旧版单账本表名（检测到有流水时告警提示升级路径，不自动迁移） */
    private static final String LEGACY_TABLE = "mse_schema_history";

    /** 建表语句解析（基线探针用）：CREATE TABLE [IF NOT EXISTS] 表名 */
    private static final Pattern CREATE_TABLE = Pattern.compile(
            "(?i)\\bCREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[\"'`]?([A-Za-z][A-Za-z0-9_$]*)");

    /** ADHOC 通道的伪 biz（界面临时 SQL 账本） */
    public static final String ADHOC_BIZ = "adhoc";

    private static volatile boolean legacyWarned;

    private final TenantMigrationScanner scanner;

    private final String operator;

    private final List<MseSchemaJavaMigration> javaMigrations;

    public TenantSchemaMigrator(TenantMigrationScanner scanner, String operator) {
        this(scanner, operator, List.of());
    }

    public TenantSchemaMigrator(TenantMigrationScanner scanner, String operator,
                                List<MseSchemaJavaMigration> javaMigrations) {
        this.scanner = scanner;
        this.operator = operator == null || operator.isBlank() ? "mse" : operator.trim();
        this.javaMigrations = javaMigrations == null ? List.of() : List.copyOf(javaMigrations);
    }

    /** 单步执行结果 */
    public record StepResult(String biz, String version, String type, String script,
                             boolean success, long ms, String error) {
    }

    /** 一次迁移的汇总：before/after 当前版本、成功/失败/checksum 冲突步数、逐步明细 */
    public record Result(String before, String after, int applied, int failed, int conflicts,
                         List<StepResult> steps) {

        public String firstError() {
            return steps.stream().filter(s -> !s.success()).findFirst()
                    .map(s -> s.biz() + " " + s.version() + ": " + s.error()).orElse(null);
        }

        /** 首个 checksum 冲突描述（已应用脚本内容被改动），无冲突返回 null */
        public String firstConflict() {
            return steps.stream().filter(s -> s.success() && s.error() != null && s.error().startsWith("CONFLICT"))
                    .findFirst().map(s -> s.biz() + " " + s.version() + ": " + s.error()).orElse(null);
        }

        /** 多行摘要（管理端弹窗直接展示） */
        public String summary() {
            StringBuilder sb = new StringBuilder("当前版本 ").append(before()).append(" → ").append(after())
                    .append("：应用 ").append(applied()).append(" 条、失败 ").append(failed()).append(" 条");
            if (conflicts() > 0) {
                sb.append("、checksum 冲突 ").append(conflicts()).append(" 条");
            }
            for (StepResult step : steps()) {
                sb.append("\n- [").append(step.biz()).append("/").append(step.type()).append("] ")
                        .append(step.version()).append(step.success() ? " ✓ " : " ✗ ").append(step.script());
                if (!step.success() && step.error() != null) {
                    sb.append("：").append(step.error());
                } else if (step.success() && step.error() != null) {
                    sb.append("：").append(step.error());
                }
            }
            return sb.toString();
        }
    }

    /** 结构状态（状态页展示，按 biz 一份） */
    public record Status(String biz, String dbType, boolean historyReady, String currentVersion,
                         String baselineVersion, List<String> pending, List<String> conflicts, String lastError) {
    }

    /** 预演单步（与实际执行同序） */
    public record PlanStep(String biz, String version, String type, String script, String description) {

        @Override
        public String toString() {
            return "[" + biz + "/" + type + "] " + version + " " + description() + "（" + script + "）";
        }
    }

    /**
     * 迁移预演结果（只读计算，不建账本、不落基线、不执行任何语句）：
     * pending=按实际执行顺序的将执行清单；conflicts=已应用脚本被改动的 checksum 冲突（实际执行将阻断）；
     * notes=基线/账本判定说明（账本未建、存量库将打基线、误判基线将自愈等）。
     */
    public record Plan(String dbType, MigrationTarget target, boolean perTenant,
                       List<PlanStep> pending, List<String> conflicts, List<String> notes) {
    }

    /** 应用 pending 脚本（按方言扫描 classpath，按 options.target 过滤） */
    public Result migrate(Connection target, String dbType, MigrateOptions options) throws SQLException {
        return migrate(target, dbType, scanner.scan(dbType), options);
    }

    /** 应用给定脚本清单（按 version 升序、按 options.target 过滤；Java 迁移由构造时注入） */
    public Result migrate(Connection target, String dbType, List<SchemaMigration> migrations,
                          MigrateOptions options) throws SQLException {
        long channelTenantId = options.perTenant() ? options.tenantId() : 0L;
        Map<String, String> placeholders = effectivePlaceholders(options, channelTenantId);

        Map<String, MseSchemaJavaMigration> javaByScript = new LinkedHashMap<>();
        List<SchemaMigration> all = effectiveMigrations(migrations, options, javaByScript);

        // 各 biz 结构通道最大版本（自动基线兜底值；每租户项不计入，避免抬高基线漏掉新租户种子）
        Map<String, String> maxStructureByBiz = new HashMap<>();
        for (SchemaMigration m : all) {
            if (!m.perTenant()) {
                maxStructureByBiz.merge(m.biz(), m.version(),
                        (a, b) -> a.compareTo(b) >= 0 ? a : b);
            }
        }

        // 第一遍：逐 biz 建账本、加载通道流水、解析/落结构基线
        Map<String, TenantSchemaHistoryDao> daos = new LinkedHashMap<>();
        Map<String, Map<String, TenantSchemaHistoryDao.HistoryRow>> appliedByBiz = new LinkedHashMap<>();
        Map<String, String> baselineByBiz = new HashMap<>();
        Map<String, Set<String>> probesByBiz = probeTablesByBiz(migrations, options.target());
        List<StepResult> steps = new ArrayList<>();
        for (String biz : all.stream().map(SchemaMigration::biz).distinct().toList()) {
            TenantSchemaHistoryDao dao = new TenantSchemaHistoryDao(biz);
            dao.ensureTable(target, dbType);
            daos.put(biz, dao);
            Map<String, TenantSchemaHistoryDao.HistoryRow> applied = dao.appliedRows(target, channelTenantId);
            appliedByBiz.put(biz, applied);
            if (baselineAware(options)) {
                baselineByBiz.put(biz, resolveBaseline(dao, target, dbType, biz, applied,
                        options, maxStructureByBiz.get(biz),
                        probesByBiz.getOrDefault(biz, Set.of()), steps));
            }
        }
        String before = currentVersion(target, daos.values());

        // 第二遍：按版本序执行
        int successCount = 0;
        int failedCount = 0;
        int conflictCount = 0;
        for (SchemaMigration migration : all) {
            TenantSchemaHistoryDao dao = daos.get(migration.biz());
            Map<String, TenantSchemaHistoryDao.HistoryRow> applied = appliedByBiz.get(migration.biz());
            String baseline = baselineByBiz.get(migration.biz());
            TenantSchemaHistoryDao.HistoryRow row = applied.get(migration.script());
            if (row != null && row.success()) {
                if (migration.checksum() != null && row.checksum() != null
                        && !row.checksum().equals(migration.checksum())) {
                    conflictCount++;
                    log.error("[mse-schema] checksum 冲突：biz[{}] {} {} 已应用但脚本内容有改动"
                                    + "（历史 {} ≠ 当前 {}），不重跑；启动自动迁移将阻断，请恢复脚本原文或人工核对账本",
                            migration.biz(), migration.version(), migration.script(),
                            row.checksum(), migration.checksum());
                    steps.add(new StepResult(migration.biz(), migration.version(), migration.type(),
                            migration.script(), true, 0, "CONFLICT：脚本已应用但内容有改动（历史 checksum 不一致），请人工核对"));
                }
                continue;
            }
            // 结构基线只跳过结构通道（每租户通道按 (script, tenant) 独立补跑，不受基线约束）
            if (!migration.perTenant() && baseline != null
                    && migration.version().compareTo(baseline) <= 0) {
                continue;
            }
            long start = System.currentTimeMillis();
            try {
                if (SchemaMigration.TYPE_JAVA.equals(migration.type())) {
                    javaByScript.get(migration.script())
                            .migrate(target, new MigrationContext(dbType, channelTenantId, options.target(), placeholders));
                } else {
                    executeScript(target, SchemaPlaceholders.resolve(migration.sql(), placeholders));
                }
                TenantSchemaHistoryDao.HistoryRow done = new TenantSchemaHistoryDao.HistoryRow(
                        migration.version(), migration.description(), migration.type(), migration.script(),
                        migration.checksum(), true, (int) (System.currentTimeMillis() - start), null);
                dao.insert(target, done, operator, channelTenantId);
                applied.put(migration.script(), done);
                steps.add(new StepResult(migration.biz(), migration.version(), migration.type(),
                        migration.script(), true, done.executionMs(), null));
                successCount++;
            } catch (Exception e) {
                String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                dao.insert(target, new TenantSchemaHistoryDao.HistoryRow(
                        migration.version(), migration.description(), migration.type(), migration.script(),
                        migration.checksum(), false, (int) (System.currentTimeMillis() - start), error), operator, channelTenantId);
                steps.add(new StepResult(migration.biz(), migration.version(), migration.type(),
                        migration.script(), false, System.currentTimeMillis() - start, error));
                failedCount++;
                // 后续脚本依赖前序结构，停止本轮；失败已入历史，下次执行自动重试该版本
                break;
            }
        }
        String after = currentVersion(target, daos.values());
        return new Result(before, after, successCount, failedCount, conflictCount, List.copyOf(steps));
    }

    /**
     * 通道过滤（target + 结构/每租户双维）+ Java 迁移并入伪 SchemaMigration + 统一排序
     * （基座 system 恒先：业务脚本可能引用 system 更高版本号脚本新加的列）。
     * migrate 与 plan 共用，保证预演与实际执行同一口径同一顺序。
     */
    private List<SchemaMigration> effectiveMigrations(List<SchemaMigration> migrations, MigrateOptions options,
                                                      Map<String, MseSchemaJavaMigration> javaByScript) {
        List<SchemaMigration> all = new ArrayList<>(migrations.stream()
                .filter(m -> m.target() == options.target() && m.perTenant() == options.perTenant())
                .toList());
        for (MseSchemaJavaMigration jm : javaMigrations) {
            if (jm.target() != options.target() || jm.perTenant() != options.perTenant()) {
                continue;
            }
            String script = jm.getClass().getName();
            if (javaByScript.put(script, jm) != null) {
                throw new IllegalStateException("Java 迁移类名重复（账本键冲突）: " + script);
            }
            all.add(new SchemaMigration(jm.biz(), jm.version(), jm.description(),
                    SchemaMigration.TYPE_JAVA, jm.target(), jm.perTenant(), script, null, null));
        }
        all.sort(Comparator
                .comparing((SchemaMigration m) -> "system".equals(m.biz()) ? 0 : 1)
                .thenComparing(SchemaMigration::version)
                .thenComparing(SchemaMigration::script));
        return all;
    }

    /**
     * 迁移预演（只读，按方言扫描 classpath）：与 {@link #migrate} 同一过滤/排序/基线口径计算
     * 「将要执行的脚本清单」，不建账本、不落基线、不执行任何语句——供启动日志预览、管理端预演
     * 与发布前核对（尤其一次变更影响多服务/多库/多方言时先看清单再执行）。
     */
    public Plan plan(Connection target, String dbType, MigrateOptions options) throws SQLException {
        return plan(target, dbType, scanner.scan(dbType), options);
    }

    /** 迁移预演（可指定脚本清单，与 {@link #migrate(Connection, String, List, MigrateOptions)} 对位） */
    public Plan plan(Connection target, String dbType, List<SchemaMigration> migrations,
                     MigrateOptions options) throws SQLException {
        long channelTenantId = options.perTenant() ? options.tenantId() : 0L;
        List<SchemaMigration> all = effectiveMigrations(migrations, options, new LinkedHashMap<>());

        // 各 biz 结构通道最大版本（自动基线兜底值；与 migrate 同口径）
        Map<String, String> maxStructureByBiz = new HashMap<>();
        for (SchemaMigration m : all) {
            if (!m.perTenant()) {
                maxStructureByBiz.merge(m.biz(), m.version(), (a, b) -> a.compareTo(b) >= 0 ? a : b);
            }
        }

        // 第一遍（只读）：账本既有流水 + 基线判定（不建表、不落基线，探测结论记入 notes）
        Map<String, Set<String>> probesByBiz = probeTablesByBiz(migrations, options.target());
        Map<String, Map<String, TenantSchemaHistoryDao.HistoryRow>> appliedByBiz = new LinkedHashMap<>();
        Map<String, String> baselineByBiz = new HashMap<>();
        List<String> notes = new ArrayList<>();
        for (String biz : all.stream().map(SchemaMigration::biz).distinct().toList()) {
            TenantSchemaHistoryDao dao = new TenantSchemaHistoryDao(biz);
            if (!dao.tableExists(target)) {
                // 账本未建按空流水参与基线判定（与 migrate 建表后的判定同口径：存量库将打基线）
                appliedByBiz.put(biz, Map.of());
                notes.add("biz[" + biz + "] 账本未建：实际执行将创建账本（" + dao.table() + "）");
                if (baselineAware(options)) {
                    baselineByBiz.put(biz, evaluateBaseline(target, dbType, biz, Map.of(), options,
                            maxStructureByBiz.get(biz), probesByBiz.getOrDefault(biz, Set.of()), notes));
                }
                continue;
            }
            Map<String, TenantSchemaHistoryDao.HistoryRow> applied = dao.appliedRows(target, channelTenantId);
            appliedByBiz.put(biz, applied);
            if (baselineAware(options)) {
                baselineByBiz.put(biz, evaluateBaseline(target, dbType, biz, applied, options,
                        maxStructureByBiz.get(biz), probesByBiz.getOrDefault(biz, Set.of()), notes));
            }
        }

        // 第二遍（只读）：按实际执行序计算待执行清单与冲突
        List<PlanStep> pending = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        for (SchemaMigration migration : all) {
            Map<String, TenantSchemaHistoryDao.HistoryRow> applied = appliedByBiz.get(migration.biz());
            String baseline = baselineByBiz.get(migration.biz());
            TenantSchemaHistoryDao.HistoryRow row = applied.get(migration.script());
            if (row != null && row.success()) {
                if (migration.checksum() != null && row.checksum() != null
                        && !row.checksum().equals(migration.checksum())) {
                    conflicts.add(migration.biz() + " " + migration.version() + " " + migration.script());
                }
                continue;
            }
            // 结构基线只跳过结构通道（与 migrate 同口径；每租户通道按 (script, tenant) 独立补跑）
            if (!migration.perTenant() && baseline != null
                    && migration.version().compareTo(baseline) <= 0) {
                continue;
            }
            pending.add(new PlanStep(migration.biz(), migration.version(), migration.type(),
                    migration.script(), migration.description()));
        }
        return new Plan(dbType, options.target(), options.perTenant(),
                List.copyOf(pending), List.copyOf(conflicts), List.copyOf(notes));
    }

    /**
     * 只读基线判定（预演用）：与 {@link #resolveBaseline} 同口径但不落账本——
     * 账本既有 BASELINE 直接用（含误判基线将自愈的探测，实际执行会删记账补脚本，预演按补齐口径展示）；
     * 账本空且满足存量自动基线条件时返回将打的基线版本并把结论记入 notes。
     */
    private String evaluateBaseline(Connection target, String dbType, String biz,
                                    Map<String, TenantSchemaHistoryDao.HistoryRow> applied,
                                    MigrateOptions options, String maxStructureVersion,
                                    Set<String> probeTables, List<String> notes) throws SQLException {
        String existing = applied.values().stream()
                .filter(r -> SchemaMigration.TYPE_BASELINE.equals(r.type()) && r.success())
                .map(TenantSchemaHistoryDao.HistoryRow::version)
                .findFirst().orElse(null);
        if (existing != null) {
            if (!probeTables.isEmpty() && !hasAnyTable(target, dbType, probeTables)) {
                notes.add("biz[" + biz + "] 账本基线 " + existing + " 与库内结构不符（疑误判），实际执行将删除基线记账并按清单补齐");
                return null;
            }
            return existing;
        }
        if (!options.autoBaseline() || !applied.isEmpty()) {
            return null;
        }
        boolean existingSchema = probeTables.isEmpty()
                ? hasUserTables(target, dbType)
                : hasAnyTable(target, dbType, probeTables);
        if (!existingSchema) {
            return null;
        }
        String base = options.bizBaselines().get(biz);
        if (base == null || base.isBlank()) {
            base = options.baselineVersion();
        }
        if (base == null || base.isBlank()) {
            base = maxStructureVersion;
        }
        if (base == null) {
            return null;
        }
        notes.add("biz[" + biz + "] 检出存量结构：实际执行将打基线到 " + base + "（≤ 基线版本的结构脚本跳过）");
        return base;
    }

    /**
     * 基线判定开关：仅「结构通道 + 显式开启自动基线 + 非 BASE 通道」参与——克隆式建库（TENANT/MASTER
     * 通道基线取自主库账本，克隆即主库结构天然正确）依赖它；BASE 通道与 autoBaseline=false 的调用方
     * （车队重放/注册摄入/目录批次追平）一律不写基线、也不受账本既有 BASELINE 行约束——base 副本
     * 以幂等 DML 为主（菜单/权限点脚本无建表，探针空集回退「全库业务表」判定，会把懒初始化②MASTER
     * 刚建完表的全新库误判存量、写基线整组跳过 base 脚本，且探针自愈因空集失效，受损行永久无自愈通道；
     * 生产事故回归：租户独立库因此整组丢失协同办公/数据中台/开放能力菜单），幂等收敛靠脚本自身。
     */
    private static boolean baselineAware(MigrateOptions options) {
        return !options.perTenant() && options.autoBaseline() && options.target() != MigrationTarget.BASE;
    }

    /** 界面临时 SQL：不做版本比较直接执行，记 ADHOC 流水（审计可查） */
    public Result applyAdhoc(Connection target, String dbType, String sql, String description) throws SQLException {
        TenantSchemaHistoryDao dao = new TenantSchemaHistoryDao(ADHOC_BIZ);
        dao.ensureTable(target, dbType);
        String before = dao.maxVersion(target);
        String version = "ADHOC_" + java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        long start = System.currentTimeMillis();
        try {
            executeScript(target, sql);
            dao.insert(target, new TenantSchemaHistoryDao.HistoryRow(version,
                    description == null || description.isBlank() ? "临时 SQL" : description.trim(),
                    SchemaMigration.TYPE_ADHOC, "adhoc", TenantMigrationScanner.checksum(sql),
                    true, (int) (System.currentTimeMillis() - start), null), operator, 0L);
            return new Result(before, dao.maxVersion(target), 1, 0, 0,
                    List.of(new StepResult(ADHOC_BIZ, version, SchemaMigration.TYPE_ADHOC, "adhoc", true,
                            System.currentTimeMillis() - start, null)));
        } catch (Exception e) {
            String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            dao.insert(target, new TenantSchemaHistoryDao.HistoryRow(
                    version, description, SchemaMigration.TYPE_ADHOC, "adhoc", null,
                    false, (int) (System.currentTimeMillis() - start), error), operator, 0L);
            throw e;
        }
    }

    /** 各 biz 结构状态（状态页展示；adhoc 账本有失败时也在列） */
    public List<Status> statusAll(Connection target, String dbType) throws SQLException {
        List<String> bizs = new ArrayList<>(scanner.bizList(dbType));
        if (!bizs.contains(ADHOC_BIZ)) {
            bizs.add(ADHOC_BIZ);
        }
        List<Status> out = new ArrayList<>();
        for (String biz : bizs) {
            out.add(status(target, dbType, biz));
        }
        return out;
    }

    /** 单 biz 结构状态：当前版本、基线、pending 清单（结构通道）、checksum 冲突、最近失败 */
    public Status status(Connection target, String dbType, String biz) throws SQLException {
        TenantSchemaHistoryDao dao = new TenantSchemaHistoryDao(biz);
        List<SchemaMigration> migrations = scanner.scanBiz(dbType, biz);
        if (!dao.tableExists(target)) {
            return new Status(biz, dbType, false, null, null,
                    migrations.stream().map(m -> m.version() + " " + m.description()).toList(), List.of(), null);
        }
        Map<String, TenantSchemaHistoryDao.HistoryRow> applied = dao.appliedRows(target, 0L);
        String baseline = applied.values().stream()
                .filter(r -> SchemaMigration.TYPE_BASELINE.equals(r.type()) && r.success())
                .map(TenantSchemaHistoryDao.HistoryRow::version)
                .findFirst().orElse(null);
        List<String> pending = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();
        for (SchemaMigration migration : migrations) {
            TenantSchemaHistoryDao.HistoryRow row = applied.get(migration.script());
            if (row == null || !row.success()) {
                if (baseline == null || migration.version().compareTo(baseline) > 0) {
                    pending.add(migration.version() + " " + migration.description());
                }
            } else if (migration.checksum() != null && row.checksum() != null
                    && !row.checksum().equals(migration.checksum())) {
                conflicts.add(migration.version() + " " + migration.script());
            }
        }
        TenantSchemaHistoryDao.HistoryRow failure = dao.lastFailure(target);
        return new Status(biz, dbType, true, dao.maxVersion(target), baseline,
                List.copyOf(pending), List.copyOf(conflicts),
                failure == null ? null : failure.version() + ": " + failure.errorMsg());
    }

    /**
     * 各 biz 在主库的结构基线（账本当前版本；主库无账本回退该 biz 已知最大脚本版本）——
     * 租户库克隆后按此打基线（克隆即主库结构，天然正确），随后仅应用主库尚未升级期间新出现的脚本。
     */
    public Map<String, String> resolveBizBaselines(Connection master, String dbType) throws SQLException {
        Map<String, String> out = new LinkedHashMap<>();
        for (String biz : scanner.bizList(dbType)) {
            TenantSchemaHistoryDao dao = new TenantSchemaHistoryDao(biz);
            String applied = dao.tableExists(master) ? dao.maxVersion(master) : null;
            out.put(biz, applied != null ? applied : scanner.maxVersion(dbType, biz));
        }
        return out;
    }

    /** 旧版单账本（mse_schema_history）检测：有流水时告警一次（不自动迁移，见升级文档） */
    public void warnLegacyLedger(Connection conn) {
        if (legacyWarned) {
            return;
        }
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM " + LEGACY_TABLE)) {
            if (rs.next() && rs.getLong(1) > 0) {
                log.warn("[mse-schema] 检测到旧版账本 {} 有历史流水：账本已改为 mse_<biz>_schema_history（按 biz 分库分册），"
                        + "旧流水不自动迁移；存量库升级请按升级文档处理（重建库或手工应用新脚本）", LEGACY_TABLE);
                legacyWarned = true;
            }
        } catch (SQLException e) {
            legacyWarned = true;
        }
    }

    /**
     * 结构通道基线解析：账本已有 BASELINE 直接用（探针表全部缺失时视为误判基线，删除记账自愈）；
     * 否则满足「存量库自动基线」条件时记账——存量判定按 biz 探针表（本通道建表 − TENANT 身份表），
     * 多积木共库不因他 biz 的表而误判；探针集为空（纯 Java 迁移等解析不出建表）退回全库业务表判定。
     */
    private String resolveBaseline(TenantSchemaHistoryDao dao, Connection target, String dbType, String biz,
                                   Map<String, TenantSchemaHistoryDao.HistoryRow> applied,
                                   MigrateOptions options, String maxStructureVersion,
                                   Set<String> probeTables, List<StepResult> steps) throws SQLException {
        String existing = applied.values().stream()
                .filter(r -> SchemaMigration.TYPE_BASELINE.equals(r.type()) && r.success())
                .map(TenantSchemaHistoryDao.HistoryRow::version)
                .findFirst().orElse(null);
        if (existing == null) {
            if (!options.autoBaseline() || !applied.isEmpty()) {
                return null;
            }
            boolean existingSchema = probeTables.isEmpty()
                    ? hasUserTables(target, dbType)
                    : hasAnyTable(target, dbType, probeTables);
            if (!existingSchema) {
                return null;
            }
            String base = options.bizBaselines().get(biz);
            if (base == null || base.isBlank()) {
                base = options.baselineVersion();
            }
            if (base == null || base.isBlank()) {
                base = maxStructureVersion;
            }
            if (base == null) {
                return null;
            }
            TenantSchemaHistoryDao.HistoryRow row = new TenantSchemaHistoryDao.HistoryRow(
                    base, "结构基线（存量库接入，此前变更视为已包含）", SchemaMigration.TYPE_BASELINE,
                    "baseline", null, true, 0, null);
            dao.insert(target, row, operator, 0L);
            applied.put("baseline", row);
            steps.add(new StepResult(biz, base, SchemaMigration.TYPE_BASELINE, "baseline", true, 0, null));
            return base;
        }
        // 误判基线自愈：账本声称「存量结构已包含」，但库内连该 biz 自身的任何建表都没有——
        // 删除基线记账并按清单补齐（脚本幂等双保险；误判来源即旧版「库里有任何业务表就基线」探针）
        if (!probeTables.isEmpty() && !hasAnyTable(target, dbType, probeTables)) {
            log.warn("[mse-schema] biz[{}] 账本基线 {} 与库内结构不符（其建表脚本的目标表均不存在，"
                    + "疑为存量库误判基线），删除基线记账并按清单补齐", biz, existing);
            dao.deleteBaseline(target);
            applied.remove("baseline");
            return null;
        }
        return existing;
    }

    /**
     * 各 biz 的基线探针表：本通道（非每租户）建表脚本的目标表 − TENANT 通道身份表 − 共享基建表。
     * 身份通道（sys_user 等）可能已在本库建出同名表（独立库懒初始化先跑身份通道），扣除后
     * BASE 通道只认自己的表（sys_menu/sys_micro_app 等），不因身份表存在而误判存量；
     * 共享基建表同理——业务脚本为租户独立库自携的 undo_log（Seata 启动即抢建）、SYS_/MSE_/ACT_/FLW_
     * 等框架/引擎表不作为 biz 存量判定的探针（口径与 {@link #hasUserTables} 排除清单一致），
     * 否则全新库会被一条与该 biz 无关的已存在表误判成存量库、整体跳过建表脚本。
     */
    private Map<String, Set<String>> probeTablesByBiz(List<SchemaMigration> migrations, MigrationTarget channel) {
        Map<String, Set<String>> identityTables = new HashMap<>();
        for (SchemaMigration m : migrations) {
            if (m.target() == MigrationTarget.TENANT && m.sql() != null) {
                extractCreatedTables(m.sql(), identityTables.computeIfAbsent(m.biz(), k -> new LinkedHashSet<>()));
            }
        }
        Map<String, Set<String>> probes = new LinkedHashMap<>();
        for (SchemaMigration m : migrations) {
            if (m.target() == channel && !m.perTenant() && m.sql() != null) {
                extractCreatedTables(m.sql(), probes.computeIfAbsent(m.biz(), k -> new LinkedHashSet<>()));
            }
        }
        for (Map.Entry<String, Set<String>> entry : probes.entrySet()) {
            entry.getValue().removeAll(identityTables.getOrDefault(entry.getKey(), Set.of()));
            entry.getValue().removeIf(TenantSchemaMigrator::isSharedInfraTable);
        }
        return probes;
    }

    /** 共享基建表（框架/引擎自建，非 biz 业务结构）：不作为存量判定的探针 */
    private static boolean isSharedInfraTable(String table) {
        return "UNDO_LOG".equals(table)
                || table.startsWith("SYS_") || table.startsWith("MSE_")
                || table.startsWith("ACT_") || table.startsWith("FLW_");
    }

    private static void extractCreatedTables(String sql, Set<String> out) {
        Matcher matcher = CREATE_TABLE.matcher(sql);
        while (matcher.find()) {
            out.add(matcher.group(1).toUpperCase(Locale.ROOT));
        }
    }

    /** 探针表存在性：目标库（schema）内是否已存在指定表中的任一张（大小写不敏感，三方言） */
    private boolean hasAnyTable(Connection target, String dbType, Set<String> tables) throws SQLException {
        if (tables.isEmpty()) {
            return false;
        }
        String inList = tables.stream()
                .map(name -> "'" + name.toUpperCase(Locale.ROOT).replace("'", "''") + "'")
                .reduce((a, b) -> a + ", " + b).orElse("' '");
        String sql = switch (dbType == null ? "mysql" : dbType.trim().toLowerCase()) {
            case "dm" -> "SELECT COUNT(*) FROM user_tables WHERE UPPER(table_name) IN (" + inList + ")";
            case "kingbase" -> "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA()"
                    + " AND table_type = 'BASE TABLE' AND UPPER(table_name) IN (" + inList + ")";
            default -> "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()"
                    + " AND table_type = 'BASE TABLE' AND UPPER(table_name) IN (" + inList + ")";
        };
        try (Statement stmt = target.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1) > 0;
        }
    }

    /** 各账本结构通道当前版本的最大值（汇总展示用） */
    private String currentVersion(Connection target, Iterable<TenantSchemaHistoryDao> daos) throws SQLException {
        String max = null;
        for (TenantSchemaHistoryDao dao : daos) {
            String version = dao.maxVersion(target);
            if (version != null && (max == null || version.compareTo(max) > 0)) {
                max = version;
            }
        }
        return max;
    }

    /** 依据 JDBC 元数据推断方言（主库迁移用：主库不经 sys_tenant_datasource 登记） */
    public static String detectDbType(Connection conn) throws SQLException {
        String product = conn.getMetaData().getDatabaseProductName().toLowerCase();
        if (product.contains("mysql") || product.contains("mariadb")) {
            return "mysql";
        }
        if (product.contains("dm ") || product.startsWith("dm")) {
            return "dm";
        }
        if (product.contains("kingbase") || product.contains("postgresql")) {
            return "kingbase";
        }
        throw new IllegalStateException("无法识别的数据库类型：" + conn.getMetaData().getDatabaseProductName());
    }

    private void executeScript(Connection target, String sql) throws SQLException {
        for (String statement : splitStatements(sql)) {
            try (Statement stmt = target.createStatement()) {
                stmt.execute(statement);
            }
        }
    }

    /**
     * 目标库是否已有「业务表」（结构基线判定）：框架自建表不计入——sys_/mse_ 前缀（基座共用表与
     * 账本）、ACT_/FLW_（Flowable 引擎表）、undo_log（Seata），避免全新库因基座结构脚本或引擎
     * 抢建表被误判为存量库。
     */
    private boolean hasUserTables(Connection target, String dbType) throws SQLException {
        String exclude = " AND UPPER(table_name) NOT LIKE 'SYS!_%' ESCAPE '!'"
                + " AND UPPER(table_name) NOT LIKE 'MSE!_%' ESCAPE '!'"
                + " AND UPPER(table_name) NOT LIKE 'ACT!_%' ESCAPE '!'"
                + " AND UPPER(table_name) NOT LIKE 'FLW!_%' ESCAPE '!'"
                + " AND UPPER(table_name) <> 'UNDO_LOG'";
        String sql = switch (dbType == null ? "mysql" : dbType.trim().toLowerCase()) {
            case "dm" -> "SELECT COUNT(*) FROM user_tables WHERE 1 = 1" + exclude;
            case "kingbase" -> "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = CURRENT_SCHEMA()"
                    + " AND table_type = 'BASE TABLE'" + exclude;
            default -> "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()"
                    + " AND table_type = 'BASE TABLE'" + exclude;
        };
        try (Statement stmt = target.createStatement(); var rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1) > 0;
        }
    }

    private static Map<String, String> effectivePlaceholders(MigrateOptions options, long tenantId) {
        Map<String, String> out = new LinkedHashMap<>();
        if (options.placeholders() != null) {
            out.putAll(options.placeholders());
        }
        out.put(SchemaPlaceholders.TENANT_ID, String.valueOf(tenantId));
        return Collections.unmodifiableMap(out);
    }

    /** 语句切分：以 ; 分隔，感知单引号字符串（含 '' 转义）；剔除空语句与纯注释语句 */
    static List<String> splitStatements(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (inString) {
                current.append(c);
                if (c == '\'') {
                    if (i + 1 < script.length() && script.charAt(i + 1) == '\'') {
                        current.append('\'');
                        i++;
                    } else {
                        inString = false;
                    }
                }
                continue;
            }
            if (c == '\'') {
                inString = true;
                current.append(c);
                continue;
            }
            if (c == ';') {
                addStatement(out, current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        addStatement(out, current.toString());
        return out;
    }

    private static void addStatement(List<String> out, String statement) {
        String trimmed = statement.trim();
        if (trimmed.isEmpty() || stripComments(trimmed).isBlank()) {
            return;
        }
        out.add(trimmed);
    }

    private static String stripComments(String sql) {
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\n", -1)) {
            int idx = line.indexOf("--");
            sb.append(idx >= 0 ? line.substring(0, idx) : line).append('\n');
        }
        return sb.toString().trim();
    }
}
