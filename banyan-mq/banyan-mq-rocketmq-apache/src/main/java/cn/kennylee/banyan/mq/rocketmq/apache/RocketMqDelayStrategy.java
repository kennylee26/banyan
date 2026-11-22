package cn.kennylee.banyan.mq.rocketmq.apache;

import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;

import java.time.Duration;

/**
 * RocketMQ 延迟发送策略：5 之前走固定延迟级别，5 及之后走任意时间。
 *
 * <p>4.x 的级别共 18 档，超出范围取最接近的一档，不静默丢弃延迟语义。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class RocketMqDelayStrategy {

    /** 4.x 的 18 个延迟级别对应秒数。 */
    private static final int[] LEVEL_SECONDS = {
            1, 5, 10, 30, 60, 120, 180, 240, 300, 360, 420, 480, 540, 600, 1200, 1800, 3600, 7200
    };

    /** 4.x 同步发送默认超时。 */
    private static final long DEFAULT_SEND_TIMEOUT_MILLIS = 3000L;

    /** 发送模板。 */
    private final RocketMQTemplate template;

    /** 客户端主版本。 */
    private final int clientMajorVersion;

    /**
     * 构造策略。
     *
     * @param template           发送模板
     * @param clientMajorVersion 客户端主版本
     */
    public RocketMqDelayStrategy(RocketMQTemplate template, int clientMajorVersion) {
        this.template = template;
        this.clientMajorVersion = clientMajorVersion;
    }

    /**
     * 按当前 classpath 上的客户端版本构造策略。
     *
     * @param template 发送模板
     * @return 策略
     */
    public static RocketMqDelayStrategy current(RocketMQTemplate template) {
        return new RocketMqDelayStrategy(template, resolveMajorVersion());
    }

    /**
     * 发送延迟消息。
     *
     * @param destination 目标
     * @param message     消息
     * @param delay       延迟时间
     */
    public void send(String destination, Message<String> message, Duration delay) {
        if (clientMajorVersion >= 5) {
            template.syncSendDelayTimeMills(destination, message, delay.toMillis());
            return;
        }
        template.syncSend(destination, message, DEFAULT_SEND_TIMEOUT_MILLIS, closestLevel(delay));
    }

    /**
     * 取最接近的延迟级别。
     *
     * @param delay 延迟时间
     * @return 1 到 18 的级别
     */
    static int closestLevel(Duration delay) {
        long seconds = Math.max(delay.getSeconds(), 0);
        int bestLevel = 1;
        long bestDiff = Long.MAX_VALUE;
        for (int index = 0; index < LEVEL_SECONDS.length; index++) {
            long diff = Math.abs(seconds - LEVEL_SECONDS[index]);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestLevel = index + 1;
            }
        }
        return bestLevel;
    }

    static int resolveMajorVersion() {
        return parseMajorVersion(MQVersion.getVersionDesc(MQVersion.CURRENT_VERSION));
    }

    /**
     * 从版本描述中解析主版本号。
     *
     * @param description 版本描述，例如 V4_9_8 或 4.9.8
     * @return 首个数字；解析不到时返回 4
     */
    static int parseMajorVersion(String description) {
        if (description == null) {
            return 4;
        }
        for (int index = 0; index < description.length(); index++) {
            char current = description.charAt(index);
            if (Character.isDigit(current)) {
                return current - '0';
            }
        }
        return 4;
    }
}
