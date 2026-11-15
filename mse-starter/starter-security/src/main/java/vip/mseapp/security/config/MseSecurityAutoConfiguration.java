package vip.mseapp.security.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpLogic;
import vip.mseapp.security.stp.StpPlatformUtil;
import vip.mseapp.security.stp.StpTenantUtil;
import vip.mseapp.security.web.SaTokenExceptionAdvice;
import vip.mseapp.security.web.TenantTokenSyncInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 安全自动装配：
 * <ul>
 *   <li>注册双账号体系 StpLogic（tenant / platform），构造即注册进 SaManager；</li>
 *   <li>SaInterceptor 开启注解式鉴权（@SaCheckLogin / @SaCheckPermission）；</li>
 *   <li>TenantTokenSyncInterceptor 在认证后同步租户/用户上下文（token 优先于请求头）。</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass(StpLogic.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MseSecurityAutoConfiguration {

    /**
     * 启动期强制注册双账号体系 StpLogic 到 SaManager。
     *
     * <p>StpXxxUtil 的静态实例在类首次使用时才初始化，而注解鉴权（SaInterceptor）
     * 可能先于任何工具类调用执行——没有本兜底会抛“未能获取对应StpLogic”。
     */
    @Bean
    public String mseStpLogicBootstrap() {
        cn.dev33.satoken.SaManager.putStpLogic(StpTenantUtil.stpLogic);
        cn.dev33.satoken.SaManager.putStpLogic(StpPlatformUtil.stpLogic);
        return "mse-stp-logic-bootstrap";
    }

    @Bean
    public SaTokenExceptionAdvice saTokenExceptionAdvice() {
        return new SaTokenExceptionAdvice();
    }

    @Bean
    public WebMvcConfigurer mseSaInterceptorConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                // 注解式鉴权（SaInterceptor 默认开启注解校验）
                registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
                // 认证后同步上下文：必须在 SaInterceptor 之后
                registry.addInterceptor(new TenantTokenSyncInterceptor()).addPathPatterns("/**");
            }
        };
    }
}
