package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 详细验签结果测试：原因映射、成功语义与副作用边界。
 *
 * @author kennylee
 * @since 0.1.0
 */
class ReplayGuardDetailedTest {

    /** 默认窗口。 */
    private static final long WINDOW = 300_000L;

    /** 可推进时钟。 */
    private final MutableClock clock = new MutableClock(Instant.ofEpochMilli(1700000000000L));

    /** 签名算法。 */
    private final SignAlgorithm algorithm = new HmacSha256SignAlgorithm();

    /** 待测密钥。 */
    private final SignKey key = new SignKey("app1", "secret");

    /** 密钥映射。 */
    private final Map<String, SignKey> keys = Map.of("app1", key);

    /** 签名被篡改时报告签名不匹配。 */
    @Test
    void reportsSignatureMismatch() {
        // given
        SignRequest signed = signedRequest();
        SignRequest tampered = new SignRequest(signed.method(), signed.path(),
                signed.queryParams(), signed.body(), signed.timestamp(), signed.nonce(),
                signed.keyId(), signed.alg(), signed.signature() + "0");
        ReplayGuard guard = guard(new InMemoryNonceStore(clock));

        // when
        VerifyOutcome outcome = guard.verifyDetailed(tampered, keys);

        // then
        assertEquals(VerifyResult.INVALID, outcome.result());
        assertEquals(FailureReason.SIGNATURE_MISMATCH, outcome.reason());
    }

    /** 超出窗口时报告已过期。 */
    @Test
    void reportsExpired() {
        // given
        SignRequest signed = signedRequest();
        ReplayGuard guard = guard(new InMemoryNonceStore(clock));
        clock.advance(Duration.ofMillis(WINDOW));

        // when
        VerifyOutcome outcome = guard.verifyDetailed(signed, keys);

        // then
        assertEquals(FailureReason.EXPIRED, outcome.reason());
    }

    /** 同一 nonce 第二次到达时报告重放。 */
    @Test
    void reportsReplay() {
        // given
        SignRequest signed = signedRequest();
        ReplayGuard guard = guard(new InMemoryNonceStore(clock));

        // when
        VerifyOutcome first = guard.verifyDetailed(signed, keys);
        VerifyOutcome second = guard.verifyDetailed(signed, keys);

        // then
        assertEquals(VerifyResult.SUCCESS, first.result());
        assertEquals(FailureReason.REPLAY, second.reason());
    }

    /** 未知 keyId 报告未知密钥。 */
    @Test
    void reportsUnknownKey() {
        // given
        ReplayGuard guard = guard(new InMemoryNonceStore(clock));

        // when
        VerifyOutcome outcome = guard.verifyDetailed(signedRequest(), Map.of());

        // then
        assertEquals(FailureReason.UNKNOWN_KEY, outcome.reason());
    }

    /** 算法标识不一致时报告算法不匹配。 */
    @Test
    void reportsAlgorithmMismatch() {
        // given
        SignRequest signed = signedRequest();
        SignRequest other = new SignRequest(signed.method(), signed.path(), signed.queryParams(),
                signed.body(), signed.timestamp(), signed.nonce(), signed.keyId(), "OTHER",
                signed.signature());
        ReplayGuard guard = guard(new InMemoryNonceStore(clock));

        // when
        VerifyOutcome outcome = guard.verifyDetailed(other, keys);

        // then
        assertEquals(FailureReason.ALG_MISMATCH, outcome.reason());
    }

    /** 缺少签名字段时报告格式非法。 */
    @Test
    void reportsMalformed() {
        // given
        SignRequest signed = signedRequest();
        SignRequest missingSign = new SignRequest(signed.method(), signed.path(),
                signed.queryParams(), signed.body(), signed.timestamp(), signed.nonce(),
                signed.keyId(), signed.alg(), null);
        ReplayGuard guard = guard(new InMemoryNonceStore(clock));

        // when
        VerifyOutcome outcome = guard.verifyDetailed(missingSign, keys);

        // then
        assertEquals(FailureReason.MALFORMED, outcome.reason());
    }

    /** header 路径的时间戳非法时报告格式非法。 */
    @Test
    void reportsMalformedHeaders() {
        // given
        ReplayGuard guard = guard(new InMemoryNonceStore(clock));

        // when
        VerifyOutcome outcome = guard.verifyDetailed(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null), Map.of(), keys);

        // then
        assertEquals(FailureReason.MALFORMED, outcome.reason());
    }

    /** 成功时不携带原因。 */
    @Test
    void successHasNoReason() {
        // given
        ReplayGuard guard = guard(new InMemoryNonceStore(clock));

        // when
        VerifyOutcome outcome = guard.verifyDetailed(signedRequest(), keys);

        // then
        assertEquals(VerifyResult.SUCCESS, outcome.result());
        assertNull(outcome.reason());
    }

    /** 过期请求不触发签名计算。 */
    @Test
    void expiredDoesNotComputeSignature() {
        // given
        AtomicInteger signCalls = new AtomicInteger();
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
        ReplayGuard guard = guard(counting, new InMemoryNonceStore(clock));
        SignRequest signed = signedRequest();
        clock.advance(Duration.ofMillis(WINDOW));

        // when
        guard.verifyDetailed(signed, keys);

        // then
        assertEquals(0, signCalls.get());
    }

    /** 失败请求不写 nonce 存储。 */
    @Test
    void failureDoesNotWriteNonce() {
        // given
        AtomicInteger writes = new AtomicInteger();
        NonceStore countingStore = (nonceKey, ttlMillis) -> {
            writes.incrementAndGet();
            return true;
        };
        ReplayGuard guard = guard(countingStore);
        SignRequest signed = signedRequest();
        SignRequest tampered = new SignRequest(signed.method(), signed.path(),
                signed.queryParams(), signed.body(), signed.timestamp(), signed.nonce(),
                signed.keyId(), signed.alg(), signed.signature() + "0");

        // when
        guard.verifyDetailed(tampered, keys);

        // then
        assertEquals(0, writes.get());
    }

    /** 成功请求只写一次 nonce 存储。 */
    @Test
    void successWritesNonceOnce() {
        // given
        AtomicInteger writes = new AtomicInteger();
        NonceStore countingStore = (nonceKey, ttlMillis) -> {
            writes.incrementAndGet();
            return true;
        };
        ReplayGuard guard = guard(countingStore);

        // when
        guard.verifyDetailed(signedRequest(), keys);

        // then
        assertEquals(1, writes.get());
    }

    /** 验签入口不会产生解密失败原因。 */
    @Test
    void neverReportsDecryptFailed() {
        // given
        ReplayGuard guard = guard(new InMemoryNonceStore(clock));
        SignRequest signed = signedRequest();
        SignRequest tampered = new SignRequest(signed.method(), signed.path(),
                signed.queryParams(), signed.body(), signed.timestamp(), signed.nonce(),
                signed.keyId(), signed.alg(), signed.signature() + "0");

        // when
        VerifyOutcome mismatch = guard.verifyDetailed(tampered, keys);
        VerifyOutcome unknown = guard.verifyDetailed(signed, Map.of());
        VerifyOutcome malformed = guard.verifyDetailed(
                UnsignedRequest.of("GET", "/openapi/ping", Map.of(), null), Map.of(), keys);

        // then
        assertNotEquals(FailureReason.DECRYPT_FAILED, mismatch.reason());
        assertNotEquals(FailureReason.DECRYPT_FAILED, unknown.reason());
        assertNotEquals(FailureReason.DECRYPT_FAILED, malformed.reason());
    }

    /** 用默认实现构造守卫。 */
    private ReplayGuard guard(NonceStore nonceStore) {
        return guard(algorithm, nonceStore);
    }

    /** 用指定算法与存储构造守卫。 */
    private ReplayGuard guard(SignAlgorithm signAlgorithm, NonceStore nonceStore) {
        return new ReplayGuard(signAlgorithm, nonceStore, clock, WINDOW);
    }

    /** 构造一个合法签名请求。 */
    private SignRequest signedRequest() {
        Map<String, String> params = Signs.sign(
                UnsignedRequest.of("POST", "/openapi/orders", Map.of("a", List.of("1")), "{}"),
                key, algorithm, clock);
        return new SignRequest("POST", "/openapi/orders", Map.of("a", List.of("1")), "{}",
                Signs.parseTimestamp(params.get(Signs.PARAM_TIMESTAMP)),
                params.get(Signs.PARAM_NONCE), key.keyId(),
                params.get(Signs.PARAM_ALG), params.get(Signs.PARAM_SIGN));
    }
}
