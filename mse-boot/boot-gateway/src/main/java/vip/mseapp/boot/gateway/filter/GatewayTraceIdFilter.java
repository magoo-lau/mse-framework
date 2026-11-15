package vip.mseapp.boot.gateway.filter;

import vip.mseapp.core.constant.MseConstants;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关链路追踪：入站请求缺 {@code X-Trace-Id} 时生成并注入下游请求头，同时回写响应头——
 * 全链路（网关 → 业务服务 → Dubbo 下游）以同一 traceId 串联日志。
 * 下游服务的 TraceIdFilter 直接透传该值不再各自生成；Dubbo 经 MseDubboContextFilter attachment 续传。
 *
 * <p>用 WebFilter 而非 GlobalFilter：WebFilter 在路由匹配与会话校验之前生效，
 * 未登录 401 短路响应同样携带 traceId，便于前端/排障取用。
 */
@Component
public class GatewayTraceIdFilter implements WebFilter, Ordered {

    /** 与各 Servlet 服务的 TraceIdFilter 同位：链路 id 在最入口定下来 */
    public static final int TRACE_ID_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(MseConstants.HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        final String tid = traceId;
        ServerWebExchange mutated = exchange.mutate()
                .request(builder -> builder.headers(headers -> headers.set(MseConstants.HEADER_TRACE_ID, tid)))
                .build();
        mutated.getResponse().getHeaders().set(MseConstants.HEADER_TRACE_ID, tid);
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return TRACE_ID_FILTER_ORDER;
    }
}
