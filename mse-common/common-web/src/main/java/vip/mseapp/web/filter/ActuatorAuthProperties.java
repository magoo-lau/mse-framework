package vip.mseapp.web.filter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Actuator 端点鉴权参数（mse.actuator.auth.*，自动装配在 starter-web）。
 *
 * <p>/actuator/** 与业务同端口暴露（health/info/metrics/slowsql 等），网关不路由该路径，
 * 进程内过滤器是单体/微服务两种形态下唯一全覆盖的保护点。静态令牌而非 Sa-Token 会话：
 * 消费方是 K8s 探针、mse-monitor 这类机器调用方，无用户会话可言。
 */
@ConfigurationProperties(prefix = "mse.actuator.auth")
public class ActuatorAuthProperties {

    /**
     * 共享令牌：空（默认）= 过滤器整体跳过、端点保持匿名——零配置兼容现状，本地开发零改动；
     * 非空后 /actuator/** 一律要求令牌头。生产经环境变量注入强随机值（openssl rand -hex 32），
     * K8s 探针与 mse-monitor 经同一环境变量自动对齐
     */
    private String token = "";

    /** 携带令牌的请求头名称（mse-monitor 探针侧按同名默认头对齐） */
    private String header = "X-Actuator-Token";

    /**
     * 免令牌放行路径（Ant 风格，仅作用于 /actuator/** 内）。默认空 = 含 health 全保护
     * （K8s 探针相应改为 exec + curl 带头）；想匿名放健康检查的环境自行配置，如 /actuator/health*
     */
    private List<String> permitAntPatterns = List.of();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public List<String> getPermitAntPatterns() {
        return permitAntPatterns;
    }

    public void setPermitAntPatterns(List<String> permitAntPatterns) {
        this.permitAntPatterns = permitAntPatterns;
    }
}
