package cn.kennylee.banyan.mq.core;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;

import java.nio.charset.StandardCharsets;

/**
 * 默认编解码：String 直通，byte[] 按 UTF-8 转换。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class StringMqMessageCodec implements MqMessageCodec {

    @Override
    public String encode(Object payload) {
        return payload == null ? null : String.valueOf(payload);
    }

    @Override
    public Object decode(String body, Class<?> type) {
        if (body == null) {
            return null;
        }
        if (type == null || type == String.class || type == Object.class) {
            return body;
        }
        if (type == byte[].class) {
            return body.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException(
                Messages.get(ErrorCode.MQ_CODEC_UNSUPPORTED, type.getName()));
    }
}
