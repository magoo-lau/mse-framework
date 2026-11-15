package vip.mseapp.boot.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 平台控制面微服务启动器（租户/套餐/数据源/服务配置管理，mse.rpc.mode=dubbo）（mse.rpc.mode=dubbo）。
 *
 * <p>基座 service 模块经 Spring Boot SPI 自装配：引入 service-platform 即得到
 * 全部接口与默认实现，启动类无需 scanBasePackages/@MapperScan；
 * 同契约 Bean 可整体替换默认实现（@ConditionalOnMissingBean 回退）。
 */
@SpringBootApplication
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
