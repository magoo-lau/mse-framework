package vip.mseapp.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 启动就绪闸门：ApplicationReadyEvent（晚于全部 ApplicationRunner——结构迁移、共享库直建、
 * 车队重放等启动动作均已完成）之前，业务请求一律 503。
 *
 * <p>动机：Servlet 容器在上下文刷新期间就开始接收请求，而 Runner 在 Started 之后才执行，
 * 清库后首次启动的迁移窗口（十余秒）内进来的请求会撞上尚未建出的表刷缺表堆栈——
 * 闸门把这些请求收敛成一个干净的 503（Retry-After: 5），窗口结束自动放行。
 * {@code /actuator/**} 默认放行（K8s 探针/compose healthcheck 行为不变）。
 *
 * <p>窗口期拦截只打一条 WARN（含 method+uri），其余降 DEBUG 防刷屏。
 */
public class ReadyGateFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ReadyGateFilter.class);

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final String NOT_READY_BODY =
            "{\"code\":503,\"message\":\"服务启动中，请稍后重试\",\"data\":null,\"success\":false}";

    private final List<String> permitAntPatterns;

    private final AtomicBoolean ready = new AtomicBoolean(false);

    private volatile boolean loggedFirstBlock = false;

    public ReadyGateFilter(ReadyGateProperties properties) {
        this.permitAntPatterns = properties.getPermitAntPatterns();
    }

    /** 开闸（幂等）：由 starter-web 的 ReadyGateStartListener 在 ApplicationReadyEvent 时调用 */
    public void markReady() {
        ready.set(true);
    }

    public boolean isReady() {
        return ready.get();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : permitAntPatterns) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (ready.get()) {
            chain.doFilter(request, response);
            return;
        }
        if (loggedFirstBlock) {
            log.debug("[mse-web] 就绪闸门拦截（启动未完成）：{} {}", request.getMethod(), request.getRequestURI());
        } else {
            loggedFirstBlock = true;
            log.warn("[mse-web] 就绪闸门拦截：应用启动未完成（结构迁移/初始化 Runner 进行中），{} {} 返回 503，就绪后自动放行",
                    request.getMethod(), request.getRequestURI());
        }
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Retry-After", "5");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(NOT_READY_BODY);
    }
}
