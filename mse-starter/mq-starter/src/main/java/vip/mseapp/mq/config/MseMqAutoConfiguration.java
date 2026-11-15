package vip.mseapp.mq.config;

import vip.mseapp.mq.TenantEventSender;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * MQ 基础设施自动装配（rocketmq-spring-boot-starter 本身负责客户端装配）。
 *
 * <p>仅类在类路径不代表 RocketMQ 可用：rocketmq-spring 只在配置了
 * {@code rocketmq.name-server}（+ producer.group）时才创建 {@link RocketMQTemplate}。
 * 因此真实发送器以 {@code @ConditionalOnBean(RocketMQTemplate.class)} 为准（装配顺序在其之后）；
 * 未配置 MQ 时降级为进程内发送器（Spring 事件直达，单体聚合形态控制面→数据面同 JVM 即时可达），
 * 消费方注入不失败、其余功能不受影响。
 */
@AutoConfiguration(after = RocketMQAutoConfiguration.class)
@ConditionalOnClass(RocketMQTemplate.class)
public class MseMqAutoConfiguration {

    @Bean
    @ConditionalOnBean(RocketMQTemplate.class)
    @ConditionalOnMissingBean(TenantEventSender.class)
    public TenantEventSender tenantEventSender(RocketMQTemplate rocketMQTemplate,
                                               ApplicationEventPublisher eventPublisher) {
        return new TenantEventSender(rocketMQTemplate, eventPublisher);
    }

    /** 未配置 RocketMQ 的环境兜底：进程内 Spring 事件直达（无进程内消费者时事件不跨服务） */
    @Bean
    @ConditionalOnMissingBean(TenantEventSender.class)
    public TenantEventSender inProcessTenantEventSender(ApplicationEventPublisher eventPublisher) {
        return new TenantEventSender(null, eventPublisher);
    }
}
