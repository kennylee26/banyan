package cn.kennylee.banyan.mq.rocketmq.apache;

import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.Message;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * RocketMQ 后端的能力声明与发送契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class RocketMqBackendTest {

    @Test
    void declaresTagAndDelayCapabilities() {
        // given
        RocketMqBackend backend = backend(mock(RocketMQTemplate.class));

        // when
        assertTrue(backend.capabilities().contains(MqCapability.TAG));
        assertTrue(backend.capabilities().contains(MqCapability.MULTI_TAG));

        // then
        assertTrue(backend.capabilities().contains(MqCapability.DELAY));
    }

    @Test
    void sendsWithTagAsDestinationSuffix() {
        // given
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        RocketMqBackend backend = backend(template);

        // when
        backend.send(MqSendRequest.builder()
                        .instance("rocketmq").topic("orders").tag("paid").payload("{\"id\":1}").build(),
                MqMessage.builder().body("{\"id\":1}").build());

        // then
        verify(template).syncSend(eq("orders:paid"), any(Message.class));
    }

    @Test
    void sendsDelayWithMills() {
        // given
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        RocketMqBackend backend = backend(template);

        // when
        backend.send(MqSendRequest.builder()
                        .instance("rocketmq").topic("orders").payload("x")
                        .delay(Duration.ofSeconds(5)).build(),
                MqMessage.builder().body("x").build());

        // then
        verify(template).syncSendDelayTimeMills(eq("orders"), any(Message.class), eq(5000L));
    }

    @Test
    void writesExtraWithPrefix() {
        // given
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        RocketMqBackend backend = backend(template);

        // when
        backend.send(MqSendRequest.builder()
                        .instance("rocketmq").topic("orders").payload("x")
                        .extra(java.util.Map.of("tenant", "t1")).build(),
                MqMessage.builder().body("x").extra(java.util.Map.of("tenant", "t1")).build());

        // then
        ArgumentCaptor<Message<String>> captor = messageCaptor();
        verify(template).syncSend(eq("orders"), captor.capture());
        assertEquals("t1", captor.getValue().getHeaders().get("x-extra-tenant"));
    }

    @Test
    void usesEmptyPayloadWhenBodyIsNull() {
        // given
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        RocketMqBackend backend = backend(template);

        // when
        backend.send(MqSendRequest.builder().instance("rocketmq").topic("orders").build(),
                MqMessage.builder().build());

        // then
        ArgumentCaptor<Message<String>> captor = messageCaptor();
        verify(template).syncSend(eq("orders"), captor.capture());
        assertEquals("", captor.getValue().getPayload());
    }

    @Test
    void copiesKeyToRocketMqHeader() {
        // given
        RocketMQTemplate template = mock(RocketMQTemplate.class);
        RocketMqBackend backend = backend(template);

        // when
        backend.send(MqSendRequest.builder()
                        .instance("rocketmq").topic("orders").key("k-1").payload("x").build(),
                MqMessage.builder().body("x").key("k-1").build());

        // then
        ArgumentCaptor<Message<String>> captor = messageCaptor();
        verify(template).syncSend(eq("orders"), captor.capture());
        assertEquals("k-1", captor.getValue().getHeaders().get(RocketMQHeaders.KEYS));
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Message<String>> messageCaptor() {
        return (ArgumentCaptor<Message<String>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(Message.class);
    }

    private static RocketMqBackend backend(RocketMQTemplate template) {
        return new RocketMqBackend(template, "127.0.0.1:9876", mock(ApplicationContext.class),
                new RocketMqDelayStrategy(template, 5));
    }
}
