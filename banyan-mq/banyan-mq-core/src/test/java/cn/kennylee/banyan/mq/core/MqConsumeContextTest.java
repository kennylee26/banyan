package cn.kennylee.banyan.mq.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 消费上下文公开字段与原生对象的契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqConsumeContextTest {

    @Test
    void exposesPublicFields() {
        // given
        Object nativeMessage = new Object();

        // when
        MqConsumeContext context = MqConsumeContext.builder()
                .topic("orders")
                .tag("paid")
                .messageId("m-1")
                .key("k-1")
                .headers(Map.of("tenant", "t1"))
                .retryTimes(2)
                .instance("kafka")
                .nativeMessage(nativeMessage)
                .build();

        // then
        assertEquals("orders", context.getTopic());
        assertEquals("paid", context.getTag());
        assertEquals("m-1", context.getMessageId());
        assertEquals("k-1", context.getKey());
        assertEquals("t1", context.getHeaders().get("tenant"));
        assertEquals(2, context.getRetryTimes());
        assertEquals("kafka", context.getInstance());
        assertSame(nativeMessage, context.getNativeMessage());
    }
}
