package cn.kennylee.banyan.mq.consumer;

import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConfigurationException;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多 tag 数组注册测试：拼接、后缀、通配与不支持后端。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MultiTagExpressionTest {

    @Test
    void joinsTagArrayIntoExpression() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        registrar.register(new MultiTagConsumer());

        // then
        assertNotNull(backend.definitionOf("rocketmq", "orders", "created || cancelled"));
    }

    @Test
    void appliesSuffixToEveryTag() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)), "uat");

        // when
        registrar.register(new SuffixedMultiTagConsumer());

        // then
        assertNotNull(backend.definitionOf("rocketmq", "orders",
                "created-uat || cancelled-uat"));
    }

    @Test
    void emptyArrayMeansWildcard() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq");
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        registrar.register(new EmptyTagConsumer());

        // then
        assertNotNull(backend.definitionOf("rocketmq", "orders", "*"));
    }

    @Test
    void wildcardElementMeansNoFiltering() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq");
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        registrar.register(new WildcardElementConsumer());

        // then
        assertNotNull(backend.definitionOf("rocketmq", "orders", "*"));
    }

    @Test
    void splitsLegacySingleElementExpressionAndAppliesSuffixPerTag() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)), "uat");

        // when
        registrar.register(new LegacyExpressionConsumer());

        // then
        assertNotNull(backend.definitionOf("rocketmq", "orders",
                "paid-uat || closed-uat"));
    }

    @Test
    void rejectsMultiTagWhenBackendDoesNotSupportTag() {
        // given
        RecordingBackend backend = new RecordingBackend("kafka");
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        MqConfigurationException error = assertThrows(MqConfigurationException.class,
                () -> registrar.register(new KafkaMultiTagConsumer()));

        // then
        assertTrue(error.getMessage().contains("不支持多 tag"));
        assertTrue(error.getMessage().contains("kafka"));
        assertEquals(0, backend.definitions().size());
    }

    /** 多个 tag 数组。 */
    static class MultiTagConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders",
                tag = {"created", "cancelled"},
                groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void onMulti(MqMessage message) {
        }
    }

    /** 开启环境后缀的多 tag 消费者。 */
    static class SuffixedMultiTagConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders",
                tag = {"created", "cancelled"}, groupSuffixEnabled = false)
        public void onMulti(MqMessage message) {
        }
    }

    /** Kafka 实例上的多 tag 消费者。 */
    static class KafkaMultiTagConsumer {

        @MqConsumer(instance = "kafka", topic = "orders",
                tag = {"created", "cancelled"},
                groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void onMulti(MqMessage message) {
        }
    }

    /** 空数组，按通配处理。 */
    static class EmptyTagConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = {})
        public void onMulti(MqMessage message) {
        }
    }

    /** 包含通配符元素。 */
    static class WildcardElementConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = {"paid", "*"})
        public void onMulti(MqMessage message) {
        }
    }

    /** 旧写法：单个元素里带 ||，也应拆开后逐 tag 加后缀。 */
    static class LegacyExpressionConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid || closed")
        public void onMulti(MqMessage message) {
        }
    }
}
