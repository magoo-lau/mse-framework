package vip.mseapp.job.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * mse-job 控制器装配（Spring Boot SPI）：观测端点 /job/state。
 * 开关：{@code mse.service.job.controller.enabled=false} 关闭默认控制器。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "mse.service.job.controller", name = "enabled", matchIfMissing = true)
@ComponentScan("vip.mseapp.job.controller")
public class JobControllerAutoConfiguration {
}
