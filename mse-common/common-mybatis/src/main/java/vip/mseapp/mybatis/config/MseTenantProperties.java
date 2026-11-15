package vip.mseapp.mybatis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 多租户配置（mse.tenant.*）。
 */
@ConfigurationProperties(prefix = "mse.tenant")
public class MseTenantProperties {

    /** 是否启用租户 SQL 隔离（独立库部署的服务可关闭） */
    private boolean enabled = true;

    /** 租户判别列名 */
    private String tenantIdColumn = "tenant_id";

    /**
     * 跳过租户隔离的表（全局表）。表名不区分大小写，会同时去掉反引号/双引号再比对。
     */
    private Set<String> ignoreTables = defaultIgnoreTables();

    /**
     * 动态租户数据源注册（mse.tenant.dynamic-ds.*）：sys_tenant_datasource 表中
     * 界面维护的租户库（共享库/独立库）启动加载 + 运行时增删 + Redis 变更广播。
     */
    private DynamicDs dynamicDs = new DynamicDs();

    /** 内置缺省共享库（每服务自declare：键默认=本服务应用名，显式 key:* =通用） */
    private SharedMain sharedMain = new SharedMain();

    /**
     * 租户快照签名/加密密钥（mse.tenant.snapshot-key）：控制面与数据面共享的口令，
     * 快照 AES-GCM 加密 + HMAC-SHA256 防篡改；默认仅供本地开发，生产必须经配置中心更换。
     */
    private String snapshotKey = "mse-snapshot-dev-key-2026";

    /**
     * 数据面快照 L1 TTL（mse.tenant.snapshot-ttl-ms，默认 60s，0=永不过期）：事件/版本校验之外的
     * 最终一致兜底——控制面改路由/表策略后，数据面进程内快照最迟本时长后过期回源重建。
     */
    private long snapshotTtlMs = 60_000;

    /** 数据面快照 L1 容量上限（mse.tenant.snapshot-max-entries，默认 10000，0=不限）：防随租户数无界增长 */
    private int snapshotMaxEntries = 10_000;

    /**
     * 快照版本号节流校验间隔（mse.tenant.snapshot-version-check-interval-ms，默认 5s，0=禁用）：
     * 命中内存快照后按此间隔读 Redis 租户版本号（与控制面失效 INCR 同源）比对，不等即失效重建——
     * 无 MQ 部署下策略变更秒级收敛，不等事件到达。
     */
    private long snapshotVersionCheckIntervalMs = 5_000;

    /**
     * 结构迁移（mse.tenant.schema.*）：启动时自动应用 classpath 各积木自带的
     * db/&lt;方言&gt;/V*.sql 与 Java 迁移（账本 mse_&lt;biz&gt;_schema_history，按 biz 分册）。
     */
    private Schema schema = new Schema();

    /**
     * 本服务名（mse.tenant.service-name，路由元数据按 (租户, 服务) 解析）。
     * 缺省回退 spring.application.name；两者皆空 = '*'（仅匹配全局默认行，单体语义）。
     */
    private String serviceName = "";

    /**
     * 服务名 → 包前缀映射（mse.tenant.service-mappings.mse-oa=vip.mseapp.oa）：
     * 单体聚合多业务模块时按 mapper 包前缀解析各语句归属的服务（最长前缀匹配）；
     * 微服务形态无需配置（整服务即一个模块）。
     */
    private Map<String, String> serviceMappings = new LinkedHashMap<>();

    /**
     * 事务绑定库与语句目标库不一致时的处置（mse.tenant.tx-ds-mismatch，默认 fail）：
     * 本地事务在 doBegin 按「应用缺省服务」钉死连接库，事务内语句按 mapper 所属服务解析的
     * 目标库不同时将静默复用绑定连接落错库（单体聚合多模块跨库场景）——fail 首条语句快速报错
     * （推荐；AGENTS 红线：一次本地事务只绑一个库，跨库须拆分方法/去事务化/@GlobalTransactional）；
     * warn 仅告警不阻断（升级观察期用）。
     */
    private String txDsMismatch = "fail";

    public String getTxDsMismatch() {
        return txDsMismatch;
    }

    public void setTxDsMismatch(String txDsMismatch) {
        this.txDsMismatch = txDsMismatch;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Map<String, String> getServiceMappings() {
        return serviceMappings;
    }

    public void setServiceMappings(Map<String, String> serviceMappings) {
        this.serviceMappings = serviceMappings;
    }

    private static Set<String> defaultIgnoreTables() {
        Set<String> tables = new LinkedHashSet<>();
        // 租户表自身（其主键即租户维度，不能被改写）
        tables.add("sys_tenant");
        tables.add("sys_tenant_package");
        // 平台级全局表
        tables.add("sys_menu");
        tables.add("sys_permission");
        tables.add("sys_domain_config");
        tables.add("sys_micro_app");
        // 门户首页：模板（scope_type + tenant_id 自行区分范围）与用户个性化（owner + user_id 定位）
        tables.add("sys_home_template");
        tables.add("sys_home_user_setting");
        // Seata AT 模式回滚日志表
        tables.add("undo_log");
        // 结构迁移历史表（每库一张，仅原生 JDBC 访问；预防未来经 mapper 查询时被租户改写）
        tables.add("mse_schema_history");
        return tables;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTenantIdColumn() {
        return tenantIdColumn;
    }

    public void setTenantIdColumn(String tenantIdColumn) {
        this.tenantIdColumn = tenantIdColumn;
    }

    public Set<String> getIgnoreTables() {
        return ignoreTables;
    }

    public void setIgnoreTables(Set<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }

    public SharedMain getSharedMain() {
        return sharedMain;
    }

    public void setSharedMain(SharedMain sharedMain) {
        this.sharedMain = sharedMain;
    }

    public DynamicDs getDynamicDs() {
        return dynamicDs;
    }

    public void setDynamicDs(DynamicDs dynamicDs) {
        this.dynamicDs = dynamicDs;
    }

    public String getSnapshotKey() {
        return snapshotKey;
    }

    public void setSnapshotKey(String snapshotKey) {
        this.snapshotKey = snapshotKey;
    }

    public long getSnapshotTtlMs() {
        return snapshotTtlMs;
    }

    public void setSnapshotTtlMs(long snapshotTtlMs) {
        this.snapshotTtlMs = snapshotTtlMs;
    }

    public int getSnapshotMaxEntries() {
        return snapshotMaxEntries;
    }

    public void setSnapshotMaxEntries(int snapshotMaxEntries) {
        this.snapshotMaxEntries = snapshotMaxEntries;
    }

    public long getSnapshotVersionCheckIntervalMs() {
        return snapshotVersionCheckIntervalMs;
    }

    public void setSnapshotVersionCheckIntervalMs(long snapshotVersionCheckIntervalMs) {
        this.snapshotVersionCheckIntervalMs = snapshotVersionCheckIntervalMs;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    /** 动态租户数据源注册配置 */
    public static class DynamicDs {

        /** 是否启用（关闭后 sys_tenant_datasource 不加载，仅 yaml 静态数据源生效） */
        private boolean enabled = true;

        /**
         * 连接密码加密密钥（AES）。默认仅供本地开发，生产必须通过环境变量/配置中心更换——
         * 更换后需在「租户数据源」界面重新保存各数据源密码。
         */
        private String encryptKey = "mse-ds-dev-key-2026";

        /**
         * 租户库懒初始化（动态开通链路第 3 级）：首次路由命中未初始化的目标库时，
         * 从本服务主库克隆租户业务表结构 + undo_log（SPLIT 表按策略加后缀）。
         */
        private boolean lazyInit = true;

        /** 动态租户/共享库连接池上限（防总连接数爆炸；每租户/共享库小池） */
        private int maxPoolSize = 5;

        private int minIdle = 1;

        /** 空闲回收毫秒（5min） */
        private long idleTimeoutMs = 300_000;

        /** 连接最大生命周期毫秒（30min） */
        private long maxLifetimeMs = 1_800_000;

        /** 取连接超时毫秒 */
        private long connectionTimeoutMs = 10_000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEncryptKey() {
            return encryptKey;
        }

        public void setEncryptKey(String encryptKey) {
            this.encryptKey = encryptKey;
        }

        public boolean isLazyInit() {
            return lazyInit;
        }

        public void setLazyInit(boolean lazyInit) {
            this.lazyInit = lazyInit;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getMinIdle() {
            return minIdle;
        }

        public void setMinIdle(int minIdle) {
            this.minIdle = minIdle;
        }

        public long getIdleTimeoutMs() {
            return idleTimeoutMs;
        }

        public void setIdleTimeoutMs(long idleTimeoutMs) {
            this.idleTimeoutMs = idleTimeoutMs;
        }

        public long getMaxLifetimeMs() {
            return maxLifetimeMs;
        }

        public void setMaxLifetimeMs(long maxLifetimeMs) {
            this.maxLifetimeMs = maxLifetimeMs;
        }

        public long getConnectionTimeoutMs() {
            return connectionTimeoutMs;
        }

        public void setConnectionTimeoutMs(long connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }
    }

    /** 内置缺省共享库配置（mse.tenant.shared-main.*）：每服务自declare，键默认=本服务应用名、显式 key:* =通用 */
    public static class SharedMain {

        /** JDBC URL（空=本服务无内置共享库，共享租户语句快速失败） */
        private String url = "";

        private String username = "";

        private String password = "";

        /** 数据库类型 mysql/dm/kingbase */
        private String dbType = "mysql";

        /** 注册键：空=本服务应用名（服务私有共享库）；'*'=通用（跨服务共用一个共享库时显式配置） */
        private String key = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDbType() {
            return dbType;
        }

        public void setDbType(String dbType) {
            this.dbType = dbType;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }

    /** 结构迁移配置（mse.tenant.schema.*） */
    public static class Schema {

        /**
         * 启动自动迁移（默认开启）：应用就绪前把 classpath 各积木自带的 db/&lt;方言&gt;/V*.sql 与
         * Java 迁移应用到目标库（master=本服务主库、base=基座库（仅基座应用）、platform=平台库），
         * 免手工执行 init SQL；失败抛异常阻断启动（fail-fast，账本 mse_&lt;biz&gt;_schema_history 可查）。
         */
        private boolean autoMigrate = true;

        /** 平台控制面库的数据源 key（mse.tenant.schema.platform-ds）：dynamic-datasource 注册同名数据源即启用平台库 */
        private String platformDs = "platform";

        /**
         * 严格扫描（默认关闭）：classpath db/&lt;方言&gt;/V*.sql 中文件名不符合
         * {@code V{yyyyMMdd_NN}_<biz>__<描述>.sql} 约定的资源默认 WARN 后跳过；开启后扫描即抛异常
         * 阻断启动——第三方模块接入防呆（脚本永不执行且零报错最易踩，CI 环境建议开启）。
         */
        private boolean strictScan = false;

        /** 缺省共享库启动 eager 建 TENANT 身份表（保证车队 BASE 重放不被 identityReady 跳过；MASTER 业务表恒懒） */
        private boolean sharedMainEagerInit = true;

        /**
         * 自定义脚本占位符（mse.tenant.schema.placeholders.xxx=yyy）：脚本 {@code ${key}} 替换；
         * 内置 {@code tenant_id} 自动注入（结构通道=0、每租户通道=实际租户）。
         */
        private Map<String, String> placeholders = new LinkedHashMap<>();

        /** 每租户数据迁移（mse.tenant.schema.tenant-data.*） */
        private TenantData tenantData = new TenantData();

        public boolean isAutoMigrate() {
            return autoMigrate;
        }

        public void setAutoMigrate(boolean autoMigrate) {
            this.autoMigrate = autoMigrate;
        }

        public String getPlatformDs() {
            return platformDs;
        }

        public void setPlatformDs(String platformDs) {
            this.platformDs = platformDs;
        }

        public boolean isStrictScan() {
            return strictScan;
        }

        public void setStrictScan(boolean strictScan) {
            this.strictScan = strictScan;
        }

        public boolean isSharedMainEagerInit() {
            return sharedMainEagerInit;
        }

        public void setSharedMainEagerInit(boolean sharedMainEagerInit) {
            this.sharedMainEagerInit = sharedMainEagerInit;
        }

        public Map<String, String> getPlaceholders() {
            return placeholders;
        }

        public void setPlaceholders(Map<String, String> placeholders) {
            this.placeholders = placeholders;
        }

        public TenantData getTenantData() {
            return tenantData;
        }

        public void setTenantData(TenantData tenantData) {
            this.tenantData = tenantData;
        }
    }

    /** 每租户数据迁移配置（mse.tenant.schema.tenant-data.*） */
    public static class TenantData {

        /**
         * 启动时自动为全部已知租户补跑 pending 每租户脚本（默认关闭——主路径为开通事件 +
         * 管理端手动升级广播；无 MQ 的微服务形态可开启此项自愈）。
         */
        private boolean autoUpgrade = false;

        /**
         * 运行期周期补偿（默认开启）：每 compensate-interval-ms 拉全量租户，为「共享库租户且
         * 账本未就绪」者补跑 pending 每租户脚本（独立库租户由懒初始化直建覆盖），每轮至多
         * compensate-batch-size 个；另经租户快照首次加载触发秒级补偿——共享库新建租户不再
         * 依赖 MQ 事件到达与启动扫捕。compensate-interval-ms<=0 关闭周期通道（首触仍生效）。
         */
        private boolean compensateEnabled = true;

        /** 周期补偿间隔（毫秒，默认 5 分钟） */
        private long compensateIntervalMs = 300_000;

        /** 每轮补偿租户上限（默认 20；账本幂等检查轻量，防大租户量下单轮过久） */
        private int compensateBatchSize = 20;

        /** doneSet 全量重查间隔（毫秒，默认 1 小时）：让新增脚本版本也能周期重查已确认租户 */
        private long compensateRecheckIntervalMs = 3_600_000;

        public boolean isAutoUpgrade() {
            return autoUpgrade;
        }

        public void setAutoUpgrade(boolean autoUpgrade) {
            this.autoUpgrade = autoUpgrade;
        }

        public boolean isCompensateEnabled() {
            return compensateEnabled;
        }

        public void setCompensateEnabled(boolean compensateEnabled) {
            this.compensateEnabled = compensateEnabled;
        }

        public long getCompensateIntervalMs() {
            return compensateIntervalMs;
        }

        public void setCompensateIntervalMs(long compensateIntervalMs) {
            this.compensateIntervalMs = compensateIntervalMs;
        }

        public int getCompensateBatchSize() {
            return compensateBatchSize;
        }

        public void setCompensateBatchSize(int compensateBatchSize) {
            this.compensateBatchSize = compensateBatchSize;
        }

        public long getCompensateRecheckIntervalMs() {
            return compensateRecheckIntervalMs;
        }

        public void setCompensateRecheckIntervalMs(long compensateRecheckIntervalMs) {
            this.compensateRecheckIntervalMs = compensateRecheckIntervalMs;
        }
    }
}
