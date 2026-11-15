package vip.mseapp.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 邮件自动装配：mse.mail.enabled=true 时注册 {@link MseMailSender}。
 *
 * <p>SMTP 连接参数走 Spring Boot 标准 {@code spring.mail.*}（host/port/username/password/auth 等），
 * 发件人缺省取 spring.mail.username，可用 {@code mse.mail.from} 覆盖。enabled=true 但 spring.mail.host
 * 缺失时不抛启动异常——MseMailSender 内部持有 null sender，isAvailable()=false、发送时报错（半配置态可见）。
 * 声明同类型 Bean 即可整体替换，基座代码零改动。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "mse.mail", name = "enabled", havingValue = "true")
public class MailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MseMailSender.class)
    public MseMailSender mseMailSender(ObjectProvider<JavaMailSender> mailSender, Environment environment) {
        String from = environment.getProperty("mse.mail.from",
                environment.getProperty("spring.mail.username", ""));
        return new MseMailSender(mailSender.getIfAvailable(), from);
    }
}
