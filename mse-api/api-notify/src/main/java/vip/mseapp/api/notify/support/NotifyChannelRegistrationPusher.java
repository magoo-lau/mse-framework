package vip.mseapp.api.notify.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import vip.mseapp.api.notify.NotifySourceProvider;
import vip.mseapp.api.notify.RemoteNotifyChannel;
import vip.mseapp.api.notify.RemoteNotifyChannelRegistry;
import vip.mseapp.api.notify.dto.NotifyChannelDescriptor;
import vip.mseapp.api.notify.dto.NotifySourceDescriptor;
import vip.mseapp.core.schedule.MseTaskScheduler;
import vip.mseapp.core.schedule.TaskDefinition;

/**
 * 通知目录自注册推送器：应用就绪后把容器内全部 {@link RemoteNotifyChannel} 的渠道描述与
 * 全部 {@link NotifySourceProvider} 的业务来源描述推送至框架通知中枢（基座 mse-system），
 * 并按周期重推自愈（中枢未就绪/重启丢失后自动补登记）。
 *
 * <p>通道自检：宿主容器内已存在 {@link RemoteNotifyChannelRegistry} 实现（单体聚合基座或
 * mse-system 自身）时整体闲置——中枢已直接收集本地 Bean，推送只会制造
 * 重复登记；纯业务微服务（dubbo 模式）经 {@link NotifyDubboRegistryReference} 远推 mse-system
 * （check=false 启动不依赖对方就绪）；local 模式且无实现说明无消费通道，告警一次后闲置。
 *
 * <p>调度经统一调度中心（taskId=notify-channel-registry-push：延迟启动 + 多实例防重）；
 * 容器内两类 Bean 均无则整体闲置。本类保持零 Dubbo import（Dubbo 引用收在
 * {@link NotifyDubboRegistryReference}）。
 */
public class NotifyChannelRegistrationPusher implements ApplicationListener<ApplicationReadyEvent>, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(NotifyChannelRegistrationPusher.class);

    private final ObjectProvider<RemoteNotifyChannel> channels;
    private final ObjectProvider<NotifySourceProvider> sourceProviders;
    private final Environment environment;
    private final org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory;
    private final long intervalMinutes;

    private final ObjectProvider<MseTaskScheduler> schedulerProvider;

    /** local 模式且宿主未聚合基座：渠道无消费通道，告警一次后停推 */
    private final AtomicBoolean unconsumableWarned = new AtomicBoolean(false);

    public NotifyChannelRegistrationPusher(ObjectProvider<RemoteNotifyChannel> channels,
                                           ObjectProvider<NotifySourceProvider> sourceProviders,
                                           Environment environment,
                                           org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory,
                                           ObjectProvider<MseTaskScheduler> schedulerProvider) {
        this.channels = channels;
        this.sourceProviders = sourceProviders;
        this.environment = environment;
        this.beanFactory = beanFactory;
        this.schedulerProvider = schedulerProvider;
        this.intervalMinutes = Math.max(1,
                environment.getProperty("mse.notify.channel-registry.interval-minutes", Long.class, 5L));
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        boolean hasChannel = channels.stream().findAny().isPresent();
        boolean hasSource = sourceProviders.stream().findAny().isPresent();
        if (!hasChannel && !hasSource) {
            log.debug("[api-notify] 无 RemoteNotifyChannel/NotifySourceProvider Bean，通知目录自注册推送器闲置");
            return;
        }
        if (!beanFactory.getBeansOfType(RemoteNotifyChannelRegistry.class).isEmpty()) {
            log.info("[api-notify] 宿主容器内已有 RemoteNotifyChannelRegistry 实现（本地收集已覆盖），目录推送器闲置");
            return;
        }
        if (!"dubbo".equalsIgnoreCase(environment.getProperty("mse.rpc.mode", "local")) || !dubboPresent()) {
            if (unconsumableWarned.compareAndSet(false, true)) {
                log.warn("[api-notify] local 模式且未聚合基座，通知渠道/来源描述无消费通道，跳过自注册推送");
            }
            return;
        }
        MseTaskScheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler == null) {
            log.info("[api-notify] 调度器未装配（mse.scheduling.mode=none 等），通知目录自注册推送不启动");
            return;
        }
        scheduler.register(TaskDefinition.fixedDelay("notify-channel-registry-push", "通知目录自注册推送",
                        "推送容器内渠道/业务来源描述至框架通知中枢并周期重推自愈", 0,
                        TimeUnit.MINUTES.toMillis(intervalMinutes))
                        .category(TaskDefinition.CATEGORY_PLATFORM),
                this::pushSafely);
    }

    private void pushSafely() {
        try {
            push();
        } catch (Throwable e) {
            log.warn("[api-notify] 目录描述推送失败，将在下个周期重试: {}", e.getMessage());
        }
    }

    private void push() {
        List<NotifyChannelDescriptor> channelDescriptors = new ArrayList<>();
        for (RemoteNotifyChannel channel : channels) {
            try {
                channelDescriptors.addAll(channel.describeChannels());
            } catch (Exception e) {
                log.warn("[api-notify] 收集渠道描述失败，跳过: {} - {}", channel.getClass().getName(), e.getMessage());
            }
        }
        List<NotifySourceDescriptor> sourceDescriptors = new ArrayList<>();
        for (NotifySourceProvider provider : sourceProviders) {
            try {
                List<NotifySourceDescriptor> declared = provider.describeSources();
                if (declared != null) {
                    sourceDescriptors.addAll(declared);
                }
            } catch (Exception e) {
                log.warn("[api-notify] 收集业务来源描述失败，跳过: {} - {}", provider.getClass().getName(), e.getMessage());
            }
        }
        if (channelDescriptors.isEmpty() && sourceDescriptors.isEmpty()) {
            return;
        }
        RemoteNotifyChannelRegistry registry = NotifyDubboRegistryReference.proxy(environment);
        if (registry == null) {
            return; // 等待下个周期（mse-system 未就绪）
        }
        String group = environment.getProperty("spring.application.name", "unknown");
        if (!channelDescriptors.isEmpty()) {
            registry.register(group, channelDescriptors);
        }
        if (!sourceDescriptors.isEmpty()) {
            registry.registerSources(group, sourceDescriptors);
        }
        log.info("[api-notify] 已推送通知目录至框架通知中枢（provider={}，渠道 {} 条，来源 {} 条）",
                group, channelDescriptors.size(), sourceDescriptors.size());
    }

    private boolean dubboPresent() {
        try {
            Class.forName("org.apache.dubbo.config.ReferenceConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void destroy() {
        if (dubboPresent()) {
            NotifyDubboRegistryReference.destroy();
        }
    }
}
