package vip.mseapp.web.filter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 启动就绪闸门参数（mse.web.ready-gate.*，自动装配在 starter-web）。
 */
@ConfigurationProperties(prefix = "mse.web.ready-gate")
public class ReadyGateProperties {

    /**
     * 闸门总开关：true（默认）= ApplicationReadyEvent（晚于全部 ApplicationRunner）之前
     * 业务请求一律 503；false = 关闭闸门，行为与旧版一致（启动窗口内的请求直达业务）
     */
    private boolean enabled = true;

    /**
     * 未就绪期间仍放行的路径（Ant 风格）。默认放行 /actuator/**——探针语义由端点自身给出，
     * 不随闸门 503（K8s readinessProbe/compose healthcheck 行为与旧版一致）
     */
    private List<String> permitAntPatterns = List.of("/actuator/**");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getPermitAntPatterns() {
        return permitAntPatterns;
    }

    public void setPermitAntPatterns(List<String> permitAntPatterns) {
        this.permitAntPatterns = permitAntPatterns;
    }
}
