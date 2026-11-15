package vip.mseapp.web.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import vip.mseapp.web.filter.ReadyGateFilter;

/**
 * 就绪闸门开闸监听：ApplicationReadyEvent（晚于全部 ApplicationRunner——结构迁移、
 * 共享库直建、车队重放等启动动作均已完成）时打开 {@link ReadyGateFilter}。
 * 至此之前到达的请求由闸门统一 503，开闸后过滤器退化为一次布尔读。
 */
public class ReadyGateStartListener implements ApplicationListener<ApplicationReadyEvent> {

    private final ReadyGateFilter readyGateFilter;

    public ReadyGateStartListener(ReadyGateFilter readyGateFilter) {
        this.readyGateFilter = readyGateFilter;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        readyGateFilter.markReady();
    }
}
