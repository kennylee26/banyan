package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
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
        // when
        assertEquals(VerifyResult.SUCCESS, guard.verify(signedRequest(), keys));
    }

    /** 签名被篡改。 */
    @Test
    void rejectsTamperedSignature() {
        // given
        SignRequest signed = signedRequest();
        SignRequest tampered = new SignRequest(signed.method(), signed.path(),
                signed.queryParams(), signed.body(), signed.timestamp(), signed.nonce(),
                signed.keyId(), signed.alg(), signed.signature() + "0");

        // when
        assertEquals(VerifyResult.INVALID, guard.verify(tampered, keys));
    }

    /** 超出时间窗口。 */
    @Test
    void rejectsExpiredRequest() {
        // given
        SignRequest signed = signedRequest();
        clock.advance(Duration.ofMillis(WINDOW + 1));

        // when
        assertEquals(VerifyResult.INVALID, guard.verify(signed, keys));
    }

    /** 同一请求重放。 */
    @Test
    void rejectsReplay() {
        // given
        SignRequest signed = signedRequest();

        // when
        assertEquals(VerifyResult.SUCCESS, guard.verify(signed, keys));
        assertEquals(VerifyResult.INVALID, guard.verify(signed, keys));
    }

    /** 恰好落在窗口边界即拒绝，避免与 nonce 过期边界形成重放窗口。 */
    @Test
    void rejectsAtExactWindowBoundary() {
        // given
        SignRequest signed = signedRequest();
        clock.advance(Duration.ofMillis(WINDOW));

        // when
        assertEquals(VerifyResult.INVALID, guard.verify(signed, keys));
    }

    /** 自定义算法同时作用于签名与验签。 */
    @Test
    void verifiesWithCustomAlgorithm() {
        // given
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

        // when
        assertEquals(VerifyResult.SUCCESS,
                customGuard.verify(signedRequest(key, custom), keys));
    }

    /** 多 keyId 并存，各自用对应密钥验签。 */
    @Test
    void supportsMultipleKeys() {
        // given
        SignKey second = new SignKey("app2", "secret2");
        Map<String, SignKey> allKeys = Map.of("app1", key, "app2", second);

        // when
        assertEquals(VerifyResult.SUCCESS,
                guard.verify(signedRequest(key, algorithm), allKeys));
        assertEquals(VerifyResult.SUCCESS,
                guard.verify(signedRequest(second, algorithm), allKeys));
    }

    /** 时间戳 0 统一返回 INVALID，1 属合法格式（随后由窗口拒绝）。 */
    @Test
    void enforcesTimestampFormatBoundary() {
        // given
        SignRequest signed = signedRequest();
        SignRequest zero = new SignRequest(signed.method(), signed.path(), signed.queryParams(),
                signed.body(), 0L, signed.nonce(), signed.keyId(), signed.alg(),
                signed.signature());
        SignRequest one = new SignRequest(signed.method(), signed.path(), signed.queryParams(),
                signed.body(), 1L, signed.nonce(), signed.keyId(), signed.alg(),
                signed.signature());

        // when
        VerifyResult zeroResult = guard.verify(zero, keys);
        VerifyResult oneResult = guard.verify(one, keys);

        // then
        assertEquals(VerifyResult.INVALID, zeroResult);
        assertEquals(VerifyResult.INVALID, oneResult);
    }

    /** 窗口必须为正：0 拒绝，1 接受。 */
    @Test
    void validatesWindowBounds() {
        // when
        assertThrows(IllegalArgumentException.class, () -> new ReplayGuard(algorithm,
                new InMemoryNonceStore(clock), clock, 0L));

        ReplayGuard oneMillis = new ReplayGuard(algorithm,
                new InMemoryNonceStore(clock), clock, 1L);
        assertEquals(VerifyResult.SUCCESS, oneMillis.verify(signedRequest(), keys));
    }

    /** 未知 keyId 统一失败。 */
    @Test
    void rejectsUnknownKeyId() {
        // when
        assertEquals(VerifyResult.INVALID, guard.verify(signedRequest(), Map.of()));
    }

    /** 算法标识不匹配统一失败。 */
    @Test
    void rejectsMismatchedAlgorithm() {
        // given
        SignRequest signed = signedRequest();
        SignRequest other = new SignRequest(signed.method(), signed.path(), signed.queryParams(),
                signed.body(), signed.timestamp(), signed.nonce(), signed.keyId(), "OTHER",
                signed.signature());

        // when
        assertEquals(VerifyResult.INVALID, guard.verify(other, keys));
    }

    /** 时间戳 0 即使窗口允许也判为未签名，返回 INVALID 而不是抛异常。 */
    @Test
    void rejectsZeroTimestampWithoutThrowing() {
        // given
        Clock epoch = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
        ReplayGuard epochGuard = new ReplayGuard(algorithm, new InMemoryNonceStore(epoch),
                epoch, WINDOW);
        SignRequest zero = new SignRequest("POST", "/openapi/orders", Map.of(), "{}",
                0L, "nonce-1", "app1", HmacSha256SignAlgorithm.NAME, "sign-1");

        // when
        VerifyResult result = epochGuard.verify(zero, keys);

        // then
        assertEquals(VerifyResult.INVALID, result);
    }

    /** 时间戳 1 是最早的合法值，签名正确时应通过。 */
    @Test
    void acceptsEarliestValidTimestamp() {
        // given
        Clock oneMillis = Clock.fixed(Instant.ofEpochMilli(1L), ZoneOffset.UTC);
        RequestSigner signer = new RequestSigner(algorithm, oneMillis);
        SignedRequest signed = signer.sign(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null), key);
        ReplayGuard oneGuard = new ReplayGuard(algorithm, new InMemoryNonceStore(oneMillis),
                oneMillis, WINDOW);
        SignRequest request = new SignRequest("GET", "/openapi/ping", Map.of(), null,
                Signs.parseTimestamp(signed.authParams().get(Signs.PARAM_TIMESTAMP)),
                signed.authParams().get(Signs.PARAM_NONCE),
                signed.authParams().get(Signs.PARAM_KEY_ID),
                signed.authParams().get(Signs.PARAM_ALG),
                signed.authParams().get(Signs.PARAM_SIGN));

        // when
        VerifyResult result = oneGuard.verify(request, keys);

        // then
        assertEquals(VerifyResult.SUCCESS, result);
    }

    /** 任一验签字段缺失都统一返回 INVALID，不抛异常。 */
    @Test
    void rejectsMalformedRequestsAsInvalid() {
        // given
        SignRequest signed = signedRequest();
        SignRequest missingNonce = new SignRequest(signed.method(), signed.path(),
                signed.queryParams(), signed.body(), signed.timestamp(), null,
                signed.keyId(), signed.alg(), signed.signature());
        SignRequest missingKeyId = new SignRequest(signed.method(), signed.path(),
                signed.queryParams(), signed.body(), signed.timestamp(), signed.nonce(),
                null, signed.alg(), signed.signature());
        SignRequest missingAlg = new SignRequest(signed.method(), signed.path(),
                signed.queryParams(), signed.body(), signed.timestamp(), signed.nonce(),
                signed.keyId(), null, signed.signature());
        SignRequest missingSign = new SignRequest(signed.method(), signed.path(),
                signed.queryParams(), signed.body(), signed.timestamp(), signed.nonce(),
                signed.keyId(), signed.alg(), null);

        // when
        assertEquals(VerifyResult.INVALID, guard.verify(missingNonce, keys));
        assertEquals(VerifyResult.INVALID, guard.verify(missingKeyId, keys));
        assertEquals(VerifyResult.INVALID, guard.verify(missingAlg, keys));
        assertEquals(VerifyResult.INVALID, guard.verify(missingSign, keys));
    }

    /** 描述信息不泄漏 body 与签名。 */
    @Test
    void descriptionsRedactSensitiveData() {
        // given
        SignRequest signed = signedRequest();

        // when
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
