package vip.mseapp.core.schedule;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;

/**
 * 统一调度装配（Spring Boot SPI）：工具本体（SPI + LocalMseTaskScheduler）在
 * common-core {@code vip.mseapp.core.schedule}，本类为同包名装配件。
 *
 * <p>接入外部调度器（xxl-job 等）：新 starter 提供自己的 {@link MseTaskScheduler}
 * 实现（建议 {@code @AutoConfigureBefore} 本类，或配置 {@code mse.scheduling.mode}
 * 为对应值使 local 实现让位），任务声明与业务代码零改动。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(MseSchedulingProperties.class)
public class MseSchedulingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CronExpressionResolver.class)
    public SpringCronExpressionResolver mseCronExpressionResolver() {
        return new SpringCronExpressionResolver();
    }

    /**
     * 本地调度实现：{@code mse.scheduling.mode=local}（缺省）或 {@code central}（本地不排程，
     * 节奏由 mse-job 薄调度中心派发）时装配；自定义 {@link MseTaskScheduler} Bean 存在时让位
     * （换调度器的整体替换语义）。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(MseTaskScheduler.class)
    @Conditional(MseSchedulingAutoConfiguration.LocalOrCentralMode.class)
    public LocalMseTaskScheduler localMseTaskScheduler(MseSchedulingProperties properties,
                                                        ObjectProvider<TaskExecutionGuard> guardProvider,
                                                        ObjectProvider<CronExpressionResolver> cronResolverProvider) {
        TaskExecutionGuard guard = properties.isGuardEnabled() ? guardProvider.getIfAvailable() : null;
        CronExpressionResolver cronResolver = cronResolverProvider.getIfAvailable();
        java.util.List<String> disabled = properties.disabledTaskIds();
        java.util.function.Predicate<String> filter = disabled.isEmpty() ? null
                : taskId -> !disabled.contains(taskId);
        boolean central = "central".equalsIgnoreCase(properties.getMode());
        return new LocalMseTaskScheduler(properties.getPoolSize(),
                properties.getExecutorPoolSize(), properties.getExecutorQueueCapacity(),
                properties.getStartupDelay().toMillis(), guard, cronResolver, filter, central);
    }

    /** local（缺省）或 central 模式装配本地调度实现（xxl-job 等其他 mode 让位给自定义实现） */
    static class LocalOrCentralMode extends AnyNestedCondition {

        LocalOrCentralMode() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(prefix = "mse.scheduling", name = "mode", havingValue = "local", matchIfMissing = true)
        static class LocalMode {
        }

        @ConditionalOnProperty(prefix = "mse.scheduling", name = "mode", havingValue = "central")
        static class CentralMode {
        }
    }

    /** @MseScheduled 注解收集器（收集与注册分离：单例就绪后统一注册，不提早实例化调度器） */
    @Bean
    public MseScheduledAnnotationProcessor mseScheduledAnnotationProcessor(
            ConfigurableListableBeanFactory beanFactory, ObjectProvider<MseTaskScheduler> schedulerProvider) {
        return new MseScheduledAnnotationProcessor(beanFactory, schedulerProvider);
    }

    /** ApplicationReadyEvent 后统一延迟启动（晚于结构迁移等全部 Runner） */
    @Bean
    public MseSchedulingStartListener mseSchedulingStartListener(ObjectProvider<MseTaskScheduler> schedulerProvider) {
        return new MseSchedulingStartListener(schedulerProvider);
    }
}
