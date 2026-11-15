package vip.mseapp.mybatis.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.datasource.TablePolicy;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteContextHolder;
import vip.mseapp.mybatis.config.MseTenantProperties;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

/**
 * 租户行级隔离策略：
 * <ul>
 *   <li>租户上下文缺失且未声明忽略时，任何 SQL 快速失败（防止无隔离条件全表读写）；</li>
 *   <li>线程级 ignore 开关（平台端/定时任务）优先级最高；</li>
 *   <li>全局表白名单（租户表、菜单、Seata undo_log 等）跳过改写；</li>
 *   <li>路由快照感知（多模式）：独立库模式整库隔离不改写；表级策略 SPLIT/GLOBAL 不追加
 *       字段条件，仅 FIELD 表追加；无快照（组件未覆盖的执行路径）保持现状字段隔离行为。</li>
 * </ul>
 */
public class MseTenantLineHandler implements TenantLineHandler {

    private final MseTenantProperties properties;

    public MseTenantLineHandler(MseTenantProperties properties) {
        this.properties = properties;
    }

    @Override
    public Expression getTenantId() {
        // 走到此处说明该语句需要租户隔离：上下文缺失即失败，绝不静默放行
        return new LongValue(TenantContext.getRequiredTenantId());
    }

    @Override
    public String getTenantIdColumn() {
        TenantRoute route = TenantRouteContextHolder.get();
        return route != null ? route.getTenantColumn() : properties.getTenantIdColumn();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        if (TenantContext.isIgnoreTenant()) {
            return true;
        }
        if (tableName == null) {
            return true;
        }
        String normalized = normalize(tableName);
        if (properties.getIgnoreTables().contains(normalized)) {
            return true;
        }
        // 结构迁移账本（按 biz 分册）：mse_<biz>_schema_history 通配豁免
        if (normalized.startsWith("mse_") && normalized.endsWith("_schema_history")) {
            return true;
        }
        TenantRoute route = TenantRouteContextHolder.get();
        if (route == null) {
            // 无路由快照（快照由 Executor 期装配，正常语句级执行必然存在）：保持字段隔离现状
            return false;
        }
        if (route.independentDb()) {
            // 独立库：整库即租户私有，无需字段条件
            return true;
        }
        // 共享模式：仅 FIELD 表追加字段条件（SPLIT 靠表名区分、GLOBAL 不隔离）
        return route.effectiveMode(normalized) != TablePolicy.Mode.FIELD;
    }

    private String normalize(String tableName) {
        return tableName.toLowerCase().replace("`", "").replace("\"", "").trim();
    }
}
