package vip.mseapp.core.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link MseScheduled} 注解收集器：BeanPostProcessor 阶段只收集（bean, method）对，
 * 全部单例就绪后（SmartInitializingSingleton，此时尚未到 ApplicationReadyEvent，
 * 也不会过早实例化调度器）统一注册到当前 {@link MseTaskScheduler}。
 *
 * <p>调度器未装配（{@code mse.scheduling.mode=none} / 宿主未引 starter-web 装配）时
 * 打印提示后放弃注册（任务不跑，应用其余能力不受影响）。属性值支持 {@code ${...}}
 * 占位符（与 @Scheduled 的 *String 属性同语义），经容器内嵌值解析器求值。
 */
public class MseScheduledAnnotationProcessor implements BeanPostProcessor, SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(MseScheduledAnnotationProcessor.class);

    private final ConfigurableListableBeanFactory beanFactory;

    private final ObjectProvider<MseTaskScheduler> schedulerProvider;

    private final java.util.List<PendingRegistration> pending = new CopyOnWriteArrayList<>();

    private record PendingRegistration(Object bean, String beanName, Method method, MseScheduled annotation) {
    }

    public MseScheduledAnnotationProcessor(ConfigurableListableBeanFactory beanFactory,
                                           ObjectProvider<MseTaskScheduler> schedulerProvider) {
        this.beanFactory = beanFactory;
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        Map<Method, MseScheduled> methods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<MseScheduled>) method ->
                        AnnotatedElementUtils.findMergedAnnotation(method, MseScheduled.class));
        if (methods.isEmpty()) {
            return bean;
        }
        for (Map.Entry<Method, MseScheduled> entry : methods.entrySet()) {
            Method method = entry.getKey();
            if (method.isSynthetic() || method.getParameterCount() > 0) {
                log.error("[mse-schedule] @MseScheduled 方法必须无参且非合成（{}.{})，跳过",
                        targetClass.getSimpleName(), method.getName());
                continue;
            }
            method.setAccessible(true);
            pending.add(new PendingRegistration(bean, beanName, method, entry.getValue()));
        }
        return bean;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (pending.isEmpty()) {
            return;
        }
        MseTaskScheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            log.info("[mse-schedule] 调度器未装配（mse.scheduling.mode=none 或自定义实现缺位），"
                    + "{} 个 @MseScheduled 任务不注册", pending.size());
            pending.clear();
            return;
        }
        for (PendingRegistration registration : pending) {
            register(scheduler, registration);
        }
        pending.clear();
    }

    private void register(MseTaskScheduler scheduler, PendingRegistration registration) {
        MseScheduled annotation = registration.annotation();
        String taskId = annotation.taskId();
        try {
            TaskDefinition definition = buildDefinition(taskId, annotation);
            scheduler.register(definition, () -> invoke(registration));
        } catch (Exception e) {
            log.error("[mse-schedule] 任务 {} 注册失败（{}.{}）: {}",
                    taskId, registration.bean().getClass().getSimpleName(), registration.method().getName(), e.getMessage());
        }
    }

    private TaskDefinition buildDefinition(String taskId, MseScheduled annotation) {
        String name = annotation.name().isBlank() ? taskId : annotation.name();
        String description = resolve(annotation.description());
        String category = resolve(annotation.category());
        if (annotation.startup()) {
            return TaskDefinition.startup(taskId, name, description).category(category);
        }
        String cron = resolve(annotation.cron());
        if (!cron.isBlank()) {
            return TaskDefinition.cron(taskId, name, description, cron, annotation.deduplicate()).category(category);
        }
        long fixedDelay = annotation.fixedDelay() > 0 ? annotation.fixedDelay()
                : Long.parseLong(resolve(annotation.fixedDelayString()).trim());
        long initialDelay = annotation.initialDelay() > 0 ? annotation.initialDelay()
                : (annotation.initialDelayString().isBlank() ? 0
                        : Long.parseLong(resolve(annotation.initialDelayString()).trim()));
        return TaskDefinition.fixedDelay(taskId, name, description, initialDelay, fixedDelay,
                annotation.deduplicate()).category(category);
    }

    private String resolve(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return beanFactory.resolveEmbeddedValue(value);
    }

    private void invoke(PendingRegistration registration) throws Exception {
        try {
            registration.method().invoke(registration.bean());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw e;
        }
    }
}
