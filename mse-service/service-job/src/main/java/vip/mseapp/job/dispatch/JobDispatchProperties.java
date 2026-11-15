package vip.mseapp.job.dispatch;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * mse-job 薄调度中心配置（{@code mse.job.*}）。
 */
@ConfigurationProperties(prefix = "mse.job")
public class JobDispatchProperties {

    /** 派发节拍：到期扫描间隔（到期到执行另有踢键→实例 sync 抢占 5s 级中转） */
    private Duration dispatchInterval = Duration.ofSeconds(5);

    /** 启动后首扫延迟（晚于结构迁移与各服务注册上报，防首轮空扫） */
    private Duration dispatchInitialDelay = Duration.ofSeconds(45);

    /** 同任务两次派发的最小间隔下限（执行与运行态上报有延迟，防执行中重复置 pending） */
    private Duration minRedispatchGap = Duration.ofSeconds(30);

    public Duration getDispatchInterval() {
        return dispatchInterval;
    }

    public void setDispatchInterval(Duration dispatchInterval) {
        this.dispatchInterval = dispatchInterval;
    }

    public Duration getDispatchInitialDelay() {
        return dispatchInitialDelay;
    }

    public void setDispatchInitialDelay(Duration dispatchInitialDelay) {
        this.dispatchInitialDelay = dispatchInitialDelay;
    }

    public Duration getMinRedispatchGap() {
        return minRedispatchGap;
    }

    public void setMinRedispatchGap(Duration minRedispatchGap) {
        this.minRedispatchGap = minRedispatchGap;
    }
}
