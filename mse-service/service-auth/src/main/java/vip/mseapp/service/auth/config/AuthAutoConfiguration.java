package vip.mseapp.service.auth.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import vip.mseapp.service.auth.support.LoginCaptchaService;
import vip.mseapp.service.auth.support.LoginLogRecorder;
import vip.mseapp.service.auth.support.LoginPolicyService;
import vip.mseapp.service.auth.support.LoginSessionManager;
import vip.mseapp.service.auth.support.TwoFactorTokenService;

/**
 * 认证服务自动装配（Spring Boot SPI）。
 *
 * <p>认证模块即登录 HTTP 端点（/auth/**），只在 Servlet Web 环境装配：
 * 接入方引入 jar 即获得登录/登出/当前身份/会话自助管理等接口，无需 component-scan 基座包名。
 *
 * <p><b>可覆盖性</b>：{@code mse.service.auth.controller.enabled=false} 可整体关闭
 * 基座登录端点，由接入方用自己的控制器替代；{@link LoginLogRecorder}、{@link LoginPolicyService}、
 * {@link LoginSessionManager} 均带缺省回退，声明同类型 Bean 即可整体替换
 * （如自定义登录策略解析），基座代码零改动。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "mse.service.auth.controller", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "vip.mseapp.service.auth.controller")
public class AuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LoginLogRecorder.class)
    public LoginLogRecorder loginLogRecorder() {
        return new LoginLogRecorder();
    }

    /** 登录策略解析（sys_config 参数：多端并发/同端互斥，租户可覆盖平台默认） */
    @Bean
    @ConditionalOnMissingBean(LoginPolicyService.class)
    public LoginPolicyService loginPolicyService() {
        return new LoginPolicyService();
    }

    /** 登录会话签发（按策略顶号 + 终端扩展信息） */
    @Bean
    @ConditionalOnMissingBean(LoginSessionManager.class)
    public LoginSessionManager loginSessionManager() {
        return new LoginSessionManager();
    }

    /** 登录图形验证码 + 风控失败计数（Redis；验证码模式/阈值经 sys_config 即时生效） */
    @Bean
    @ConditionalOnMissingBean(LoginCaptchaService.class)
    public LoginCaptchaService loginCaptchaService(org.springframework.data.redis.core.StringRedisTemplate redis) {
        return new LoginCaptchaService(redis);
    }

    /** 双因子阶段临时凭证（密码已过、因子未验的中间态；Redis 5 分钟 + 限 5 次尝试） */
    @Bean
    @ConditionalOnMissingBean(TwoFactorTokenService.class)
    public TwoFactorTokenService twoFactorTokenService(org.springframework.data.redis.core.StringRedisTemplate redis) {
        return new TwoFactorTokenService(redis);
    }
}
