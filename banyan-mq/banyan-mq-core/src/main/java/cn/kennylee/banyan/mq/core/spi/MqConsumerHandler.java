package cn.kennylee.banyan.mq.core.spi;

import cn.kennylee.banyan.mq.core.MqConsumeContext;
import cn.kennylee.banyan.mq.core.MqMessage;

/**
 * 消费处理器：由消费者模块把业务方法包装后交给后端。
 *
 * @author kennylee
 * @since 0.1.0
 */
@FunctionalInterface
public interface MqConsumerHandler {

    /**
     * 处理一条消息。
     *
     * @param message 消息
     * @param context 消费上下文
     * @return true 表示消费成功，false 或抛异常表示消费失败
     */
    boolean handle(MqMessage message, MqConsumeContext context);
}
