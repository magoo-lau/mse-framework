package vip.mseapp.service.platform.service.deliver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * deliver-studio 直推客户端：登录换 token → POST /api/deliveries（multipart 上传附件包，origin=push）。
 * JDK HttpClient 零第三方依赖；附件包经流式 multipart 直传（ofOutputStream，大体包不双份占内存）。
 * 配置 mse.deliver.studio.{url,user,password}（url 空 = 未启用，仅手动下载上传通道）。
 * 推送失败由调用方兜底（签发不受影响），此处只抛异常。
 */
public class StudioPushClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final String baseUrl;
    private final String user;
    private final String password;

    private StudioPushClient(String baseUrl, String user, String password) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.user = user;
        this.password = password;
    }

    /** url/user/password 任一为空返回 null（未启用直推） */
    public static StudioPushClient of(String url, String user, String password) {
        if (url == null || url.isBlank() || user == null || user.isBlank() || password == null || password.isBlank()) {
            return null;
        }
        return new StudioPushClient(url.trim(), user.trim(), password);
    }

    public boolean enabled() {
        return true;
    }

    public String baseUrl() {
        return baseUrl;
    }

    /**
     * 推送附件包：登录 → multipart 上传 → 返回 studio 侧附件记录 id。
     * 大体包经管道流式上传（写线程产 multipart，请求体按 InputStream 消费，不双份占内存）。
     * studio 响应 {"error":"..."} 非 2xx 时抛 IllegalStateException（错误文案透出）。
     */
    public long push(Path zipFile, String fileName, String tenantCode) throws IOException, InterruptedException {
        String token = login();
        String boundary = "mse-deliver-" + new SecureRandom().nextInt(Integer.MAX_VALUE) + "-"
                + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(("" + System.nanoTime()).getBytes(StandardCharsets.UTF_8));
        java.io.PipedInputStream body = new java.io.PipedInputStream(64 * 1024);
        java.io.PipedOutputStream pipe = new java.io.PipedOutputStream(body);
        Thread writer = new Thread(() -> {
            try (OutputStream os = pipe) {
                writeField(os, boundary, "origin", "push");
                writeFileField(os, boundary, "file", fileName, zipFile);
                os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // 请求体被放弃（上传失败/连接中断）：关管道即可，主流程以 HTTP 结果为准
            }
        }, "mse-deliver-studio-push");
        writer.setDaemon(true);
        writer.start();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/deliveries"))
                .timeout(Duration.ofSeconds(600))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-Studio-Token", token)
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> body))
                .build();
        HttpResponse<String> resp;
        try {
            resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            body.close(); // 解阻塞写线程（写已关管道抛 IOException 退出）
            throw e;
        }
        writer.join(Duration.ofSeconds(10));
        JsonNode bodyJson = readJson(resp.body());
        if (resp.statusCode() / 100 != 2 || bodyJson.has("error")) {
            throw new IllegalStateException("deliver-studio 拒绝附件包（HTTP " + resp.statusCode() + "）: "
                    + bodyJson.path("error").asText(resp.body()));
        }
        long deliveryId = bodyJson.path("id").asLong(0);
        if (deliveryId <= 0) {
            throw new IllegalStateException("deliver-studio 响应缺少附件记录 id: " + resp.body());
        }
        return deliveryId;
    }

    private String login() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/login"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"user\":" + MAPPER.writeValueAsString(user)
                                + ",\"password\":" + MAPPER.writeValueAsString(password) + "}"))
                .build();
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode body = readJson(resp.body());
        if (resp.statusCode() / 100 != 2 || body.has("error")) {
            throw new IllegalStateException("deliver-studio 登录失败（HTTP " + resp.statusCode() + "）: "
                    + body.path("error").asText(resp.body()));
        }
        String token = body.path("token").asText("");
        if (token.isBlank()) {
            throw new IllegalStateException("deliver-studio 登录响应缺少 token");
        }
        return token;
    }

    private static JsonNode readJson(String body) throws IOException {
        try {
            return MAPPER.readTree(body == null || body.isBlank() ? "{}" : body);
        } catch (IOException e) {
            return MAPPER.createObjectNode();
        }
    }

    private static void writeField(OutputStream os, String boundary, String name, String value) throws IOException {
        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(value.getBytes(StandardCharsets.UTF_8));
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFileField(OutputStream os, String boundary, String name,
                                       String fileName, Path file) throws IOException {
        os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        os.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + fileName.replace("\"", "_") + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        os.write("Content-Type: application/zip\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        try (var in = java.nio.file.Files.newInputStream(file)) {
            in.transferTo(os);
        }
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
}
