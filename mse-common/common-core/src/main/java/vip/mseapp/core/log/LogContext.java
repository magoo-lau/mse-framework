package vip.mseapp.core.log;

import org.slf4j.MDC;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.UserContext;

/**
 * 日志上下文（slf4j MDC 镜像）：把线程上下文中的租户/用户等关键排查信息镜像进 MDC，
 * 配合日志 pattern 的 {@code %X{key}} 输出，让每一行日志自带定位线索。
 *
 * <p>标准键与写入点：
 * <ul>
 *   <li>{@link #TRACE_ID}：链路追踪 id。HTTP 入口 TraceIdFilter 写入（透传或生成），
 *       Dubbo 链路经 RPC attachment 透传后在 provider 恢复；</li>
 *   <li>{@link #TENANT_ID} / {@link #USER_ID}：HTTP 由 TenantContextFilter 预置（header）、
 *       TenantTokenSyncInterceptor 以 token 权威覆盖，Dubbo provider 恢复 attachment 时写入，
 *       开放 API 等免登录入口在各自认证处调用 {@link #syncContext()}；</li>
 *   <li>{@link #DS}：语句级数据源标注，由 TenantDsExecutorInterceptor 在路由窗口内写入，
 *       表示「这条 SQL 实际连的库」（租户库 ds_key）；落主库（共享库/全局表/无租户）时缺席。</li>
 * </ul>
 *
 * <p>约束：MDC 是普通 ThreadLocal，不随 TransmittableThreadLocal 透传——异步线程池与
 * {@code runAs} 闭包内切换租户的场景不刷新镜像（语句级 ds 标注不受影响）；各写入点负责
 * 在出口清理，最外层过滤器/过滤器链出口统一 {@link #clear()} 兜底。
 */
public final class LogContext {

    /** 链路追踪 id（X-Trace-Id） */
    public static final String TRACE_ID = "traceId";
    /** 当前租户 id */
    public static final String TENANT_ID = "tenantId";
    /** 当前登录用户 id */
    public static final String USER_ID = "userId";
    /** 语句级数据源标注（租户库 ds_key；落主库/全局表时缺席） */
    public static final String DS = "ds";

    private LogContext() {
    }

    /** 把 TenantContext/UserContext 当前值镜像进 MDC（幂等，认证/上下文恢复后调用） */
    public static void syncContext() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            MDC.put(TENANT_ID, String.valueOf(tenantId));
        }
        Long userId = UserContext.getUserId();
        if (userId != null) {
            MDC.put(USER_ID, String.valueOf(userId));
        }
    }

    /** 清理租户/用户镜像（不动 traceId/ds，由各自写入点清理） */
    public static void clearContext() {
        MDC.remove(TENANT_ID);
        MDC.remove(USER_ID);
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /** 语句级数据源标注（路由窗口内调用，出口 {@link #removeDs()}） */
    public static void setDs(String dsKey) {
        MDC.put(DS, dsKey);
    }

    public static void removeDs() {
        MDC.remove(DS);
    }

    /** 全量清理（请求/调用出口兜底，防止线程复用串号） */
    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(TENANT_ID);
        MDC.remove(USER_ID);
        MDC.remove(DS);
    }
}
