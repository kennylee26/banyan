package cn.kennylee.banyan.mq.rocketmq.apache;

import cn.kennylee.banyan.mq.core.MqConstants;
import lombok.Builder;
import lombok.Getter;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.SelectorType;

/**
 * 厂商注解属性值：代理按这些值实现 {@code RocketMQMessageListener} 的全部方法。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Builder
public class RocketMqAnnotationSettings {

    /** 消费组。 */
    private final String consumerGroup;

    /** topic。 */
    private final String topic;

    /** 选择器类型。 */
    @Builder.Default
    private final SelectorType selectorType = SelectorType.TAG;

    /** 选择器表达式。 */
    @Builder.Default
    private final String selectorExpression = MqConstants.WILDCARD_TAG;

    /** 消费模式。 */
    @Builder.Default
    private final ConsumeMode consumeMode = ConsumeMode.CONCURRENTLY;

    /** 消息模型。 */
    @Builder.Default
    private final MessageModel messageModel = MessageModel.CLUSTERING;

    /** 消费线程上限。 */
    @Builder.Default
    private final int consumeThreadMax = 64;

    /** 消费线程数。 */
    @Builder.Default
    private final int consumeThreadNumber = 20;

    /** 最大重试次数。 */
    @Builder.Default
    private final int maxReconsumeTimes = -1;

    /** 消费超时，分钟。 */
    @Builder.Default
    private final long consumeTimeout = 15L;

    /** 回复超时，毫秒。 */
    @Builder.Default
    private final int replyTimeout = 3000;

    /** 访问密钥。 */
    @Builder.Default
    private final String accessKey = "";

    /** 密钥。 */
    @Builder.Default
    private final String secretKey = "";

    /** 是否开启消息轨迹。 */
    @Builder.Default
    private final boolean enableMsgTrace = false;

    /** 自定义轨迹 topic。 */
    @Builder.Default
    private final String customizedTraceTopic = "";

    /** name server。 */
    @Builder.Default
    private final String nameServer = "";

    /** 访问通道。 */
    @Builder.Default
    private final String accessChannel = "";

    /** TLS 开关。 */
    @Builder.Default
    private final String tlsEnable = "";

    /** 命名空间。 */
    @Builder.Default
    private final String namespace = "";

    /** 下次消费延迟级别。 */
    @Builder.Default
    private final int delayLevelWhenNextConsume = 0;

    /** 挂起当前队列时间，毫秒。 */
    @Builder.Default
    private final int suspendCurrentQueueTimeMillis = 1000;

    /** 关闭等待时间，毫秒。 */
    @Builder.Default
    private final int awaitTerminationMillisWhenShutdown = 0;

    /** 实例名。 */
    @Builder.Default
    private final String instanceName = "DEFAULT";
}
