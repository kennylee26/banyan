package cn.kennylee.banyan.mq.core;

/**
 * 消息公共常量。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class MqConstants {

    /** 通配 tag，表示匹配全部。 */
    public static final String WILDCARD_TAG = "*";

    /** 多 tag 表达式的分隔符，仅框架内部拼接使用。 */
    public static final String TAG_SEPARATOR = "||";

    private MqConstants() {
    }
}
