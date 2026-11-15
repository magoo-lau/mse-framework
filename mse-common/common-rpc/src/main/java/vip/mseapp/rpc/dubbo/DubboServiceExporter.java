package vip.mseapp.rpc.dubbo;

import vip.mseapp.core.annotation.RpcService;
import org.apache.dubbo.config.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dubbo 服务导出器：mse.rpc.mode=dubbo 时把本地契约实现导出为 Dubbo 服务。
 *
 * <p>由 {@code RpcServiceExportBeanPostProcessor} 在确认 classpath 存在 Dubbo 后调用，
 * 本类仅在 dubbo 模式下被触达加载（单体模式无 Dubbo 依赖，不加载本类）。
 */
public final class DubboServiceExporter {

    private static final Logger log = LoggerFactory.getLogger(DubboServiceExporter.class);

    /** 已导出的 ServiceConfig，容器关闭时统一 unexport */
    private static final List<ServiceConfig<?>> EXPORTED = new CopyOnWriteArrayList<>();

    private DubboServiceExporter() {
    }

    public static void export(Class<?> apiType, Object ref, ConfigurableListableBeanFactory beanFactory) {
        doExport(apiType, ref, beanFactory, null);
    }

    /** {@code dubbo} 为 null 时等价于无属性导出；非空时全量映射（含 methods 方法级）到 ServiceConfig */
    public static void export(Class<?> apiType, Object ref, ConfigurableListableBeanFactory beanFactory,
                              RpcService.Dubbo dubbo) {
        doExport(apiType, ref, beanFactory, dubbo);
    }

    private static <T> void doExport(Class<T> apiType, Object ref, ConfigurableListableBeanFactory beanFactory,
                                      RpcService.Dubbo dubbo) {
        ServiceConfig<T> serviceConfig = MseDubboSupport.serviceConfig(apiType, apiType.cast(ref), beanFactory, dubbo);
        serviceConfig.export();
        EXPORTED.add(serviceConfig);
        log.info("[mse-rpc] 自动导出 Dubbo 服务: {} (version={}, group={})", apiType.getName(),
                dubbo == null ? "" : dubbo.version(), dubbo == null ? "" : dubbo.group());
    }

    public static void unexportAll() {
        for (ServiceConfig<?> serviceConfig : EXPORTED) {
            try {
                serviceConfig.unexport();
            } catch (Exception e) {
                log.warn("[mse-rpc] unexport {} 失败: {}", serviceConfig.getInterfaceClass().getName(), e.getMessage());
            }
        }
        EXPORTED.clear();
    }
}
