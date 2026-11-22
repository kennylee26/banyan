package cn.kennylee.banyan.mq.rocketmq.apache;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 厂商注解接口代理的属性完整性测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class RocketMqAnnotationProxyTest {

    @Test
    void returnsEveryAnnotationProperty() {
        // given
        RocketMqAnnotationSettings settings = RocketMqAnnotationSettings.builder()
                .consumerGroup("group-1")
                .topic("orders")
                .selectorType(SelectorType.SQL92)
                .selectorExpression("amount > 1")
                .consumeMode(ConsumeMode.ORDERLY)
                .messageModel(MessageModel.BROADCASTING)
                .consumeThreadMax(32)
                .consumeThreadNumber(8)
                .maxReconsumeTimes(5)
                .consumeTimeout(20)
                .replyTimeout(1000)
                .accessKey("ak")
                .secretKey("sk")
                .enableMsgTrace(true)
                .customizedTraceTopic("trace-topic")
                .nameServer("127.0.0.1:9876")
                .accessChannel("LOCAL")
                .tlsEnable("true")
                .namespace("ns")
                .delayLevelWhenNextConsume(2)
                .suspendCurrentQueueTimeMillis(500)
                .awaitTerminationMillisWhenShutdown(100)
                .instanceName("inst")
                .build();

        // when
        RocketMQMessageListener annotation = RocketMqAnnotationProxy.create(settings);

        // then
        assertEquals("group-1", annotation.consumerGroup());
        assertEquals("orders", annotation.topic());
        assertEquals(SelectorType.SQL92, annotation.selectorType());
        assertEquals("amount > 1", annotation.selectorExpression());
        assertEquals(ConsumeMode.ORDERLY, annotation.consumeMode());
        assertEquals(MessageModel.BROADCASTING, annotation.messageModel());
        assertEquals(32, annotation.consumeThreadMax());
        assertEquals(8, annotation.consumeThreadNumber());
        assertEquals(5, annotation.maxReconsumeTimes());
        assertEquals(20, annotation.consumeTimeout());
        assertEquals(1000, annotation.replyTimeout());
        assertEquals("ak", annotation.accessKey());
        assertEquals("sk", annotation.secretKey());
        assertTrue(annotation.enableMsgTrace());
        assertEquals("trace-topic", annotation.customizedTraceTopic());
        assertEquals("127.0.0.1:9876", annotation.nameServer());
        assertEquals("LOCAL", annotation.accessChannel());
        assertEquals("true", annotation.tlsEnable());
        assertEquals("ns", annotation.namespace());
        assertEquals(2, annotation.delayLevelWhenNextConsume());
        assertEquals(500, annotation.suspendCurrentQueueTimeMillis());
        assertEquals(100, annotation.awaitTerminationMillisWhenShutdown());
        assertEquals("inst", annotation.instanceName());
        assertEquals(RocketMQMessageListener.class, annotation.annotationType());
    }

    @Test
    void defaultsDoNotReturnNull() {
        // given
        RocketMqAnnotationSettings settings = RocketMqAnnotationSettings.builder()
                .consumerGroup("group-1").topic("orders").build();

        // when
        RocketMQMessageListener annotation = RocketMqAnnotationProxy.create(settings);

        // then
        assertEquals("", annotation.accessKey());
        assertEquals("", annotation.secretKey());
        assertEquals("", annotation.customizedTraceTopic());
        assertFalse(annotation.enableMsgTrace());
        assertEquals(SelectorType.TAG, annotation.selectorType());
        assertEquals("*", annotation.selectorExpression());
    }
}
