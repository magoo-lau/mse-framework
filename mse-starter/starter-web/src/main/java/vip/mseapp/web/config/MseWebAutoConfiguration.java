package vip.mseapp.web.config;

import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.UserContext;
import vip.mseapp.core.log.LogContext;
import vip.mseapp.web.aspect.TenantIgnoreAspect;
import vip.mseapp.web.filter.ActuatorAuthFilter;
import vip.mseapp.web.filter.ActuatorAuthProperties;
import vip.mseapp.web.filter.ReadyGateFilter;
import vip.mseapp.web.filter.ReadyGateProperties;
import vip.mseapp.web.filter.TenantContextFilter;
import vip.mseapp.web.filter.TraceIdFilter;
import vip.mseapp.web.handler.GlobalExceptionHandler;
import vip.mseapp.web.log.OperLogAspect;
import vip.mseapp.web.log.OperLogRecorder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 基础设施自动装配：traceId → 启动就绪闸门 → 租户上下文过滤器链 + actuator 端点鉴权 + 全局异常 + Jackson + TenantIgnore 切面。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({ActuatorAuthProperties.class, ReadyGateProperties.class})
public class MseWebAutoConfiguration {

    /** 过滤器顺序：traceId 最先、租户解析紧随其后（网关→认证→业务均需租户上下文） */
    public static final int TRACE_ID_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;
    public static final int TENANT_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;
    /** 就绪闸门在 traceId 之后（503 响应仍带 traceId）、租户解析之前（被拦请求不做上下文无用功） */
    public static final int READY_GATE_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 15;
    /** actuator 鉴权在上下文过滤器之后（令牌校验不依赖 traceId/租户上下文，仅保持链序可读） */
    public static final int ACTUATOR_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 30;

    @Bean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    @Bean
    public TenantContextFilter tenantContextFilter() {
        return new TenantContextFilter();
    }

    /**
     * 启动就绪闸门：ApplicationReadyEvent（晚于全部 ApplicationRunner）之前业务请求一律 503，
     * /actuator/** 默认放行；清库首启等迁移窗口内不再刷缺表堆栈。
     */
    @Bean
    @ConditionalOnProperty(prefix = "mse.web.ready-gate", name = "enabled", matchIfMissing = true)
    public ReadyGateFilter readyGateFilter(ReadyGateProperties properties) {
        return new ReadyGateFilter(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "mse.web.ready-gate", name = "enabled", matchIfMissing = true)
    public FilterRegistrationBean<ReadyGateFilter> readyGateFilterRegistration(ReadyGateFilter filter) {
        FilterRegistrationBean<ReadyGateFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(READY_GATE_FILTER_ORDER);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnProperty(prefix = "mse.web.ready-gate", name = "enabled", matchIfMissing = true)
    public ReadyGateStartListener readyGateStartListener(ReadyGateFilter filter) {
        return new ReadyGateStartListener(filter);
    }

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration(TraceIdFilter filter) {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(TRACE_ID_FILTER_ORDER);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantFilterRegistration(TenantContextFilter filter) {
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(TENANT_FILTER_ORDER);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /** actuator 端点鉴权：mse.actuator.auth.token 为空时过滤器内部直接放行（零配置兼容现状） */
    @Bean
    public FilterRegistrationBean<ActuatorAuthFilter> actuatorAuthFilterRegistration(ActuatorAuthProperties properties) {
        FilterRegistrationBean<ActuatorAuthFilter> registration =
                new FilterRegistrationBean<>(new ActuatorAuthFilter(properties));
        registration.setOrder(ACTUATOR_FILTER_ORDER);
        registration.addUrlPatterns("/actuator/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public DefaultJacksonCustomizer defaultJacksonCustomizer() {
        return new DefaultJacksonCustomizer();
    }

    @Bean
    public TenantIgnoreAspect tenantIgnoreAspect() {
        return new TenantIgnoreAspect();
    }

    /** 操作日志切面：Recorder 由持有日志表的模块（service-system）按需提供，缺省时切面静默跳过 */
    @Bean
    public OperLogAspect operLogAspect(ObjectProvider<OperLogRecorder> recorderProvider) {
        return new OperLogAspect(recorderProvider);
    }

    /**
     * 收口 Servlet 请求结束时对上下文的兜底清理（TenantContextFilter 已在 finally 清理，此处防御性双保险）。
     */
    @Bean
    public WebMvcConfigurer mseContextCleanupConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new HandlerInterceptor() {
                    @Override
                    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                                Object handler, Exception ex) {
                        TenantContext.clear();
                        UserContext.clear();
                        LogContext.clear();
                    }
                });
            }
        };
    }
}
