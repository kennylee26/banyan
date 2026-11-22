package cn.kennylee.banyan.mq.core;

/**
 * 消息配置错误：实例未声明、后端类型重复、使用后端不支持的能力等。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class MqConfigurationException extends IllegalStateException {

    /**
     * 构造异常。
     *
     * @param message 错误说明
     */
    public MqConfigurationException(String message) {
        super(message);
    }
}
