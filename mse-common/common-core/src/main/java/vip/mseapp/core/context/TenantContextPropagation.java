package vip.mseapp.core.context;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;

/**
 * 租户上下文跨线程自动传播注册器（micrometer context-propagation 适配）。
 *
 * <p>注册后两类异步场景自动透传 {@link TenantContext} 的租户号：
 * <ul>
 *   <li>Reactor：starter-web 在 reactor-core 存在的应用开启
 *       {@code Hooks.enableAutomaticContextPropagation()}——SSE/Flux 回调线程自动恢复订阅线程的租户；</li>
 *   <li>经 micrometer {@code ContextExecutorService} / TaskDecorator（captureAll）包装的线程池：
 *       提交时快照、执行时恢复。</li>
 * </ul>
 *
 * <p>只注册租户号，不注册 IGNORE——「跳过隔离」是调用点的刻意临时语义，不得隐式跨越线程边界泄漏；
 * 无上下文线程仍按既有规则快速失败（TENANT_CONTEXT_MISSING）。
 *
 * <p>装配由 starter-web 的 MseContextPropagationAutoConfiguration 完成（common-core 只提供工具，
 * 无自动装配）；MQ 消费（租户号在消息载体）、Dubbo（Filter 透传）等既有通道不受影响、叠加生效。
 */
public final class TenantContextPropagation {

    /** 传播键（context-propagation 注册表内标识） */
    public static final String KEY = "mse.tenantId";

    private static volatile boolean registered;

    private TenantContextPropagation() {
    }

    /** 幂等注册（重复调用无副作用） */
    public static synchronized void register() {
        if (registered) {
            return;
        }
        ContextRegistry.getInstance().registerThreadLocalAccessor(new TenantIdAccessor());
        registered = true;
    }

    /** 租户号 ThreadLocal 适配器：null 语义统一为「清空本线程租户」（restore 空值走 reset） */
    private static final class TenantIdAccessor implements ThreadLocalAccessor<Long> {

        @Override
        public Object key() {
            return KEY;
        }

        @Override
        public Long getValue() {
            return TenantContext.getTenantId();
        }

        @Override
        public void setValue(Long value) {
            TenantContext.setTenantId(value);
        }

        @Override
        public void reset() {
            TenantContext.setTenantId(null);
        }
    }
}
