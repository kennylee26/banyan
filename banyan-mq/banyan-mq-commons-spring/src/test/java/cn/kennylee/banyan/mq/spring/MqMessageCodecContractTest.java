package cn.kennylee.banyan.mq.spring;

import cn.kennylee.banyan.mq.core.MqMessageCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 消息编解码契约：每个实现都必须满足的基础行为。
 *
 * @author kennylee
 * @since 0.1.0
 */
abstract class MqMessageCodecContractTest {

    /**
     * 被测实现。
     *
     * @return 编解码器
     */
    protected abstract MqMessageCodec codec();

    @Test
    void encodesNullAsNull() {
        // when
        String encoded = codec().encode(null);

        // then
        assertNull(encoded);
    }

    @Test
    void decodesNullAsNull() {
        // when
        Object decoded = codec().decode(null, String.class);

        // then
        assertNull(decoded);
    }

    @Test
    void passesStringPayloadThrough() {
        // when
        String encoded = codec().encode("hello");

        // then
        assertEquals("hello", encoded);
    }

    @Test
    void returnsBodyForStringTarget() {
        // when
        Object decoded = codec().decode("hello", String.class);

        // then
        assertEquals("hello", decoded);
    }
}
