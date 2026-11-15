package vip.mseapp.rpc.dubbo;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.springframework.core.env.Environment;

/**
 * Dubbo 3.3 起 application/registry 配置不再以 Spring Bean 形式注册（直接绑定到 ConfigManager），
 * 本类从 Environment 读取 dubbo.* 属性构建编程式配置，供原生 ReferenceConfig/ServiceConfig 使用。
 */
public final class DubboRuntimeConfigs {

    private static volatile ApplicationConfig application;
    private static volatile RegistryConfig registry;

    private DubboRuntimeConfigs() {
    }

    public static synchronized ApplicationConfig application(Environment env) {
        if (application == null) {
            ApplicationConfig config = new ApplicationConfig();
            config.setName(env.getProperty("dubbo.application.name",
                    env.getProperty("spring.application.name", "mse-app")));
            config.setQosEnable(Boolean.parseBoolean(env.getProperty("dubbo.application.qos-enable", "false")));
            application = config;
        }
        return application;
    }

    public static synchronized RegistryConfig registry(Environment env) {
        if (registry == null) {
            String address = env.getProperty("dubbo.registry.address", "N/A");
            registry = new RegistryConfig(address);
        }
        return registry;
    }
}
