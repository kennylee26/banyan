package cn.kennylee.banyan.openapi.sign.client.jdkhttp;

import cn.kennylee.banyan.openapi.sign.client.OpenApiRequestSigner;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * JDK HttpClient 适配器测试：签名后的请求能被真实 HTTP 服务收到。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SigningJdkHttpRequestsTest {

    /** 测试密钥。 */
    private static final String SECRET = "secret-for-tests";

    private HttpServer server;

    private final Map<String, String> captured = new LinkedHashMap<>();

    private volatile String capturedBody;

    /** 启动本地服务。 */
    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            capture(exchange);
            respond(exchange);
        });
        server.start();
    }

    /** 停止本地服务。 */
    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    /** 加密模式：body 为密文，签名与发送内容一致。 */
    @Test
    void signsEncryptedRequest() throws Exception {
        // given
        HttpRequest unsigned = HttpRequest.newBuilder(uri("/openapi/orders?page=1"))
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

        // when
        HttpRequest signed = SigningJdkHttpRequests.sign(signer(), unsigned, "{\"id\":1}");
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(signed, HttpResponse.BodyHandlers.ofString());

        // then
        assertEquals(200, response.statusCode());
        assertNotEquals("{\"id\":1}", capturedBody);
        assertEquals(5, banyanHeaders().size());
        assertEquals(hmac(canonicalOf("/openapi/orders", "page=1"), SECRET),
                header("x-banyan-sign"));
    }

    /** 明文模式：body 原样发送。 */
    @Test
    void sendsPlainBody() throws Exception {
        // given
        HttpRequest unsigned = HttpRequest.newBuilder(uri("/openapi/orders"))
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "application/json")
                .build();

        // when
        HttpRequest signed = SigningJdkHttpRequests.sign(plainSigner(), unsigned, "{\"id\":1}");
        HttpClient.newHttpClient().send(signed, HttpResponse.BodyHandlers.ofString());

        // then
        assertEquals("{\"id\":1}", capturedBody);
        assertEquals(5, banyanHeaders().size());
    }

    /** 无 body：签名空 body。 */
    @Test
    void signsEmptyBody() throws Exception {
        // given
        HttpRequest unsigned = HttpRequest.newBuilder(uri("/openapi/orders/ping")).GET().build();

        // when
        HttpRequest signed = SigningJdkHttpRequests.sign(signer(), unsigned, null);
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(signed, HttpResponse.BodyHandlers.ofString());

        // then
        assertEquals(200, response.statusCode());
        assertEquals(5, banyanHeaders().size());
    }

    /** multipart 请求明确失败。 */
    @Test
    void rejectsMultipartRequest() {
        // given
        HttpRequest unsigned = HttpRequest.newBuilder(uri("/openapi/orders"))
                .method("POST", HttpRequest.BodyPublishers.noBody())
                .header("Content-Type", "multipart/form-data; boundary=x")
                .build();

        // when
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> SigningJdkHttpRequests.sign(signer(), unsigned, "data"));
    }

    /** 构造签名门面。 */
    private OpenApiRequestSigner signer() {
        return OpenApiRequestSigner.builder().keyId("app1").secret(SECRET).build();
    }

    /** 构造明文模式签名门面。 */
    private OpenApiRequestSigner plainSigner() {
        return OpenApiRequestSigner.builder().keyId("app1").secret(SECRET)
                .plainBody().build();
    }

    /** 服务地址。 */
    private URI uri(String pathAndQuery) {
        return URI.create("http://localhost:" + server.getAddress().getPort() + pathAndQuery);
    }

    /** 按约定重算规范化串。 */
    private String canonicalOf(String path, String query) {
        return String.join("\n", "BANYAN-SIGN-V1", "POST", path, query,
                sha256(capturedBody), header("x-banyan-timestamp"),
                header("x-banyan-nonce"), "app1", "HmacSHA256");
    }

    /** 大小写无关地取捕获的 header。 */
    private String header(String lowerName) {
        return captured.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(lowerName))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
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
    private static void respond(HttpExchange exchange) throws IOException {
        byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    /** SHA-256 小写十六进制。 */
    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** HMAC-SHA256 小写十六进制。 */
    private static String hmac(String canonical, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
