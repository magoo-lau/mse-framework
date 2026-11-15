package vip.mseapp.rpc.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import vip.mseapp.api.system.RemoteScheduleStateApi;
import vip.mseapp.rpc.dubbo.DubboRuntimeConfigs;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;

/**
 * Dubbo 注册引用持有（仅供 {@link ScheduleStateSyncer} 在 dubbo 模式下加载，
 * 宿主无 Dubbo 时本类不被类加载）。编程式 ReferenceConfig 创建
 * {@link RemoteScheduleStateApi} 代理，check=false 启动不依赖 mse-platform 就绪。
 */
final class ScheduleStateDubboReference {

    private static final Logger log = LoggerFactory.getLogger(ScheduleStateDubboReference.class);

    private static volatile ReferenceConfig<RemoteScheduleStateApi> reference;

    private ScheduleStateDubboReference() {
    }

    /** 创建代理；注册中心不可用/对端未就绪返回 null（调用方下个周期重试） */
    static RemoteScheduleStateApi proxy(Environment environment) {
        ReferenceConfig<RemoteScheduleStateApi> config = reference;
        if (config == null) {
            synchronized (ScheduleStateDubboReference.class) {
                if (reference == null) {
                    ReferenceConfig<RemoteScheduleStateApi> created = new ReferenceConfig<>();
                    created.setInterface(RemoteScheduleStateApi.class);
                    created.setCheck(false);
                    created.setTimeout(10000);
                    created.setApplication(DubboRuntimeConfigs.application(environment));
                    created.setRegistry(DubboRuntimeConfigs.registry(environment));
                    reference = created;
                }
                config = reference;
            }
        }
        try {
            return config.get();
        } catch (Exception e) {
            log.warn("[mse-schedule] mse-platform 调度状态引用暂不可用（将重试）: {}", e.getMessage());
            return null;
        }
    }

    static void destroy() {
        ReferenceConfig<RemoteScheduleStateApi> config = reference;
        if (config != null) {
            try {
                config.destroy();
            } catch (Exception ignored) {
                // 下线阶段的清理失败无需处理
            }
        }
    }
}
