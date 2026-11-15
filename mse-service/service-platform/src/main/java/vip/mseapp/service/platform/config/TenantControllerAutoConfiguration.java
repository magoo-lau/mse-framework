package vip.mseapp.service.platform.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 租户管理控制器自动装配（Spring Boot SPI）。
 *
 * <p>接入方引入 jar 即获得全部租户管理接口，无需 component-scan 基座包名。
 * 与 {@link TenantServiceAutoConfiguration} 拆开的原因：控制器只在 Servlet Web
 * 环境有意义，非 Web 部署（纯 Dubbo 提供者等）不应注册；同时提供整体关闭开关，
 * 供接入方用自己的控制器全量替代（基座代码零改动）。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "mse.service.platform.controller", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "vip.mseapp.service.platform.controller")
public class TenantControllerAutoConfiguration {
}
