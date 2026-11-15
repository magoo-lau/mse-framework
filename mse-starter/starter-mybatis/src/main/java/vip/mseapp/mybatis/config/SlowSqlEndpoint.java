package vip.mseapp.mybatis.config;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import vip.mseapp.mybatis.slowsql.SlowSqlCollector;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 慢 SQL 观测端点：GET /actuator/slowsql。
 *
 * <p>监控中心（mse-monitor）拉取本端点采集慢 SQL；enabled=false 表示服务侧未开启
 * 慢 SQL 采集（mse.mybatis.slow-sql.enabled，默认 false），与"开启但暂无慢 SQL"区分。
 * 端点仅内网可达（网关不路由 /actuator/**），并受 starter-web 的 Actuator 端点鉴权保护
 * （mse.actuator.auth.token 非空时需 X-Actuator-Token 头，监控中心经同一令牌自动携带）；
 * SQL 只含静态文本与占位符，无参数值。
 */
@Endpoint(id = "slowsql")
public class SlowSqlEndpoint {

    private final SlowSqlCollector collector;

    public SlowSqlEndpoint(SlowSqlCollector collector) {
        this.collector = collector;
    }

    @ReadOperation
    public Map<String, Object> slowSql() {
        SlowSqlCollector.Snapshot snapshot = collector.snapshot();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", snapshot.enabled());
        result.put("thresholdMs", snapshot.thresholdMs());
        result.put("totalSlow", snapshot.totalSlow());
        result.put("recent", snapshot.recent());
        return result;
    }
}
