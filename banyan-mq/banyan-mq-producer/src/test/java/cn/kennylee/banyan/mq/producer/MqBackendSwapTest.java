package cn.kennylee.banyan.mq.producer;

import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 换后端不改业务代码：同一段发送代码分别跑在两个假后端上。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqBackendSwapTest {

    @Test
    void sameBusinessCodeWorksWithDifferentBackends() {
        // given
        FakeMqBackend kafka = new FakeMqBackend("kafka");
        FakeMqBackend rocketmq = new FakeMqBackend("rocketmq");
        MqProducer kafkaProducer = new MqProducer(new MqInstanceRegistry(List.of(kafka)));
        MqProducer rocketmqProducer = new MqProducer(new MqInstanceRegistry(List.of(rocketmq)));

        // when
        sendOrder(kafkaProducer);
        sendOrder(rocketmqProducer);

        // then
        assertEquals(1, kafka.messages().size());
        assertEquals(1, rocketmq.messages().size());
        assertEquals("{\"id\":1}", kafka.messages().get(0).getBody());
        assertEquals("{\"id\":1}", rocketmq.messages().get(0).getBody());
    }

    private static void sendOrder(MqProducer producer) {
        producer.send(MqSendRequest.builder()
                .topic("orders")
                .payload("{\"id\":1}")
                .build());
    }
}
