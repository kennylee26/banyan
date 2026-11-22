package cn.kennylee.banyan.mq.core.spi;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.util.Map;

/**
 * 发送请求：只承载寻址与发送选项，消息体统一由 {@code MqMessage} 表达。
 *
 * <p>topic / tag / instance 属于寻址；payload / key / headers / extra 属于消息体；
 * delay 与 timestamp 属于发送选项。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
public final class MqSendRequest {

    /** 逻辑实例名。 */
    private final String instance;

    /** topic。 */
    private final String topic;

    /** tag。 */
    private final String tag;

    /** 业务对象，由生产者用编解码器编码为消息体。 */
    private final Object payload;

    /** 业务键。 */
    private final String key;

    /** 消息头，不可变。 */
    private final Map<String, String> headers;

    /** 扩展信息，不可变。 */
    private final Map<String, String> extra;

    /** 延迟时间，为 null 表示不延迟。 */
    private final Duration delay;

    /** 发送时间戳，为 null 表示使用当前时间。 */
    private final Long timestamp;

    /** 是否给 tag 追加环境后缀，默认 true。 */
    private final boolean tagSuffixEnabled;

    /**
     * 构造发送请求。
     *
     * @param instance         逻辑实例名
     * @param topic            topic
     * @param tag              tag
     * @param payload          业务对象
     * @param key              业务键
     * @param headers          消息头
     * @param extra            扩展信息
     * @param delay            延迟时间
     * @param timestamp        发送时间戳，可为 null
     * @param tagSuffixEnabled 是否给 tag 追加环境后缀，为 null 时默认 true
     * @throws IllegalArgumentException topic 为空
     */
    @Builder(toBuilder = true)
    private MqSendRequest(String instance, String topic, String tag, Object payload, String key,
                          Map<String, String> headers, Map<String, String> extra, Duration delay,
                          Long timestamp, Boolean tagSuffixEnabled) {
        if (Strings.isBlank(topic)) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.MQ_TOPIC_REQUIRED));
        }
        this.instance = instance;
        this.topic = topic;
        this.tag = tag;
        this.payload = payload;
        this.key = key;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
        this.extra = extra == null ? Map.of() : Map.copyOf(extra);
        this.delay = delay;
        this.timestamp = timestamp;
        this.tagSuffixEnabled = tagSuffixEnabled == null || tagSuffixEnabled;
    }
}
