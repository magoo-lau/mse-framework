package vip.mseapp.core.context;

import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.exception.ServiceException;

import java.util.function.Supplier;

/**
 * 租户上下文（全链路核心）。
 *
 * <p>存储采用 TransmittableThreadLocal：提交到经 TtlExecutors / TTL Agent 增强过的线程池时上下文可自动透传。
 * 注意：JDK 21 虚拟线程“每任务一线程”，创建时会继承 InheritableThreadLocal，但线程池复用场景仍需 TTL 包装。
 *
 * <p>使用纪律：
 * <ul>
 *   <li>HTTP 入口由 common-web 的 TenantContextFilter 写入并在 finally 清理；</li>
 *   <li>Dubbo 入口由 common-rpc 的 Provider Filter 写入；RocketMQ 消费由 common-mq 的基类写入；</li>
 *   <li>定时任务/平台管理跨租户操作必须显式 {@link #runIgnore(Supplier)} 或 {@link #setTenantId(Long)}；</li>
 *   <li>业务代码中未设置租户且未声明忽略时，SQL 层会快速失败，避免全表泄漏。</li>
 * </ul>
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new com.alibaba.ttl.TransmittableThreadLocal<>();
    /** 线程级“跳过租户隔离”开关，优先级高于 ignoreTable 白名单 */
    private static final ThreadLocal<Boolean> IGNORE = new com.alibaba.ttl.TransmittableThreadLocal<>();

    private TenantContext() {
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    /** 要求租户上下文必须存在，否则抛错——用于业务层快速失败 */
    public static Long getRequiredTenantId() {
        Long tenantId = TENANT_ID.get();
        if (tenantId == null) {
            throw new ServiceException(GlobalErrorCodeContext.TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }

    public static boolean isPlatform() {
        Long tenantId = TENANT_ID.get();
        return tenantId != null && tenantId == MseConstants.PLATFORM_TENANT_ID;
    }

    public static boolean isIgnoreTenant() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    public static void setIgnoreTenant(boolean ignore) {
        if (ignore) {
            IGNORE.set(Boolean.TRUE);
        } else {
            IGNORE.remove();
        }
    }

    /** 清理当前线程全部上下文（请求出口必须调用） */
    public static void clear() {
        TENANT_ID.remove();
        IGNORE.remove();
    }

    /** 在忽略租户隔离的语义下执行（平台管理端/定时任务遍历场景） */
    public static <T> T runIgnore(Supplier<T> action) {
        boolean old = isIgnoreTenant();
        try {
            setIgnoreTenant(true);
            return action.get();
        } finally {
            setIgnoreTenant(old);
        }
    }

    /** 在忽略租户隔离的语义下执行（无返回值版本） */
    public static void runIgnore(Runnable action) {
        boolean old = isIgnoreTenant();
        try {
            setIgnoreTenant(true);
            action.run();
        } finally {
            setIgnoreTenant(old);
        }
    }

    /** 在指定租户语义下执行（定时任务/消息消费等无请求上下文场景） */
    public static <T> T runAs(Long tenantId, Supplier<T> action) {
        Long old = getTenantId();
        try {
            setTenantId(tenantId);
            return action.get();
        } finally {
            restore(old);
        }
    }

    /** 在指定租户语义下执行（无返回值版本） */
    public static void runAs(Long tenantId, Runnable action) {
        Long old = getTenantId();
        try {
            setTenantId(tenantId);
            action.run();
        } finally {
            restore(old);
        }
    }

    /**
     * 以平台身份（tenant 0）执行读取——读平台公共行（全局模型/工具/公共智能体等）的标准原语。
     *
     * <p>与 {@link #runIgnore(Supplier)} 的区别：runIgnore 完全跳过租户改写（需自行携带隔离条件，
     * 供开放 API 鉴权等无上下文场景使用）；本原语仍受租户插件管辖，注入 {@code tenant_id = 0}，
     * 只能读到平台公共行，语义更收口。租户侧读"公共 + 本租户"合并数据 = 本原语查询 ∪ 当前上下文查询，
     */
    public static <T> T runAsPlatform(Supplier<T> action) {
        return runAs(MseConstants.PLATFORM_TENANT_ID, action);
    }

    /** 以平台身份执行（无返回值版本） */
    public static void runAsPlatform(Runnable action) {
        runAs(MseConstants.PLATFORM_TENANT_ID, action);
    }

    private static void restore(Long old) {
        if (old != null) {
            setTenantId(old);
        } else {
            TENANT_ID.remove();
        }
    }

    /** 延迟加载 ErrorCode，避免 core 模块间循环依赖的占位（见 GlobalErrorCode） */
    static final class GlobalErrorCodeContext {
        static final vip.mseapp.core.result.ErrorCode TENANT_CONTEXT_MISSING =
                vip.mseapp.core.result.GlobalErrorCode.TENANT_CONTEXT_MISSING;
    }
}
