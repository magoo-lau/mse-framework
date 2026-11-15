package vip.mseapp.api.notify.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import vip.mseapp.api.notify.NotifySourceProvider;
import vip.mseapp.api.notify.RemoteNotifyChannel;
import vip.mseapp.api.notify.support.NotifyChannelRegistrationPusher;
import vip.mseapp.core.schedule.MseTaskScheduler;

/**
 * 通知目录自注册推送装配：业务工程依赖 api-notify 即生效——容器内声明了
 * {@link RemoteNotifyChannel} 渠道 Bean 或 {@link NotifySourceProvider} 来源声明的
 * 微服务（dubbo 模式）启动后，描述随同一任务推送至框架通知中枢（mse-system）登记；
 * 单体聚合形态推送器自检闲置（中枢本地收集已覆盖）。
 */
@AutoConfiguration
public class NotifyChannelRegistryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NotifyChannelRegistrationPusher notifyChannelRegistrationPusher(
            ObjectProvider<RemoteNotifyChannel> channels,
            ObjectProvider<NotifySourceProvider> sourceProviders,
            Environment environment,
            ConfigurableListableBeanFactory beanFactory,
            ObjectProvider<MseTaskScheduler> schedulerProvider) {
        return new NotifyChannelRegistrationPusher(channels, sourceProviders, environment, beanFactory, schedulerProvider);
    }
}
