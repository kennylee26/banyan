package cn.kennylee.banyan.mq.consumer.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 缺少 Spring 生命周期接口时只打 WARN，不注册 bean，应用正常启动。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "banyan.mq", name = "enabled", matchIfMissing = true)
public class MqConsumerMissingClassAutoConfiguration {

    /**
     * 缺少 SmartInitializingSingleton。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.beans.factory.SmartInitializingSingleton")
    static class LifecycleMissing {

        /**
         * 构造并告警。
         */
        LifecycleMissing() {
            log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, "Spring Lifecycle",
                    "banyan-mq-consumer"));
        }
    }
}
