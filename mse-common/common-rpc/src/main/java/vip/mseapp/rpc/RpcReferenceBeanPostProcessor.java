package vip.mseapp.rpc;

import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link RpcReference} 字段注入处理器（单体/微服务双模式的装配核心）。
 *
 * <p>处理时机等同 @Autowired；按 {@code mse.rpc.mode} 决定解析策略：
 * <ul>
 *   <li>local：从本地容器按类型解析实现 Bean（单体模式，无任何中间件依赖）——
 *       {@code @RpcReference.local().required()} 镜像 @Autowired 语义（false 时无实现注入 null），
 *       多候选时按 primary（bean 定义 primary 或提供端 {@code @RpcService.local().primary()}）消歧；</li>
 *   <li>dubbo：委托 {@link DubboReferenceFactory}（classpath 存在 Dubbo 时生效），
 *       {@code @RpcReference.dubbo()} 的全部属性（@DubboReference 镜像）全量落到 ReferenceConfig。</li>
 * </ul>
 */
public class RpcReferenceBeanPostProcessor implements InstantiationAwareBeanPostProcessor, BeanFactoryAware {

    private static final Logger log = LoggerFactory.getLogger(RpcReferenceBeanPostProcessor.class);

    private final String mode;
    private ConfigurableListableBeanFactory beanFactory;

    /** 类 → 声明了 @RpcReference 的字段缓存，避免逐 Bean 反射扫描开销 */
    private final Map<Class<?>, Field[]> annotatedFieldsCache = new ConcurrentHashMap<>();

    public RpcReferenceBeanPostProcessor(String mode) {
        this.mode = mode == null ? "local" : mode.toLowerCase();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
            throws BeansException {
        Class<?> targetClass = bean.getClass();
        Field[] fields = annotatedFieldsCache.computeIfAbsent(targetClass, this::findAnnotatedFields);
        for (Field field : fields) {
            RpcReference annotation = AnnotatedElementUtils.findMergedAnnotation(field, RpcReference.class);
            if (annotation == null) {
                continue;
            }
            Object proxy = resolve(field.getType(), annotation);
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, bean, proxy);
            log.debug("[mse-rpc] 注入 {}#{} <- {}({})", targetClass.getSimpleName(), field.getName(),
                    field.getType().getSimpleName(), this.mode);
        }
        return pvs;
    }

    private Field[] findAnnotatedFields(Class<?> clazz) {
        return java.util.Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> AnnotatedElementUtils.findMergedAnnotation(f, RpcReference.class) != null)
                .toArray(Field[]::new);
    }

    private Object resolve(Class<?> apiType, RpcReference annotation) {
        if ("dubbo".equals(mode) && DubboReferenceFactory.isAvailable()) {
            return DubboReferenceFactory.create(apiType, annotation, beanFactory);
        }
        return resolveLocal(apiType, annotation);
    }

    /**
     * 本地直调：按类型从容器解析实现（单体模式下所有 service 均在同容器）。
     * required=false 时无实现注入 null（可选依赖）；多实现时恰一个 primary 候选胜出。
     */
    private Object resolveLocal(Class<?> apiType, RpcReference annotation) {
        Map<String, ?> candidates = beanFactory.getBeansOfType(apiType);
        if (candidates.isEmpty()) {
            if (annotation.local().required()) {
                throw new IllegalStateException("[mse-rpc] 本地模式下找不到 " + apiType.getName()
                        + " 的实现 Bean——请确认依赖了对应 service 模块，或改用 mse.rpc.mode=dubbo");
            }
            log.debug("[mse-rpc] 本地模式下无 {} 实现 Bean 且 local().required=false，注入 null", apiType.getName());
            return null;
        }
        if (candidates.size() > 1) {
            Object winner = selectPrimary(apiType, candidates);
            if (winner != null) {
                return winner;
            }
            throw new IllegalStateException("[mse-rpc] 本地模式下 " + apiType.getName()
                    + " 存在多个实现: " + candidates.keySet() + "，请检查是否存在重复装配"
                    + "（可用 @RpcService.local().primary=true 消歧）");
        }
        return candidates.values().iterator().next();
    }

    /** 多候选中恰一个 primary（bean 定义 primary 或实现类 @RpcService local().primary()）时返回该候选，否则 null */
    private Object selectPrimary(Class<?> apiType, Map<String, ?> candidates) {
        Object winner = null;
        String winnerName = null;
        for (Map.Entry<String, ?> entry : candidates.entrySet()) {
            if (!isPrimary(entry.getKey(), entry.getValue())) {
                continue;
            }
            if (winner != null) {
                return null;
            }
            winner = entry.getValue();
            winnerName = entry.getKey();
        }
        if (winner != null) {
            log.info("[mse-rpc] {} 存在多个实现，按 primary 选中 bean [{}]", apiType.getName(), winnerName);
        }
        return winner;
    }

    private boolean isPrimary(String beanName, Object bean) {
        if (beanFactory.containsBeanDefinition(beanName)
                && beanFactory.getBeanDefinition(beanName).isPrimary()) {
            return true;
        }
        RpcService rpcService = AnnotatedElementUtils.findMergedAnnotation(
                ClassUtils.getUserClass(bean.getClass()), RpcService.class);
        return rpcService != null && rpcService.local().primary();
    }
}
