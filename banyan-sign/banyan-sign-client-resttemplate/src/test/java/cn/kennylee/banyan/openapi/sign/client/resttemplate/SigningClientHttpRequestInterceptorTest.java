package cn.kennylee.banyan.openapi.sign.client.resttemplate;

import cn.kennylee.banyan.openapi.sign.client.OpenApiRequestSigner;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RestTemplate 适配器端到端测试：使用 JDK 内置 HTTP 服务捕获实际请求。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SigningClientHttpRequestInterceptorTest {

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
    void signsEncryptedRequest() {
        // given
        RestTemplate rest = SigningRestTemplates.create(signer().build());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // when
        ResponseEntity<String> response = rest.exchange(baseUrl() + "/openapi/orders?page=1",
                HttpMethod.POST, new HttpEntity<>("{\"id\":1}", headers), String.class);

        // then
        assertEquals(200, response.getStatusCode().value());
        assertNotEquals("{\"id\":1}", capturedBody);
        assertEquals(5, banyanHeaders().size());
        assertEquals(hmac(canonicalOf(), SECRET), header("x-banyan-sign"));
    }

    /** 给已有 RestTemplate 挂拦截器；重复挂载不会重复添加。 */
    @Test
    void attachesToExistingTemplate() {
        // given
        RestTemplate rest = new RestTemplate();
        SigningRestTemplates.attach(rest, signer().build());
        SigningRestTemplates.attach(rest, signer().build());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // when
        rest.exchange(baseUrl() + "/openapi/orders?page=1", HttpMethod.POST,
                new HttpEntity<>("{\"id\":2}", headers), String.class);

        // then
        assertEquals(1, rest.getInterceptors().size());
        assertNotEquals("{\"id\":2}", capturedBody);
        assertEquals(5, banyanHeaders().size());
        assertEquals(hmac(canonicalOf(), SECRET), header("x-banyan-sign"));
    }

    /** 无论 Content-Type 声明什么字符集，body 都按 UTF-8 处理。 */
    @Test
    void treatsBodyAsUtf8RegardlessOfDeclaredCharset() {
        // given
        RestTemplate rest = SigningRestTemplates.create(signer().plainBody().build());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json;charset=ISO-8859-1"));
        byte[] utf8 = "{\"name\":\"中\"}".getBytes(StandardCharsets.UTF_8);

        // when
        rest.exchange(baseUrl() + "/openapi/orders", HttpMethod.POST,
                new HttpEntity<>(utf8, headers), String.class);

        // then
        assertEquals("{\"name\":\"中\"}", capturedBody);
    }

    /** 明文模式：body 原样发送。 */
    @Test
    void sendsPlainBody() {
        // given
        RestTemplate rest = SigningRestTemplates.create(signer().plainBody().build());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // when
        rest.exchange(baseUrl() + "/openapi/orders", HttpMethod.POST,
                new HttpEntity<>("{\"id\":1}", headers), String.class);

        // then
        assertEquals("{\"id\":1}", capturedBody);
        assertEquals(5, banyanHeaders().size());
    }

    /** multipart 请求明确失败。 */
    @Test
    void rejectsMultipartRequest() {
        // given
        RestTemplate rest = SigningRestTemplates.create(signer().build());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource("data".getBytes(StandardCharsets.UTF_8)));

        // when
        assertThrows(IllegalArgumentException.class, () -> rest.exchange(
                baseUrl() + "/openapi/upload", HttpMethod.POST, new HttpEntity<>(form, headers),
                String.class));
    }

    /** 默认不跟随重定向。 */
    @Test
    void doesNotFollowRedirects() {
        // given
        RestTemplate rest = SigningRestTemplates.create(signer().build());

        // when
        ResponseEntity<String> response = rest.getForEntity(baseUrl() + "/redirect", String.class);

        // then
        assertEquals(302, response.getStatusCode().value());
        assertFalse(targetHit);
    }

    /** 构造签名门面。 */
    private OpenApiRequestSigner.Builder signer() {
        return OpenApiRequestSigner.builder().keyId("app1").secret(SECRET);
    }

    /** 服务根地址。 */
    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    /** 按约定重算规范化串，验证签名与发出的 body 一致。 */
    private String canonicalOf() {
        return String.join("\n", "BANYAN-SIGN-V1", "POST", "/openapi/orders", "page=1",
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

    /** SHA-256 小写十六进制。 */
    private static String sha256(String text) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest
                    .getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** HMAC-SHA256 小写十六进制。 */
    private static String hmac(String canonical, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(
                    mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
