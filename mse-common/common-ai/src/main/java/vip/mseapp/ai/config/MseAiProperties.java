package vip.mseapp.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 基础设施配置（mse.ai.*）。保留扩展位：当前仅总开关，
 * 模型参数一律走 Spring AI 标准 spring.ai.* 配置（Nacos 管理）。
 */
@ConfigurationProperties(prefix = "mse.ai")
public class MseAiProperties {

    /** 总开关：关闭时不装配 mseChatClient（ChatModel 是否存在仍由类路径决定） */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
