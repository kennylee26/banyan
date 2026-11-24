package cn.kennylee.banyan.mq.consumer;

import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConfigurationException;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多 tag 数组注册测试：拼接、后缀、通配与不支持后端。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MultiTagExpressionTest {

    /** 固定中文消息，避免不同 JVM 默认语言下断言失败。 */
    @BeforeEach
    void fixLocale() {
        Messages.setLocale(Locale.SIMPLIFIED_CHINESE);
    }

    /** 恢复语言。 */
    @AfterEach
    void resetLocale() {
        Messages.setLocale(Locale.getDefault());
    }

    @Test
    void joinsTagArrayIntoExpression() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG, MqCapability.MULTI_TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        registrar.register(new MultiTagConsumer());

        // then
        assertEquals("created || cancelled",
                backend.definitionOf("rocketmq", "orders", "created || cancelled").getTag());
    }

    @Test
    void appliesSuffixToEveryTag() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG, MqCapability.MULTI_TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)), "uat");

        // when
        registrar.register(new SuffixedMultiTagConsumer());

        // then
        assertEquals("created-uat || cancelled-uat",
                backend.definitionOf("rocketmq", "orders",
                        "created-uat || cancelled-uat").getTag());
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
        assertEquals("*", backend.definitionOf("rocketmq", "orders", "*").getTag());
    }

    @Test
    void pureWildcardArrayMeansNoFiltering() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq");
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        registrar.register(new PureWildcardConsumer());

        // then
        assertEquals("*", backend.definitionOf("rocketmq", "orders", "*").getTag());
    }

    @Test
    void rejectsSeparatorInsideTagElement() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq",
                MqCapability.TAG, MqCapability.MULTI_TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        MqConfigurationException error = assertThrows(MqConfigurationException.class,
                () -> registrar.register(new LegacyExpressionConsumer()));

        // then
        assertTrue(error.getMessage().contains("不要写分隔符"));
    }

    @Test
    void rejectsWildcardMixedWithOtherTags() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq");
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        MqConfigurationException error = assertThrows(MqConfigurationException.class,
                () -> registrar.register(new WildcardElementConsumer()));

        // then
        assertTrue(error.getMessage().contains("不能与其它 tag 混用"));
    }

    @Test
    void rejectsMultiTagWhenBackendSupportsOnlySingleTag() {
        // given
        RecordingBackend backend = new RecordingBackend("single-tag", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        MqConfigurationException error = assertThrows(MqConfigurationException.class,
                () -> registrar.register(new SingleTagBackendConsumer()));

        // then
        assertTrue(error.getMessage().contains("MULTI_TAG"));
        assertTrue(error.getMessage().contains("single-tag"));
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
        assertTrue(error.getMessage().contains("MULTI_TAG"));
        assertTrue(error.getMessage().contains("kafka"));
        assertTrue(backend.definitions().isEmpty());
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

    /** 只声明单 tag 能力的后端实例上的多 tag 消费者。 */
    static class SingleTagBackendConsumer {

        @MqConsumer(instance = "single-tag", topic = "orders",
                tag = {"created", "cancelled"},
                groupSuffixEnabled = false, tagSuffixEnabled = false)
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

    /** 纯通配数组。 */
    static class PureWildcardConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = {"*"})
        public void onMulti(MqMessage message) {
        }
    }

    /** 通配符与具体 tag 混用。 */
    static class WildcardElementConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = {"paid", "*"})
        public void onMulti(MqMessage message) {
        }
    }

    /** 旧写法：单个元素里带分隔符，应直接报错。 */
    static class LegacyExpressionConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid || closed")
        public void onMulti(MqMessage message) {
        }
    }
}
