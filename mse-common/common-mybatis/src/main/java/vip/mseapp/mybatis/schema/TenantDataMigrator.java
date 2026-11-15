package vip.mseapp.mybatis.schema;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每租户数据迁移器（数据面）：把 classpath 各积木 per-tenant 脚本应用到指定租户——共享库租户在
 * 缺省共享库（登记表 is_default 行的动态池）执行 INSERT（占位符 ${tenant_id} 注入实际租户），
 * 独立库租户由懒初始化的同通道在租户库内覆盖。账本按 (script, tenant) 记账，天然支持
 * 「新增种子脚本后存量租户增量补跑」。
 *
 * <p>触发源（均在数据面各服务执行，控制面不直连业务库）：开通事件（tenant.created）、管理端手动
 * 升级广播（tenant.data-upgrade）、auto-upgrade 启动扫捕。运行期访问零依赖平台服务。
 */
public class TenantDataMigrator {

    private static final Logger log = LoggerFactory.getLogger(TenantDataMigrator.class);

    private static final Duration LOCK_TTL = Duration.ofSeconds(600);

    private final ObjectProvider<DynamicRoutingDataSource> routingProvider;

    private final ObjectProvider<vip.mseapp.mybatis.datasource.TenantDsRegistry> registryProvider;

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    private final TenantSchemaMigrator migrator;

    private final Map<String, String> placeholders;

    private final ConcurrentHashMap<String, Object> DB_LOCKS = new ConcurrentHashMap<>();

    public TenantDataMigrator(ObjectProvider<DynamicRoutingDataSource> routingProvider,
                              ObjectProvider<vip.mseapp.mybatis.datasource.TenantDsRegistry> registryProvider,
                              ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                              TenantSchemaMigrator migrator,
                              Map<String, String> placeholders) {
        this.routingProvider = routingProvider;
        this.registryProvider = registryProvider;
        this.redisTemplateProvider = redisTemplateProvider;
        this.migrator = migrator;
        this.placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);
    }

    /** 为单个租户应用 pending 每租户脚本（幂等，账本续跑）；失败抛异常由调用方决定重试策略 */
    public TenantSchemaMigrator.Result migrateTenant(long tenantId) throws SQLException {
        DynamicRoutingDataSource routing = routingProvider.getIfAvailable();
        if (routing == null) {
            throw new IllegalStateException("dynamic-datasource 未装配，无法执行每租户数据迁移");
        }
        // 共享租户的每租户种子落本服务内置共享库（mse.tenant.shared-main.* 自declare）；primary=平台库不可承载
        vip.mseapp.mybatis.datasource.TenantDsRegistry registry = registryProvider.getIfAvailable();
        String sharedKey = registry == null ? null : registry.builtinKey();
        if (sharedKey == null) {
            throw new IllegalStateException("本服务未配置内置共享库（mse.tenant.shared-main.*），"
                    + "共享租户每租户种子无处执行: tenant=" + tenantId);
        }
        DataSource sharedDs = routing.getDataSources().get(sharedKey);
        if (sharedDs == null) {
            throw new IllegalStateException("缺省共享库连接池未注册（等待启动注册/广播回查）: key=" + sharedKey);
        }
        try (Connection master = sharedDs.getConnection()) {
            String dbType = TenantSchemaMigrator.detectDbType(master);
            // 库粒度统一锁（与启动迁移/懒初始化/管理端结构操作同一把）：同库结构操作跨实例串行
            String lockKey = SchemaOpsLock.lockKeyFor(master);
            Object lock = DB_LOCKS.computeIfAbsent(lockKey, k -> new Object());
            synchronized (lock) {
                String token;
                try {
                    token = SchemaOpsLock.acquire(redisTemplateProvider.getIfAvailable(), lockKey,
                            LOCK_TTL, 2000, LOCK_TTL);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("每租户数据迁移等待锁被中断: " + lockKey, e);
                }
                try {
                    return migrator.migrate(master, dbType, MigrateOptions.forTenant(tenantId, placeholders));
                } finally {
                    SchemaOpsLock.release(redisTemplateProvider.getIfAvailable(), lockKey, token);
                }
            }
        }
    }

    /** 批量补跑（存量租户升级）：逐租户执行，失败不中断，返回失败租户清单 */
    public List<Long> migrateAll(List<Long> tenantIds) {
        List<Long> failed = new ArrayList<>();
        for (Long tenantId : tenantIds) {
            if (tenantId == null) {
                continue;
            }
            try {
                TenantSchemaMigrator.Result result = migrateTenant(tenantId);
                if (result.applied() > 0 || result.failed() > 0 || result.conflicts() > 0) {
                    log.info("[mse-schema] 租户 {} 每租户数据迁移: 应用 {}、失败 {}、冲突 {}",
                            tenantId, result.applied(), result.failed(), result.conflicts());
                }
                if (result.failed() > 0 || result.conflicts() > 0) {
                    failed.add(tenantId);
                }
            } catch (Exception e) {
                log.warn("[mse-schema] 租户 {} 每租户数据迁移失败: {}", tenantId, e.getMessage());
                failed.add(tenantId);
            }
        }
        return failed;
    }
}
