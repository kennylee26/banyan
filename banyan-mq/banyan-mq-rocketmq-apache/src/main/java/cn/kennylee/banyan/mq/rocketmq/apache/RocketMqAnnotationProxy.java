package cn.kennylee.banyan.mq.rocketmq.apache;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 用 JDK 动态代理构造 {@code RocketMQMessageListener} 接口实例。
 *
 * <p>厂商容器初始化时会读取该注解的全部属性；一个代理类即可复用给所有消费者，
 * 不需要字节码生成库。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class RocketMqAnnotationProxy {

    private RocketMqAnnotationProxy() {
    }

    /**
     * 构造注解接口实例。
     *
     * @param settings 属性值
     * @return 注解接口实例
     */
    public static RocketMQMessageListener create(RocketMqAnnotationSettings settings) {
        return (RocketMQMessageListener) Proxy.newProxyInstance(
                RocketMqAnnotationProxy.class.getClassLoader(),
                new Class<?>[]{RocketMQMessageListener.class},
                new Handler(settings));
    }

    /**
     * 注解代理处理器。
     *
     * @author kennylee
     * @since 0.1.0
     */
    private static final class Handler implements InvocationHandler {

        /** 注解属性值。 */
        private final RocketMqAnnotationSettings settings;

        private Handler(RocketMqAnnotationSettings settings) {
            this.settings = settings;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "annotationType" -> RocketMQMessageListener.class;
                case "toString" -> "RocketMQMessageListenerProxy(" + settings.getConsumerGroup() + ")";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == (args == null ? null : args[0]);
                case "consumerGroup" -> settings.getConsumerGroup();
                case "topic" -> settings.getTopic();
                case "selectorType" -> settings.getSelectorType();
                case "selectorExpression" -> settings.getSelectorExpression();
                case "consumeMode" -> settings.getConsumeMode();
                case "messageModel" -> settings.getMessageModel();
                case "consumeThreadMax" -> settings.getConsumeThreadMax();
                case "consumeThreadNumber" -> settings.getConsumeThreadNumber();
                case "maxReconsumeTimes" -> settings.getMaxReconsumeTimes();
                case "consumeTimeout" -> settings.getConsumeTimeout();
                case "replyTimeout" -> settings.getReplyTimeout();
                case "accessKey" -> settings.getAccessKey();
                case "secretKey" -> settings.getSecretKey();
                case "enableMsgTrace" -> settings.isEnableMsgTrace();
                case "customizedTraceTopic" -> settings.getCustomizedTraceTopic();
                case "nameServer" -> settings.getNameServer();
                case "accessChannel" -> settings.getAccessChannel();
                case "tlsEnable" -> settings.getTlsEnable();
                case "namespace" -> settings.getNamespace();
                case "delayLevelWhenNextConsume" -> settings.getDelayLevelWhenNextConsume();
                case "suspendCurrentQueueTimeMillis" -> settings.getSuspendCurrentQueueTimeMillis();
                case "awaitTerminationMillisWhenShutdown" -> settings.getAwaitTerminationMillisWhenShutdown();
                case "instanceName" -> settings.getInstanceName();
                default -> throw new IllegalStateException(Messages.get(
                        ErrorCode.MQ_CONFIGURATION_INVALID,
                        "未支持的注解属性: " + method.getName()));
            };
        }
    }
}
