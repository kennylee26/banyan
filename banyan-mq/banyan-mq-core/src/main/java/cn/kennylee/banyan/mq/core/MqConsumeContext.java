package cn.kennylee.banyan.mq.core;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 消费上下文：向业务方法暴露公共元信息与不透明原生对象。
 *
 * <p>原生对象统一为 {@link Object}，避免 core 泄漏任何厂商类型；
 * 需要厂商专有信息时由业务方自行转换。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
public final class MqConsumeContext {

    /** topic。 */
    private final String topic;

    /** tag。 */
    private final String tag;

    /** 消息 id。 */
    private final String messageId;

    /** 业务键。 */
    private final String key;

    /** 消息头，不可变。 */
    private final Map<String, String> headers;

    /** 已重试次数。 */
    private final int retryTimes;

    /** 逻辑实例名。 */
    private final String instance;

    /** 后端原生消息对象，不透明。 */
    private final Object nativeMessage;

    /**
     * 构造消费上下文。
     *
     * @param topic         topic
     * @param tag           tag
     * @param messageId     消息 id
     * @param key           业务键
     * @param headers       消息头
     * @param retryTimes    已重试次数
     * @param instance      逻辑实例名
     * @param nativeMessage 原生消息对象
     */
    @Builder
    private MqConsumeContext(String topic, String tag, String messageId, String key,
                             Map<String, String> headers, int retryTimes, String instance,
                             Object nativeMessage) {
        this.topic = topic;
        this.tag = tag;
        this.messageId = messageId;
        this.key = key;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
        this.retryTimes = retryTimes;
        this.instance = instance;
        this.nativeMessage = nativeMessage;
    }
}
