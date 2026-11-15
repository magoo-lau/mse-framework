package vip.mseapp.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * AI 基础设施自动装配（Spring AI 2.x）。
 *
 * <p>设计要点：本模块只提供 ChatClient 装配基线，不绑定任何模型厂商——具体模型 starter
 * （如 spring-ai-starter-model-openai）由使用方按需引入。没有 ChatModel Bean 时
 * Spring AI 的 ChatClient.Builder 不会装配，本类的 mseChatClient 随之退让，
 * 引入 common-ai 而不配模型是完全无副作用的。
 *
 * <p>模型连接参数（API Key、base-url、model 等）走标准 spring.ai.* 配置，
 * 微服务形态经 Nacos 留档管理，密钥用环境变量占位，不落仓库。
 */
@AutoConfiguration
@ConditionalOnClass(ChatClient.Builder.class)
@ConditionalOnProperty(prefix = "mse.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MseAiProperties.class)
public class MseAiAutoConfiguration {

    /**
     * 平台级默认 ChatClient：直接采用 Spring AI 默认装配参数（模型/温度经 spring.ai.* 配置调整）。
     * 业务有自定义需求时声明自己的 ChatClient Bean 即可整体替代。
     */
    @Bean
    @ConditionalOnBean(ChatClient.Builder.class)
    @ConditionalOnMissingBean(name = "mseChatClient")
    public ChatClient mseChatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
