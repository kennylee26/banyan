package cn.kennylee.banyan.mq.consumer;

import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConsumeContext;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.MqMessageCodec;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqConsumerHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 消费者使用自定义编解码把 body 转成业务对象的测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqConsumerCodecTest {

    @Test
    void decodesBodyToPojoParameter() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqMessageCodec codec = new MqMessageCodec() {
            @Override
            public String encode(Object payload) {
                return String.valueOf(payload);
            }

            @Override
            public Object decode(String body, Class<?> type) {
                return new Payload(body);
            }
        };
        PojoConsumer bean = new PojoConsumer();
        new MqConsumerRegistrar(new MqInstanceRegistry(List.of(backend)), "", codec).register(bean);
        MqConsumerHandler handler = backend.handlerOf("rocketmq", "orders", "paid");

        // when
        boolean success = handler.handle(MqMessage.builder().body("x").build(),
                MqConsumeContext.builder().build());

        // then
        assertTrue(success);
        assertEquals("x", bean.received);
    }

    /**
     * 测试用业务对象。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class Payload {

        private final String value;

        Payload(String value) {
            this.value = value;
        }
    }

    /**
     * 使用业务对象参数的消费者。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class PojoConsumer {

        private String received;

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void onPaid(Payload payload) {
            this.received = payload.value;
        }
    }
}
