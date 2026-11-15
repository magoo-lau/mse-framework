package vip.mseapp.mybatis.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 版本化迁移脚本扫描器：收集 classpath 上各积木自带的迁移脚本。
 *
 * <p>位置约定 {@code classpath*:db/<方言>/V*.sql}（方言 mysql/dm/kingbase，与
 * sys_tenant_datasource.db_type 一致）；文件名约定 {@code V{yyyyMMdd_NN}_<biz>__<描述>.sql}——
 * biz 段路由账本（mse_&lt;biz&gt;_schema_history），同 biz 内版本字典序即时间序。脚本头部注释指令
 * 声明目标与粒度：{@code -- mse-target: master|base|platform|tenant}（缺省 master；tenant=租户库
 * 身份表直建通道）、{@code -- mse-per-tenant: true}（缺省 false）、{@code -- mse-base-scope: platform|tenant}
 * （缺省 tenant，仅 base 通道有意义——平台域落基座库、租户域落租户库车队）；指令只在首个语句前
 * 的注释区生效，未知指令忽略。
 * 文件名不匹配的资源 WARN 后跳过（可经 {@code mse.tenant.schema.strict-scan=true} 改为启动即失败）。
 * 结果按方言缓存（classpath 资源运行期不变）。
 */
public class TenantMigrationScanner {

    private static final Logger log = LoggerFactory.getLogger(TenantMigrationScanner.class);

    /** 缺省扫描位置（%s = 方言占位） */
    public static final String DEFAULT_LOCATION_PATTERN = "classpath*:db/%s/V*.sql";

    /** 文件名：V版本_biz__描述.sql（描述允许中文/中划线/下划线） */
    private static final Pattern NAME = Pattern.compile("V(\\d{8}_\\d{2})_([a-z][a-z0-9]*)__(.+)\\.sql");

    /** 头指令行：-- mse-xxx: value */
    private static final Pattern DIRECTIVE = Pattern.compile("^--\\s*mse-([a-z-]+):\\s*(.+?)\\s*$");

    /**
     * 基座身份表（分域护栏对象，建/写均不得出现在 base 通道脚本）：平台身份归 platform 通道
     * （平台库），租户身份归 tenant 通道（租户库）；sys_menu、sys_permission、sys_micro_app、
     * sys_home 系、sys_config、sys_dict 系为全局表或租户域默认模板，base 通道合法。
     */
    private static final Pattern IDENTITY_TABLE = Pattern.compile(
            "(?i)\\b(?:CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?|INSERT\\s+INTO\\s+|TRUNCATE\\s+(?:TABLE\\s+)?)"
                    + "(sys_user|sys_role|sys_dept|sys_user_role|sys_role_menu|sys_role_permission|sys_role_dept"
                    + "|sys_post|sys_user_post|sys_message|sys_oper_log|sys_logininfor|sys_tenant_snapshot)\\b");

    private final String locationPattern;

    private final String sourcePrefix;

    private final Map<String, List<SchemaMigration>> cache = new ConcurrentHashMap<>();

    /** 严格扫描（mse.tenant.schema.strict-scan=true）：文件名不匹配直接抛异常（CI 防呆），缺省仅 WARN */
    private final boolean strictScan;

    public TenantMigrationScanner() {
        this(DEFAULT_LOCATION_PATTERN);
    }

    public TenantMigrationScanner(String locationPattern) {
        this(locationPattern, false);
    }

    /** 指定扫描位置（缺省即可；pusher 等复用同一模式自行按 target 过滤）+ 严格模式开关 */
    public TenantMigrationScanner(String locationPattern, boolean strictScan) {
        this.locationPattern = locationPattern;
        String path = locationPattern.replace("classpath*:", "");
        this.sourcePrefix = path.substring(0, path.indexOf("%s"));
        this.strictScan = strictScan;
    }

    /** 按方言扫描（结果缓存）；未知方言/无脚本返回空列表 */
    public List<SchemaMigration> scan(String dbType) {
        String dialect = normalize(dbType);
        if (dialect == null) {
            return List.of();
        }
        return cache.computeIfAbsent(dialect, this::doScan);
    }

    /** 脚本清单覆盖的全部 biz（账本建表/基线解析用，字典序） */
    public List<String> bizList(String dbType) {
        return scan(dbType).stream().map(SchemaMigration::biz).distinct().sorted().toList();
    }

    /** 指定 biz 的已扫描脚本（无则空列表） */
    public List<SchemaMigration> scanBiz(String dbType, String biz) {
        return scan(dbType).stream().filter(m -> m.biz().equals(biz)).toList();
    }

    /** 已知最大脚本版本（无脚本返回 null），作为无历史表库的基线回退值 */
    public String maxVersion(String dbType) {
        return scan(dbType).stream().map(SchemaMigration::version).max(String::compareTo).orElse(null);
    }

    /** 指定 biz 的已知最大脚本版本（无则 null） */
    public String maxVersion(String dbType, String biz) {
        return scanBiz(dbType, biz).stream().map(SchemaMigration::version).max(String::compareTo).orElse(null);
    }

    /** 重新扫描（管理端需要强制刷新时用；常规流程不需要） */
    public void clear() {
        cache.clear();
    }

    private List<SchemaMigration> doScan(String dialect) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<SchemaMigration> migrations = new ArrayList<>();
        try {
            for (Resource resource : resolver.getResources(String.format(locationPattern, dialect))) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                var matcher = NAME.matcher(filename);
                if (!matcher.matches()) {
                    // 扫描模式已限定 db/<方言>/V*.sql——能进来的都是「疑似版本脚本」，命名不符即
                    // 该脚本永不执行（账本零记录、无报错），第三方接入最易踩：必须显式告警而非静默跳过
                    if (strictScan) {
                        throw new IllegalStateException("迁移脚本文件名不符合约定（mse.tenant.schema.strict-scan=true）: "
                                + resource.getDescription() + "，期望 V{yyyyMMdd_NN}_<biz>__<描述>.sql");
                    }
                    log.warn("[mse-schema] 迁移脚本文件名不符合命名约定，已跳过（该脚本不会被执行）: {}，"
                            + "期望 V{{yyyyMMdd_NN}}_<biz>__<描述>.sql（biz 段须为小写积木名，详见 docs/dev/sql.md）",
                            resource.getDescription());
                    continue;
                }
                String sql = read(resource);
                Map<String, String> directives = parseDirectives(sql);
                SchemaMigration migration = new SchemaMigration(matcher.group(2), matcher.group(1), matcher.group(3),
                        SchemaMigration.TYPE_CLASSPATH, MigrationTarget.of(directives.get("target")),
                        "true".equalsIgnoreCase(directives.getOrDefault("per-tenant", "false")),
                        sourcePrefix + dialect + "/" + filename, sql, checksum(sql),
                        BaseScope.of(directives.get("base-scope")));
                if (migration.target() == MigrationTarget.BASE) {
                    warnIdentityInBase(migration);
                }
                migrations.add(migration);
            }
        } catch (IOException e) {
            throw new IllegalStateException("扫描迁移脚本失败: " + sourcePrefix + dialect, e);
        }
        // 基座（system）先行：业务脚本可能引用 system 更高版本号脚本新加的列（如 oa 移动端菜单引用
        // system menu_client 的 client 列），纯版本号全局排序会在全新库上颠倒依赖顺序
        migrations.sort(Comparator
                .comparing((SchemaMigration m) -> "system".equals(m.biz()) ? 0 : 1)
                .thenComparing(SchemaMigration::version)
                .thenComparing(SchemaMigration::script));
        return List.copyOf(migrations);
    }

    /**
     * 解析脚本头指令：仅识别首个非注释语句前的注释区（{@code -- mse-xxx: value}），
     * 键名去掉 mse- 前缀；未知指令忽略（前向扩展）。
     */
    public static Map<String, String> parseDirectives(String sql) {
        Map<String, String> out = new LinkedHashMap<>();
        if (sql == null) {
            return out;
        }
        for (String line : sql.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!trimmed.startsWith("--")) {
                break;
            }
            var matcher = DIRECTIVE.matcher(trimmed);
            if (matcher.matches()) {
                out.putIfAbsent(matcher.group(1), matcher.group(2));
            }
        }
        return out;
    }

    /**
     * 分域护栏：base 通道脚本建/写基座身份表 = 分域分类错误（平台身份应走 platform 通道落平台库、
     * 租户身份应走 tenant 通道落租户库），告警提示人工核对（不阻断——脚本幂等，历史误分类脚本
     * 靠人工清理，护栏防新增）。
     */
    private static void warnIdentityInBase(SchemaMigration migration) {
        Set<String> tables = findIdentityTables(migration.sql());
        if (!tables.isEmpty()) {
            log.warn("[mse-schema] base 通道脚本 {} 引用基座身份表 {}——平台身份归 platform 通道（平台库）、"
                    + "租户身份归 tenant 通道（租户库），疑为分域分类错误，请核对脚本归属",
                    migration.script(), tables);
        }
    }

    /** 提取脚本中建/写基座身份表的表名（大写；无则空集） */
    static Set<String> findIdentityTables(String sql) {
        if (sql == null || sql.isEmpty()) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        var matcher = IDENTITY_TABLE.matcher(sql);
        while (matcher.find()) {
            out.add(matcher.group(1).toUpperCase(Locale.ROOT));
        }
        return out;
    }

    private static String read(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("读取迁移脚本失败: " + resource.getFilename(), e);
        }
    }

    /** 规范化文本（\r 归一 + trim）的 SHA-256 */
    public static String checksum(String sql) {
        String normalized = sql.replace("\r\n", "\n").replace('\r', '\n').trim();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static String normalize(String dbType) {
        if (dbType == null) {
            return null;
        }
        String dialect = dbType.trim().toLowerCase(Locale.ROOT);
        return switch (dialect) {
            case "mysql", "dm", "kingbase" -> dialect;
            default -> null;
        };
    }
}
