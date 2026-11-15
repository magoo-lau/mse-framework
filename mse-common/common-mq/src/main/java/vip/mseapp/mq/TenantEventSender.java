package vip.mseapp.mq;

import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.support.MessageBuilder;

/**
 * 租户事件发送器：租户上下文自动写入消息头（userProperty）+ 消息体。
 *
 * <p>投递通道双轨：配置了 RocketMQ 走 MQ；未配置时经 Spring 事件进程内直达
 * （单体聚合形态控制面与数据面同 JVM，事件即时可达），两者都缺省才降级丢弃。
 */
public class TenantEventSender {

    private static final Logger log = LoggerFactory.getLogger(TenantEventSender.class);

    private final RocketMQTemplate rocketMQTemplate;

    private final ApplicationEventPublisher eventPublisher;

    public TenantEventSender(RocketMQTemplate rocketMQTemplate, ApplicationEventPublisher eventPublisher) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 降级发送器：供未配置 RocketMQ 且无进程内消费者的环境使用，
     * send 记 warn 后丢弃事件——事件不可用，其余功能不受影响。
     */
    public static TenantEventSender noop() {
        return new TenantEventSender(null, null) {
            @Override
            public void send(String topic, String eventType, Object payload) {
                log.warn("RocketMQ 未配置（缺少 rocketmq.name-server），租户事件已丢弃：topic={}, eventType={}", topic, eventType);
            }
        };
    }

    /** 发送到指定 topic，tag 使用事件类型；租户取自当前上下文 */
    public void send(String topic, String eventType, Object payload) {
        sendTenant(topic, TenantContext.getTenantId(), eventType, payload);
    }

    /** 显式携带租户的事件发送（控制面编排场景：发布方上下文与目标租户无关） */
    public void sendTenant(String topic, Long tenantId, String eventType, Object payload) {
        TenantEvent event = new TenantEvent(tenantId, eventType, String.valueOf(payload));
        if (rocketMQTemplate != null) {
            rocketMQTemplate.syncSend(topic + ":" + eventType,
                    MessageBuilder.withPayload(event)
                            .setHeader(MseConstants.MQ_TENANT_KEY, tenantId == null ? "" : String.valueOf(tenantId))
                            .build());
            return;
        }
        if (eventPublisher != null) {
            // 进程内直达（单体聚合：控制面与数据面同 JVM）；微服务无 MQ 时到达面受限，
            // 数据面靠 auto-upgrade 扫捕与快照拉取自愈
            eventPublisher.publishEvent(event);
            return;
        }
        log.warn("RocketMQ 与进程内通道均不可用，租户事件已丢弃：topic={}, eventType={}", topic, eventType);
    }
}
