package cn.kennylee.banyan.spring.mvc.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 缺少 Spring MVC 核心类时跳过装配并提示；校验、Feign、Jackson 属可选增强，缺失时静默跳过。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "banyan.spring.mvc", name = "enabled", matchIfMissing = true)
public class MvcMissingClassAutoConfiguration {

    /**
     * 缺少 Spring MVC 核心类。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.web.servlet.DispatcherServlet")
    static class SpringMvcMissing {

        /**
         * 构造实例并告警。
         */
        SpringMvcMissing() {
            log.warn(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED, "Spring MVC", "banyan-spring-mvc"));
        }
    }
}
