package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户侧一步式签名门面测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class OpenApiSignerTest {

    /** 固定时钟。 */
    private static final MutableClock CLOCK =
            new MutableClock(Instant.ofEpochMilli(1700000000000L));

    /** 默认验签窗口。 */
    private static final long WINDOW = ReplayGuard.DEFAULT_WINDOW_MILLIS;

    /** 待测密钥。 */
    private static final SignKey KEY = new SignKey("app1", "secret-for-aes-gcm");

    /** 默认加密实现。 */
    private static final PayloadCipher CIPHER = new AesGcmPayloadCipher();

    /** 加密模式返回可发送的 headers 与密文。 */
    @Test
    void signsEncryptedPayload() {
        // given
        OpenApiSigner signer = signer().build();
        UnsignedRequest request = UnsignedRequest.of("POST", "/openapi/orders",
                Map.of("page", List.of("1")), "{\"id\":1}");

        // when
        OpenApiSigner.SignedPayload payload = signer.sign(request);

        // then
        assertEquals(5, payload.headers().size());
        assertNotEquals(request.body(), payload.body());
        assertEquals(request.body(), CIPHER.decrypt(payload.body(), KEY.secret()));
        assertEquals(VerifyResult.SUCCESS, verify(request, payload).result());
    }

    /** 连续两次加密结果不同，但都能通过验签。 */
    @Test
    void producesDifferentCiphertextEachCall() {
        // given
        OpenApiSigner signer = signer().build();
        UnsignedRequest request = UnsignedRequest.of("POST", "/openapi/orders",
                Map.of(), "{\"id\":1}");

        // when
        OpenApiSigner.SignedPayload first = signer.sign(request);
        OpenApiSigner.SignedPayload second = signer.sign(request);

        // then
        assertNotEquals(first.body(), second.body());
        assertEquals(VerifyResult.SUCCESS, verify(request, first).result());
        assertEquals(VerifyResult.SUCCESS, verify(request, second).result());
    }

    /** 自定义 header 名生效。 */
    @Test
    void usesCustomHeaderNames() {
        // given
        SignHeaders headers = new SignHeaders(
                "X-Acme-Alg", "X-Acme-Key", "X-Acme-Time", "X-Acme-Nonce", "X-Acme-Sign");
        OpenApiSigner signer = signer().headers(headers).build();

        // when
        OpenApiSigner.SignedPayload payload = signer.sign(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null));

        // then
        assertEquals(5, payload.headers().size());
        assertTrue(payload.headers().containsKey("X-Acme-Sign"));
        assertTrue(payload.headers().containsKey("X-Acme-Nonce"));
    }

    /** 自定义加密实现生效。 */
    @Test
    void supportsCustomCipher() {
        // given
        PayloadCipher custom = new PayloadCipher() {

            @Override
            public String name() {
                return "test-cipher";
            }

            @Override
            public String encrypt(String plaintext, String secret) {
                return "test:" + plaintext;
            }

            @Override
            public String decrypt(String ciphertext, String secret) {
                return ciphertext.substring("test:".length());
            }
        };
        OpenApiSigner signer = signer().cipher(custom).build();

        // when
        OpenApiSigner.SignedPayload payload = signer.sign(
                UnsignedRequest.of("POST", "/openapi/orders", Map.of(), "{}"));

        // then
        assertEquals("test:{}", payload.body());
    }

    /** 按标识选择内置加密实现。 */
    @Test
    void selectsBuiltInCipherByIdentifier() {
        // given
        OpenApiSigner signer = signer().cipher(AesGcmPayloadCipher.NAME).build();

        // when
        OpenApiSigner.SignedPayload payload = signer.sign(
                UnsignedRequest.of("POST", "/openapi/orders", Map.of(), "{}"));

        // then
        assertEquals("{}", CIPHER.decrypt(payload.body(), KEY.secret()));
    }

    /** 关闭加密后直接发送明文。 */
    @Test
    void supportsPlainBody() {
        // given
        OpenApiSigner signer = signer().plainBody().build();
        UnsignedRequest request = UnsignedRequest.of("POST", "/openapi/orders",
                Map.of(), "{\"id\":1}");

        // when
        OpenApiSigner.SignedPayload payload = signer.sign(request);

        // then
        assertEquals(request.body(), payload.body());
        assertEquals(VerifyResult.SUCCESS, verify(request, payload).result());
    }

    /** 空 body 不加密。 */
    @Test
    void keepsEmptyBody() {
        // given
        OpenApiSigner signer = signer().build();

        // when
        OpenApiSigner.SignedPayload nullBody = signer.sign(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null));
        OpenApiSigner.SignedPayload emptyBody = signer.sign(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), ""));

        // then
        assertNull(nullBody.body());
        assertEquals("", emptyBody.body());
        assertEquals(VerifyResult.SUCCESS,
                verify(UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null), nullBody).result());
    }

    /** 未知签名算法立即失败。 */
    @Test
    void rejectsUnknownAlgorithm() {
        // when
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> signer().algorithm("MD5").build());

        // then
        assertTrue(error.getMessage().contains("algorithm"));
    }

    /** 未知加密实现立即失败，不静默回退。 */
    @Test
    void rejectsUnknownCipher() {
        // when
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> signer().cipher("sm4").build());

        // then
        assertTrue(error.getMessage().contains("cipher"));
    }

    /** 缺少 keyId 或 secret 时构建失败。 */
    @Test
    void requiresKeyMaterials() {
        // when
        assertThrows(IllegalArgumentException.class,
                () -> OpenApiSigner.builder().secret(KEY.secret()).build());
        assertThrows(IllegalArgumentException.class,
                () -> OpenApiSigner.builder().keyId(KEY.keyId()).build());
    }

    /** 返回的 headers 不可修改。 */
    @Test
    void returnsUnmodifiableHeaders() {
        // given
        OpenApiSigner.SignedPayload payload = signer().build().sign(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null));

        // when
        assertThrows(UnsupportedOperationException.class,
                () -> payload.headers().put("X-Test", "1"));
    }

    /** 描述不泄漏 secret。 */
    @Test
    void redactsSecretInDescription() {
        // given
        OpenApiSigner signer = signer().build();

        // when
        String description = signer.toString();

        // then
        assertFalse(description.contains(KEY.secret()));
        assertTrue(description.contains(KEY.keyId()));
    }

    /** 发送原始明文但沿用密文签名时验签失败。 */
    @Test
    void rejectsPlaintextBodyWithCiphertextSignature() {
        // given
        OpenApiSigner signer = signer().build();
        UnsignedRequest request = UnsignedRequest.of("POST", "/openapi/orders",
                Map.of(), "{\"id\":1}");
        OpenApiSigner.SignedPayload payload = signer.sign(request);
        ReplayGuard guard = new ReplayGuard(new HmacSha256SignAlgorithm(),
                new InMemoryNonceStore(CLOCK), CLOCK, WINDOW);

        // when
        VerifyOutcome outcome = guard.verifyDetailed(
                UnsignedRequest.of(request.method(), request.path(), request.queryParams(),
                        request.body()),
                payload.headers(), Map.of(KEY.keyId(), KEY));

        // then
        assertEquals(FailureReason.SIGNATURE_MISMATCH, outcome.reason());
    }

    /** 关闭加密时描述显示 none。 */
    @Test
    void describesPlainBodyMode() {
        // when
        String description = signer().plainBody().build().toString();

        // then
        assertTrue(description.contains("cipher=none"));
    }

    /** 使用注入的时钟生成时间戳。 */
    @Test
    void usesInjectedClock() {
        // given
        OpenApiSigner signer = signer().build();

        // when
        OpenApiSigner.SignedPayload payload = signer.sign(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null));

        // then
        assertEquals(Long.toString(CLOCK.millis()),
                payload.headers().get(SignHeaders.DEFAULT.timestamp()));
    }

    /** 构造测试用 Builder。 */
    private OpenApiSigner.Builder signer() {
        return OpenApiSigner.builder()
                .keyId(KEY.keyId())
                .secret(KEY.secret())
                .clock(CLOCK);
    }

    /** 用默认验签守卫校验返回结果。 */
    private VerifyOutcome verify(UnsignedRequest request, OpenApiSigner.SignedPayload payload) {
        ReplayGuard guard = new ReplayGuard(new HmacSha256SignAlgorithm(),
                new InMemoryNonceStore(CLOCK), CLOCK, WINDOW);
        UnsignedRequest received = UnsignedRequest.of(request.method(), request.path(),
                request.queryParams(), payload.body());
        return guard.verifyDetailed(received, payload.headers(), Map.of(KEY.keyId(), KEY));
    }
}
