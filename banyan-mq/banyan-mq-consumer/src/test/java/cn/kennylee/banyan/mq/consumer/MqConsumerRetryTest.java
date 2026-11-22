package cn.kennylee.banyan.mq.consumer;

import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConsumeContext;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqConsumerHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * boolean 重试语义与双参数上下文的契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MqConsumerRetryTest {

    @Test
    void falseReturnMeansRetry() {
        // given
        MqConsumerHandler handler = handlerOf(new ReturningConsumer());

        // when
        boolean result = handler.handle(message(), context());

        // then
        assertFalse(result);
    }

    @Test
    void trueReturnMeansSuccess() {
        // given
        MqConsumerHandler handler = handlerOf(new SucceedingConsumer());

        // when
        boolean result = handler.handle(message(), context());

        // then
        assertTrue(result);
    }

    @Test
    void voidReturnMeansSuccess() {
        // given
        MqConsumerHandler handler = handlerOf(new VoidConsumer());

        // when
        boolean result = handler.handle(message(), context());

        // then
        assertTrue(result);
    }

    @Test
    void exceptionPropagates() {
        // given
        MqConsumerHandler handler = handlerOf(new ThrowingConsumer());

        // when
        assertThrows(IllegalArgumentException.class, () -> handler.handle(message(), context()));
    }

    @Test
    void passesConsumeContextToTwoParameterMethod() {
        // given
        TwoParameterConsumer bean = new TwoParameterConsumer();
        MqConsumerHandler handler = handlerOf(bean);

        // when
        boolean result = handler.handle(message(), context());

        // then
        assertTrue(result);
        assertEquals("orders", bean.topic);
        assertEquals(3, bean.retryTimes);
    }

    private static MqConsumerHandler handlerOf(Object bean) {
        RecordingBackend backend = new RecordingBackend("rocketmq", MqCapability.TAG);
        new MqConsumerRegistrar(new MqInstanceRegistry(List.of(backend))).register(bean);
        return backend.handlerOf("rocketmq", "orders", "paid");
    }

    private static MqMessage message() {
        return MqMessage.builder().body("{}").build();
    }

    private static MqConsumeContext context() {
        return MqConsumeContext.builder()
                .instance("rocketmq").topic("orders").tag("paid").retryTimes(3).build();
    }

    /**
     * 返回 false。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class ReturningConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public boolean onPaid(MqMessage message) {
            return false;
        }
    }

    /**
     * 返回 true。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class SucceedingConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public boolean onPaid(MqMessage message) {
            return true;
        }
    }

    /**
     * void 方法。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class VoidConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void onPaid(MqMessage message) {
        }
    }

    /**
     * 抛异常。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class ThrowingConsumer {

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void onPaid(MqMessage message) {
            throw new IllegalArgumentException("boom");
        }
    }

    /**
     * 双参数。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class TwoParameterConsumer {

        private String topic;
        private int retryTimes;

        @MqConsumer(instance = "rocketmq", topic = "orders", tag = "paid", groupSuffixEnabled = false, tagSuffixEnabled = false)
        public void onPaid(MqMessage message, MqConsumeContext context) {
            this.topic = context.getTopic();
            this.retryTimes = context.getRetryTimes();
        }
    }
}
