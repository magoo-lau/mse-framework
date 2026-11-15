package vip.mseapp.service.system.notify;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;
import vip.mseapp.api.notify.RemoteNotifyChannel;
import vip.mseapp.api.notify.dto.NotifyChannelDescriptor;
import vip.mseapp.api.notify.dto.NotifyMessage;
import vip.mseapp.core.exception.ServiceException;

/**
 * 框架内置 Webhook 群发渠道：钉钉/企微/飞书群机器人——地址与报文格式为租户级配置
 * （{@code sys_notify_webhook} 每世界一行），在「通知渠道设置」路由勾选处内联维护，
 * 本租户全部勾选 webhook 的路由共用一份。
 *
 * <p>群发渠道语义（receiverRequired=false）：不消费个人接收人，消息标题/正文按格式渲染后
 * POST 至群机器人地址；{@link #available} 即地址已配置，未配置时中枢门禁跳过（渠道不可用）。
 */
public class WebhookNotifyChannel implements RemoteNotifyChannel {

    /** 渠道编码（全局唯一；曾由 mse-monitor 业务渠道提供，v3.1 内置化收编） */
    public static final String CODE = "webhook";

    private static final int TIMEOUT_MS = 8_000;

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final SysNotifyWebhookService webhookService;

    public WebhookNotifyChannel(SysNotifyWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @Override
    public List<NotifyChannelDescriptor> describeChannels() {
        return List.of(new NotifyChannelDescriptor(CODE, "Webhook 机器人",
                "钉钉/企微/飞书群机器人（地址与格式在路由勾选处内联配置；群发，无需接收人）",
                false, null, false));
    }

    @Override
    public boolean available(String channelCode) {
        if (!CODE.equals(channelCode)) {
            return false;
        }
        String url = webhookService.current().getWebhookUrl();
        return url != null && !url.isBlank();
    }

    @Override
    public void send(String channelCode, NotifyMessage message) {
        var config = webhookService.current();
        try {
            HttpResponse<String> response = CLIENT.send(
                    HttpRequest.newBuilder(URI.create(config.getWebhookUrl().trim()))
                            .timeout(Duration.ofMillis(TIMEOUT_MS))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    webhookBody(config.getWebhookFormat(), message.getTitle(),
                                            message.getContent()), StandardCharsets.UTF_8))
                            .build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode() + ": "
                        + abbreviate(response.body()));
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            // 抛非受检异常由中枢记投递日志（站内信渠道同口径）
            throw new ServiceException(502, "Webhook 投递失败：" + abbreviate(e.getMessage()));
        }
    }

    /** 群机器人报文：钉钉/企微 markdown，飞书 text（post 类型结构复杂，text 足够） */
    private String webhookBody(String format, String title, String content) {
        Map<String, Object> body = new LinkedHashMap<>();
        String safeTitle = title == null ? "通知" : title;
        String safeContent = content == null ? "" : content;
        switch (format == null ? "dingtalk" : format) {
            case "wecom" -> {
                Map<String, Object> markdown = new LinkedHashMap<>();
                markdown.put("content", "**" + safeTitle + "**\n" + safeContent);
                body.put("msgtype", "markdown");
                body.put("markdown", markdown);
            }
            case "feishu" -> {
                Map<String, Object> inner = new LinkedHashMap<>();
                inner.put("text", safeTitle + "\n" + safeContent);
                body.put("msg_type", "text");
                body.put("content", inner);
            }
            default -> {
                Map<String, Object> markdown = new LinkedHashMap<>();
                markdown.put("title", safeTitle);
                markdown.put("text", "#### " + safeTitle + "\n\n" + safeContent.replace("\n", "\n\n"));
                body.put("msgtype", "markdown");
                body.put("markdown", markdown);
            }
        }
        return JSON.writeValueAsString(body);
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 200 ? text : text.substring(0, 200) + "…";
    }
}
