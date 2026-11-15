package vip.mseapp.mybatis.datasource;

import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import vip.mseapp.core.datasource.TablePolicy;
import vip.mseapp.core.datasource.TenantRoute;
import vip.mseapp.core.datasource.TenantRouteContextHolder;

/**
 * 共享库分表改写（SHARED_TABLE_SPLIT / 表级 SPLIT 策略）：逻辑表名追加租户后缀
 * （如 order → order_t001）。
 *
 * <p>注册顺序必须在租户行级插件之后：行级插件按「逻辑表名」判定 GLOBAL/SPLIT/FIELD，
 * 改写发生在其之后才能保证白名单与表级策略看到的是逻辑名。表名不一致的 SPLIT 表
 * 无法跨租户 UNION 查询，聚合需应用层或独立汇总库（见 docs 已知限制）。
 */
public class TenantSplitTableInterceptor extends DynamicTableNameInnerInterceptor {

    public TenantSplitTableInterceptor() {
        setTableNameHandler((sql, tableName) -> rewrite(tableName));
    }

    private String rewrite(String tableName) {
        TenantRoute route = TenantRouteContextHolder.get();
        if (route == null || route.independentDb()) {
            return tableName;
        }
        String normalized = tableName.toLowerCase().replace("`", "").replace("\"", "").trim();
        if (route.effectiveMode(normalized) != TablePolicy.Mode.SPLIT) {
            return tableName;
        }
        String suffix = route.effectiveSuffix(normalized);
        return suffix == null || suffix.isBlank() ? tableName : tableName + suffix;
    }
}
