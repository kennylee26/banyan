package cn.kennylee.banyan.mq.rocketmq.apache.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 缺少 RocketMQ 客户端时只打 WARN，不注册 bean，应用正常启动。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "banyan.mq", name = "enabled", matchIfMissing = true)
public class RocketMqMissingClassAutoConfiguration {

    /**
     * 缺少 RocketMQTemplate。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.apache.rocketmq.spring.core.RocketMQTemplate")
    static class RocketMqMissing {

        /**
         * 构造并告警。
         */
        RocketMqMissing() {
            log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, "RocketMQ Spring", "banyan-mq"));
        }
    }
}
