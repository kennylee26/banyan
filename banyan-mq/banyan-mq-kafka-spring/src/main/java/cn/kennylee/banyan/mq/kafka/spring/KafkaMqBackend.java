package cn.kennylee.banyan.mq.kafka.spring;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConsumeContext;
import cn.kennylee.banyan.mq.core.MqHeaders;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.MqMessageAttributes;
import cn.kennylee.banyan.mq.core.spi.MqBackend;
import cn.kennylee.banyan.mq.core.spi.MqConsumerDefinition;
import cn.kennylee.banyan.mq.core.spi.MqConsumerHandler;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Kafka 后端：同步发送与编程式消费者容器注册。
 *
 * <p>Kafka 没有原生 tag 与延迟消息能力，二者在能力声明中不出现，
 * 使用时会由路由层在使用前拒绝。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public final class KafkaMqBackend implements MqBackend {

    /** 后端类型标识。 */
    private static final String TYPE = "kafka";

    /** 发送模板。 */
    private final KafkaTemplate<String, String> template;

    /** 消费者工厂。 */
    private final ConsumerFactory<String, String> consumerFactory;

    /**
     * 构造 Kafka 后端。
     *
     * @param template        发送模板
     * @param consumerFactory 消费者工厂
     */
    public KafkaMqBackend(KafkaTemplate<String, String> template,
                          ConsumerFactory<String, String> consumerFactory) {
        this.template = template;
        this.consumerFactory = consumerFactory;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Set<MqCapability> capabilities() {
        return Set.of();
    }

    @Override
    public void send(MqSendRequest request, MqMessage message) {
        log.debug(Messages.get(ErrorCode.MQ_KAFKA_SEND_DEBUG, request.getTopic(), message.getKey()));
        ProducerRecord<String, String> record = new ProducerRecord<>(
                request.getTopic(), null, message.getTimestamp(), message.getKey(), message.getBody());
        message.getHeaders().forEach((name, value) -> addHeader(record, name, value));
        message.getExtra().forEach((name, value) ->
                addHeader(record, MqHeaders.EXTRA_PREFIX + name, value));
        addHeader(record, MqHeaders.TIMESTAMP, String.valueOf(message.getTimestamp()));
        try {
            template.send(record).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(Messages.get(ErrorCode.MQ_SEND_FAILED,
                    "Kafka 发送被中断"), e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(Messages.get(ErrorCode.MQ_SEND_FAILED,
                    String.valueOf(e.getCause())), e.getCause());
        }
    }

    @Override
    public void registerConsumer(MqConsumerDefinition definition, MqConsumerHandler handler) {
        warnUnsupportedRetry(definition);
        ContainerProperties properties = new ContainerProperties(definition.getTopic());
        properties.setGroupId(definition.getGroup());
        properties.setMessageListener((org.springframework.kafka.listener.MessageListener<String, String>)
                record -> {
                    MqMessageAttributes attributes = attributesOf(record);
                    long timestamp = attributes.timestamp() == null
                            ? record.timestamp()
                            : attributes.timestamp();
                    MqMessage message = MqMessage.builder()
                            .body(record.value())
                            .key(record.key())
                            .headers(attributes.headers())
                            .extra(attributes.extra())
                            .timestamp(timestamp)
                            .build();
                    MqConsumeContext context = MqConsumeContext.builder()
                            .instance(definition.getInstance())
                            .topic(record.topic())
                            .messageId(messageIdOf(record))
                            .key(record.key())
                            .headers(attributes.headers())
                            .retryTimes(0)
                            .nativeMessage(record)
                            .build();
                    if (!handler.handle(message, context)) {
                        throw new IllegalStateException(Messages.get(ErrorCode.MQ_CONSUME_FAILED,
                                "Kafka " + messageIdOf(record)));
                    }
                });

        int concurrency = definition.getConcurrency() > 0 ? definition.getConcurrency() : 1;
        ConcurrentMessageListenerContainer<String, String> container =
                new ConcurrentMessageListenerContainer<>(consumerFactory, properties);
        container.setConcurrency(concurrency);
        container.start();
        log.info(Messages.get(ErrorCode.MQ_KAFKA_CONSUMER_STARTED,
                definition.getTopic(), definition.getGroup(), definition.getConcurrency()));
    }

    private static void addHeader(ProducerRecord<String, String> record, String name, String value) {
        if (value != null) {
            record.headers().add(name, value.getBytes(StandardCharsets.UTF_8));
        }
    }

    static void warnUnsupportedRetry(MqConsumerDefinition definition) {
        if (definition.getMaxRetryTimes() >= 0) {
            log.warn(Messages.get(ErrorCode.MQ_KAFKA_RETRY_UNSUPPORTED,
                    definition.getTopic(), definition.getGroup()));
        }
    }

    static MqMessageAttributes attributesOf(ConsumerRecord<String, String> record) {
        Map<String, String> headers = new LinkedHashMap<>();
        Map<String, String> extra = new LinkedHashMap<>();
        Long timestamp = null;
        for (org.apache.kafka.common.header.Header header : record.headers()) {
            String key = header.key();
            if (header.value() == null) {
                continue;
            }
            String value = new String(header.value(), StandardCharsets.UTF_8);
            if (MqHeaders.TIMESTAMP.equals(key)) {
                timestamp = MqHeaders.parseTimestamp(value);
            } else if (key.startsWith(MqHeaders.EXTRA_PREFIX)) {
                extra.put(key.substring(MqHeaders.EXTRA_PREFIX.length()), value);
            } else {
                headers.put(key, value);
            }
        }
        return new MqMessageAttributes(headers, extra, timestamp);
    }

    private static String messageIdOf(ConsumerRecord<String, String> record) {
        return record.topic() + "-" + record.partition() + "-" + record.offset();
    }
}
