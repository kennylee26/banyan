package cn.kennylee.banyan.mq.core.spi;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.mq.core.MqCapability;
import cn.kennylee.banyan.mq.core.MqConfigurationException;
import cn.kennylee.banyan.mq.core.MqMessage;

import java.util.Set;

/**
 * 消息后端扩展点。
 *
 * <p>实现方负责把统一契约落到具体消息中间件；本接口不出现任何厂商类型。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface MqBackend {

    /**
     * 后端类型标识，例如 kafka、rocketmq。
     *
     * @return 类型标识
     */
    String type();

    /**
     * 声明支持的可选能力。
     *
     * @return 能力集合，不可为 null
     */
    Set<MqCapability> capabilities();

    /**
     * 校验本后端是否支持某能力，不支持时抛明确错误。
     *
     * @param capability 能力
     * @throws MqConfigurationException 后端不支持该能力
     */
    default void requireCapability(MqCapability capability) {
        if (!capabilities().contains(capability)) {
            throw new MqConfigurationException(Messages.get(ErrorCode.MQ_CONFIGURATION_INVALID,
                    "后端 " + type() + " 不支持能力 " + capability));
        }
    }

    /**
     * 同步发送。
     *
     * @param request 寻址与发送选项
     * @param message 统一消息体
     */
    void send(MqSendRequest request, MqMessage message);

    /**
     * 注册一个消费者。
     *
     * @param definition 消费者定义
     * @param handler    消费处理器
     */
    void registerConsumer(MqConsumerDefinition definition, MqConsumerHandler handler);
}
