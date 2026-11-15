package vip.mseapp.web.filter;

import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.log.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪：优先透传上游 traceId，否则生成；写入 MDC（pattern 经 %X{traceId} 输出）、响应头回写；
 * 出口全量清理日志上下文（最外层过滤器兜底，覆盖内层各写入点）。
 */
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(MseConstants.HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        LogContext.setTraceId(traceId);
        response.setHeader(MseConstants.HEADER_TRACE_ID, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            LogContext.clear();
        }
    }
}
