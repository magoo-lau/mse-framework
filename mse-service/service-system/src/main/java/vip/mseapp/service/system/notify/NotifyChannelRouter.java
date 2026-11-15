package vip.mseapp.service.system.notify;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import vip.mseapp.api.notify.RemoteNotifyChannel;
import vip.mseapp.api.notify.dto.NotifyMessage;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.service.system.notify.SysNotifyCatalogService.CatalogEntry;

/**
 * 通知渠道路由：渠道码 → 提供方调用。本地容器内声明该渠道的 {@link RemoteNotifyChannel} Bean
 * 优先直调（单体聚合形态）；否则（dubbo 形态）按登记目录的 providerGroup 经编程式
 * ReferenceConfig 定向调用提供方服务（check=false，引用按组缓存）。
 *
 * <p>本类直接 Dubbo import 仅出现在方法体内且受 dubboPresent() 守卫——单体宿主无 Dubbo
 * classpath 时相关代码不执行、类可正常加载（ai-api AiDubboToolRegistryReference 同款懒加载口径）。
 */
@Service
public class NotifyChannelRouter {

    private static final Logger log = LoggerFactory.getLogger(NotifyChannelRouter.class);

    /** 渠道投递超时（毫秒）：含逐接收人外呼，异步派发允许长耗 */
    private static final int SEND_TIMEOUT_MS = 30_000;

    private final ObjectProvider<RemoteNotifyChannel> localChannels;
    private final Environment environment;

    /** dubbo 提供组 → 引用配置（进程内缓存） */
    private final Map<String, org.apache.dubbo.config.ReferenceConfig<RemoteNotifyChannel>> remoteReferences =
            new ConcurrentHashMap<>();

    public NotifyChannelRouter(ObjectProvider<RemoteNotifyChannel> localChannels, Environment environment) {
        this.localChannels = localChannels;
        this.environment = environment;
    }

    /** 渠道可用性探测（本地直调/远程 RPC；异常按不可用返回，不抛出） */
    public boolean available(CatalogEntry entry) {
        try {
            RemoteNotifyChannel channel = route(entry);
            return channel != null && channel.available(entry.channelCode());
        } catch (Exception e) {
            log.debug("[notify] 渠道可用性探测失败: {} - {}", entry.channelCode(), e.getMessage());
            return false;
        }
    }

    /** 渠道投递（异常由派发器兜底记录，不影响其他渠道） */
    public void send(CatalogEntry entry, NotifyMessage message) {
        RemoteNotifyChannel channel = route(entry);
        if (channel == null) {
            throw new ServiceException(503, "渠道提供方不可达：" + entry.providerGroup());
        }
        channel.send(entry.channelCode(), message);
    }

    private RemoteNotifyChannel route(CatalogEntry entry) {
        if (entry.local()) {
            return localChannel(entry.channelCode());
        }
        return remoteChannel(entry.providerGroup());
    }

    /** 本地容器内声明该渠道码的提供方 Bean（无则 null） */
    private RemoteNotifyChannel localChannel(String channelCode) {
        for (RemoteNotifyChannel channel : localChannels) {
            try {
                var descriptors = channel.describeChannels();
                if (descriptors != null && descriptors.stream()
                        .anyMatch(d -> d != null && channelCode.equals(d.getChannelCode()))) {
                    return channel;
                }
            } catch (Exception e) {
                log.debug("[notify] 本地渠道描述读取失败: {} - {}", channel.getClass().getName(), e.getMessage());
            }
        }
        return null;
    }

    /** dubbo 提供组引用（非 dubbo 模式/无 Dubbo classpath 返回 null；对端未就绪返回 null 由调用方跳过） */
    private RemoteNotifyChannel remoteChannel(String providerGroup) {
        if (providerGroup == null || providerGroup.isBlank()
                || !"dubbo".equalsIgnoreCase(environment.getProperty("mse.rpc.mode", "local"))
                || !dubboPresent()) {
            return null;
        }
        try {
            org.apache.dubbo.config.ReferenceConfig<RemoteNotifyChannel> config =
                    remoteReferences.computeIfAbsent(providerGroup.trim(), group -> {
                        org.apache.dubbo.config.ReferenceConfig<RemoteNotifyChannel> created =
                                new org.apache.dubbo.config.ReferenceConfig<>();
                        created.setInterface(RemoteNotifyChannel.class);
                        created.setGroup(group);
                        created.setCheck(false);
                        created.setTimeout(SEND_TIMEOUT_MS);
                        created.setApplication(vip.mseapp.rpc.dubbo.DubboRuntimeConfigs.application(environment));
                        created.setRegistry(vip.mseapp.rpc.dubbo.DubboRuntimeConfigs.registry(environment));
                        return created;
                    });
            return config.get();
        } catch (Exception e) {
            log.warn("[notify] 渠道提供方引用暂不可用（provider={}）: {}", providerGroup, e.getMessage());
            return null;
        }
    }

    @PreDestroy
    void shutdown() {
        if (dubboPresent()) {
            remoteReferences.values().forEach(config -> {
                try {
                    config.destroy();
                } catch (Exception ignored) {
                    // 下线阶段的清理失败无需处理
                }
            });
            remoteReferences.clear();
        }
    }

    private boolean dubboPresent() {
        try {
            Class.forName("org.apache.dubbo.config.ReferenceConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
