package vip.mseapp.security.web;

import cn.dev33.satoken.exception.SaTokenException;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.UserContext;
import vip.mseapp.core.log.LogContext;
import vip.mseapp.security.stp.StpPlatformUtil;
import vip.mseapp.security.stp.StpTenantUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证结果 → 上下文同步拦截器。
 *
 * <p>关键安全语义：token 中的租户归属覆盖请求头 X-Tenant-Id，防止已登录用户伪造请求头越权；
 * 平台端 token 绑定平台租户（0），跨租户管理接口需显式标注 @TenantIgnore。
 * 日志上下文（MDC）随权威租户/用户同步刷新并镜像，出口清理（过滤器链出口另有兜底）。
 */
public class TenantTokenSyncInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Long authoritative = resolveAuthoritative();
        if (authoritative == null) {
            return true;
        }
        // 防串租户加固：显式租户标头必须等于会话租户（loginId 绑定），不一致即 401——
        // token 只能访问其登录租户的数据，任何标头伪造直接拒绝。
        // 抛 SaTokenException 交由 SaTokenExceptionAdvice 统一渲染（401 + 明确文案），
        // 不用 sendError：/error 的 ERROR 分发不再经过过滤器链，Sa-Token 上下文已解绑，
        // 兜底渲染会变成内部文案“SaTokenContext 上下文尚未初始化”
        String headerTenant = request.getHeader(MseConstants.HEADER_TENANT_ID);
        if (headerTenant != null && !headerTenant.isBlank()) {
            try {
                if (Long.parseLong(headerTenant.trim()) != authoritative) {
                    throw new SaTokenException("登录会话与 X-Tenant-Id 请求头不匹配，请求已拒绝");
                }
            } catch (NumberFormatException e) {
                throw new SaTokenException("X-Tenant-Id 请求头格式非法");
            }
        }
        return true;
    }

    /**
     * 解析会话权威租户（tenant=租户会话 / 0=平台会话 / null=匿名）。
     *
     * <p>ERROR 分发（sendError → /error）不再经过过滤器链，Sa-Token 上下文已解绑，
     * 此时任何 Stp 调用都会抛「SaTokenContext 上下文尚未初始化」并被兜底 advice 渲染成
     * 假 401、吞掉真实状态码——此处捕获放行，让 /error 正常渲染（本拦截器在错误页上
     * 无事可做：上下文同步与标头校验都只对真实业务请求有意义）。
     */
    private Long resolveAuthoritative() {
        try {
            if (StpTenantUtil.isLogin()) {
                TenantContext.setTenantId(StpTenantUtil.getTenantIdFromLoginId());
                UserContext.set(new UserContext.UserInfo(
                        StpTenantUtil.getUserIdFromLoginId(), StpTenantUtil.getUsername(), false));
                LogContext.syncContext();
                return TenantContext.getTenantId();
            }
            if (StpPlatformUtil.isLogin()) {
                TenantContext.setTenantId(MseConstants.PLATFORM_TENANT_ID);
                UserContext.set(new UserContext.UserInfo(
                        StpPlatformUtil.getUserId(), StpPlatformUtil.getUsername(), true));
                LogContext.syncContext();
                return MseConstants.PLATFORM_TENANT_ID;
            }
        } catch (SaTokenException e) {
            return null;
        }
        return null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        LogContext.clearContext();
    }
}
