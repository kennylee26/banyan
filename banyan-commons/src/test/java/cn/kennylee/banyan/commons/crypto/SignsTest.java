package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 签名门面的出站参数与时间戳解析测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class SignsTest {

    private final SignKey key = new SignKey("app1", "secret");

    /** 合法时间戳解析。 */
    @Test
    void parsesTimestamp() {
        assertEquals(123L, Signs.parseTimestamp("123"));
    }

    /** 非法时间戳转成带错误码的参数异常。 */
    @Test
    void rejectsInvalidTimestamp() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Signs.parseTimestamp("not-a-number"));

        assertTrue(exception.getMessage().contains(Signs.PARAM_TIMESTAMP));
    }

    /** 出站产出五个参数，且 nonce 每次不同。 */
    @Test
    void signOutboundProducesFiveParamsWithUniqueNonce() {
        SignRequest request = SignRequest.outbound("GET", "/openapi/ping", Map.of(), null);

        Map<String, String> first = Signs.signOutbound(request, key);
        Map<String, String> second = Signs.signOutbound(request, key);

        assertEquals(Set.of(Signs.PARAM_ALG, Signs.PARAM_KEY_ID, Signs.PARAM_TIMESTAMP,
                Signs.PARAM_NONCE, Signs.PARAM_SIGN), first.keySet());
        assertEquals("app1", first.get(Signs.PARAM_KEY_ID));
        assertEquals(HmacSha256SignAlgorithm.NAME, first.get(Signs.PARAM_ALG));
        assertEquals(32, first.get(Signs.PARAM_NONCE).length());
        assertNotEquals(first.get(Signs.PARAM_NONCE), second.get(Signs.PARAM_NONCE));
    }
}
