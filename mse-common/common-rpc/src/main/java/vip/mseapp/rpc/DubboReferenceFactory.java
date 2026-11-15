package vip.mseapp.rpc;

import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.rpc.dubbo.DubboConfigBinder;
import org.apache.dubbo.config.ReferenceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dubbo 引用工厂：mse.rpc.mode=dubbo 时为 API 接口编程式创建 ReferenceConfig 代理。
 *
 * <p>采用 Dubbo 原生 API（ReferenceConfig），application/registry 配置取自容器中
 * dubbo-spring-boot-starter 注册的 ApplicationConfig/RegistryConfig Bean。
 * 引用属性的唯一来源为 {@code @RpcReference.dubbo()}（@DubboReference 全量镜像），
 * 经 {@link DubboConfigBinder} 全量落到 ReferenceConfig（含缺省：timeout=5000、check=false）。
 * 同一接口 + version + group 的引用全局缓存，多个注入点共享一个代理。
 */
final class DubboReferenceFactory {

    private static final Logger log = LoggerFactory.getLogger(DubboReferenceFactory.class);

    /** 接口全名#version#group → 代理实例 */
    private static final Map<String, Object> PROXY_CACHE = new ConcurrentHashMap<>();

    private static final boolean DUBBO_PRESENT;

    static {
        boolean present = false;
        try {
            Class.forName("org.apache.dubbo.config.ReferenceConfig");
            present = true;
        } catch (ClassNotFoundException ignored) {
            // classpath 无 dubbo：保持 local 模式
        }
        DUBBO_PRESENT = present;
    }

    private DubboReferenceFactory() {
    }

    static boolean isAvailable() {
        return DUBBO_PRESENT;
    }

    @SuppressWarnings("unchecked")
    static <T> T create(Class<T> apiType, RpcReference annotation,
                        ConfigurableListableBeanFactory beanFactory) {
        RpcReference.Dubbo dubbo = annotation.dubbo();
        // 方法级配置参与缓存键（注解 hashCode 按值定义，同契约不同方法级配置各自缓存）
        String cacheKey = apiType.getName() + "#" + dubbo.version() + "#" + dubbo.group()
                + "#" + java.util.Arrays.hashCode(dubbo.methods());
        Object proxy = PROXY_CACHE.get(cacheKey);
        if (proxy != null) {
            return (T) proxy;
        }
        Environment env = beanFactory.getBean(Environment.class);
        ReferenceConfig<T> referenceConfig = new ReferenceConfig<>();
        referenceConfig.setInterface(apiType);
        DubboConfigBinder.bindReference(referenceConfig, dubbo);
        referenceConfig.setApplication(vip.mseapp.rpc.dubbo.DubboRuntimeConfigs.application(env));
        referenceConfig.setRegistry(vip.mseapp.rpc.dubbo.DubboRuntimeConfigs.registry(env));
        proxy = referenceConfig.get();
        PROXY_CACHE.put(cacheKey, proxy);
        log.info("[mse-rpc] 创建 Dubbo 引用: {} (version={}, group={}, timeout={})",
                apiType.getSimpleName(), dubbo.version(), dubbo.group(), dubbo.timeout());
        return (T) proxy;
    }
}
