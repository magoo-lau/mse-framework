package vip.mseapp.boot.gateway.config;

import org.springframework.core.env.Environment;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Actuator 端点鉴权（WebFlux 版，与 common-web 的 {@code ActuatorAuthFilter} 同规则同配置）：
 * mse.actuator.auth.token 非空时，/actuator/** 除放行清单外一律要求令牌头，恒时比较、统一 401。
 *
 * <p>网关不走 Servlet 栈（starter-web 装配不加载），故在此等价实现；配置经 mse-micro-common.yaml
 * 与各服务同源（${MSE_ACTUATOR_TOKEN:} 占位）。读取方式同 {@link GatewayAuthProperties}：
 * Binder 启动绑定一次，不参与 spring-cloud refresh 重绑（令牌变更随重启生效）。
 */
@Component
public class ActuatorAuthWebFilter implements WebFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final String DEFAULT_HEADER = "X-Actuator-Token";

    private static final byte[] UNAUTHORIZED_BODY =
            "{\"code\":401,\"message\":\"actuator token 缺失或不正确\",\"data\":null,\"success\":false}"
                    .getBytes(StandardCharsets.UTF_8);

    private final String token;
    private final String header;
    private final List<String> permitAntPatterns;

    public ActuatorAuthWebFilter(Environment environment) {
        Binder binder = Binder.get(environment);
        this.token = binder.bind("mse.actuator.auth.token", String.class).orElse("");
        this.header = binder.bind("mse.actuator.auth.header", String.class).orElse(DEFAULT_HEADER);
        this.permitAntPatterns = binder
                .bind("mse.actuator.auth.permit-ant-patterns", Bindable.listOf(String.class))
                .orElse(List.of());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (token.isBlank()) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (!path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        for (String pattern : permitAntPatterns) {
            if (PATH_MATCHER.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }
        String presented = exchange.getRequest().getHeaders().getFirst(header);
        if (presented != null && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8))) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setRawStatusCode(401);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(UNAUTHORIZED_BODY)));
    }
}
