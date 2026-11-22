package cn.kennylee.banyan.mq.spring;

import cn.kennylee.banyan.commons.spring.json.JsonCodec;
import cn.kennylee.banyan.mq.core.MqMessageCodec;

import java.lang.reflect.Type;

/**
 * 基于 JsonCodec 的消息编解码：默认由 Jackson 提供，使用方可替换 JsonCodec 实现。
 *
 * <p>String 视为已经是 JSON 文本，直接透传，避免被再次序列化成带引号的字符串。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class JsonMqMessageCodec implements MqMessageCodec {

    private final JsonCodec jsonCodec;

    /**
     * 构造编解码器。
     *
     * @param jsonCodec JSON 编解码
     */
    public JsonMqMessageCodec(JsonCodec jsonCodec) {
        this.jsonCodec = jsonCodec;
    }

    @Override
    public String encode(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof String text) {
            return text;
        }
        return jsonCodec.toJson(payload);
    }

    @Override
    public Object decode(String body, Class<?> type) {
        if (body == null) {
            return null;
        }
        if (type == null || type == String.class || type == Object.class) {
            return body;
        }
        return jsonCodec.fromJson(body, type);
    }

    @Override
    public Object decode(String body, Type type) {
        return body == null ? null : jsonCodec.fromJson(body, type);
    }
}
