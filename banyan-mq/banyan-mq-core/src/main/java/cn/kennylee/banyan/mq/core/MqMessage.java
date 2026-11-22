package cn.kennylee.banyan.mq.core;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 统一消息体：业务数据与消息元信息，不包含 topic / tag 等寻址信息。
 *
 * <p>body 是 UTF-8 JSON 文本；headers 与 extra 为不可变副本。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
public final class MqMessage {

    /** 消息体 JSON 文本。 */
    private final String body;

    /** 业务键，对应 hashKey，影响路由与顺序。 */
    private final String key;

    /** 消息头，不可变。 */
    private final Map<String, String> headers;

    /** 扩展信息，不可变。 */
    private final Map<String, String> extra;

    /** 创建时间戳，毫秒。 */
    private final long timestamp;

    /** 编解码器，用于把 body 转为业务对象。 */
    @Getter(AccessLevel.NONE)
    private final MqMessageCodec codec;

    /**
     * 构造消息。
     *
     * @param body      消息体
     * @param key       业务键
     * @param headers   消息头
     * @param extra     扩展信息
     * @param timestamp 创建时间戳
     * @param codec     编解码器
     */
    @Builder
    private MqMessage(String body, String key, Map<String, String> headers,
                      Map<String, String> extra, long timestamp, MqMessageCodec codec) {
        this.body = body;
        this.key = key;
        this.headers = headers == null ? Map.of() : Map.copyOf(headers);
        this.extra = extra == null ? Map.of() : Map.copyOf(extra);
        this.timestamp = timestamp;
        this.codec = codec;
    }

    /**
     * 以 JSON 数据体形式访问，可直接转业务对象。
     *
     * @return 数据体
     */
    public MqData getData() {
        return new MqData(body, codec);
    }

    /**
     * 绑定编解码器，返回新实例。
     *
     * @param codec 编解码器
     * @return 绑定后的消息
     */
    public MqMessage withCodec(MqMessageCodec codec) {
        if (this.codec == codec) {
            return this;
        }
        return MqMessage.builder()
                .body(body)
                .key(key)
                .headers(headers)
                .extra(extra)
                .timestamp(timestamp)
                .codec(codec)
                .build();
    }
}
