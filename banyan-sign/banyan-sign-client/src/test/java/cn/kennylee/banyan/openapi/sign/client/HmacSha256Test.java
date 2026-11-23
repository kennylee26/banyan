package cn.kennylee.banyan.openapi.sign.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HMAC-SHA256 已知答案测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class HmacSha256Test {

    /** RFC 4231 Test Case 2 的已知答案。 */
    private static final String RFC4231_CASE2 =
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843";

    /** 与服务端实现输出一致。 */
    @Test
    void matchesRfc4231Vector() {
        // when
        assertEquals(RFC4231_CASE2,
                HmacSha256.sign("what do ya want for nothing?", "Jefe"));
    }
}
