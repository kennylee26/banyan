package cn.kennylee.banyan.mq.core;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;

import java.lang.reflect.Type;

/**
 * 消息体编解码扩展点。
 *
 * <p>默认实现只做 String 直通；使用方需要自动转 POJO 时，
 * 自行注册基于其应用自带 JSON 库的实现，本库不引入 JSON 依赖。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface MqMessageCodec {

    /**
     * 把业务对象编码为消息体文本。
     *
     * @param payload 业务对象，可为 null
     * @return 消息体文本，payload 为 null 时返回 null
     */
    String encode(Object payload);

    /**
     * 把消息体文本解码为指定类型。
     *
     * @param body 消息体文本，可为 null
     * @param type 目标类型
     * @return 解码结果，body 为 null 时返回 null
     * @throws IllegalArgumentException 默认实现遇到不支持的目标类型
     */
    Object decode(String body, Class<?> type);

    /**
     * 把消息体文本解码为带泛型信息的类型。
     *
     * <p>默认实现不支持泛型；基于 JSON 的编解码器应覆盖此方法以支持
     * {@code List<Bean>}、{@code Map<String, Bean>} 等类型。</p>
     *
     * @param body 消息体文本，可为 null
     * @param type 目标类型（含泛型参数）
     * @return 解码结果，body 为 null 时返回 null
     * @throws UnsupportedOperationException 当前实现不支持泛型
     */
    default Object decode(String body, Type type) {
        throw new UnsupportedOperationException(
                Messages.get(ErrorCode.MQ_CODEC_UNSUPPORTED, type.getTypeName()));
    }
}
