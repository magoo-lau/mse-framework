package vip.mseapp.rpc;

import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.rpc.dubbo.DubboServiceExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link RpcService} 契约自动导出处理器（提供端，与 {@link RpcReferenceBeanPostProcessor} 消费端成对）。
 *
 * <p>实现 Bean 初始化完成后，若 {@code mse.rpc.mode=dubbo} 且 classpath 存在 Dubbo，
 * 将其 {@code value} 声明的契约接口自动导出为 Dubbo 服务，并应用 {@code @RpcService.dubbo()}
 * 的全部属性（@DubboService 镜像，经 DubboConfigBinder 全量映射）；同一契约接口 + version + group
 * 视为同一服务，不同 version/group 的导出互不冲突。local 模式或无 Dubbo 依赖时不做任何事。
 *
 * <p>本类保持零 Dubbo import：仅经 {@link DubboReferenceFactory#isAvailable()} 探测后
 * 才触达 {@link DubboServiceExporter}（随 Dubbo 依赖按需加载）。
 */
public class RpcServiceExportBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RpcServiceExportBeanPostProcessor.class);

    private final String mode;
    private ConfigurableListableBeanFactory beanFactory;

    /** 类 → 合并后的 @RpcService 注解缓存（absent = 未计算，empty = 无注解），避免逐 Bean 反射扫描开销 */
    private final Map<Class<?>, Optional<RpcService>> annotationCache = new ConcurrentHashMap<>();

    /** 已导出的服务（契约接口#version#group），防止重复导出（同契约同参数多实现 Bean / prototype 重复实例化） */
    private final Set<String> exported = ConcurrentHashMap.newKeySet();

    private final AtomicBoolean unavailableWarned = new AtomicBoolean(false);

    public RpcServiceExportBeanPostProcessor(String mode) {
        this.mode = mode == null ? "local" : mode.toLowerCase();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!"dubbo".equals(mode)) {
            return bean;
        }
        if (!DubboReferenceFactory.isAvailable()) {
            if (unavailableWarned.compareAndSet(false, true)) {
                log.warn("[mse-rpc] mse.rpc.mode=dubbo 但 classpath 无 Dubbo 依赖，跳过契约自动导出");
            }
            return bean;
        }
        Class<?> targetClass = ClassUtils.getUserClass(bean.getClass());
        RpcService annotation = annotationCache.computeIfAbsent(targetClass, this::findAnnotation)
                .orElse(null);
        if (annotation == null) {
            return bean;
        }
        RpcService.Dubbo dubbo = annotation.dubbo();
        for (Class<?> apiType : annotation.value()) {
            if (!apiType.isInstance(bean)) {
                throw new IllegalStateException("[mse-rpc] " + targetClass.getName() + " 标注 @RpcService("
                        + apiType.getName() + ".class) 但未实现该接口，请检查注解声明");
            }
            if (!exported.add(exportKey(apiType, dubbo))) {
                log.warn("[mse-rpc] {}(version={}, group={}) 已有导出实现，忽略 Bean [{}] 的重复导出",
                        apiType.getName(), dubbo.version(), dubbo.group(), beanName);
                continue;
            }
            DubboServiceExporter.export(apiType, bean, beanFactory, dubbo);
        }
        return bean;
    }

    private Optional<RpcService> findAnnotation(Class<?> clazz) {
        return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(clazz, RpcService.class));
    }

    static String exportKey(Class<?> apiType, RpcService.Dubbo dubbo) {
        return apiType.getName() + "#" + dubbo.version() + "#" + dubbo.group();
    }

    @Override
    public void destroy() {
        DubboServiceExporter.unexportAll();
    }
}
