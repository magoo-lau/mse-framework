package vip.mseapp.rpc.dubbo;

import vip.mseapp.core.annotation.RpcService;
import org.apache.dubbo.config.ServiceConfig;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;

/**
 * Dubbo 服务导出工具：微服务启动器把本地 API 实现暴露为 Dubbo 服务的统一入口。
 *
 * <p>常规方式无需本类：实现类标注 {@code @RpcService(契约.class)} 后由
 * RpcServiceExportBeanPostProcessor 自动导出，{@code @RpcService.dubbo()} 的全部属性
 * （@DubboService 镜像）经 {@link DubboConfigBinder} 全量映射到 ServiceConfig。
 * 需要精细控制单个契约的导出时机或配置时，仍可按下面的方式手写 ServiceConfig：
 * <pre>{@code
 * @Bean(initMethod = "export", destroyMethod = "unexport")
 * public ServiceConfig<RemoteUserService> remoteUserServiceConfig(RemoteUserService impl) {
 *     return MseDubboSupport.serviceConfig(RemoteUserService.class, impl, beanFactory);
 * }
 * }</pre>
 */
public final class MseDubboSupport {

    private MseDubboSupport() {
    }

    public static <T> ServiceConfig<T> serviceConfig(Class<T> apiType, T ref,
                                                      ConfigurableListableBeanFactory beanFactory) {
        Environment env = beanFactory.getBean(Environment.class);
        ServiceConfig<T> serviceConfig = new ServiceConfig<>();
        serviceConfig.setInterface(apiType);
        serviceConfig.setRef(ref);
        serviceConfig.setApplication(DubboRuntimeConfigs.application(env));
        serviceConfig.setRegistry(DubboRuntimeConfigs.registry(env));
        return serviceConfig;
    }

    /** 在基础导出配置上全量应用 {@link RpcService.Dubbo} 声明的属性（含 methods 方法级；null 时零动作） */
    public static <T> ServiceConfig<T> serviceConfig(Class<T> apiType, T ref,
                                                      ConfigurableListableBeanFactory beanFactory,
                                                      RpcService.Dubbo dubbo) {
        ServiceConfig<T> serviceConfig = serviceConfig(apiType, ref, beanFactory);
        DubboConfigBinder.bindService(serviceConfig, dubbo);
        return serviceConfig;
    }
}
