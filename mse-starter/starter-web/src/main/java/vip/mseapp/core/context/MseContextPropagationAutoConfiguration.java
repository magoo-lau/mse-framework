package vip.mseapp.core.context;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 租户上下文跨线程自动传播装配。
 *
 * <p>① 注册 {@link TenantContextPropagation}（TenantContext 租户号的 ThreadLocalAccessor）——
 * 经 micrometer ContextExecutorService / TaskDecorator 包装的线程池自动透传；
 * ② reactor-core 存在时开启 Reactor 自动上下文传播（{@code Hooks.enableAutomaticContextPropagation()}，
 * JVM 级全局开关）——SSE/Flux 订阅线程的租户快照在回调线程自动恢复，业务端异步落库免显式 runAs。
 *
 * <p>非 Reactor 应用（mp/open/im 等）零感知；IGNORE 不参与传播（刻意逃逸不得跨线程泄漏）。
 */
@AutoConfiguration
public class MseContextPropagationAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MseContextPropagationAutoConfiguration.class);

    @Bean
    TenantContextPropagationRegistrar tenantContextPropagationRegistrar() {
        return new TenantContextPropagationRegistrar();
    }

    static class TenantContextPropagationRegistrar {

        @PostConstruct
        void register() {
            TenantContextPropagation.register();
        }
    }

    /** Reactor 自动传播（仅 reactor-core 在 classpath 时装配；钩子类隔离在内部类，避免无 Reactor 应用加载失败） */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "reactor.core.publisher.Hooks")
    static class ReactorPropagationConfiguration {

        @Bean
        ReactorAutoPropagationHook reactorAutoPropagationHook() {
            return new ReactorAutoPropagationHook();
        }
    }

    static class ReactorAutoPropagationHook {

        @PostConstruct
        void enable() {
            reactor.core.publisher.Hooks.enableAutomaticContextPropagation();
            log.info("[mse-context] Reactor 自动上下文传播已开启（SSE/Flux 回调线程自动恢复租户上下文）");
        }
    }
}
