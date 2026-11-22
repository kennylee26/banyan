package cn.kennylee.banyan.mq.kafka.spring.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 缺少 Kafka 客户端时只打 WARN，不注册 bean，应用正常启动。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "banyan.mq", name = "enabled", matchIfMissing = true)
public class KafkaMqMissingClassAutoConfiguration {

    /**
     * 缺少 KafkaTemplate。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.kafka.core.KafkaTemplate")
    static class KafkaMissing {

        /**
         * 构造并告警。
         */
        KafkaMissing() {
            log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, "Spring Kafka", "banyan-mq"));
        }
    }
}
