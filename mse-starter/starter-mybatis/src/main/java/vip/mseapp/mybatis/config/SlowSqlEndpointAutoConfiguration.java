package vip.mseapp.mybatis.config;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import vip.mseapp.mybatis.slowsql.SlowSqlCollector;

/**
 * 慢 SQL 端点装配：classpath 存在 actuator 时注册 /actuator/slowsql（是否对外暴露由
 * management.endpoints.web.exposure.include 控制）。无 actuator（纯工具场景）时整体跳过。
 */
@AutoConfiguration
@ConditionalOnClass(Endpoint.class)
public class SlowSqlEndpointAutoConfiguration {

    @Bean
    public SlowSqlEndpoint slowSqlEndpoint(SlowSqlCollector collector) {
        return new SlowSqlEndpoint(collector);
    }
}
