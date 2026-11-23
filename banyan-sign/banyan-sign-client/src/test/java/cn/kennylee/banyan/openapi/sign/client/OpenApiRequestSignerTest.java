package cn.kennylee.banyan.openapi.sign.client;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端签名门面测试：加密、明文、basePath 与参数校验。
 *
 * @author kennylee
 * @since 0.1.0
 */
class OpenApiRequestSignerTest {

    /** 固定时钟。 */
    private static final Clock CLOCK =
            Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC);

    /** 固定密钥。 */
    private static final String KEY_ID = "app1";

    /** 固定密钥明文。 */
    private static final String SECRET = "secret-for-tests";

    /** 协议算法标识。 */
    private static final String ALG = "Hmac" + "SHA256";

    /** 加密模式返回五个 header 与密文。 */
    @Test
    void signsEncryptedPayload() {
        // given
        OpenApiRequestSigner signer = signer().build();
        UnsignedRequest request = new UnsignedRequest("POST", "/openapi/orders",
                Map.of("page", List.of("1")), "{\"id\":1}");

        // when
        SignedRequest signed = signer.sign(request);

        // then
        assertEquals(5, signed.headers().size());
        assertNotEquals(request.body(), signed.body());
        assertEquals(request.body(), new AesGcmCipher().decrypt(signed.body(), SECRET));
    }

    /** 连续两次加密结果不同。 */
    @Test
    void producesDifferentCiphertextEachCall() {
        // given
        OpenApiRequestSigner signer = signer().build();
        UnsignedRequest request = new UnsignedRequest("POST", "/openapi/orders",
                Map.of(), "{\"id\":1}");

        // when
        SignedRequest first = signer.sign(request);
        SignedRequest second = signer.sign(request);

        // then
        assertNotEquals(first.body(), second.body());
    }

    /** 明文模式不加密 body。 */
    @Test
    void supportsPlainBody() {
        // given
        OpenApiRequestSigner signer = signer().plainBody().build();

        // when
        SignedRequest signed = signer.sign(new UnsignedRequest("POST", "/openapi/orders",
                Map.of(), "{\"id\":1}"));

        // then
        assertEquals("{\"id\":1}", signed.body());
    }

    /** 空 body 不加密。 */
    @Test
    void keepsEmptyBody() {
        // given
        OpenApiRequestSigner signer = signer().build();

        // when
        SignedRequest nullBody = signer.sign(new UnsignedRequest("GET", "/openapi/ping",
                Map.of(), null));
        SignedRequest emptyBody = signer.sign(new UnsignedRequest("GET", "/openapi/ping",
                Map.of(), ""));

        // then
        assertNull(nullBody.body());
        assertEquals("", emptyBody.body());
    }

    /** 自定义 header 名生效。 */
    @Test
    void usesCustomHeaderNames() {
        // given
        SignHeaders headers = new SignHeaders(
                "X-Acme-Alg", "X-Acme-Key", "X-Acme-Time", "X-Acme-Nonce", "X-Acme-Sign");
        OpenApiRequestSigner signer = signer().headers(headers).build();

        // when
        SignedRequest signed = signer.sign(new UnsignedRequest("GET", "/openapi/ping",
                Map.of(), null));

        // then
        assertTrue(signed.headers().containsKey("X-Acme-Sign"));
        assertTrue(signed.headers().containsKey("X-Acme-Nonce"));
    }

    /** basePath 从签名路径中剥离。 */
    @Test
    void stripsBasePathBeforeSigning() {
        // given
        OpenApiRequestSigner signer = signer().basePath("/api").build();

        // when
        SignedRequest signed = signer.sign(new UnsignedRequest("POST", "/api/openapi/orders",
                Map.of(), "{\"id\":1}"));

        // then
        String canonical = canonicalOf("/openapi/orders", signed);
        assertEquals(HmacSha256.sign(canonical, SECRET),
                signed.headers().get(SignHeaders.DEFAULT.sign()));
    }

    /** basePath 不匹配时立即失败。 */
    @Test
    void rejectsPathWithoutBasePath() {
        // given
        OpenApiRequestSigner signer = signer().basePath("/api").build();

        // when
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> signer.sign(new UnsignedRequest("GET", "/openapi/ping", Map.of(), null)));

        // then
        assertTrue(error.getMessage().contains("/api"));
    }

    /** 缺少 keyId 或 secret 时构建失败。 */
    @Test
    void requiresKeyMaterials() {
        // when
        assertThrows(IllegalArgumentException.class,
                () -> OpenApiRequestSigner.builder().secret(SECRET).build());
        assertThrows(IllegalArgumentException.class,
                () -> OpenApiRequestSigner.builder().keyId(KEY_ID).build());
    }

    /** 返回的 header 不可修改。 */
    @Test
    void returnsUnmodifiableHeaders() {
        // given
        SignedRequest signed = signer().build().sign(
                new UnsignedRequest("GET", "/openapi/ping", Map.of(), null));

        // when
        assertThrows(UnsupportedOperationException.class,
                () -> signed.headers().put("X-Test", "1"));
    }

    /** 描述不泄漏 secret。 */
    @Test
    void redactsSecretInDescription() {
        // when
        String description = signer().build().toString();

        // then
        assertFalse(description.contains(SECRET));
        assertTrue(description.contains(KEY_ID));
    }

    /** 使用注入的时钟生成时间戳。 */
    @Test
    void usesInjectedClock() {
        // when
        SignedRequest signed = signer().build().sign(
                new UnsignedRequest("GET", "/openapi/ping", Map.of(), null));

        // then
        assertEquals(Long.toString(CLOCK.millis()),
                signed.headers().get(SignHeaders.DEFAULT.timestamp()));
    }

    /** 构造测试用 Builder。 */
    private OpenApiRequestSigner.Builder signer() {
        return OpenApiRequestSigner.builder()
                .keyId(KEY_ID)
                .secret(SECRET)
                .clock(CLOCK);
    }

    /** 按给定 path 重算规范化串。 */
    private String canonicalOf(String path, SignedRequest signed) {
        Map<String, String> headers = signed.headers();
        return Signs.canonicalize(new CanonicalRequest("POST", path, Map.of(), signed.body(),
                Long.parseLong(headers.get(SignHeaders.DEFAULT.timestamp())),
                headers.get(SignHeaders.DEFAULT.nonce()), KEY_ID, ALG));
    }
}
