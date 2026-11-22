package cn.kennylee.banyan.mq.consumer;

import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConfigurationException;
import cn.kennylee.banyan.mq.core.MqConsumeContext;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqConsumerHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 方法级注解注册的契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqConsumerRegistrationTest {

    @Test
    void registersEveryAnnotatedMethodOfOneClass() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        registrar.register(new MultiConsumer());

        // then
        assertEquals(2, backend.definitions().size());
    }

    @Test
    void ignoresMethodsWithoutAnnotation() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        registrar.register(new PartiallyAnnotated());

        // then
        assertEquals(1, backend.definitions().size());
    }

    @Test
    void rejectsUnsupportedReturnType() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        assertThrows(MqConfigurationException.class, () -> registrar.register(new BadReturn()));
    }

    @Test
    void rejectsUnsupportedParameterCount() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        assertThrows(MqConfigurationException.class, () -> registrar.register(new BadParameters()));
    }

    @Test
    void rejectsTagWhenBackendDoesNotSupportIt() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq");
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        assertThrows(MqConfigurationException.class, () -> registrar.register(new TaggedConsumer()));
    }

    @Test
    void allowsWildcardTagWithoutTagCapability() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq");
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(
                new MqInstanceRegistry(List.of(backend)));

        // when
        assertDoesNotThrow(() -> registrar.register(new WildcardConsumer()));

        // then
        assertNotNull(backend.definitionOf("rocketmq", "orders", "*"));
    }

    @Test
    void invokesPrivateConsumerMethod() {
        // given
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        PrivateConsumer bean = new PrivateConsumer();
        new MqConsumerRegistrar(new MqInstanceRegistry(List.of(backend))).register(bean);
        MqConsumerHandler handler = backend.handlerOf("rocketmq", "orders", "paid");

        // when
        boolean success = handler.handle(MqMessage.builder().body("x").build(),
                MqConsumeContext.builder().build());

        // then
        assertTrue(success);
        assertEquals("x", bean.received);
    }

    /**
     * 一个类多个消费者。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class MultiConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void onPaid(MqMessage message) {
        }

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "closed", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public boolean onClosed(MqMessage message) {
            return true;
        }
    }

    /**
     * 仅一个方法带注解。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class PartiallyAnnotated {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void annotated(MqMessage message) {
        }

        public void notAnnotated(MqMessage message) {
        }
    }

    /**
     * 非法返回值。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class BadReturn {

        @MqConsumer(instance = "rocketmq", topic = "orders", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public String bad(MqMessage message) {
            return "x";
        }
    }

    /**
     * 非法参数数量。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class BadParameters {

        @MqConsumer(instance = "rocketmq", topic = "orders", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void bad() {
        }
    }

    /**
     * 带非通配 tag 的消费者。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class TaggedConsumer {

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

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "*", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void onPaid(MqMessage message) {
        }
    }

    /**
     * 私有消费方法。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class PrivateConsumer {

        private String received;

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        private void onPaid(MqMessage message) {
            this.received = message.getBody();
        }
    }
}
