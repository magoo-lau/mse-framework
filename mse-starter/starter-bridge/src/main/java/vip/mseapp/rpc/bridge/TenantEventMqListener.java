package vip.mseapp.rpc.bridge;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import vip.mseapp.mq.TenantEvent;

/**
 * 租户事件 MQ 消费端（微服务形态即时通道）：订阅 {@code mse-tenant-events} 主题（tag=事件类型），
 * 弥合「控制面 TenantEventSender 有 MQ 时发 MQ、但数据面无人消费」的断链——此前微服务形态下
 * 租户事件只有同 JVM 进程内通道可达，数据面快照失效与每租户种子补跑完全依赖 TTL/扫捕兜底。
 *
 * <p>处理逻辑统一在 {@link TenantEventInboundHandler}（与进程内 Spring 事件通道共用）。
 * 装配条件：宿主引入 mq-starter（rocketmq-spring 在 classpath）且已配置
 * {@code rocketmq.name-server}（rocketmq-spring 的监听容器 BeanPostProcessor 无条件注册，
 * 缺 name-server 时创建容器即失败）+ {@code mse.tenant.events.mq-enabled=true}（缺省开）；
 * 单体聚合形态消费自身发布的事件幂等无害（失效快照 + 幂等迁移）。
 */
@RocketMQMessageListener(topic = TenantEvent.TOPIC, consumerGroup = "mse-tenant-bridge-events")
public class TenantEventMqListener implements RocketMQListener<TenantEvent> {

    private final TenantEventInboundHandler handler;

    public TenantEventMqListener(TenantEventInboundHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onMessage(TenantEvent event) {
        handler.handle(event);
    }
}
