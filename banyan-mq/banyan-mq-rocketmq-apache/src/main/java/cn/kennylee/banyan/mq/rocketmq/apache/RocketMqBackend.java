package cn.kennylee.banyan.mq.rocketmq.apache;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConsumeContext;
import cn.kennylee.banyan.mq.core.MqConstants;
import cn.kennylee.banyan.mq.core.MqHeaders;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.MqMessageAttributes;
import cn.kennylee.banyan.mq.core.spi.MqBackend;
import cn.kennylee.banyan.mq.core.spi.MqConsumerDefinition;
import cn.kennylee.banyan.mq.core.spi.MqConsumerHandler;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * RocketMQ 后端：同步发送、延迟发送与编程式消费者容器注册。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public final class RocketMqBackend implements MqBackend {

    /** 后端类型标识。 */
    private static final String TYPE = "rocketmq";

    /** RocketMQ 系统属性，消费端不放进业务 headers。 */
    private static final Set<String> SYSTEM_PROPERTIES = Set.of(
            "KEYS", "TAGS", "WAIT", "TRACE_ON", "MSG_REGION", "DELAY",
            "__STARTDELIVERTIME", "TRAN_MSG", "PGROUP", "UNIQ_KEY",
            "CONSUME_START_TIME", "RECONSUME_TIME", "SHARDING_KEY", "SHARDING_BATCH");

    /** 发送模板。 */
    private final RocketMQTemplate template;

    /** name server。 */
    private final String nameServer;

    /** 容器，用于解析占位符。 */
    private final ApplicationContext applicationContext;

    /** 延迟发送策略。 */
    private final RocketMqDelayStrategy delayStrategy;

    /**
     * 按当前客户端版本构造 RocketMQ 后端。
     *
     * @param template           发送模板
     * @param nameServer         name server
     * @param applicationContext 容器
     */
    public RocketMqBackend(RocketMQTemplate template, String nameServer,
                           ApplicationContext applicationContext) {
        this(template, nameServer, applicationContext, RocketMqDelayStrategy.current(template));
    }

    /**
     * 构造 RocketMQ 后端。
     *
     * @param template           发送模板
     * @param nameServer         name server
     * @param applicationContext 容器
     * @param delayStrategy      延迟发送策略
     */
    public RocketMqBackend(RocketMQTemplate template, String nameServer,
                           ApplicationContext applicationContext,
                           RocketMqDelayStrategy delayStrategy) {
        this.template = template;
        this.nameServer = nameServer;
        this.applicationContext = applicationContext;
        this.delayStrategy = delayStrategy;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Set<MqCapability> capabilities() {
        return Set.of(MqCapability.TAG, MqCapability.MULTI_TAG, MqCapability.DELAY);
    }

    @Override
    public void send(MqSendRequest request, MqMessage message) {
        log.debug(Messages.get(ErrorCode.MQ_ROCKETMQ_SEND_DEBUG, request.getTopic(),
                request.getTag(), message.getKey()));
        String destination = destinationOf(request.getTopic(), request.getTag());
        String body = message.getBody() == null ? "" : message.getBody();
        MessageBuilder<String> builder = MessageBuilder.withPayload(body);
        if (message.getKey() != null) {
            builder.setHeader(RocketMQHeaders.KEYS, message.getKey());
        }
        message.getHeaders().forEach(builder::setHeader);
        message.getExtra().forEach((name, value) ->
                builder.setHeader(MqHeaders.EXTRA_PREFIX + name, value));
        builder.setHeader(MqHeaders.TIMESTAMP, String.valueOf(message.getTimestamp()));
        org.springframework.messaging.Message<String> payload = builder.build();

        if (request.getDelay() != null) {
            delayStrategy.send(destination, payload, request.getDelay());
            return;
        }
        template.syncSend(destination, payload);
    }

    @Override
    public void registerConsumer(MqConsumerDefinition definition, MqConsumerHandler handler) {
        RocketMQListener<MessageExt> listener = new RocketMQListener<MessageExt>() {
            @Override
            public void onMessage(MessageExt messageExt) {
                String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
                MqMessageAttributes attributes = attributesOf(messageExt);
                long timestamp = attributes.timestamp() == null
                        ? messageExt.getBornTimestamp()
                        : attributes.timestamp();
                MqMessage message = MqMessage.builder()
                        .body(body)
                        .key(messageExt.getKeys())
                        .headers(attributes.headers())
                        .extra(attributes.extra())
                        .timestamp(timestamp)
                        .build();
                MqConsumeContext context = MqConsumeContext.builder()
                        .instance(definition.getInstance())
                        .topic(messageExt.getTopic())
                        .tag(messageExt.getTags())
                        .messageId(messageExt.getMsgId())
                        .key(messageExt.getKeys())
                        .headers(attributes.headers())
                        .retryTimes(messageExt.getReconsumeTimes())
                        .nativeMessage(messageExt)
                        .build();
                if (!handler.handle(message, context)) {
                    throw new IllegalStateException(Messages.get(ErrorCode.MQ_CONSUME_FAILED,
                            "RocketMQ " + messageExt.getMsgId()));
                }
            }
        };

        RocketMQMessageListener annotation = RocketMqAnnotationProxy.create(settingsOf(definition));
        DefaultRocketMQListenerContainer container = new DefaultRocketMQListenerContainer();
        container.setConsumerGroup(definition.getGroup());
        container.setTopic(definition.getTopic());
        container.setRocketMQMessageListener(annotation);
        container.setRocketMQListener(listener);
        container.setNameServer(nameServer);
        container.setApplicationContext(applicationContext);
        try {
            container.afterPropertiesSet();
            container.start();
            log.info(Messages.get(ErrorCode.MQ_ROCKETMQ_CONSUMER_STARTED,
                    definition.getTopic(), definition.getTag(), definition.getGroup()));
        } catch (Exception e) {
            throw new IllegalStateException(Messages.get(ErrorCode.MQ_CONSUME_FAILED,
                    "RocketMQ 消费者启动失败: " + definition.getTopic()), e);
        }
    }

    private static String destinationOf(String topic, String tag) {
        if (Strings.isBlank(tag) || MqConstants.WILDCARD_TAG.equals(tag)) {
            return topic;
        }
        return topic + ":" + tag;
    }

    private static MqMessageAttributes attributesOf(MessageExt messageExt) {
        Map<String, String> headers = new LinkedHashMap<>();
        Map<String, String> extra = new LinkedHashMap<>();
        Long timestamp = null;
        if (messageExt.getProperties() != null) {
            for (Map.Entry<String, String> entry : messageExt.getProperties().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value == null) {
                    continue;
                }
                if (MqHeaders.TIMESTAMP.equals(key)) {
                    timestamp = MqHeaders.parseTimestamp(value);
                } else if (SYSTEM_PROPERTIES.contains(key)) {
                    continue;
                } else if (key.startsWith(MqHeaders.EXTRA_PREFIX)) {
                    extra.put(key.substring(MqHeaders.EXTRA_PREFIX.length()), value);
                } else {
                    headers.put(key, value);
                }
            }
        }
        return new MqMessageAttributes(headers, extra, timestamp);
    }

    private RocketMqAnnotationSettings settingsOf(MqConsumerDefinition definition) {
        RocketMqAnnotationSettings.RocketMqAnnotationSettingsBuilder builder =
                RocketMqAnnotationSettings.builder()
                .consumerGroup(definition.getGroup())
                .topic(definition.getTopic())
                .selectorExpression(definition.getTag())
                .nameServer(nameServer);
        if (definition.getConcurrency() > 0) {
            builder.consumeThreadNumber(definition.getConcurrency())
                    .consumeThreadMax(definition.getConcurrency());
        }
        if (definition.getMaxRetryTimes() >= 0) {
            builder.maxReconsumeTimes(definition.getMaxRetryTimes());
        }
        return builder.build();
    }
}
