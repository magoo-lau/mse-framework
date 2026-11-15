package vip.mseapp.web.filter;

import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.log.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 入口租户解析：读取 {@code X-Tenant-Id} 请求头写入 TenantContext，出口统一清理。
 *
 * <p>注意：本过滤器只负责“预置”，认证通过后 common-security 的拦截器会用 token 中的租户覆盖，
 * 防止伪造请求头越权访问其它租户数据。日志上下文（MDC）同步镜像：此处预置值先可见，
 * 认证后被拦截器以权威值覆盖。
 */
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String tenantId = request.getHeader(MseConstants.HEADER_TENANT_ID);
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.setTenantId(Long.parseLong(tenantId.trim()));
                LogContext.syncContext();
            }
            chain.doFilter(request, response);
        } catch (NumberFormatException e) {
            // 直接写统一信封 JSON，不走 sendError：/error 的 ERROR 分发不再经过过滤器链，
            // Sa-Token 上下文已解绑，兜底渲染会被劫持成内部文案“SaTokenContext 上下文尚未初始化”
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":400,\"message\":\"X-Tenant-Id 请求头格式非法\",\"data\":null,\"success\":false}");
        } finally {
            TenantContext.clear();
            LogContext.clearContext();
        }
    }
}
