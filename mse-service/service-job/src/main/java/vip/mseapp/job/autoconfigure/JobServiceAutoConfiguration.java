package vip.mseapp.job.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import vip.mseapp.job.dispatch.JobDispatchCenter;
import vip.mseapp.job.dispatch.JobDispatchProperties;
import vip.mseapp.job.mapper.JobScheduleTaskMapper;
import vip.mseapp.mybatis.config.MseMybatisAutoConfiguration;

/**
 * mse-job 薄调度中心装配（Spring Boot SPI）：派发中心 + 配置绑定。
 *
 * <p>开关：{@code mse.service.job.dispatch.enabled=false} 可整体关闭派发（如只想用该进程
 * 承担其他职责时）；mapper 扫描经 {@code @MapperScan}（本模块 mapper 包）。
 */
@AutoConfiguration(before = MseMybatisAutoConfiguration.class)
@org.mybatis.spring.annotation.MapperScan(basePackages = "vip.mseapp.job.mapper",
        annotationClass = org.apache.ibatis.annotations.Mapper.class)
@EnableConfigurationProperties(JobDispatchProperties.class)
public class JobServiceAutoConfiguration {

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(JobDispatchCenter.class)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
            prefix = "mse.service.job.dispatch", name = "enabled", matchIfMissing = true)
    public JobDispatchCenter jobDispatchCenter(JobScheduleTaskMapper taskMapper,
                                               ObjectProvider<StringRedisTemplate> redisProvider,
                                               JobDispatchProperties properties) {
        return new JobDispatchCenter(taskMapper, redisProvider, properties);
    }
}
