package vip.mseapp.starter.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import vip.mseapp.core.context.TenantContext;

import java.io.IOException;

/**
 * standalone 固定租户上下文：所有请求（含无 token 的登录页/公开接口）预置本机租户，
 * 认证后 token 权威值覆盖（登录桩返回固定租户 id，两者一致）；外部携带其他租户编号的
 * 请求头被权威值纠偏，无法越权切换租户。
 */
public class StandaloneTenantContextFilter extends OncePerRequestFilter {

    private final long tenantId;

    public StandaloneTenantContextFilter(long tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (TenantContext.getTenantId() == null) {
            TenantContext.setTenantId(tenantId);
        }
        chain.doFilter(request, response);
    }
}
