package cn.kennylee.banyan.mq.consumer;

import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 环境后缀隔离的契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqConsumerSuffixTest {

    @Test
    void appendsSuffixToTagAndGroup() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)), "uat");

        // when
        registrar.register(new SuffixedConsumer());

        // then
        assertNotNull(backend.definitionOf("rocketmq", "orders", "paid-uat"));
        assertTrue(backend.definitionOf("rocketmq", "orders", "paid-uat")
                .getGroup().endsWith("-uat"));
    }

    @Test
    void keepsTagWhenSuffixDisabled() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)), "uat");

        // when
        registrar.register(new NoSuffixConsumer());

        // then
        assertNotNull(backend.definitionOf("rocketmq", "orders", "paid"));
    }

    @Test
    void groupSuffixOnly() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)), "uat");

        // when
        registrar.register(new GroupSuffixOnlyConsumer());

        // then
        assertNotNull(backend.definitionOf("rocketmq", "orders", "paid"));
        assertTrue(backend.definitionOf("rocketmq", "orders", "paid")
                .getGroup().endsWith("-uat"));
    }

    @Test
    void tagSuffixOnly() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)), "uat");

        // when
        registrar.register(new TagSuffixOnlyConsumer());

        // then
        assertNotNull(backend.definitionOf("rocketmq", "orders", "paid-uat"));
        assertFalse(backend.definitionOf("rocketmq", "orders", "paid-uat")
                .getGroup().endsWith("-uat"));
    }

    @Test
    void wildcardTagKeepsWildcardWithoutSuffix() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq");
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)), "uat");

        // when
        registrar.register(new WildcardConsumer());

        // then
        assertNotNull(backend.definitionOf("rocketmq", "orders", "*"));
    }

    @Test
    void differentSuffixesDoNotCollide() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqInstanceRegistry registry = new MqInstanceRegistry(List.of(backend));

        // when
        new MqConsumerRegistrar(registry, "test").register(new SuffixedConsumer());
        new MqConsumerRegistrar(registry, "uat").register(new SuffixedConsumer());

        // then
        assertEquals(2, backend.definitions().size());
        assertNotNull(backend.definitionOf("rocketmq", "orders", "paid-test"));
        assertNotNull(backend.definitionOf("rocketmq", "orders", "paid-uat"));
        assertNull(backend.definitionOf("rocketmq", "orders", "paid"));
    }

    /**
     * 使用后缀的消费者。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class SuffixedConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid")
        public void onPaid(MqMessage message) {
        }
    }

    /**
     * 关闭后缀的消费者。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class NoSuffixConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void onPaid(MqMessage message) {
        }
    }

    /**
     * 通配 tag 的消费者。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class WildcardConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "*")
        public void onPaid(MqMessage message) {
        }
    }

    /**
     * 只给消费组加后缀。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class GroupSuffixOnlyConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", tagSuffixEnabled = false)
        public void onPaid(MqMessage message) {
        }
    }

    /**
     * 只给 tag 加后缀。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class TagSuffixOnlyConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false)
        public void onPaid(MqMessage message) {
        }
    }
}
