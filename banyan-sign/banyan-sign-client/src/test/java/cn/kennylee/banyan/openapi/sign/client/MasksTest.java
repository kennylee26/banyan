package cn.kennylee.banyan.openapi.sign.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 日志脱敏测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MasksTest {

    /** 签名只保留前 8 位。 */
    @Test
    void masksSignature() {
        // when
        assertEquals("01234567...", Masks.first8("0123456789abcdef"));
    }

    /** 短值原样返回。 */
    @Test
    void keepsShortValue() {
        // when
        assertEquals("abc", Masks.first8("abc"));
    }

    /** null 返回占位。 */
    @Test
    void handlesNull() {
        // when
        assertEquals("<null>", Masks.first8(null));
    }
}
