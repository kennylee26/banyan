package cn.kennylee.banyan.openapi.sign.autoconfigure;

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
@ConditionalOnProperty(prefix = "banyan.openapi-sign", name = "enabled", matchIfMissing = true)
public class OpenApiSignMissingClassAutoConfiguration {

    /**
     * 输出跳过告警。
     *
     * @param component 缺失的能力名
     */
    private static void warn(String component) {
        log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, component, "banyan-openapi-sign"));
    }

    /**
     * 缺少 Servlet 能力。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("jakarta.servlet.Filter")
    static class ServletMissing {

        /**
         * 构造实例并告警。
         */
        ServletMissing() {
            warn("Servlet");
        }
    }

    /**
     * 缺少 Spring MVC 能力。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.web.servlet.HandlerInterceptor")
    static class SpringMvcMissing {

        /**
         * 构造实例并告警。
         */
        SpringMvcMissing() {
            warn("Spring MVC");
        }
    }
}
