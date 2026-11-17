package cn.kennylee.banyan.cache.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 缺少可选依赖时只打 WARN，不注册任何 bean，应用正常启动。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "banyan.cache", name = "enabled", matchIfMissing = true)
public class CacheMissingClassAutoConfiguration {

    /**
     * 输出跳过告警。
     *
     * @param component 缺失的能力名
     */
    private static void warn(String component) {
        log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, component, "banyan-cache"));
    }

    /**
     * 缺少 Spring Data Redis。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.data.redis.core.StringRedisTemplate")
    static class RedisMissing {

        /**
         * 构造实例并告警。
         */
        RedisMissing() {
            warn("Spring Data Redis");
        }
    }

    /**
     * 缺少 Jackson。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("com.fasterxml.jackson.databind.ObjectMapper")
    static class JacksonMissing {

        /**
         * 构造实例并告警。
         */
        JacksonMissing() {
            warn("Jackson");
        }
    }

    /**
     * 缺少 Jackson JSR-310。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule")
    static class JacksonTimeMissing {

        /**
         * 构造实例并告警。
         */
        JacksonTimeMissing() {
            warn("Jackson JSR-310");
        }
    }

    /**
     * 缺少 Spring Boot 配置属性能力。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.context.properties.ConfigurationProperties")
    static class BootConfigMissing {

        /**
         * 构造实例并告警。
         */
        BootConfigMissing() {
            warn("Spring Boot 配置属性");
        }
    }
}
