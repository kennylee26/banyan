package cn.kennylee.banyan.mq.core;

/**
 * 消息头约定：扩展信息使用统一前缀，与普通 headers 区分。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class MqHeaders {

    /** 扩展信息在消息头中的前缀。 */
    public static final String EXTRA_PREFIX = "x-extra-";

    /** 发送时间戳在消息头中的键。 */
    public static final String TIMESTAMP = "x-message-timestamp";

    private MqHeaders() {
    }

    /**
     * 解析发送时间戳，非法或为空时返回 null。
     *
     * @param value 时间戳文本
     * @return 时间戳，可能为 null
     */
    public static Long parseTimestamp(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
