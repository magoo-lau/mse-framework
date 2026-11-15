package vip.mseapp.rpc.bridge.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import vip.mseapp.core.schedule.MseTaskScheduler;
import vip.mseapp.rpc.bridge.ScheduleStateSyncer;

/**
 * 调度状态同步装配（local 模式管理面数据通道，Spring Boot SPI）：实例侧调度监控线程——
 * 独立守护线程（不注册进调度中心），控制节拍 HGETALL Redis 控制哈希秒级应用启停/覆盖，
 * 同步节拍与 mse-platform sync() 往返上报快照/日志并兜底下行。可用
 * {@code mse.bridge.schedule-sync.enabled=false} 整体关闭（如明确不部署平台管理端）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "mse.bridge.schedule-sync", name = "enabled", matchIfMissing = true)
public class ScheduleStateBridgeAutoConfiguration {

    @Bean
    public ScheduleStateSyncer scheduleStateSyncer(Environment environment,
                                                   ConfigurableListableBeanFactory beanFactory,
                                                   ObjectProvider<MseTaskScheduler> schedulerProvider,
                                                   ObjectProvider<StringRedisTemplate> redisProvider,
                                                   ObjectProvider<vip.mseapp.core.concurrent.TenantTaskGates> gatesProvider) {
        return new ScheduleStateSyncer(environment, beanFactory, schedulerProvider, redisProvider, gatesProvider);
    }
}
