package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 入站验签与防重放测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ReplayGuardTest {

    /** 默认窗口。 */
    private static final long WINDOW = 300_000L;

    private final MutableClock clock = new MutableClock(Instant.ofEpochMilli(1700000000000L));

    private final SignAlgorithm algorithm = new HmacSha256SignAlgorithm();

    private final ReplayGuard guard = new ReplayGuard(algorithm,
            new InMemoryNonceStore(clock), clock, WINDOW);

    private final SignKey key = new SignKey("app1", "secret");

    private final Map<String, SignKey> keys = Map.of("app1", key);

    /** 合法请求通过。 */
    @Test
    void acceptsValidRequest() {
        assertEquals(VerifyResult.SUCCESS, guard.verify(signedRequest(), keys));
    }

    /** 签名被篡改。 */
    @Test
    void rejectsTamperedSignature() {
        SignRequest signed = signedRequest();
        SignRequest tampered = new SignRequest(signed.method(), signed.path(),
                signed.queryParams(), signed.body(), signed.timestamp(), signed.nonce(),
                signed.keyId(), signed.alg(), signed.signature() + "0");

        assertEquals(VerifyResult.INVALID, guard.verify(tampered, keys));
    }

    /** 超出时间窗口。 */
    @Test
    void rejectsExpiredRequest() {
        SignRequest signed = signedRequest();
        clock.advance(Duration.ofMillis(WINDOW + 1));

        assertEquals(VerifyResult.INVALID, guard.verify(signed, keys));
    }

    /** 同一请求重放。 */
    @Test
    void rejectsReplay() {
        SignRequest signed = signedRequest();

        assertEquals(VerifyResult.SUCCESS, guard.verify(signed, keys));
        assertEquals(VerifyResult.INVALID, guard.verify(signed, keys));
    }

    /** 恰好落在窗口边界即拒绝，避免与 nonce 过期边界形成重放窗口。 */
    @Test
    void rejectsAtExactWindowBoundary() {
        SignRequest signed = signedRequest();
        clock.advance(Duration.ofMillis(WINDOW));

        assertEquals(VerifyResult.INVALID, guard.verify(signed, keys));
    }

    /** 超窗口请求在签名计算前就被拒绝。 */
    @Test
    void doesNotComputeSignatureForExpiredRequest() {
        java.util.concurrent.atomic.AtomicInteger signCalls =
                new java.util.concurrent.atomic.AtomicInteger();
        SignAlgorithm counting = new SignAlgorithm() {

            @Override
            public String name() {
                return HmacSha256SignAlgorithm.NAME;
            }

            @Override
            public String sign(String canonical, String secret) {
                signCalls.incrementAndGet();
                return "deadbeef";
            }
        };
        ReplayGuard countingGuard = new ReplayGuard(counting,
                new InMemoryNonceStore(clock), clock, WINDOW);
        SignRequest signed = signedRequest();
        clock.advance(Duration.ofMillis(WINDOW));

        assertEquals(VerifyResult.INVALID, countingGuard.verify(signed, keys));
        assertEquals(0, signCalls.get());
    }

    /** 自定义算法同时作用于签名与验签。 */
    @Test
    void verifiesWithCustomAlgorithm() {
        SignAlgorithm custom = new SignAlgorithm() {

            @Override
            public String name() {
                return "CUSTOM";
            }

            @Override
            public String sign(String canonical, String secret) {
                return "custom-" + canonical.hashCode();
            }
        };
        ReplayGuard customGuard = new ReplayGuard(custom,
                new InMemoryNonceStore(clock), clock, WINDOW);

        assertEquals(VerifyResult.SUCCESS,
                customGuard.verify(signedRequest(key, custom), keys));
    }

    /** 多 keyId 并存，各自用对应密钥验签。 */
    @Test
    void supportsMultipleKeys() {
        SignKey second = new SignKey("app2", "secret2");
        Map<String, SignKey> allKeys = Map.of("app1", key, "app2", second);

        assertEquals(VerifyResult.SUCCESS,
                guard.verify(signedRequest(key, algorithm), allKeys));
        assertEquals(VerifyResult.SUCCESS,
                guard.verify(signedRequest(second, algorithm), allKeys));
    }

    /** 时间戳 0 视为格式非法，1 视为合法格式（随后由窗口拒绝）。 */
    @Test
    void enforcesTimestampFormatBoundary() {
        SignRequest signed = signedRequest();
        SignRequest zero = new SignRequest(signed.method(), signed.path(), signed.queryParams(),
                signed.body(), 0L, signed.nonce(), signed.keyId(), signed.alg(),
                signed.signature());
        SignRequest one = new SignRequest(signed.method(), signed.path(), signed.queryParams(),
                signed.body(), 1L, signed.nonce(), signed.keyId(), signed.alg(),
                signed.signature());

        assertThrows(IllegalArgumentException.class, () -> guard.verify(zero, keys));
        assertEquals(VerifyResult.INVALID, guard.verify(one, keys));
    }

    /** 窗口必须为正：0 拒绝，1 接受。 */
    @Test
    void validatesWindowBounds() {
        assertThrows(IllegalArgumentException.class, () -> new ReplayGuard(algorithm,
                new InMemoryNonceStore(clock), clock, 0L));

        ReplayGuard oneMillis = new ReplayGuard(algorithm,
                new InMemoryNonceStore(clock), clock, 1L);
        assertEquals(VerifyResult.SUCCESS, oneMillis.verify(signedRequest(), keys));
    }

    /** 未知 keyId 统一失败。 */
    @Test
    void rejectsUnknownKeyId() {
        assertEquals(VerifyResult.INVALID, guard.verify(signedRequest(), Map.of()));
    }

    /** 算法标识不匹配统一失败。 */
    @Test
    void rejectsMismatchedAlgorithm() {
        SignRequest signed = signedRequest();
        SignRequest other = new SignRequest(signed.method(), signed.path(), signed.queryParams(),
                signed.body(), signed.timestamp(), signed.nonce(), signed.keyId(), "OTHER",
                signed.signature());

        assertEquals(VerifyResult.INVALID, guard.verify(other, keys));
    }

    /** 格式非法抛参数异常。 */
    @Test
    void rejectsMalformedRequest() {
        SignRequest signed = signedRequest();
        SignRequest missingNonce = new SignRequest(signed.method(), signed.path(),
                signed.queryParams(), signed.body(), signed.timestamp(), null,
                signed.keyId(), signed.alg(), signed.signature());

        assertThrows(IllegalArgumentException.class, () -> guard.verify(missingNonce, keys));
    }

    /** 描述信息不泄漏 body 与签名。 */
    @Test
    void descriptionsRedactSensitiveData() {
        SignRequest signed = signedRequest();

        assertFalse(signed.toString().contains("{}"));
        assertFalse(signed.toString().contains(signed.signature()));
        assertFalse(signed.toString().contains("secret"));
    }

    /** 构造一个合法签名请求。 */
    private SignRequest signedRequest() {
        return signedRequest(key, algorithm);
    }

    /** 用指定密钥与算法构造合法签名请求。 */
    private SignRequest signedRequest(SignKey signingKey, SignAlgorithm signingAlgorithm) {
        Map<String, String> params = Signs.sign(
                UnsignedRequest.of("POST", "/openapi/orders", Map.of("a", List.of("1")), "{}"),
                signingKey, signingAlgorithm, clock);
        return new SignRequest("POST", "/openapi/orders", Map.of("a", List.of("1")), "{}",
                Signs.parseTimestamp(params.get(Signs.PARAM_TIMESTAMP)),
                params.get(Signs.PARAM_NONCE), signingKey.keyId(),
                params.get(Signs.PARAM_ALG), params.get(Signs.PARAM_SIGN));
    }
}
