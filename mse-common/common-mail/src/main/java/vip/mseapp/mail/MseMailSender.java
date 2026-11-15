package vip.mseapp.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import vip.mseapp.core.exception.ServiceException;

/**
 * 邮件发送工具（JavaMailSender 薄封装，纯工具无自动装配；装配在 mail-starter）。
 *
 * <p>SMTP 连接走 Spring Boot 标准 {@code spring.mail.*} 配置。mse.mail.enabled=false（默认）时
 * 装配层不注册本 Bean，使用方经 {@code ObjectProvider<MseMailSender>} 判空即知邮箱通道不可用；
 * Bean 存在但 spring.mail.host 缺失时 {@link #isAvailable()} 返回 false，发送时报错而非静默吞掉。
 */
public class MseMailSender {

    private final JavaMailSender mailSender;
    private final String from;

    /** mailSender 允许为 null（enabled=true 但 spring.mail.host 未配置的半配置态，见 isAvailable） */
    public MseMailSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    /** 邮箱通道是否真正可用（SMTP 已配置）；使用方据此决定是否暴露邮箱验证码入口 */
    public boolean isAvailable() {
        return mailSender != null;
    }

    /**
     * 发送纯文本邮件（UTF-8）。
     *
     * @throws ServiceException SMTP 未配置或发送失败（连接超时、认证失败、收件地址非法等）
     */
    public void send(String to, String subject, String text) {
        if (mailSender == null) {
            throw new ServiceException(500, "邮件服务未配置（需开启 mse.mail.enabled 并配置 spring.mail.host）");
        }
        if (from == null || from.isBlank()) {
            throw new ServiceException(500, "邮件服务未配置发件人（mse.mail.from 或 spring.mail.username）");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            message.setFrom(from);
            message.setRecipients(MimeMessage.RecipientType.TO, to);
            message.setSubject(subject, "UTF-8");
            message.setText(text, "UTF-8");
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new ServiceException(500, "邮件构建失败：" + ex.getMessage());
        }
    }
}
