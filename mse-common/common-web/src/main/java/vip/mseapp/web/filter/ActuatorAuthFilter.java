package vip.mseapp.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Actuator 端点鉴权：静态令牌保护 /actuator/**（注册路径 /actuator/*，未暴露端点的 404 探测
 * 也一并挡住，防端点枚举）。
 *
 * <p>token 为空 = 直接放行（零配置兼容现状，K8s 探针/mse-monitor 等匿名消费方不受影响）；
 * 配置后除 permitAntPatterns 外一律要求令牌头，恒时比较防逐字节试探，失败统一 401
 * （响应口径与网关 SaTokenGatewayConfig 一致）。
 */
public class ActuatorAuthFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final String UNAUTHORIZED_BODY =
            "{\"code\":401,\"message\":\"actuator token 缺失或不正确\",\"data\":null,\"success\":false}";

    private final ActuatorAuthProperties properties;

    public ActuatorAuthFilter(ActuatorAuthProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = properties.getToken();
        if (token == null || token.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        for (String pattern : properties.getPermitAntPatterns()) {
            if (PATH_MATCHER.match(pattern, path)) {
                chain.doFilter(request, response);
                return;
            }
        }
        String presented = request.getHeader(properties.getHeader());
        if (presented != null && MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(UNAUTHORIZED_BODY);
    }
}
