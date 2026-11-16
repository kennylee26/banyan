package cn.kennylee.banyan.commons.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 十六进制编解码测试。
 *
 * @author kennylee
 * @since 0.1.0
 */

class HexsTest {

    /** 编解码往返一致。 */
    @Test
    void encodeAndDecodeRoundTrip() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);

        assertEquals("68656c6c6f", Hexs.encode(bytes));
        assertArrayEquals(bytes, Hexs.decode("68656c6c6f"));
    }

    /** null 往返返回 null。 */
    @Test
    void nullRoundTrip() {
        assertNull(Hexs.encode(null));
        assertNull(Hexs.decode(null));
    }
}
