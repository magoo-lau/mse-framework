package vip.mseapp.rpc.config;

import vip.mseapp.rpc.RpcReferenceBeanPostProcessor;
import vip.mseapp.rpc.RpcServiceExportBeanPostProcessor;
import vip.mseapp.rpc.RpcServiceFallbackRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;

/**
 * RPC 装配自动配置。
 *
 * <p>{@code mse.rpc.mode}：local（默认，单体）/ dubbo（微服务）。
 * 处理器必须以静态 @Bean 注册，确保在业务 Bean 初始化前生效。
 * 兜底注册器为普通无序 BeanDefinitionRegistryPostProcessor（自守卫：仅 local 模式且配置
 * {@code mse.rpc.local.scan-packages} 时工作），晚于配置类处理执行以获得完整的覆盖判定视图。
 */
@AutoConfiguration
public class MseRpcAutoConfiguration {

    @Bean
    public static RpcReferenceBeanPostProcessor rpcReferenceBeanPostProcessor(Environment environment) {
        return new RpcReferenceBeanPostProcessor(environment.getProperty("mse.rpc.mode", "local"));
    }

    @Bean
    public static RpcServiceExportBeanPostProcessor rpcServiceExportBeanPostProcessor(Environment environment) {
        return new RpcServiceExportBeanPostProcessor(environment.getProperty("mse.rpc.mode", "local"));
    }

    @Bean
    public static RpcServiceFallbackRegistrar rpcServiceFallbackRegistrar() {
        return new RpcServiceFallbackRegistrar();
    }
}
