package cn.kennylee.banyan.mq.producer;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConfigurationException;
import cn.kennylee.banyan.mq.core.MqConstants;
import cn.kennylee.banyan.mq.core.MqMessage;
import cn.kennylee.banyan.mq.core.MqMessageCodec;
import cn.kennylee.banyan.mq.core.StringMqMessageCodec;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqBackend;
import cn.kennylee.banyan.mq.core.spi.MqSendRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Objects;

/**
 * 发送门面：同步发送、事务提交后发送与延迟能力校验。
 *
 * <p>两种用法：</p>
 * <ul>
 *     <li>路由模式：持有一个 {@link MqInstanceRegistry}，按实例名路由；</li>
 *     <li>直连模式：直接持有一个 {@link MqBackend}，适合同类型多集群时每个生产者绑定自己的连接。</li>
 * </ul>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class MqProducer {

    /** 逻辑实例路由，直连模式下为 null。 */
    private final MqInstanceRegistry registry;

    /** 直连后端，路由模式下为 null。 */
    private final MqBackend directBackend;

    /** 编解码器。 */
    private final MqMessageCodec codec;

    /** 默认逻辑实例，空表示按路由自动选择。 */
    private final String defaultInstance;

    /** tag 环境后缀，空表示不追加。 */
    private final String tagSuffix;

    /** 是否默认给 tag 追加环境后缀。 */
    private final boolean tagSuffixEnabled;

    /**
     * 使用默认配置构造路由模式发送门面。
     *
     * @param registry 逻辑实例路由
     */
    public MqProducer(MqInstanceRegistry registry) {
        this(registry, null, new StringMqMessageCodec(), "", true);
    }

    /**
     * 绑定默认逻辑实例构造路由模式发送门面。
     *
     * @param registry        逻辑实例路由
     * @param defaultInstance 默认实例，可为空
     */
    public MqProducer(MqInstanceRegistry registry, String defaultInstance) {
        this(registry, defaultInstance, new StringMqMessageCodec(), "", true);
    }

    /**
     * 使用指定编解码构造路由模式发送门面。
     *
     * @param registry 逻辑实例路由
     * @param codec    编解码器
     */
    public MqProducer(MqInstanceRegistry registry, MqMessageCodec codec) {
        this(registry, null, codec, "", true);
    }

    /**
     * 构造路由模式发送门面。
     *
     * @param registry        逻辑实例路由
     * @param defaultInstance 默认实例，可为空
     * @param codec           编解码器
     */
    public MqProducer(MqInstanceRegistry registry, String defaultInstance, MqMessageCodec codec) {
        this(registry, defaultInstance, codec, "", true);
    }

    /**
     * 构造路由模式发送门面。
     *
     * @param registry        逻辑实例路由
     * @param defaultInstance 默认实例，可为空
     * @param codec           编解码器
     * @param tagSuffix       tag 环境后缀，可为空
     * @param tagSuffixEnabled 是否默认给 tag 追加环境后缀
     */
    public MqProducer(MqInstanceRegistry registry, String defaultInstance, MqMessageCodec codec,
                      String tagSuffix, boolean tagSuffixEnabled) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.directBackend = null;
        this.defaultInstance = defaultInstance;
        this.codec = Objects.requireNonNull(codec, "codec");
        this.tagSuffix = tagSuffix;
        this.tagSuffixEnabled = tagSuffixEnabled;
    }

    /**
     * 使用默认配置构造直连模式发送门面。
     *
     * @param backend 后端实现
     */
    public MqProducer(MqBackend backend) {
        this(backend, new StringMqMessageCodec());
    }

    /**
     * 构造直连模式发送门面。
     *
     * @param backend 后端实现
     * @param codec   编解码器
     */
    public MqProducer(MqBackend backend, MqMessageCodec codec) {
        this.registry = null;
        this.directBackend = Objects.requireNonNull(backend, "backend");
        this.defaultInstance = null;
        this.codec = Objects.requireNonNull(codec, "codec");
        this.tagSuffix = "";
        this.tagSuffixEnabled = true;
    }

    /**
     * 发送消息；带 tag 或延迟时先校验后端能力。
     *
     * @param request 发送请求
     * @throws MqConfigurationException 实例未声明或后端不支持所用能力
     */
    public void send(MqSendRequest request) {
        if (request.getPayload() == null) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.MQ_PAYLOAD_REQUIRED));
        }
        MqSendRequest resolved = withDefaultInstance(request);
        MqBackend backend = resolveBackend(resolved);
        String tag = resolved.getTag();
        if (Strings.isNotBlank(tag) && !MqConstants.WILDCARD_TAG.equals(tag)) {
            if (resolved.isTagSuffixEnabled() && tagSuffixEnabled && Strings.isNotBlank(tagSuffix)) {
                resolved = resolved.toBuilder().tag(tag + "-" + tagSuffix).build();
            }
            requireCapability(backend, MqCapability.TAG);
        }
        if (resolved.getDelay() != null) {
            requireCapability(backend, MqCapability.DELAY);
        }

        long timestamp = resolved.getTimestamp() == null
                ? System.currentTimeMillis()
                : resolved.getTimestamp();
        MqMessage message = MqMessage.builder()
                .body(codec.encode(resolved.getPayload()))
                .key(resolved.getKey())
                .headers(resolved.getHeaders())
                .extra(resolved.getExtra())
                .timestamp(timestamp)
                .codec(codec)
                .build();
        log.debug(Messages.get(ErrorCode.MQ_SEND_DEBUG, resolved.getInstance(),
                resolved.getTopic(), resolved.getTag(), resolved.getDelay()));
        backend.send(resolved, message);
    }

    /**
     * 按 topic 发送，实例使用构造时绑定值或唯一默认实例。
     *
     * @param topic   topic
     * @param payload 业务对象
     */
    public void send(String topic, Object payload) {
        send(topic, null, payload, null);
    }

    /**
     * 按 topic 与 tag 发送。
     *
     * @param topic   topic
     * @param tag     tag
     * @param payload 业务对象
     */
    public void send(String topic, String tag, Object payload) {
        send(topic, tag, payload, null);
    }

    /**
     * 按 topic 发送延迟消息。
     *
     * @param topic   topic
     * @param payload 业务对象
     * @param delay   延迟时间
     */
    public void send(String topic, Object payload, Duration delay) {
        send(topic, null, payload, delay);
    }

    /**
     * 按 topic、tag 与延迟发送。
     *
     * @param topic   topic
     * @param tag     tag
     * @param payload 业务对象
     * @param delay   延迟时间
     */
    public void send(String topic, String tag, Object payload, Duration delay) {
        send(MqSendRequest.builder()
                .topic(topic)
                .tag(tag)
                .payload(payload)
                .delay(delay)
                .build());
    }

    /**
     * 本地事务提交后发送；无事务时立即发送。
     *
     * <p>只保证「本地事务提交后才发」，不保证消息与数据库事务的原子性，
     * 不是 MQ 事务消息。</p>
     *
     * @param request 发送请求
     * @throws MqConfigurationException 实例未声明或后端不支持所用能力
     */
    public void sendAfterCommit(MqSendRequest request) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(request);
                }
            });
            return;
        }
        send(request);
    }

    /**
     * 本地事务提交后按 topic 发送。
     *
     * @param topic topic
     * @param body  消息体
     */
    public void sendAfterCommit(String topic, String body) {
        sendAfterCommit(MqSendRequest.builder().topic(topic).payload(body).build());
    }

    /**
     * 本地事务提交后按 topic 与 tag 发送。
     *
     * @param topic topic
     * @param tag   tag
     * @param body  消息体
     */
    public void sendAfterCommit(String topic, String tag, String body) {
        sendAfterCommit(MqSendRequest.builder().topic(topic).tag(tag).payload(body).build());
    }

    private MqBackend resolveBackend(MqSendRequest request) {
        return directBackend != null ? directBackend : registry.backendOf(request.getInstance());
    }

    private void requireCapability(MqBackend backend, MqCapability capability) {
        backend.requireCapability(capability);
    }

    private MqSendRequest withDefaultInstance(MqSendRequest request) {
        if (Strings.isNotBlank(request.getInstance())) {
            return request;
        }
        String instance = Strings.isNotBlank(defaultInstance)
                ? defaultInstance
                : (registry == null ? null : registry.defaultInstance());
        if (instance == null) {
            return request;
        }
        return request.toBuilder().instance(instance).build();
    }
}
