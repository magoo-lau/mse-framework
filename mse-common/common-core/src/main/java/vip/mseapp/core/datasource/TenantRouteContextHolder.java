package vip.mseapp.core.datasource;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 租户路由快照持有器（请求级，由 TenantDsExecutorInterceptor 在语句执行外围 set/restore，
 * 租户行级插件、分表改写等 StatementHandler 期组件读取）。
 *
 * <p>快照解析走 TenantRouteResolver 的 30s 缓存，持有器只保证同一语句内
 * 「数据源路由 / 字段隔离判定 / 分表改写」看到同一路由视图。
 */
public final class TenantRouteContextHolder {

    private static final TransmittableThreadLocal<TenantRoute> HOLDER = new TransmittableThreadLocal<>();

    private TenantRouteContextHolder() {
    }

    public static TenantRoute get() {
        return HOLDER.get();
    }

    /** 设置并返回旧值（嵌套执行用 restore 语义保护外层） */
    public static TenantRoute set(TenantRoute route) {
        TenantRoute previous = HOLDER.get();
        HOLDER.set(route);
        return previous;
    }

    public static void set(TenantRoute route, TenantRoute previous) {
        if (previous == null) {
            HOLDER.remove();
        } else {
            HOLDER.set(previous);
        }
    }

    public static void clear() {
        HOLDER.remove();
    }
}
