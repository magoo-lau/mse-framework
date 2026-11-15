package vip.mseapp.rpc.bridge.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import vip.mseapp.core.schedule.MseTaskScheduler;
import vip.mseapp.rpc.bridge.BaseDataScriptPusher;

/**
 * 基座库脚本推送装配（db/base 通道，Spring Boot SPI）：检测本应用 classpath 上的
 * {@code db/base/<方言>/V*.sql}（业务积木自带的原 base.sql 版本化形态），存在则在应用就绪后
 * 推送至 mse-system 并周期重推自愈；不存在时推送器闲置（零副作用）。可用
 * {@code mse.bridge.registry.enabled=false} 整体关闭。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "mse.bridge.registry", name = "enabled", matchIfMissing = true)
public class BaseDataRegistryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BaseDataScriptPusher baseDataScriptPusher(Environment environment,
                                                     ConfigurableListableBeanFactory beanFactory,
                                                     ObjectProvider<MseTaskScheduler> schedulerProvider) {
        return new BaseDataScriptPusher(environment, beanFactory, schedulerProvider);
    }
}
