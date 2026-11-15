package vip.mseapp.service.platform.service;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteResolver;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.mybatis.datasource.TenantDatasourceDefinition;
import vip.mseapp.mybatis.datasource.TenantDsLazyInitializer;
import vip.mseapp.mybatis.datasource.TenantDsRegistry;
import vip.mseapp.mybatis.schema.TenantSchemaMigrator;
import vip.mseapp.service.platform.entity.SysTenant;
import vip.mseapp.service.platform.mapper.SysTenantMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户换库搬迁：把单个租户在当前绑定库（含平台库残留行）中各含 tenant_id 列表的行搬移到
 * 新绑定的数据源——租户换绑 sys_tenant.ds_key 后调用，数据随绑定走。
 *
 * <p>实现约束：逐表「读出行 → 目标库先删同键行再批量插入 → 源库删除」，逐表提交可断点重跑
 * （目标侧先删后插保证幂等）；同构表直接按列名搬运（跨实例同样可用）；搬迁前对目标库触发
 * 脚本直建（TenantDsLazyInitializer），保证结构就绪。Redis 锁防并发重入。
 */
public class TenantDataMigrationService {

    private static final Logger log = LoggerFactory.getLogger(TenantDataMigrationService.class);

    private static final Duration LOCK_TTL = Duration.ofSeconds(600);

    private static final int BATCH_SIZE = 500;

    private final DynamicRoutingDataSource routingDataSource;

    private final SysTenantMapper tenantMapper;

    private final TenantDsRegistry dsRegistry;

    private final ObjectProvider<TenantDsLazyInitializer> lazyInitializerProvider;

    private final TenantRouteResolver routeResolver;

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    /** 配置文件/Nacos 默认主库的数据源 key（spring.datasource.dynamic.primary，缺省 master）：绑定它的租户数据留在默认库不迁移 */
    private final String primaryDsKey;

    public TenantDataMigrationService(DynamicRoutingDataSource routingDataSource,
                                      SysTenantMapper tenantMapper,
                                      TenantDsRegistry dsRegistry,
                                      ObjectProvider<TenantDsLazyInitializer> lazyInitializerProvider,
                                      TenantRouteResolver routeResolver,
                                      ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                      String primaryDsKey) {
        this.routingDataSource = routingDataSource;
        this.tenantMapper = tenantMapper;
        this.dsRegistry = dsRegistry;
        this.lazyInitializerProvider = lazyInitializerProvider;
        this.routeResolver = routeResolver;
        this.redisTemplateProvider = redisTemplateProvider;
        this.primaryDsKey = primaryDsKey == null || primaryDsKey.isBlank() ? "master" : primaryDsKey.trim();
    }

    /** 是否内置主库 key（绑定它的租户=共享字段隔离落本服务主库，数据本就应在默认库） */
    private boolean isBuiltinMaster(String dsKey) {
        return dsKey != null && primaryDsKey.equalsIgnoreCase(dsKey.trim());
    }

    /** 单租户换库搬迁：把该租户在当前绑定库（或平台库）的行搬到 toDsKey */
    public String migrateTenant(Long tenantId, String toDsKey) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException(404, "租户不存在");
        }
        if (toDsKey == null || toDsKey.isBlank()) {
            throw new ServiceException(400, "目标数据源不能为空");
        }
        String target = toDsKey.trim();
        if (target.equals(tenant.getDsKey())) {
            throw new ServiceException(400, "目标数据源与当前绑定相同，无需搬迁");
        }
        return withLock("mse:lock:tenant-data-migrate:" + tenantId, () -> {
            Map<Long, String> single = Map.of(tenantId, target);
            StringBuilder report = new StringBuilder();
            // 来源优先当前绑定库（存在行则搬），平台库残留行一并清扫；
            // 来源与目标同为内置主库（=本服务主库）时物理同库，跳过以免自搬自删
            List<String> sources = new ArrayList<>();
            if (tenant.getDsKey() != null && !tenant.getDsKey().isBlank()) {
                sources.add(tenant.getDsKey().trim());
            }
            sources.add(null); // null = primary（平台库）
            for (String fromDs : sources) {
                if (target.equals(fromDs) || (isBuiltinMaster(target) && (fromDs == null || isBuiltinMaster(fromDs)))) {
                    continue;
                }
                try (Connection source = connectionOf(fromDs)) {
                    for (String table : tenantIdTables(source)) {
                        long moved = migrateTable(source, table, single);
                        if (moved > 0) {
                            report.append((fromDs == null ? "平台库" : fromDs)).append('.')
                                    .append(table).append(": ").append(moved).append(" 行\n");
                        }
                    }
                } catch (SQLException e) {
                    throw new ServiceException(500, "迁移失败: " + e.getMessage());
                }
            }
            return report.isEmpty() ? "该租户无可搬迁数据" : report.toString();
        });
    }

    /**
     * 单表搬移：按 tenant_id 分组读出行 → 目标库先删同租户行再批量插入 → 源库删除已迁移行。
     * 逐表一个处理单元（读全量行到内存按租户分组；表级失败抛出阻断，已提交的表不回滚——重跑幂等）。
     */
    private long migrateTable(Connection source, String table, Map<Long, String> dsByTenant) throws SQLException {
        List<String> columns = columnNames(source, table);
        String columnList = String.join(", ", columns);
        Map<Long, List<Object[]>> rowsByTenant = new LinkedHashMap<>();
        long total = 0;
        String select = "SELECT " + columnList + " FROM " + table + " WHERE tenant_id > 0";
        try (Statement stmt = source.createStatement(); ResultSet rs = stmt.executeQuery(select)) {
            ResultSetMetaData meta = rs.getMetaData();
            int width = meta.getColumnCount();
            while (rs.next()) {
                long tenantId = rs.getLong("tenant_id");
                String dsKey = dsByTenant.get(tenantId);
                if (dsKey == null) {
                    continue; // 非本次搬迁租户的行不动（其他租户数据留在原库）
                }
                Object[] row = new Object[width];
                for (int i = 0; i < width; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                rowsByTenant.computeIfAbsent(tenantId, k -> new ArrayList<>()).add(row);
                total++;
            }
        }
        if (total == 0) {
            return 0;
        }
        // 逐租户写入目标库（先删同租户行保证幂等），全部成功后仅删源库中已搬移租户的行
        //（源库可能还住着绑定内置主库/未搬迁的其他租户，绝不误删）
        Map<Long, Connection> targets = new LinkedHashMap<>();
        try {
            String placeholders = "(" + String.join(", ", java.util.Collections.nCopies(columns.size(), "?")) + ")";
            String insert = "INSERT INTO " + table + " (" + columnList + ") VALUES " + placeholders;
            String deleteTarget = "DELETE FROM " + table + " WHERE tenant_id = ?";
            for (Map.Entry<Long, List<Object[]>> entry : rowsByTenant.entrySet()) {
                Connection target = targets.get(entry.getKey());
                if (target == null) {
                    target = connectionOf(dsByTenant.get(entry.getKey()));
                    targets.put(entry.getKey(), target);
                }
                ensureStructureReady(entry.getKey(), dsByTenant.get(entry.getKey()));
                try (PreparedStatement del = target.prepareStatement(deleteTarget)) {
                    del.setLong(1, entry.getKey());
                    del.executeUpdate();
                }
                try (PreparedStatement ins = target.prepareStatement(insert)) {
                    int pending = 0;
                    for (Object[] row : entry.getValue()) {
                        for (int i = 0; i < row.length; i++) {
                            ins.setObject(i + 1, row[i]);
                        }
                        ins.addBatch();
                        if (++pending >= BATCH_SIZE) {
                            ins.executeBatch();
                            pending = 0;
                        }
                    }
                    if (pending > 0) {
                        ins.executeBatch();
                    }
                }
            }
        } finally {
            targets.values().forEach(c -> {
                try {
                    c.close();
                } catch (SQLException ignored) {
                    // 连接关闭异常忽略
                }
            });
        }
        int deleted = 0;
        try (PreparedStatement del = source.prepareStatement("DELETE FROM " + table + " WHERE tenant_id = ?")) {
            for (Long movedTenant : rowsByTenant.keySet()) {
                del.setLong(1, movedTenant);
                deleted += del.executeUpdate();
            }
        }
        log.info("[mse-tenant-data] 表搬移完成: {}（{} 行）", table, deleted);
        return total;
    }

    /** 目标库结构就绪（触发脚本直建，幂等；失败抛出阻断搬移；内置主库已由启动迁移建好，跳过） */
    private void ensureStructureReady(Long tenantId, String dsKey) {
        if (isBuiltinMaster(dsKey)) {
            return;
        }
        TenantDsLazyInitializer initializer = lazyInitializerProvider.getIfAvailable();
        if (initializer == null) {
            return;
        }
        try {
            TenantRoute route = routeResolver.resolveRoute(tenantId, "*");
            initializer.ensureInitialized(dsKey, route);
        } catch (Exception e) {
            throw new ServiceException(500, "目标库结构初始化失败（" + dsKey + "）: " + e.getMessage());
        }
    }

    /** 主库（平台库）中含 tenant_id 列的业务表清单（排除账本/框架表；三方言） */
    private List<String> tenantIdTables(Connection conn) throws SQLException {
        String dbType = TenantSchemaMigrator.detectDbType(conn);
        String sql = switch (dbType) {
            case "dm" -> "SELECT t.TABLE_NAME FROM USER_TAB_COLUMNS t WHERE t.COLUMN_NAME = 'TENANT_ID'"
                    + " AND EXISTS (SELECT 1 FROM USER_TABLES tb WHERE tb.TABLE_NAME = t.TABLE_NAME)"
                    + " AND t.TABLE_NAME NOT LIKE 'MSE!_%' ESCAPE '!'";
            case "kingbase" -> "SELECT c.TABLE_NAME FROM information_schema.COLUMNS c"
                    + " WHERE c.TABLE_SCHEMA = CURRENT_SCHEMA() AND c.COLUMN_NAME = 'tenant_id'"
                    + " AND c.TABLE_NAME NOT LIKE 'mse!_%' ESCAPE '!'";
            default -> "SELECT c.TABLE_NAME FROM information_schema.COLUMNS c"
                    + " WHERE c.TABLE_SCHEMA = DATABASE() AND c.COLUMN_NAME = 'tenant_id'"
                    + " AND c.TABLE_NAME NOT LIKE 'mse!_%' ESCAPE '!'";
        };
        List<String> tables = new ArrayList<>();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        return tables;
    }

    private List<String> columnNames(Connection conn, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " WHERE 1 = 0")) {
            ResultSetMetaData meta = rs.getMetaData();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columns.add(meta.getColumnName(i));
            }
            return columns;
        }
    }

    private Connection primaryConnection() throws SQLException {
        return routingDataSource.getDataSources().get(primaryDsKey) != null
                ? routingDataSource.getDataSources().get(primaryDsKey).getConnection()
                : routingDataSource.getConnection();
    }

    /** 按数据源 key 取连接；null = primary（平台库） */
    private Connection connectionOf(String dsKey) throws SQLException {
        if (dsKey == null) {
            return primaryConnection();
        }
        javax.sql.DataSource ds = routingDataSource.getDataSources().get(dsKey);
        if (ds == null) {
            // 未注册（服务作用域外/未同步）：按定义即时注册后再取
            TenantDatasourceDefinition def = dsRegistry.loadDefinition(dsKey);
            if (def == null) {
                throw new ServiceException(400, "数据源定义不存在: " + dsKey);
            }
            dsRegistry.register(def);
            ds = routingDataSource.getDataSources().get(dsKey);
        }
        if (ds == null) {
            throw new ServiceException(400, "数据源注册失败（与主库同库或定义无效）: " + dsKey);
        }
        return ds.getConnection();
    }

    private <T> T withLock(String key, java.util.function.Supplier<T> action) {
        StringRedisTemplate template = redisTemplateProvider.getIfAvailable();
        // 统一锁工具（随机 token + Lua 比对删除）：杜绝固定值 "1" 与直接 delete 在锁过期被他任务
        // 接手后的误释放；无 Redis 降级进程内执行（单实例语义不变）
        String token = vip.mseapp.mybatis.schema.SchemaOpsLock.tryAcquire(template, key, LOCK_TTL);
        if (token == null) {
            throw new ServiceException(400, "迁移任务执行中，请稍后再试");
        }
        try {
            return action.get();
        } finally {
            vip.mseapp.mybatis.schema.SchemaOpsLock.release(template, key, token);
        }
    }
}
