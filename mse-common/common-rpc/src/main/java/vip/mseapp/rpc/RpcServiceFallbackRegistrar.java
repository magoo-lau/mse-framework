package vip.mseapp.rpc;

import vip.mseapp.core.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * {@link RpcService} 实现 Bean 兜底自动注册器（仅 local 单体模式，opt-in）。
 *
 * <p>解决「实现类标了 @RpcService 但模块 AutoConfiguration 漏写 @Bean」导致消费端
 * {@code @RpcReference} 本地解析失败的问题：配置 {@code mse.rpc.local.scan-packages}
 * （逗号分隔扫描根包）后，扫描 @RpcService 实现类，仅当其契约接口在容器中<b>没有任何实现
 * Bean 定义</b>时才兜底注册——既有 {@code @Bean + @ConditionalOnMissingBean}（SPI 整体替换语义）
 * 永远优先，零冲突；缺省不配置即完全关闭，行为与既往一致。
 *
 * <p>注册时机为本类作为普通无序 BeanDefinitionRegistryPostProcessor 的执行点，
 * 晚于 ConfigurationClassPostProcessor（PriorityOrdered），届时全部 @Bean/@ComponentScan
 * 定义已就绪，覆盖判定结果可靠。扫描走 ASM 元数据（不预加载实现类）；bean 名取
 * {@code @RpcService.local().value()}（缺省按类名生成，与 Spring 组件扫描命名一致），
 * {@code local().primary()} 映射为 bean primary。
 */
public class RpcServiceFallbackRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(RpcServiceFallbackRegistrar.class);

    /** 兜底注册扫描根（逗号分隔），缺省为空 = 功能关闭 */
    public static final String SCAN_PACKAGES_PROPERTY = "mse.rpc.local.scan-packages";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String mode = environment.getProperty("mse.rpc.mode", "local");
        if (!"local".equals(mode)) {
            return;
        }
        String scanPackages = environment.getProperty(SCAN_PACKAGES_PROPERTY, "");
        if (!StringUtils.hasText(scanPackages)) {
            return;
        }
        if (!(registry instanceof ConfigurableListableBeanFactory)) {
            log.warn("[mse-rpc] 注册表非 ConfigurableListableBeanFactory，跳过 RpcService 兜底注册");
            return;
        }
        registerFallbacks(registry, (ConfigurableListableBeanFactory) registry, scanPackages);
    }

    @SuppressWarnings("unchecked")
    private void registerFallbacks(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory,
                                   String scanPackages) {
        Map<String, Map<String, Object>> candidates = scanCandidates(scanPackages);
        if (candidates.isEmpty()) {
            log.debug("[mse-rpc] {} 未发现 @RpcService 实现类", scanPackages);
            return;
        }
        ClassLoader classLoader = beanFactory.getBeanClassLoader() != null
                ? beanFactory.getBeanClassLoader() : ClassUtils.getDefaultClassLoader();
        // 装载候选声明的契约（元数据默认已把 Class 值解析为 Class 对象，仅 String 形态需手动加载）
        Set<Class<?>> allContracts = new LinkedHashSet<>();
        for (Map<String, Object> attrs : candidates.values()) {
            for (Object type : (Object[]) attrs.get("value")) {
                allContracts.add(toClass(type, classLoader));
            }
        }
        Set<Class<?>> covered = coveredContracts(beanFactory, allContracts);

        int registered = 0;
        for (Map.Entry<String, Map<String, Object>> entry : candidates.entrySet()) {
            String implClassName = entry.getKey();
            Map<String, Object> attrs = entry.getValue();
            List<Class<?>> apis = new ArrayList<>();
            boolean alreadyCovered = false;
            for (Object type : (Object[]) attrs.get("value")) {
                Class<?> apiType = toClass(type, classLoader);
                apis.add(apiType);
                alreadyCovered = alreadyCovered || covered.contains(apiType);
            }
            if (alreadyCovered) {
                continue;
            }
            Map<String, Object> local = (Map<String, Object>) attrs.getOrDefault("local", Map.of());
            String explicitName = (String) local.getOrDefault("value", "");
            boolean primary = (Boolean) local.getOrDefault("primary", Boolean.FALSE);

            AbstractBeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(implClassName)
                    .getBeanDefinition();
            definition.setPrimary(primary);
            String beanName = StringUtils.hasText(explicitName) ? explicitName
                    : AnnotationBeanNameGenerator.INSTANCE.generateBeanName(definition, registry);
            if (registry.containsBeanDefinition(beanName)) {
                log.warn("[mse-rpc] 兜底注册跳过 {}：bean 名 [{}] 已被占用，请检查 @RpcService.local().value 命名冲突",
                        implClassName, beanName);
                continue;
            }
            registry.registerBeanDefinition(beanName, definition);
            apis.forEach(covered::add);
            registered++;
            log.info("[mse-rpc] 兜底注册 @RpcService 实现: {} -> bean [{}] (primary={}, 契约={})",
                    implClassName, beanName, primary, apis);
        }
        log.debug("[mse-rpc] RpcService 兜底注册完成: 候选 {} 个，注册 {} 个（其余契约已有实现 Bean）",
                candidates.size(), registered);
    }

    /** 元数据的 Class 值默认已解析为 Class 对象；String 形态（个别 reader 路径）按类名加载兜底 */
    private Class<?> toClass(Object type, ClassLoader classLoader) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }
        return resolveClass(type.toString(), classLoader);
    }

    /** 扫描根下的 @RpcService 实现类（key=实现类名，TreeMap 按类名排序保证多实现兜底的确定性） */
    private Map<String, Map<String, Object>> scanCandidates(String scanPackages) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, environment);
        scanner.setResourceLoader(new PathMatchingResourcePatternResolver());
        scanner.addIncludeFilter(new AnnotationTypeFilter(RpcService.class));
        Map<String, Map<String, Object>> result = new TreeMap<>();
        for (String basePackage : StringUtils.commaDelimitedListToStringArray(scanPackages.trim())) {
            String pkg = basePackage.trim();
            if (pkg.isEmpty()) {
                continue;
            }
            for (BeanDefinition candidate : scanner.findCandidateComponents(pkg)) {
                if (candidate instanceof AnnotatedBeanDefinition annotated) {
                    Map<String, Object> attrs = annotated.getMetadata()
                            .getAnnotationAttributes(RpcService.class.getName());
                    if (attrs != null && candidate.getBeanClassName() != null) {
                        result.put(candidate.getBeanClassName(), attrs);
                    }
                }
            }
        }
        return result;
    }

    /** 遍历既有 bean 定义，收集已被容器内 bean 实现的契约（兜底注册的让位判定） */
    private Set<Class<?>> coveredContracts(ConfigurableListableBeanFactory beanFactory,
                                           Iterable<Class<?>> contracts) {
        Set<Class<?>> covered = new HashSet<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            Class<?> type = beanFactory.getType(beanName);
            if (type == null) {
                continue;
            }
            for (Class<?> contract : contracts) {
                if (contract.isAssignableFrom(type)) {
                    covered.add(contract);
                }
            }
        }
        return covered;
    }

    private Class<?> resolveClass(String className, ClassLoader classLoader) {
        try {
            return ClassUtils.forName(className, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("[mse-rpc] @RpcService 契约类不存在: " + className, ex);
        }
    }
}
