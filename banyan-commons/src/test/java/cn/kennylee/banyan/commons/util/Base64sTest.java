package cn.kennylee.banyan.commons.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Base64 编解码测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class Base64sTest {

    /** 编解码往返一致。 */
    @Test
    void encodeAndDecodeRoundTrip() {
        String expected = Base64.getEncoder().encodeToString("hello".getBytes(StandardCharsets.UTF_8));

        assertEquals(expected, Base64s.encode("hello"));
        assertEquals("hello", Base64s.decode(expected));
    }

    /** 支持非 ASCII 文本。 */
    @Test
    void handlesNonAscii() {
        String text = "中文";
        assertEquals(text, Base64s.decode(Base64s.encode(text)));
    }

    /** null 往返返回 null。 */
    @Test
    void nullRoundTrip() {
        assertNull(Base64s.encode(null));
        assertNull(Base64s.decode(null));
    }
}
