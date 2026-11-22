package cn.kennylee.banyan.mq.producer;

import cn.kennylee.banyan.mq.consumer.MqConsumer;
import cn.kennylee.banyan.mq.consumer.MqConsumerRegistrar;
import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 发送、实例路由、编解码与分发到业务方法的端到端测试（不连真实 broker）。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqEndToEndTest {

    @Test
    void routesSendToAnnotatedConsumerMethod() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq", MqCapability.TAG);
        MqInstanceRegistry registry = new MqInstanceRegistry(List.of(backend));
        MqProducer producer = new MqProducer(registry);
        OrderConsumer consumerBean = new OrderConsumer();
        new MqConsumerRegistrar(registry).register(consumerBean);

        // when
        producer.send(MqSendRequest.builder()
                .instance("rocketmq").topic("orders").tag("paid").payload("{\"id\":1}").build());
        boolean handled = backend.deliver("rocketmq", "orders", "paid", "{\"id\":1}");

        // then
        assertTrue(handled);
        assertEquals(1, backend.sent().size());
        assertEquals("{\"id\":1}", consumerBean.received);
    }

    @Test
    void doesNotDeliverToUnmatchedTag() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq", MqCapability.TAG);
        MqInstanceRegistry registry = new MqInstanceRegistry(List.of(backend));
        new MqConsumerRegistrar(registry).register(new OrderConsumer());

        // when
        boolean handled = backend.deliver("rocketmq", "orders", "refunded", "{}");

        // then
        assertFalse(handled);
    }

    /**
     * 测试用消费者：一个类里两个消费者方法。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class OrderConsumer {

        private String received;

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public boolean onPaid(MqMessage message) {
            this.received = message.getBody();
            return true;
        }

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "closed", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void onClosed(MqMessage message) {
            this.received = "closed:" + message.getBody();
        }
    }
}
