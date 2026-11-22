package cn.kennylee.banyan.mq.producer;

import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConfigurationException;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqBackend;
import cn.kennylee.banyan.mq.core.spi.MqConsumerDefinition;
import cn.kennylee.banyan.mq.core.spi.MqConsumerHandler;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 发送门面的契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqProducerTest {

    @Test
    void sendsWithAllFields() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq", MqCapability.TAG, MqCapability.DELAY);
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(backend)));

        // when
        producer.send(MqSendRequest.builder()
                .instance("rocketmq")
                .topic("orders")
                .tag("paid")
                .key("k-1")
                .payload("{\"id\":1}")
                .headers(Map.of("tenant", "t1"))
                .build());

        // then
        assertEquals(1, backend.sent().size());
        MqSendRequest request = backend.sent().get(0);
        assertEquals("orders", request.getTopic());
        assertEquals("paid", request.getTag());
        assertEquals("k-1", request.getKey());
        assertEquals("t1", request.getHeaders().get("tenant"));
    }

    @Test
    void propagatesSendFailure() {
        // given
        MqBackend failing = new FailingBackend();
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(failing)));

        // when
        assertThrows(IllegalStateException.class, () -> producer.send(MqSendRequest.builder()
                .instance("rocketmq").topic("orders").payload("x").build()));
    }

    @Test
    void rejectsUndeclaredInstance() {
        // given
        MqProducer producer = new MqProducer(
                new MqInstanceRegistry(List.of(new FakeMqBackend("rocketmq"))));

        // when
        assertThrows(MqConfigurationException.class, () -> producer.send(MqSendRequest.builder()
                .instance("missing").topic("orders").payload("x").build()));
    }

    @Test
    void rejectsDelayWhenBackendDoesNotSupportIt() {
        // given
        MqProducer producer = new MqProducer(
                new MqInstanceRegistry(List.of(new FakeMqBackend("rocketmq"))));

        // when
        assertThrows(MqConfigurationException.class, () -> producer.send(MqSendRequest.builder()
                .instance("rocketmq").topic("orders").payload("x")
                .delay(Duration.ofSeconds(5)).build()));
    }

    @Test
    void acceptsDelayWhenBackendSupportsIt() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq", MqCapability.DELAY);
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(backend)));

        // when
        producer.send(MqSendRequest.builder().instance("rocketmq").topic("orders").payload("x")
                .delay(Duration.ofSeconds(5)).build());

        // then
        assertEquals(Duration.ofSeconds(5), backend.sent().get(0).getDelay());
    }

    @Test
    void rejectsTagWhenBackendDoesNotSupportIt() {
        // given
        MqProducer producer = new MqProducer(
                new MqInstanceRegistry(List.of(new FakeMqBackend("rocketmq"))));

        // when
        assertThrows(MqConfigurationException.class, () -> producer.send(MqSendRequest.builder()
                .instance("rocketmq").topic("orders").tag("paid").payload("x").build()));
    }

    @Test
    void acceptsWildcardTagWithoutTagCapability() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq");
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(backend)));

        // when
        producer.send(MqSendRequest.builder()
                .instance("rocketmq").topic("orders").tag("*").payload("x").build());

        // then
        assertEquals(1, backend.sent().size());
    }

    @Test
    void sendsByTopicWithoutInstance() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq");
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(backend)));

        // when
        producer.send("orders", "{\"id\":1}");

        // then
        assertEquals(1, backend.sent().size());
        assertEquals("orders", backend.sent().get(0).getTopic());
        assertEquals("rocketmq", backend.sent().get(0).getInstance());
    }

    @Test
    void usesBoundInstanceWhenMultipleBackends() {
        // given
        FakeMqBackend kafka = new FakeMqBackend("kafka");
        FakeMqBackend rocketmq = new FakeMqBackend("rocketmq");
        MqProducer producer = new MqProducer(
                new MqInstanceRegistry(List.of(kafka, rocketmq)), "rocketmq");

        // when
        producer.send("orders", "x");

        // then
        assertEquals(0, kafka.sent().size());
        assertEquals(1, rocketmq.sent().size());
    }

    @Test
    void sendsByTopicWithTagAndDelay() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq",
                MqCapability.TAG, MqCapability.DELAY);
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(backend)));

        // when
        producer.send("orders", "paid", "x", Duration.ofSeconds(5));

        // then
        MqSendRequest sent = backend.sent().get(0);
        assertEquals("paid", sent.getTag());
        assertEquals(Duration.ofSeconds(5), sent.getDelay());
    }

    @Test
    void appendsTagSuffixByDefault() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq", MqCapability.TAG);
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(backend)),
                null, new cn.kennylee.banyan.mq.core.StringMqMessageCodec(), "uat", true);

        // when
        producer.send("orders", "paid", "x");

        // then
        assertEquals("paid-uat", backend.sent().get(0).getTag());
    }

    @Test
    void skipsTagSuffixWhenRequestDisables() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq", MqCapability.TAG);
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(backend)),
                null, new cn.kennylee.banyan.mq.core.StringMqMessageCodec(), "uat", true);

        // when
        producer.send(MqSendRequest.builder()
                .topic("orders").tag("paid").payload("x").tagSuffixEnabled(false).build());

        // then
        assertEquals("paid", backend.sent().get(0).getTag());
    }

    @Test
    void usesExplicitTimestamp() {
        // given
        FakeMqBackend backend = new FakeMqBackend("rocketmq");
        MqProducer producer = new MqProducer(new MqInstanceRegistry(List.of(backend)));

        // when
        producer.send(MqSendRequest.builder()
                .topic("orders").payload("x").timestamp(1700000000000L).build());

        // then
        assertEquals(1700000000000L, backend.messages().get(0).getTimestamp());
    }

    /**
     * 总是失败的后端。
     *
     * @author kennylee
     * @since 0.1.0
     */
    private static final class FailingBackend implements MqBackend {

        @Override
        public String type() {
            return "rocketmq";
        }

        @Override
        public Set<MqCapability> capabilities() {
            return Set.of();
        }

        @Override
        public void send(MqSendRequest request, cn.kennylee.banyan.mq.core.MqMessage message) {
            throw new IllegalStateException("broker down");
        }

        @Override
        public void registerConsumer(MqConsumerDefinition definition, MqConsumerHandler handler) {
        }
    }
}
