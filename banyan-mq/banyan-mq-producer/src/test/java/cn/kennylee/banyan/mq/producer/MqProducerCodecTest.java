package cn.kennylee.banyan.mq.producer;

import cn.kennylee.banyan.mq.core.MqMessageCodec;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 生产者使用自定义编解码的测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqProducerCodecTest {

    @Test
    void encodesPayloadWithCustomCodec() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq");
        MqMessageCodec codec = new MqMessageCodec() {
            @Override
            public String encode(Object payload) {
                return "encoded:" + payload;
            }

            @Override
            public Object decode(String body, Class<?> type) {
                return body;
            }
        };
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(backend)), codec);

        // when
        producer.send(MqSendRequest.builder()
                .instance("rocketmq").topic("orders").payload("hello").build());

        // then
        assertEquals("encoded:hello", backend.messages().get(0).getBody());
    }
}
