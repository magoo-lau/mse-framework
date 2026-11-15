package vip.mseapp.starter.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import vip.mseapp.core.license.DeliverCodec;
import vip.mseapp.redis.TenantCache;
import vip.mseapp.service.system.security.LicenseHolder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 离线交付导入服务（standalone 私有环境）：部署包验签 → 导入计划 → 分阶段应用。
 *
 * <p>结构不随包携带（StandaloneBootstrap 启动已直建）；导入前置校验：
 * ①RS256 验签（与 license 同一公钥）+ 逐文件 SHA-256；②版本基线（包内 requires 与
 * 本地账本比对，不足则要求先升级介质）；③防串用（包内租户编码与本机配置一致）。
 * 数据装载经原生 JDBC（不经租户插件），按表「整表替换」（delete + JSONL 流式 insert），
 * 每表独立事务，外键依赖拓扑排序；license.token 随包落盘（60s 内热生效）。
 */
public class DeliverImportService {

    private static final Logger log = LoggerFactory.getLogger(DeliverImportService.class);

    /** 强制拒绝装载的表（与导出端排除清单同口径——签名包可信任，此处防御纵深） */
    private static final Set<String> REFUSED_TABLES = Set.of(
            "undo_log", "sys_tenant_snapshot", "sys_domain_config",
            "sys_menu", "sys_permission", "sys_micro_app",
            "sys_home_template", "sys_home_user_setting");

    /** Jackson 2：manifest 回读/行导入（版本由 Boot 4 的 jackson-2-bom 托管）——与平台侧导出端
     *  DeliverPackageService/TenantDataExporter 同轨，签名按字节验证，如切 Jackson 3 需两端成对切 */
    private static final ObjectMapper JSON = new ObjectMapper();

    private final JdbcTemplate jdbcTemplate;
    private final LicenseHolder licenseHolder;
    private final String publicKey;
    private final String licenseFile;
    private final String tenantCode;
    /** 租户行缓存（sys_tenant 不在 REFUSED_TABLES，整表替换后须失效；standalone 仅本租户一条） */
    private final ObjectProvider<TenantCache> tenantCacheProvider;
    private final long tenantId;

    public DeliverImportService(JdbcTemplate jdbcTemplate,
                                LicenseHolder licenseHolder,
                                @Value("${mse.license.public-key:}") String publicKey,
                                @Value("${mse.license.file:./conf/license.token}") String licenseFile,
                                @Value("${mse.standalone.tenant-code:tenant}") String tenantCode,
                                ObjectProvider<TenantCache> tenantCacheProvider,
                                @Value("${mse.standalone.tenant-id:1}") long tenantId) {
        this.jdbcTemplate = jdbcTemplate;
        this.licenseHolder = licenseHolder;
        this.publicKey = publicKey;
        this.licenseFile = isBlank(licenseFile) ? "./conf/license.token" : licenseFile;
        this.tenantCode = tenantCode;
        this.tenantCacheProvider = tenantCacheProvider;
        this.tenantId = tenantId;
    }

    // ===== 视图 =====

    public record TableInfo(String table, long rows) {
    }

    /** 导入计划（verify + plan 结果；ready=false 时 apply 拒绝执行） */
    public record ImportPlan(String packageType, String tenantCode, String tenantName, String licenseId,
                             boolean licenseIncluded, boolean sharedMode, List<TableInfo> tables, long totalRows,
                             List<String> checks, boolean ready, String manifestJson) {
    }

    /** 应用结果 */
    public record ApplyResult(boolean success, int appliedTables, long appliedRows,
                              String licenseState, String summary, String error) {
    }

    // ===== verify + plan（不落库） =====

    public ImportPlan plan(Path packageFile) throws IOException, SQLException {
        try (ZipFile zip = new ZipFile(packageFile.toFile())) {
            ZipEntry manifestEntry = zipEntry(zip, "manifest.json");
            ZipEntry signatureEntry = zipEntry(zip, "signature.sig");
            if (manifestEntry == null || signatureEntry == null) {
                throw new IOException("部署包缺少 manifest.json / signature.sig");
            }
            if (isBlank(publicKey)) {
                throw new IOException("本机未配置验签公钥（mse.license.public-key），无法校验部署包");
            }
            byte[] manifestBytes = zip.getInputStream(manifestEntry).readAllBytes();
            String signature = new String(zip.getInputStream(signatureEntry).readAllBytes(), StandardCharsets.UTF_8);
            DeliverCodec.verify(manifestBytes, signature, publicKey);
            Map<String, Object> manifest = JSON.readValue(manifestBytes, Map.class);
            if (!Integer.valueOf(1).equals(((Number) manifest.getOrDefault("formatVersion", 0)).intValue())) {
                throw new IOException("不支持的包格式版本: " + manifest.get("formatVersion"));
            }
            verifyFiles(zip, manifest);

            List<String> checks = new ArrayList<>();
            boolean ready = true;
            checks.add("验签：平台签名与逐文件 SHA-256 校验通过");

            String type = str(manifest, "packageType");
            Map<?, ?> tenant = map(manifest.get("tenant"));
            String pkgCode = tenant == null ? null : str(tenant, "code");

            // 防串用：包内租户编码与本机 standalone 配置一致
            if (pkgCode != null && !pkgCode.equalsIgnoreCase(tenantCode)) {
                checks.add("租户编码不符：包内 " + pkgCode + " ≠ 本机 " + tenantCode + "（拒绝导入，防串用）");
                ready = false;
            } else {
                checks.add("租户编码一致: " + pkgCode);
            }

            // 版本基线：本地账本版本 ≥ 包内 requires（不足 = 运行时介质落后于包，先升级介质）
            Map<?, ?> requires = map(manifest.get("requires"));
            Map<?, ?> perBiz = requires == null ? Map.of() : map(requires.get("perBizSchemaVersion"));
            Map<String, String> localVersions = localLedgerVersions();
            boolean versionOk = true;
            for (Map.Entry<?, ?> entry : perBiz.entrySet()) {
                String biz = String.valueOf(entry.getKey());
                String required = String.valueOf(entry.getValue());
                String local = localVersions.get(biz);
                if (local == null || local.compareTo(required) < 0) {
                    checks.add("版本基线不足: " + biz + " 本地 " + (local == null ? "无账本" : local)
                            + " < 要求 " + required + "——请先升级运行时介质");
                    versionOk = false;
                }
            }
            if (perBiz.isEmpty()) {
                checks.add("版本基线：包未声明 requires（跳过校验）");
            } else if (versionOk) {
                checks.add("版本基线：本地账本版本满足包内要求（" + perBiz.size() + " 个 biz）");
            }
            ready = ready && versionOk;

            // 菜单缺口：license 域要求的菜单在本地是否已建（仅提示——缺失菜单会被权限域过滤）
            if (licenseHolder.payload() != null && licenseHolder.payload().menuIds() != null
                    && !licenseHolder.payload().menuIds().isEmpty()) {
                List<Long> gaps = missingMenus(licenseHolder.payload().menuIds());
                checks.add(gaps.isEmpty() ? "菜单覆盖：license 域菜单在本地全部存在"
                        : "菜单缺口（本地缺失，导入后不可见）: " + gaps);
            }

            List<TableInfo> tables = readTables(manifest);
            long totalRows = tables.stream().mapToLong(TableInfo::rows).sum();
            String licenseId = str(manifest, "licenseId");
            boolean licenseIncluded = zipEntry(zip, "license.token") != null;
            checks.add(licenseIncluded ? "随包授权凭证：license.token（应用后 60s 内热生效）"
                    : "未随包签发凭证（沿用本机现有 license）");
            return new ImportPlan(type, pkgCode, tenant == null ? null : str(tenant, "name"), licenseId,
                    licenseIncluded, Boolean.TRUE.equals(manifest.get("sharedMode")),
                    tables, totalRows, List.copyOf(checks), ready, new String(manifestBytes, StandardCharsets.UTF_8));
        }
    }

    // ===== apply（license 落盘 + 数据装载，每表独立事务） =====

    public ApplyResult apply(Path packageFile, String uploadName) throws IOException, SQLException {
        ImportPlan plan = plan(packageFile);
        if (!plan.ready()) {
            recordImport(uploadName, plan, "FAILED", 0, 0, "前置校验未通过（见计划报告）");
            return new ApplyResult(false, 0, 0, licenseHolder.state().name(),
                    "前置校验未通过，未执行导入", String.join("；", plan.checks()));
        }
        int appliedTables = 0;
        long appliedRows = 0;
        String error = null;
        try {
            applyLicense(packageFile);
            try (ZipFile zip = new ZipFile(packageFile.toFile());
                 Connection conn = jdbcTemplate.getDataSource().getConnection()) {
                List<String> order = topoOrder(conn, plan.tables().stream().map(TableInfo::table).toList());
                for (String table : order) {
                    long rows = loadTable(zip, conn, table);
                    appliedTables++;
                    appliedRows += rows;
                    log.info("[mse-deliver] 表 {} 装载完成 {} 行（{}/{}）", table, rows, appliedTables, order.size());
                }
            }
        } catch (Exception e) {
            error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.error("[mse-deliver] 部署包装载失败: {}", error, e);
        }
        recordImport(uploadName, plan, error == null ? "DONE" : "FAILED", appliedTables, appliedRows, error);
        String licenseState = licenseHolder.state().name();
        if (error == null) {
            // sys_tenant 可被整表替换（不在 REFUSED_TABLES）：导入成功即失效本租户行缓存
            TenantCache tenantCache = tenantCacheProvider.getIfAvailable();
            if (tenantCache != null) {
                tenantCache.evictTenant(tenantId);
            }
            return new ApplyResult(true, appliedTables, appliedRows, licenseState,
                    "导入完成：装载 " + appliedTables + " 张表 " + appliedRows + " 行；license 状态 " + licenseState
                            + "（60s 内热生效，建议重新登录）", null);
        }
        return new ApplyResult(false, appliedTables, appliedRows, licenseState,
                "导入中断（可修复后重试，已装载表保留）", error);
    }

    /** license.token 落盘（LicenseHolder 按文件 mtime 热更新，60s 内生效） */
    private void applyLicense(Path packageFile) throws IOException {
        try (ZipFile zip = new ZipFile(packageFile.toFile())) {
            ZipEntry entry = zipEntry(zip, "license.token");
            if (entry == null) {
                return;
            }
            byte[] token = zip.getInputStream(entry).readAllBytes();
            Path target = Path.of(licenseFile);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.write(target, token);
            log.info("[mse-deliver] 授权凭证已写入 {}（{} 字节）", target, token.length);
        }
    }

    /** 按表整表替换装载：delete + JSONL 流式批量 insert，每表独立事务（不经租户插件，tenant_id 原值原样） */
    private long loadTable(ZipFile zip, Connection conn, String table) throws IOException, SQLException {
        if (REFUSED_TABLES.contains(table.toLowerCase(Locale.ROOT))
                || table.toLowerCase(Locale.ROOT).startsWith("mse_")) {
            throw new SQLException("拒绝装载受保护表: " + table);
        }
        ZipEntry entry = zipEntry(zip, "data/" + table + ".jsonl");
        if (entry == null) {
            throw new SQLException("包内缺少数据文件: data/" + table + ".jsonl");
        }
        List<String> columns = new ArrayList<>();
        List<Boolean> numeric = new ArrayList<>();
        List<Boolean> binary = new ArrayList<>();
        long rows = 0;
        conn.setAutoCommit(false);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
            Map<?, ?> header = JSON.readValue(reader.readLine(), Map.class);
            String headerTable = str(header, "table");
            if (!table.equals(headerTable)) {
                throw new SQLException("schema 头与计划不符: " + headerTable + " ≠ " + table);
            }
            for (Object col : (List<?>) header.get("columns")) {
                Map<?, ?> c = (Map<?, ?>) col;
                String name = String.valueOf(c.get("name"));
                if (!name.matches("[A-Za-z0-9_$]+")) {
                    throw new SQLException("非法列名: " + name);
                }
                columns.add(name);
                numeric.add(Boolean.TRUE.equals(c.get("numeric")));
                binary.add(Boolean.TRUE.equals(c.get("binary")));
            }
            String insert = "INSERT INTO `" + table + "` ("
                    + String.join(", ", columns.stream().map(c -> "`" + c + "`").toList())
                    + ") VALUES (" + "?, ".repeat(columns.size() - 1) + "?)";
            try (Statement del = conn.createStatement()) {
                del.executeUpdate("DELETE FROM `" + table + "`");
            }
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                String line;
                int pending = 0;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    Map<?, ?> row = JSON.readValue(line, Map.class);
                    for (int i = 0; i < columns.size(); i++) {
                        bind(ps, i + 1, row.get(columns.get(i)), numeric.get(i), binary.get(i));
                    }
                    ps.addBatch();
                    if (++pending >= 500) {
                        ps.executeBatch();
                        pending = 0;
                    }
                    rows++;
                }
                if (pending > 0) {
                    ps.executeBatch();
                }
            }
            conn.commit();
            return rows;
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /** 参数绑定：数值/布尔原样，二进制 Base64 解码，NULL → setNull，其余按字符串 */
    private void bind(PreparedStatement ps, int index, Object value, boolean numeric, boolean binary)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NULL);
            return;
        }
        if (binary) {
            ps.setBytes(index, Base64.getDecoder().decode(String.valueOf(value)));
            return;
        }
        if (numeric) {
            if (value instanceof Number || value instanceof Boolean) {
                ps.setObject(index, value);
            } else {
                ps.setObject(index, new java.math.BigDecimal(String.valueOf(value)));
            }
            return;
        }
        ps.setString(index, String.valueOf(value));
    }

    /** 外键依赖拓扑排序（Kahn；环/无依赖信息时回退字典序） */
    private List<String> topoOrder(Connection conn, List<String> tables) throws SQLException {
        Map<String, Set<String>> deps = new LinkedHashMap<>();
        DatabaseMetaData meta = conn.getMetaData();
        for (String table : tables) {
            Set<String> imported = new LinkedHashSet<>();
            try (ResultSet rs = meta.getImportedKeys(conn.getCatalog(), null, table)) {
                while (rs.next()) {
                    String pk = rs.getString("PKTABLE_NAME");
                    if (pk != null && !pk.equalsIgnoreCase(table) && tables.contains(pk)) {
                        imported.add(pk);
                    }
                }
            }
            deps.put(table, imported);
        }
        List<String> order = new ArrayList<>();
        List<String> pending = new ArrayList<>(tables);
        while (!pending.isEmpty()) {
            List<String> ready = pending.stream().filter(t -> deps.getOrDefault(t, Set.of()).stream()
                    .allMatch(order::contains)).toList();
            if (ready.isEmpty()) {
                order.addAll(pending.stream().sorted().toList());
                break;
            }
            ready.forEach(t -> {
                order.add(t);
                pending.remove(t);
            });
        }
        return order;
    }

    /** 本地各账本（mse_&lt;biz&gt;_schema_history）当前版本 */
    public Map<String, String> localLedgerVersions() throws SQLException {
        Map<String, String> out = new LinkedHashMap<>();
        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()"
                             + " AND table_name LIKE 'mse\\_%\\_schema\\_history'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String ledger = rs.getString(1).toLowerCase(Locale.ROOT);
                String biz = ledger.substring("mse_".length(), ledger.length() - "_schema_history".length());
                if (biz.isBlank() || "adhoc".equals(biz)) {
                    continue;
                }
                try (Statement stmt = conn.createStatement(); ResultSet version = stmt.executeQuery(
                        "SELECT MAX(version) FROM " + ledger + " WHERE success = 1 AND tenant_id = 0")) {
                    version.next();
                    String v = version.getString(1);
                    if (v != null) {
                        out.put(biz, v);
                    }
                }
            }
        }
        return out;
    }

    /** 本地缺失的 license 域菜单 */
    private List<Long> missingMenus(List<Long> menuIds) throws SQLException {
        String in = menuIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
        Set<Long> existing = new LinkedHashSet<>();
        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM sys_menu WHERE deleted = 0 AND id IN (" + in + ")")) {
            for (int i = 0; i < menuIds.size(); i++) {
                ps.setLong(i + 1, menuIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    existing.add(rs.getLong(1));
                }
            }
        }
        return menuIds.stream().filter(id -> !existing.contains(id)).toList();
    }

    /** 本机环境信息（导入页展示用） */
    public String localTenantCode() {
        return tenantCode;
    }

    /** 主数据源（自动导入 Runner 等待结构就绪时探测用） */
    public javax.sql.DataSource dataSource() {
        return jdbcTemplate.getDataSource();
    }

    public String licenseState() {
        return licenseHolder.state().name();
    }

    public boolean publicKeyConfigured() {
        return !isBlank(publicKey);
    }

    /** 最近导入记录（审计，最近 50 条；表未就绪返回空列表） */
    public List<Map<String, Object>> recentRecords() {
        try {
            return jdbcTemplate.queryForList(
                    "SELECT id, file_name, package_type, tenant_code, license_id, phase, applied_tables,"
                            + " applied_rows, error_msg, create_time FROM deliver_import_record ORDER BY id DESC LIMIT 50");
        } catch (Exception e) {
            log.warn("[mse-deliver] 导入记录查询失败（表未就绪？）: {}", e.getMessage());
            return List.of();
        }
    }

    /** 逐文件 SHA-256：manifest 全部区段（files/tables/deploy/software/runtime）——最终包整体完整性校验 */
    private void verifyFiles(ZipFile zip, Map<String, Object> manifest) throws IOException {
        List<Map<String, Object>> entries = new ArrayList<>();
        collectFileEntries(manifest.get("files"), entries);
        collectFileEntries(manifest.get("tables"), entries);
        collectFileEntries(manifest.get("deploy"), entries);
        collectFileEntries(manifest.get("software"), entries);
        collectFileEntries(manifest.get("runtime"), entries);
        for (Map<String, Object> entry : entries) {
            String name = String.valueOf(entry.get("file"));
            String expected = String.valueOf(entry.get("sha256"));
            ZipEntry zipEntry = zipEntry(zip, name);
            if (zipEntry == null) {
                throw new IOException("包内缺少文件: " + name);
            }
            String actual;
            try (var in = zip.getInputStream(zipEntry)) {
                actual = sha256(in);
            }
            if (!actual.equalsIgnoreCase(expected)) {
                throw new IOException("文件校验不符: " + name);
            }
        }
    }

    private void collectFileEntries(Object node, List<Map<String, Object>> out) {
        if (!(node instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> copy = new LinkedHashMap<>();
                map.forEach((k, v) -> copy.put(String.valueOf(k), v));
                out.add(copy);
            }
        }
    }

    private List<TableInfo> readTables(Map<String, Object> manifest) {
        List<Map<String, Object>> entries = new ArrayList<>();
        collectFileEntries(manifest.get("tables"), entries);
        return entries.stream()
                .map(e -> new TableInfo(String.valueOf(e.get("table")),
                        ((Number) e.getOrDefault("rows", 0)).longValue()))
                .toList();
    }

    private ZipEntry zipEntry(ZipFile zip, String name) {
        ZipEntry entry = zip.getEntry(name);
        return entry != null ? entry : zip.stream()
                .filter(e -> e.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    /** 导入审计落库（表由 MASTER 通道启动直建；原生 JDBC，失败仅日志不阻断） */
    private void recordImport(String fileName, ImportPlan plan, String phase, int tables, long rows, String error) {
        String checks = plan == null ? "" : String.join("；", plan.checks());
        try (Connection conn = jdbcTemplate.getDataSource().getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO deliver_import_record (file_name, package_type, tenant_code, license_id,"
                             + " verify_result, phase, applied_tables, applied_rows, error_msg)"
                             + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, fileName);
            ps.setString(2, plan == null ? null : plan.packageType());
            ps.setString(3, plan == null ? null : plan.tenantCode());
            ps.setString(4, plan == null ? null : plan.licenseId());
            ps.setString(5, checks.substring(0, Math.min(480, checks.length())));
            ps.setString(6, phase);
            ps.setInt(7, tables);
            ps.setLong(8, rows);
            ps.setString(9, error == null ? null : error.substring(0, Math.min(1900, error.length())));
            ps.executeUpdate();
        } catch (Exception e) {
            log.warn("[mse-deliver] 导入记录落库失败: {}", e.getMessage());
        }
    }

    private static String str(Map<?, ?> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> map ? map : null;
    }

    /** 流式 SHA-256（hex 小写） */
    private static String sha256(java.io.InputStream in) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
