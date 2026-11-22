package cn.kennylee.banyan.mq.spring;

import cn.kennylee.banyan.mq.core.MqMessageCodec;
import cn.kennylee.banyan.mq.core.StringMqMessageCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 默认 String 编解码的契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class StringMqMessageCodecTest extends MqMessageCodecContractTest {

    private final MqMessageCodec codec = new StringMqMessageCodec();

    @Override
    protected MqMessageCodec codec() {
        return codec;
    }

    @Test
    void decodesByteArray() {
        // when
        Object decoded = codec.decode("hi", byte[].class);

        // then
        assertArrayEquals(new byte[]{'h', 'i'}, (byte[]) decoded);
    }

    @Test
    void rejectsUnsupportedTargetType() {
        // when
        assertThrows(IllegalArgumentException.class, () -> codec.decode("x", Integer.class));
    }
}
