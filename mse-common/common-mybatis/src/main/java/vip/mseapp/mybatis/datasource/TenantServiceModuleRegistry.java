package vip.mseapp.mybatis.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import vip.mseapp.core.annotation.TenantServiceModule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@link TenantServiceModule} 模块身份收集器（懒扫描）：首次使用时从容器收集所有标注了
 * 服务身份注解的自动装配类，合并为「服务名 ↔ mapper 包根」映射，供租户路由服务名解析
 * （TenantDsExecutorInterceptor）与租户数据源元数据加载（TenantDsRegistry）消费。
 *
 * <p>合并语义：yaml service-mappings 显式配置优先于本收集结果（逃生舱可覆盖注解）；
 * 同名模块多包根（如 oa 聚合 form/bpm）以 pair 列表承载，最长前缀匹配。
 * 收集必须在容器就绪后进行，故懒触发（volatile 双检）而非启动期急切——消费方均在
 * 运行期（首条语句/启动收尾注册）调用，时序天然满足。
 */
public class TenantServiceModuleRegistry implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(TenantServiceModuleRegistry.class);

    /** 服务名 ↔ mapper 包根映射对（同名模块多包根多条并存） */
    public record ServiceMapping(String serviceName, String packagePrefix) {
    }

    private ApplicationContext applicationContext;

    /** 懒收集缓存（volatile 双检；映射对按包前缀长度降序，消费方可直接 find-first 最长匹配） */
    private volatile List<ServiceMapping> mappings;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /** 服务名 → 包根 映射对（按包前缀长度降序；无标注解模块时为空列表） */
    public List<ServiceMapping> mappings() {
        List<ServiceMapping> result = mappings;
        if (result == null) {
            synchronized (this) {
                result = mappings;
                if (result == null) {
                    result = mappings = collect();
                }
            }
        }
        return result;
    }

    /** 已声明的模块服务名集合（去重） */
    public Set<String> serviceNames() {
        Set<String> names = new LinkedHashSet<>();
        for (ServiceMapping mapping : mappings()) {
            names.add(mapping.serviceName());
        }
        return names;
    }

    /** 最长前缀匹配：mapper 全名（如 vip.mseapp.oa.form.mapper.XxxMapper）→ 服务名；未命中 null */
    public String resolveServiceName(String mapperName) {
        for (ServiceMapping mapping : mappings()) {
            if (mapperName.startsWith(mapping.packagePrefix())) {
                return mapping.serviceName();
            }
        }
        return null;
    }

    /** 读类上声明的服务身份（无则返回 fallback）——模块自取身份用（如引擎命令拦截器） */
    public static String ownServiceName(Class<?> clazz, String fallback) {
        TenantServiceModule annotation = AnnotatedElementUtils.findMergedAnnotation(clazz, TenantServiceModule.class);
        String name = annotation == null ? null : annotation.value();
        return name == null || name.isBlank() ? fallback : name.trim();
    }

    private List<ServiceMapping> collect() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(TenantServiceModule.class);
        List<ServiceMapping> collected = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        beans.forEach((beanName, bean) -> {
            Class<?> clazz = resolveBeanClass(beanName, bean);
            if (clazz == null) {
                return;
            }
            TenantServiceModule annotation = AnnotatedElementUtils.findMergedAnnotation(clazz, TenantServiceModule.class);
            if (annotation == null) {
                return;
            }
            String serviceName = annotation.value().trim();
            names.add(serviceName);
            String[] packages = annotation.packages();
            if (packages.length > 0) {
                for (String pkg : packages) {
                    if (pkg != null && !pkg.isBlank()) {
                        collected.add(new ServiceMapping(serviceName, pkg.trim()));
                    }
                }
            } else {
                collected.add(new ServiceMapping(serviceName, derivePackageRoot(clazz)));
            }
        });
        collected.sort(Comparator.comparingInt((ServiceMapping m) -> m.packagePrefix().length()).reversed());
        if (!names.isEmpty()) {
            log.info("[mse-tenant] 已收集模块服务身份: {}（包前缀 {} 条）", names, collected.size());
        }
        return List.copyOf(collected);
    }

    /** 包根推导：注解类所在包去掉末段 .autoconfigure（业务模块）/ .config（基座模块） */
    private String derivePackageRoot(Class<?> clazz) {
        String pkg = clazz.getPackageName();
        for (String suffix : new String[]{".autoconfigure", ".config"}) {
            if (pkg.endsWith(suffix)) {
                return pkg.substring(0, pkg.length() - suffix.length());
            }
        }
        return pkg;
    }

    private Class<?> resolveBeanClass(String beanName, Object bean) {
        Class<?> type = applicationContext.getType(beanName);
        return type != null ? type : bean.getClass();
    }
}
