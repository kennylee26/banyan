package cn.kennylee.banyan.openapi.sign.client.httpclient5;

import cn.kennylee.banyan.openapi.sign.client.OpenApiRequestSigner;
import cn.kennylee.banyan.openapi.sign.client.SignTestSupport;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HttpClient5 适配器端到端测试：使用 JDK 内置 HTTP 服务捕获实际请求。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SigningHttpRequestInterceptorTest {

    /** 测试密钥。 */
    private static final String SECRET = "secret-for-tests";

    private HttpServer server;

    private final Map<String, String> captured = new LinkedHashMap<>();

    private volatile String capturedBody;

    private volatile boolean targetHit;

    /** 启动本地服务。 */
    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            capture(exchange);
            respond(exchange, 200);
        });
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/target");
            respond(exchange, 302);
        });
        server.createContext("/target", exchange -> {
            targetHit = true;
            respond(exchange, 200);
        });
        server.start();
    }

    /** 停止本地服务。 */
    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    /** 加密模式：body 被替换为密文并附加五个 header。 */
    @Test
    void signsEncryptedRequest() throws Exception {
        // given
        HttpPost post = new HttpPost(baseUrl() + "/openapi/orders?page=1");
        post.setEntity(new StringEntity("{\"id\":1}", ContentType.APPLICATION_JSON));

        // when
        try (CloseableHttpClient client = client();
             CloseableHttpResponse response = client.execute(post)) {
            assertEquals(200, response.getCode());
        }

        // then
        assertEquals("POST", captured.get("method"));
        assertEquals("/openapi/orders", captured.get("path"));
        assertNotEquals("{\"id\":1}", capturedBody);
        assertEquals(5, banyanHeaders().size());
        assertEquals(hmac(canonicalOf("/openapi/orders"), SECRET), header("x-banyan-sign"));
    }

    /** 原地签名后可以用任意 HttpClient 发送。 */
    @Test
    void signsRequestInPlaceWithAnyClient() throws Exception {
        // given
        HttpPost post = new HttpPost(baseUrl() + "/openapi/orders?page=1");
        post.setEntity(new StringEntity("{\"id\":2}", ContentType.APPLICATION_JSON));
        SigningHttpRequests.sign(signer().build(), post);

        // when
        try (CloseableHttpClient client = HttpClientBuilder.create().build();
             CloseableHttpResponse response = client.execute(post)) {
            assertEquals(200, response.getCode());
        }

        // then
        assertNotEquals("{\"id\":2}", capturedBody);
        assertEquals(5, banyanHeaders().size());
        assertEquals(hmac(canonicalOf("/openapi/orders"), SECRET), header("x-banyan-sign"));
    }

    /** 明文模式：body 原样发送。 */
    @Test
    void sendsPlainBody() throws Exception {
        // given
        OpenApiRequestSigner signer = signer().plainBody().build();
        HttpPost post = new HttpPost(baseUrl() + "/openapi/orders");
        post.setEntity(new StringEntity("{\"id\":1}", ContentType.APPLICATION_JSON));

        // when
        try (CloseableHttpClient client = SigningHttpClients.builder(signer).build();
             CloseableHttpResponse response = client.execute(post)) {
            assertEquals(200, response.getCode());
        }

        // then
        assertEquals("{\"id\":1}", capturedBody);
        assertEquals(5, banyanHeaders().size());
    }

    /** 不可重复读取的 entity 明确失败。 */
    @Test
    void rejectsNonRepeatableEntity() throws Exception {
        // given
        HttpPost post = new HttpPost(baseUrl() + "/openapi/orders");
        post.setEntity(new InputStreamEntity(new ByteArrayInputStream("{}".getBytes(
                StandardCharsets.UTF_8)), ContentType.APPLICATION_JSON));

        // when
        try (CloseableHttpClient client = client()) {
            assertThrows(IllegalArgumentException.class, () -> client.execute(post));
        }
    }

    /** 可重复但不是文本的 entity 明确失败。 */
    @Test
    void rejectsFileEntity() throws Exception {
        // given
        Path file = Files.createTempFile("sign-test", ".json");
        Files.writeString(file, "{}", StandardCharsets.UTF_8);
        HttpPost post = new HttpPost(baseUrl() + "/openapi/orders");
        post.setEntity(new FileEntity(file.toFile(), ContentType.APPLICATION_JSON));

        // when
        try (CloseableHttpClient client = client()) {
            assertThrows(IllegalArgumentException.class, () -> client.execute(post));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /** 无论 Content-Type 声明什么字符集，body 都按 UTF-8 处理。 */
    @Test
    void treatsBodyAsUtf8RegardlessOfDeclaredCharset() throws Exception {
        // given
        OpenApiRequestSigner signer = signer().plainBody().build();
        byte[] utf8 = "{\"name\":\"中\"}".getBytes(StandardCharsets.UTF_8);
        HttpPost post = new HttpPost(baseUrl() + "/openapi/orders");
        post.setEntity(new ByteArrayEntity(utf8,
                ContentType.parse("application/json;charset=ISO-8859-1")));

        // when
        try (CloseableHttpClient client = SigningHttpClients.builder(signer).build();
             CloseableHttpResponse response = client.execute(post)) {
            assertEquals(200, response.getCode());
        }

        // then
        assertEquals("{\"name\":\"中\"}", capturedBody);
    }

    /** 默认不自动重试：连接被断开时只尝试一次。 */
    @Test
    void doesNotRetryAfterConnectionDrop() throws Exception {
        // given
        AtomicInteger hits = new AtomicInteger();
        HttpServer dropServer = HttpServer.create(new InetSocketAddress(0), 0);
        dropServer.createContext("/drop", exchange -> {
            if (hits.incrementAndGet() == 1) {
                exchange.close();
                return;
            }
            respond(exchange, 200);
        });
        dropServer.start();

        // when
        try (CloseableHttpClient client = client()) {
            HttpGet get = new HttpGet("http://localhost:"
                    + dropServer.getAddress().getPort() + "/drop");
            assertThrows(IOException.class, () -> client.execute(get));
        } finally {
            dropServer.stop(0);
        }

        // then
        assertEquals(1, hits.get());
    }

    /** 默认不跟随重定向。 */
    @Test
    void doesNotFollowRedirects() throws Exception {
        // when
        try (CloseableHttpClient client = client();
             CloseableHttpResponse response = client.execute(new HttpGet(baseUrl() + "/redirect"))) {
            assertEquals(302, response.getCode());
        }

        // then
        assertFalse(targetHit);
    }

    /** 构造签名门面。 */
    private OpenApiRequestSigner.Builder signer() {
        return OpenApiRequestSigner.builder().keyId("app1").secret(SECRET);
    }

    /** 构造客户端。 */
    private CloseableHttpClient client() {
        return SigningHttpClients.builder(signer().build()).build();
    }

    /** 服务根地址。 */
    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    /** 按约定重算规范化串，验证签名与发出的 body 一致。 */
    private String canonicalOf(String path) {
        return SignTestSupport.canonical("POST", path, Map.of("page", List.of("1")),
                capturedBody, Long.parseLong(header("x-banyan-timestamp")),
                header("x-banyan-nonce"), "app1");
    }

    /** 大小写无关地取捕获的 header。 */
    private String header(String lowerName) {
        return captured.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(lowerName))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }


    /** HMAC-SHA256 小写十六进制。 */
    private static String hmac(String canonical, String secret) {
        return SignTestSupport.hmac(canonical, secret);
    }

    /** 只取五个签名 header。 */
    private Map<String, String> banyanHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        captured.forEach((name, value) -> {
            if (name.toLowerCase(java.util.Locale.ROOT).startsWith("x-banyan-")) {
                headers.put(name, value);
            }
        });
        return headers;
    }

    /** 记录请求。 */
    private void capture(HttpExchange exchange) throws IOException {
        captured.put("method", exchange.getRequestMethod());
        captured.put("path", exchange.getRequestURI().getRawPath());
        captured.put("query", exchange.getRequestURI().getRawQuery());
        exchange.getRequestHeaders().forEach((name, values) ->
                captured.put(name, values.isEmpty() ? "" : values.get(0)));
        capturedBody = new String(exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    /** 返回固定响应。 */
    private static void respond(HttpExchange exchange, int status) throws IOException {
        byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }
}
