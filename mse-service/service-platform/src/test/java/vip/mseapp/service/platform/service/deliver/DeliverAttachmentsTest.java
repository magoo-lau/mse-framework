package vip.mseapp.service.platform.service.deliver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vip.mseapp.service.platform.entity.SysTenant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 交付附件签发（ADR-9 职责归位）单测：附件包合同（attachments format:1——studio 侧
 * POST /api/deliveries 的对端产物）、模块集过滤口径、deliver-studio 直推客户端（真实 HTTP 往返）。
 */
class DeliverAttachmentsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private SysTenant tenant() {
        SysTenant tenant = new SysTenant();
        tenant.setId(7L);
        tenant.setCode("demo");
        tenant.setName("演示租户");
        return tenant;
    }

    // ---- 附件包合同 ----

    @Test
    void attachmentsBundleMatchesStudioContract() throws IOException {
        Path dataZip = Files.createTempFile("inner-data", ".zip");
        Files.write(dataZip, "PK-fake-signed-inner".getBytes(StandardCharsets.UTF_8));
        Path out = Files.createTempFile("attachments", ".zip");
        Map<String, String> requires = new LinkedHashMap<>();
        requires.put("oa", "20261001.02");
        try {
            DeliverPackageService.writeAttachmentsBundle(out, "FULL", tenant(), "L-001", "oa,fs",
                    requires, "0.1.0", "TENANT_ID=7\nTENANT_CODE=demo\n",
                    "SET NAMES utf8mb4;\n", dataZip);
            try (ZipFile zip = new ZipFile(out.toFile())) {
                for (String want : new String[]{"attachments.json", "delivery/vars.env",
                        "delivery/deploy-package.zip", "extras/schema.sql"}) {
                    assertNotNull(zip.getEntry(want), "附件包缺条目: " + want);
                }
                assertEquals("PK-fake-signed-inner", readEntry(zip, "delivery/deploy-package.zip"));
                assertEquals("TENANT_ID=7\nTENANT_CODE=demo\n", readEntry(zip, "delivery/vars.env"));

                JsonNode decl = mapper.readTree(readEntry(zip, "attachments.json"));
                assertEquals(1, decl.path("format").asInt());
                assertEquals("demo", decl.path("tenant").path("code").asText());
                assertEquals(7, decl.path("tenant").path("id").asInt());
                assertEquals("L-001", decl.path("licenseId").asText());
                assertEquals("FULL", decl.path("packageType").asText());
                assertEquals("0.1.0", decl.path("requires").path("platformVersion").asText());
                assertEquals("20261001.02", decl.path("requires").path("perBizSchemaVersion").path("oa").asText());
                assertTrue(decl.path("modules").isArray());
                assertEquals("oa", decl.path("modules").path(0).asText());
                assertEquals("fs", decl.path("modules").path(1).asText());
                // files = 除描述符外逐文件 sha256（描述符自身不入清单——studio 校验合同）
                JsonNode files = decl.path("files");
                assertEquals(3, files.size());
                assertFalse(files.has("attachments.json"));
                for (String entry : new String[]{"delivery/vars.env", "delivery/deploy-package.zip", "extras/schema.sql"}) {
                    String expected = vip.mseapp.service.platform.service.deliver.TenantDataExporter
                            .sha256(readEntryBytes(zip, entry));
                    assertEquals(expected, files.path(entry).asText(), entry + " 哈希应与内容一致");
                }
            }
        } finally {
            Files.deleteIfExists(out);
            Files.deleteIfExists(dataZip);
        }
    }

    @Test
    void moduleFilterSemantics() {
        assertEquals(null, DeliverPackageService.moduleFilterOf(null));
        assertEquals(null, DeliverPackageService.moduleFilterOf(List.of()));
        assertEquals(null, DeliverPackageService.moduleFilterOf(List.of(" ", "")));
        var filter = DeliverPackageService.moduleFilterOf(List.of("oa", " fs ", "oa"));
        assertNotNull(filter);
        assertTrue(filter.contains("oa") && filter.contains("fs") && filter.size() == 2);
    }

    // ---- deliver-studio 直推客户端（JDK HttpServer 真实往返） ----

    private HttpServer server;
    private String tokenSeen;
    private byte[] fileSeen;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void studioPushLoginAndUpload() throws Exception {
        startServer(200, "{\"id\":7,\"tenantCode\":\"demo\"}", false);
        StudioPushClient client = StudioPushClient.of("http://127.0.0.1:" + server.getAddress().getPort() + "/",
                "admin", "secret");
        assertNotNull(client);
        Path bundle = Files.createTempFile("bundle", ".zip");
        byte[] payload = "fake-attachment-bundle".getBytes(StandardCharsets.UTF_8);
        Files.write(bundle, payload);
        try {
            long id = client.push(bundle, "attachments-demo-FULL-x.zip", "demo");
            assertEquals(7L, id);
            assertEquals("t-123", tokenSeen);
            assertNotNull(fileSeen);
            String multipart = new String(fileSeen, StandardCharsets.UTF_8);
            assertTrue(multipart.contains("name=\"origin\""));
            assertTrue(multipart.contains("push"));
            assertTrue(multipart.contains("filename=\"attachments-demo-FULL-x.zip\""));
            assertTrue(new String(fileSeen, StandardCharsets.UTF_8).contains("fake-attachment-bundle"));
        } finally {
            Files.deleteIfExists(bundle);
        }
    }

    @Test
    void studioPushSurfacesStudioRejection() throws Exception {
        startServer(400, "{\"error\":\"附件包缺少 attachments.json 描述符\"}", false);
        StudioPushClient client = StudioPushClient.of("http://127.0.0.1:" + server.getAddress().getPort(),
                "admin", "secret");
        Path bundle = Files.createTempFile("bundle", ".zip");
        Files.write(bundle, "x".getBytes(StandardCharsets.UTF_8));
        try {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> client.push(bundle, "b.zip", "demo"));
            assertTrue(e.getMessage().contains("attachments.json"), "studio 错误文案应透出: " + e.getMessage());
        } finally {
            Files.deleteIfExists(bundle);
        }
    }

    @Test
    void studioPushDisabledWithoutConfig() {
        assertEquals(null, StudioPushClient.of("", "admin", "secret"));
        assertEquals(null, StudioPushClient.of("http://x", "", "secret"));
        assertEquals(null, StudioPushClient.of("http://x", "admin", ""));
    }

    /** 模拟 studio：/api/login 按 /api/deliveries 记录 token 与请求体（原始字节，multipart 文件段含其中） */
    private void startServer(int deliverStatus, String deliverBody, boolean requireAuth) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/login", exchange -> {
            String body = """
                    {"token":"t-123"}
                    """;
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.createContext("/api/deliveries", exchange -> {
            tokenSeen = exchange.getRequestHeaders().getFirst("X-Studio-Token");
            fileSeen = exchange.getRequestBody().readAllBytes();
            byte[] out = deliverBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(deliverStatus, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
    }

    private static String readEntry(ZipFile zip, String name) throws IOException {
        return new String(readEntryBytes(zip, name), StandardCharsets.UTF_8);
    }

    private static byte[] readEntryBytes(ZipFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        try (var in = zip.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toByteArray();
        }
    }
}
