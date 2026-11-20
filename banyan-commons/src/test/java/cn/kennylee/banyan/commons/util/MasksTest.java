package cn.kennylee.banyan.commons.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 脱敏工具测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class MasksTest {

    /** 手机号脱敏。 */
    @Test
    void maskMobile() {
        // when
        assertEquals("138****5678", Masks.maskMobile("13812345678"));
        assertEquals("123", Masks.maskMobile("123"));
        assertNull(Masks.maskMobile(null));
    }

    /** 邮箱脱敏。 */
    @Test
    void maskEmail() {
        // when
        assertEquals("a***@example.com", Masks.maskEmail("abc@example.com"));
        assertEquals("a***@b.com", Masks.maskEmail("a@b.com"));
        assertNull(Masks.maskEmail(null));
    }
}
