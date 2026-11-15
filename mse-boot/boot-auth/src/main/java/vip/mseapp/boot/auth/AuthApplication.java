package vip.mseapp.boot.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证微服务启动器：纯 Dubbo 消费端（用户/租户数据经 RPC 获取），不直连数据库。
 *
 * <p>service-auth 经 Spring Boot SPI 自装配（登录端点 + 登录日志投递），
 * 启动类无需 scanBasePackages/@MapperScan。
 */
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
