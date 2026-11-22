package cn.kennylee.banyan.mq.consumer;

import cn.kennylee.banyan.mq.core.MqConstants;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级消费者注解：一个业务类里的多个方法可以分别消费不同 topic / tag。
 *
 * <p>topic / tag / group 支持 Spring 占位符（如 {@code ${samples.topic}}）；
 * threads / maxRetryTimes 为数值，Java 注解无法承载占位符。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqConsumer {

    /**
     * 逻辑实例名；为空时使用唯一默认实例。
     *
     * @return 逻辑实例名
     */
    String instance() default "";

    /**
     * topic，支持 Spring 占位符。
     *
     * @return topic
     */
    String topic();

    /**
     * tag 过滤表达式，默认匹配全部，支持 Spring 占位符。
     *
     * @return tag 表达式
     */
    String tag() default MqConstants.WILDCARD_TAG;

    /**
     * 消费组；为空时按业务类与方法名自动生成，支持 Spring 占位符。
     *
     * @return 消费组
     */
    String group() default "";

    /**
     * 并发数；0 表示使用后端默认值。
     *
     * <p>Kafka 映射为容器并发数；RocketMQ 同时设置 {@code consumeThreadNumber}
     * 与 {@code consumeThreadMax}。</p>
     *
     * @return 并发数
     */
    int concurrency() default 0;

    /**
     * 最大重试次数；-1 表示使用后端默认值。
     *
     * <p>RocketMQ 映射 {@code maxReconsumeTimes}；Kafka 没有内建重试能力，
     * 该属性在 Kafka 上不生效并会打印 WARN，需要重试时请使用手动 ACK 或自定义错误处理器。</p>
     *
     * @return 最大重试次数
     */
    int maxRetryTimes() default -1;

    /**
     * 是否给消费组追加环境后缀。
     *
     * @return true 表示追加
     */
    boolean groupSuffixEnabled() default true;

    /**
     * 是否给 tag 追加环境后缀。
     *
     * @return true 表示追加
     */
    boolean tagSuffixEnabled() default true;
}
