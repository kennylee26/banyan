package cn.kennylee.banyan.trace.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 缺少可选技术栈依赖时只打 WARN，不注册任何 bean，应用正常启动。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "banyan.trace", name = "enabled", matchIfMissing = true)
public class TraceMissingClassAutoConfiguration {

    /**
     * 输出跳过告警。
     *
     * @param component 缺失的能力名
     */
    private static void warn(String component) {
        log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, component, "banyan-trace"));
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
     * 缺少 Servlet 能力。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("jakarta.servlet.Filter")
    @ConditionalOnProperty(prefix = "banyan.trace.servlet", name = "enabled", matchIfMissing = true)
    static class ServletMissing {

        /**
         * 构造实例并告警。
         */
        ServletMissing() {
            warn("Servlet");
        }
    }

    /**
     * 缺少 WebFlux 能力。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.web.server.WebFilter")
    @ConditionalOnProperty(prefix = "banyan.trace.webflux", name = "enabled", matchIfMissing = true)
    static class WebfluxMissing {

        /**
         * 构造实例并告警。
         */
        WebfluxMissing() {
            warn("WebFlux");
        }
    }

    /**
     * 缺少 Feign 能力。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("feign.RequestInterceptor")
    @ConditionalOnProperty(prefix = "banyan.trace.feign", name = "enabled", matchIfMissing = true)
    static class FeignMissing {

        /**
         * 构造实例并告警。
         */
        FeignMissing() {
            warn("Feign");
        }
    }

    /**
     * 缺少 Dubbo 能力。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.apache.dubbo.rpc.Filter")
    @ConditionalOnProperty(prefix = "banyan.trace.dubbo", name = "enabled", matchIfMissing = true)
    static class DubboMissing {

        /**
         * 构造实例并告警。
         */
        DubboMissing() {
            warn("Dubbo");
        }
    }
}
