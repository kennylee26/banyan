package cn.kennylee.banyan.mq.kafka.spring;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConfigurationException;
import cn.kennylee.banyan.mq.core.MqHeaders;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.MqMessageAttributes;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqConsumerDefinition;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kafka 后端的能力声明与发送契约测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class KafkaMqBackendTest {

    @Test
    void doesNotDeclareTagCapability() {
        // given
        KafkaMqBackend backend = backend();

        // when
        assertFalse(backend.capabilities().contains(MqCapability.TAG));
    }

    @Test
    void doesNotDeclareDelayCapability() {
        // given
        KafkaMqBackend backend = backend();

        // when
        assertFalse(backend.capabilities().contains(MqCapability.DELAY));
    }

    @Test
    void sendsTopicKeyBodyAndHeaders() {
        // given
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        when(template.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        KafkaMqBackend backend = new KafkaMqBackend(template, consumerFactory());

        // when
        backend.send(MqSendRequest.builder()
                        .instance("kafka").topic("orders").key("k-1").payload("{\"id\":1}")
                        .headers(java.util.Map.of("tenant", "t1")).build(),
                MqMessage.builder().body("{\"id\":1}").key("k-1")
                        .headers(java.util.Map.of("tenant", "t1")).build());

        // then
        ArgumentCaptor<ProducerRecord<String, String>> captor = recordCaptor();
        verify(template).send(captor.capture());
        ProducerRecord<String, String> record = captor.getValue();
        assertEquals("orders", record.topic());
        assertEquals("k-1", record.key());
        assertEquals("{\"id\":1}", record.value());
        assertEquals("t1", new String(record.headers().lastHeader("tenant").value(),
                java.nio.charset.StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<ProducerRecord<String, String>> recordCaptor() {
        return (ArgumentCaptor<ProducerRecord<String, String>>) (ArgumentCaptor<?>)
                ArgumentCaptor.forClass(ProducerRecord.class);
    }

    @Test
    void writesExtraWithPrefix() {
        // given
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        when(template.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        KafkaMqBackend backend = new KafkaMqBackend(template, consumerFactory());

        // when
        backend.send(MqSendRequest.builder()
                        .instance("kafka").topic("orders").payload("x")
                        .extra(java.util.Map.of("tenant", "t1")).build(),
                MqMessage.builder().body("x").extra(java.util.Map.of("tenant", "t1")).build());

        // then
        ArgumentCaptor<ProducerRecord<String, String>> captor = recordCaptor();
        verify(template).send(captor.capture());
        assertEquals("t1", new String(
                captor.getValue().headers().lastHeader("x-extra-tenant").value(),
                java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void propagatesSendFailure() {
        // given
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        when(template.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));
        KafkaMqBackend backend = new KafkaMqBackend(template, consumerFactory());

        // when
        assertThrows(IllegalStateException.class, () -> backend.send(
                MqSendRequest.builder().instance("kafka").topic("orders").payload("x").build(),
                MqMessage.builder().body("x").build()));
    }

    @Test
    void rejectsTagBecauseNotSupported() {
        // given
        MqInstanceRegistry registry = new MqInstanceRegistry(List.of(backend()));

        // when
        assertThrows(MqConfigurationException.class,
                () -> registry.requireCapability("kafka", MqCapability.TAG));
    }

    @Test
    void rejectsDelayBecauseNotSupported() {
        // given
        MqInstanceRegistry registry = new MqInstanceRegistry(List.of(backend()));

        // when
        assertThrows(MqConfigurationException.class,
                () -> registry.requireCapability("kafka", MqCapability.DELAY));
    }

    private static KafkaMqBackend backend() {
        return new KafkaMqBackend(mock(KafkaTemplate.class), consumerFactory());
    }

    @Test
    void warnsWhenMaxRetryTimesConfigured() {
        // given
        Logger logger = (Logger) LoggerFactory.getLogger(KafkaMqBackend.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        MqConsumerDefinition definition = MqConsumerDefinition.builder()
                .instance("kafka").topic("orders").group("order-consumer").maxRetryTimes(3).build();

        try {
            // when
            KafkaMqBackend.warnUnsupportedRetry(definition);

            // then
            assertEquals(1, appender.list.size());
            assertTrue(appender.list.get(0).getFormattedMessage().contains("maxRetryTimes"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void skipsNullHeaderValues() {
        // given
        ConsumerRecord<String, String> record = new ConsumerRecord<>("orders", 0, 0L, null, "x");
        record.headers().add("tenant", null);
        record.headers().add(MqHeaders.TIMESTAMP,
                "1700000000000".getBytes(StandardCharsets.UTF_8));

        // when
        MqMessageAttributes attributes = KafkaMqBackend.attributesOf(record);

        // then
        assertTrue(attributes.headers().isEmpty());
        assertEquals(1700000000000L, attributes.timestamp());
    }

    @SuppressWarnings("unchecked")
    private static ConsumerFactory<String, String> consumerFactory() {
        return mock(ConsumerFactory.class);
    }
}
