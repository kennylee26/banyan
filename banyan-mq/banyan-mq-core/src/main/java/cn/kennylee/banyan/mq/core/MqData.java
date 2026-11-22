package cn.kennylee.banyan.mq.core;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;

import java.lang.reflect.Type;

/**
 * 消息数据体：JSON 文本 + 编解码器，便于直接转业务对象。
 *
 * <pre>{@code
 * OrderEvent event = message.getData().toBean(OrderEvent.class);
 * }</pre>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class MqData {

    /** JSON 文本。 */
    private final String json;

    /** 编解码器，可为 null。 */
    private final MqMessageCodec codec;

    MqData(String json, MqMessageCodec codec) {
        this.json = json;
        this.codec = codec;
    }

    /**
     * 原始 JSON 文本。
     *
     * @return JSON 文本，可能为 null
     */
    public String asString() {
        return json;
    }

    /**
     * 转换为业务对象。
     *
     * @param type 目标类型
     * @param <T>  目标类型
     * @return 业务对象
     * @throws MqConfigurationException 消息未绑定编解码器
     */
    public <T> T toBean(Class<T> type) {
        if (codec == null) {
            throw new MqConfigurationException(Messages.get(ErrorCode.MQ_CONFIGURATION_INVALID,
                    "MqMessage 未绑定编解码器，无法转换为 " + type.getName()));
        }
        return type.cast(codec.decode(json, type));
    }

    /**
     * 转换为带泛型信息的业务对象，例如 {@code List<Bean>}、{@code Map<String, Bean>}。
     *
     * @param type 目标类型（含泛型参数）
     * @param <T>  目标类型
     * @return 业务对象
     * @throws MqConfigurationException 消息未绑定编解码器
     */
    @SuppressWarnings("unchecked")
    public <T> T toBean(Type type) {
        if (codec == null) {
            throw new MqConfigurationException(Messages.get(ErrorCode.MQ_CONFIGURATION_INVALID,
                    "MqMessage 未绑定编解码器，无法转换为 " + type.getTypeName()));
        }
        return (T) codec.decode(json, type);
    }
}
