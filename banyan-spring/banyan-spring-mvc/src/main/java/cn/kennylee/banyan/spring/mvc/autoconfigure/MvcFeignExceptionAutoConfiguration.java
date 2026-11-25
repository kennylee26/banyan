package cn.kennylee.banyan.spring.mvc.autoconfigure;

import cn.kennylee.banyan.spring.mvc.error.ExceptionResultFactory;
import cn.kennylee.banyan.spring.mvc.handler.BanyanFeignExceptionAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Feign 异常装配：仅在 classpath 存在 Feign 且是 Spring MVC 应用时生效。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration(after = MvcCoreAutoConfiguration.class)
@ConditionalOnClass(name = {
        "org.springframework.web.servlet.DispatcherServlet",
        "feign.FeignException"})
@ConditionalOnProperty(prefix = "banyan.spring.mvc", name = "enabled", matchIfMissing = true)
public class MvcFeignExceptionAutoConfiguration {

    /**
     * Feign 异常处理。
     *
     * @param resultFactory 错误响应契约
     * @return Feign 异常 advice
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "banyan.spring.mvc.feign-exception", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public BanyanFeignExceptionAdvice banyanFeignExceptionAdvice(ExceptionResultFactory resultFactory) {
        return new BanyanFeignExceptionAdvice(resultFactory);
    }
}
