package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自定义 HTTP header 名测试：调用方与提供方使用同一份配置。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SignHeadersTest {

    /** 固定时钟。 */
    private static final Clock CLOCK =
            Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC);

    /** 默认 header 名。 */
    @Test
    void exposesDefaultHeaderNames() {
        // given
        SignHeaders defaults = SignHeaders.DEFAULT;

        // when
        assertEquals(Signs.HEADER_ALG, defaults.alg());
        assertEquals(Signs.HEADER_KEY_ID, defaults.keyId());
        assertEquals(Signs.HEADER_TIMESTAMP, defaults.timestamp());
        assertEquals(Signs.HEADER_NONCE, defaults.nonce());
        assertEquals(Signs.HEADER_SIGN, defaults.sign());
    }

    /** 五个名字都不能为空或重复；重复判断不区分大小写。 */
    @Test
    void rejectsBlankOrDuplicateNames() {
        // when
        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
                () -> new SignHeaders("", "k", "t", "n", "s"));
        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class,
                () -> new SignHeaders("a", "a", "t", "n", "s"));
        IllegalArgumentException caseInsensitive = assertThrows(IllegalArgumentException.class,
                () -> new SignHeaders("X-Alg", "x-alg", "t", "n", "s"));

        // then
        assertTrue(blank.getMessage().contains("header"));
        assertTrue(duplicate.getMessage().contains("header"));
        assertTrue(caseInsensitive.getMessage().contains("header"));
    }

    /** header 名只允许 RFC 7230 token 字符。 */
    @Test
    void rejectsIllegalHeaderCharacters() {
        // when
        IllegalArgumentException space = assertThrows(IllegalArgumentException.class,
                () -> new SignHeaders("X Alg", "k", "t", "n", "s"));
        IllegalArgumentException colon = assertThrows(IllegalArgumentException.class,
                () -> new SignHeaders("X:Alg", "k", "t", "n", "s"));
        IllegalArgumentException chinese = assertThrows(IllegalArgumentException.class,
                () -> new SignHeaders("签名", "k", "t", "n", "s"));

        // then
        assertTrue(space.getMessage().contains("header"));
        assertTrue(colon.getMessage().contains("header"));
        assertTrue(chinese.getMessage().contains("header"));
    }

    /** 调用方按自定义配置生成 headers。 */
    @Test
    void callerUsesCustomHeaderNames() {
        // given
        SignHeaders custom = new SignHeaders(
                "X-Acme-Alg", "X-Acme-Key", "X-Acme-Time", "X-Acme-Nonce", "X-Acme-Sign");
        RequestSigner signer = new RequestSigner(new HmacSha256SignAlgorithm(), CLOCK, custom);

        // when
        SignedRequest signed = signer.sign(
                UnsignedRequest.of("POST", "/openapi/orders",
                        Map.of("page", List.of("1")), "{}"),
                new SignKey("app1", "secret"));

        // then
        assertEquals(5, signed.headers().size());
        assertTrue(signed.headers().containsKey("X-Acme-Sign"));
        assertTrue(signed.headers().containsKey("X-Acme-Nonce"));
        assertEquals(signed.authParams().get(Signs.PARAM_SIGN),
                signed.headers().get("X-Acme-Sign"));
        assertEquals(custom, signed.headersConfig());
    }

    /** 提供方用同一份配置从 headers 验签通过。 */
    @Test
    void providerVerifiesWithSameCustomHeaders() {
        // given
        SignHeaders custom = new SignHeaders(
                "X-Acme-Alg", "X-Acme-Key", "X-Acme-Time", "X-Acme-Nonce", "X-Acme-Sign");
        RequestSigner caller = new RequestSigner(new HmacSha256SignAlgorithm(), CLOCK, custom);
        SignedRequest signed = caller.sign(
                UnsignedRequest.of("POST", "/openapi/orders",
                        Map.of("page", List.of("1")), "{}"),
                new SignKey("app1", "secret"));
        ReplayGuard guard = new ReplayGuard(new HmacSha256SignAlgorithm(),
                new InMemoryNonceStore(CLOCK), CLOCK,
                ReplayGuard.DEFAULT_WINDOW_MILLIS, custom);

        // when
        assertEquals(VerifyResult.SUCCESS,
                guard.verify(signed.request(), signed.headers(),
                        Map.of("app1", new SignKey("app1", "secret"))));
    }

    /** 提供方仍用默认 header 名时读不到自定义 header，统一失败。 */
    @Test
    void mismatchedHeaderConfigIsRejected() {
        // given
        SignHeaders custom = new SignHeaders(
                "X-Acme-Alg", "X-Acme-Key", "X-Acme-Time", "X-Acme-Nonce", "X-Acme-Sign");
        SignedRequest signed = new RequestSigner(new HmacSha256SignAlgorithm(), CLOCK, custom)
                .sign(UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null),
                        new SignKey("app1", "secret"));
        ReplayGuard defaultGuard = new ReplayGuard(new HmacSha256SignAlgorithm(),
                new InMemoryNonceStore(CLOCK), CLOCK, ReplayGuard.DEFAULT_WINDOW_MILLIS);

        // when
        assertEquals(VerifyResult.INVALID,
                defaultGuard.verify(signed.request(), signed.headers(),
                        Map.of("app1", new SignKey("app1", "secret"))));
    }

    /** 五个 header 中缺少任意一个都统一失败。 */
    @Test
    void anyMissingSingleHeaderIsRejected() {
        // given
        SignHeaders custom = new SignHeaders(
                "X-Acme-Alg", "X-Acme-Key", "X-Acme-Time", "X-Acme-Nonce", "X-Acme-Sign");
        RequestSigner caller = new RequestSigner(new HmacSha256SignAlgorithm(), CLOCK, custom);
        SignedRequest signed = caller.sign(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null),
                new SignKey("app1", "secret"));
        ReplayGuard guard = new ReplayGuard(new HmacSha256SignAlgorithm(),
                new InMemoryNonceStore(CLOCK), CLOCK,
                ReplayGuard.DEFAULT_WINDOW_MILLIS, custom);

        // when
        for (String name : signed.headers().keySet()) {
            java.util.Map<String, String> incomplete =
                    new java.util.HashMap<>(signed.headers());
            incomplete.remove(name);
            assertEquals(VerifyResult.INVALID,
                    guard.verify(signed.request(), incomplete,
                            Map.of("app1", new SignKey("app1", "secret"))),
                    "缺少 " + name);
        }
    }

    /** 时间戳字段不是数字时统一失败，不抛异常。 */
    @Test
    void malformedTimestampHeaderIsRejected() {
        // given
        SignHeaders custom = new SignHeaders(
                "X-Acme-Alg", "X-Acme-Key", "X-Acme-Time", "X-Acme-Nonce", "X-Acme-Sign");
        SignedRequest signed = new RequestSigner(new HmacSha256SignAlgorithm(), CLOCK, custom)
                .sign(UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null),
                        new SignKey("app1", "secret"));
        java.util.Map<String, String> bad = new java.util.HashMap<>(signed.headers());
        bad.put(custom.timestamp(), "not-a-number");
        ReplayGuard guard = new ReplayGuard(new HmacSha256SignAlgorithm(),
                new InMemoryNonceStore(CLOCK), CLOCK,
                ReplayGuard.DEFAULT_WINDOW_MILLIS, custom);

        // when
        assertEquals(VerifyResult.INVALID,
                guard.verify(signed.request(), bad,
                        Map.of("app1", new SignKey("app1", "secret"))));
    }
}
