package cn.kennylee.banyan.mq.producer;

import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 多后端并存与按实例名路由的测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MultiBackendRoutingTest {

    @Test
    void routesToDifferentBackendsByInstance() {
        // given
        FakeMqBackend kafka = new FakeMqBackend("kafka");
        FakeMqBackend rocketmq = new FakeMqBackend("rocketmq");
        MqInstanceRegistry registry = new MqInstanceRegistry(List.of(kafka, rocketmq),
                Map.of("orders", "kafka", "notify", "rocketmq"));
        MqProducer producer = new MqProducer(registry);

        // when
        producer.send(request("orders"));
        producer.send(request("notify"));

        // then
        assertEquals(1, kafka.sent().size());
        assertEquals(1, rocketmq.sent().size());
    }

    @Test
    void singleBackendNeedsNoInstanceConfiguration() {
        // given
        FakeMqBackend kafka = new FakeMqBackend("kafka");
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(kafka)));

        // when
        producer.send(MqSendRequest.builder().topic("orders").payload("x").build());

        // then
        assertEquals(1, kafka.sent().size());
        assertEquals("kafka", kafka.sent().get(0).getInstance());
    }

    private static MqSendRequest request(String instance) {
        return MqSendRequest.builder().instance(instance).topic("orders").payload("x").build();
    }
}
