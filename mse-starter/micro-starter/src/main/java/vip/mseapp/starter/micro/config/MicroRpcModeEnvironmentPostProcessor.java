package vip.mseapp.starter.micro.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 微服务形态默认值：classpath 出现 micro-starter 且未显式配置 mse.rpc.mode 时注入 dubbo。
 * 属性源挂在最低优先级（addLast），application.yaml / Nacos 配置中心的任何显式配置均可覆盖；
 * 单体形态不引入本模块，common-rpc 保持 local 默认。
 */
public class MicroRpcModeEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getProperty("mse.rpc.mode") == null) {
            Map<String, Object> source = new HashMap<>(2);
            source.put("mse.rpc.mode", "dubbo");
            environment.getPropertySources().addLast(new MapPropertySource("mseMicroRpcModeDefaults", source));
        }
    }
}
