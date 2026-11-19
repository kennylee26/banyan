package cn.kennylee.banyan.log.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 缺少必需依赖时只打 WARN，不注册任何 bean，应用正常启动。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "banyan.log", name = "enabled", matchIfMissing = true)
public class LogMissingClassAutoConfiguration {

    /**
     * 输出跳过告警。
     *
     * @param component 缺失的能力名
     */
    private static void warn(String component) {
        log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, component, "banyan-log"));
    }

    /**
     * 缺少 Spring Boot 配置属性能力。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.context.properties.ConfigurationProperties")
    static class SpringBootMissing {

        /**
         * 构造实例并告警。
         */
        SpringBootMissing() {
            warn("Spring Boot 配置属性");
        }
    }

    /**
     * 缺少 Spring 事件能力。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.context.ApplicationEvent")
    static class SpringEventMissing {

        /**
         * 构造实例并告警。
         */
        SpringEventMissing() {
            warn("Spring 事件");
        }
    }
}
