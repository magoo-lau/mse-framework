package vip.mseapp.core.schedule;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * 调度启动监听：ApplicationReadyEvent（晚于全部 ApplicationRunner——结构迁移、
 * 车队重放等启动动作均已完成）时触发 local 调度器的统一延迟启动。
 * 外部调度器适配实现自行决定启动时机（本监听对其无感）。
 */
public class MseSchedulingStartListener implements ApplicationListener<ApplicationReadyEvent> {

    private final ObjectProvider<MseTaskScheduler> schedulerProvider;

    public MseSchedulingStartListener(ObjectProvider<MseTaskScheduler> schedulerProvider) {
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        MseTaskScheduler scheduler = schedulerProvider.getIfAvailable();
        if (scheduler instanceof LocalMseTaskScheduler local) {
            local.start();
        }
    }
}
