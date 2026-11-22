package cn.kennylee.banyan.mq.core.spi;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.util.Strings;
import lombok.Builder;
import lombok.Getter;

/**
 * 消费者定义：由注解解析而来，交给后端注册。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
public final class MqConsumerDefinition {

    /** 逻辑实例名。 */
    private final String instance;

    /** topic。 */
    private final String topic;

    /** tag 过滤表达式。 */
    private final String tag;

    /** 消费组。 */
    private final String group;

    /** 并发数；0 表示后端默认。 */
    private final int concurrency;

    /** 最大重试次数；-1 表示后端默认。 */
    private final int maxRetryTimes;

    /**
     * 构造消费者定义。
     *
     * @param instance      逻辑实例名
     * @param topic         topic
     * @param tag           tag 过滤表达式
     * @param group         消费组
     * @param concurrency   并发数
     * @param maxRetryTimes 最大重试次数
     * @throws IllegalArgumentException topic 为空
     */
    @Builder
    private MqConsumerDefinition(String instance, String topic, String tag, String group,
                                 int concurrency, int maxRetryTimes) {
        if (Strings.isBlank(topic)) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.MQ_TOPIC_REQUIRED));
        }
        this.instance = instance;
        this.topic = topic;
        this.tag = tag;
        this.group = group;
        this.concurrency = concurrency;
        this.maxRetryTimes = maxRetryTimes;
    }
}
