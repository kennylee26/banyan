package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 签名算法与可替换实现测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SignAlgorithmTest {

    /** RFC 4231 Test Case 2 的已知答案。 */
    private static final String RFC4231_CASE2 =
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843";

    private final HmacSha256SignAlgorithm algorithm = new HmacSha256SignAlgorithm();

    /** 算法标识固定为 HmacSHA256。 */
    @Test
    void exposesAlgorithmName() {
        assertEquals("HmacSHA256", algorithm.name());
    }

    /** RFC 4231 known-answer 向量。 */
    @Test
    void matchesRfc4231Vector() {
        assertEquals(RFC4231_CASE2, algorithm.sign("what do ya want for nothing?", "Jefe"));
    }

    /** 验签成功与失败。 */
    @Test
    void verifiesSignature() {
        String signature = algorithm.sign("payload", "secret");

        assertTrue(algorithm.verify("payload", "secret", signature));
        assertFalse(algorithm.verify("payload", "secret", signature + "0"));
        assertFalse(algorithm.verify("payload", "secret", null));
    }

    /** 默认验签是常量时间比较，子类不得覆写为 equals。 */
    @Test
    void verificationIsConstantTimeContract() {
        assertThrows(NoSuchMethodException.class, () -> HmacSha256SignAlgorithm.class
                .getDeclaredMethod("verify", String.class, String.class, String.class));
    }

    /** 自定义算法实现可替换默认实现。 */
    @Test
    void supportsCustomAlgorithm() {
        SignAlgorithm custom = new SignAlgorithm() {

            @Override
            public String name() {
                return "TEST";
            }

            @Override
            public String sign(String canonical, String secret) {
                return "deadbeef";
            }
        };
        SignRequest request = SignRequest.outbound("GET", "/openapi/ping", Map.of(), null);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneOffset.UTC);

        Map<String, String> params = Signs.signOutbound(request, new SignKey("app1", "secret"),
                custom, clock);

        assertEquals("TEST", params.get("alg"));
        assertEquals("deadbeef", params.get("sign"));
    }
}
