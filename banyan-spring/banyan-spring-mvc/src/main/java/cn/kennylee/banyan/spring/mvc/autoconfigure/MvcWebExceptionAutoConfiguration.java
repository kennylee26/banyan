package cn.kennylee.banyan.spring.mvc.autoconfigure;

import cn.kennylee.banyan.spring.mvc.error.ExceptionResultFactory;
import cn.kennylee.banyan.spring.mvc.handler.BanyanConstraintViolationAdvice;
import cn.kennylee.banyan.spring.mvc.handler.BanyanWebExceptionAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MVC 参数与绑定异常装配；校验异常仅在 classpath 存在 jakarta.validation 时装配。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration(after = MvcCoreAutoConfiguration.class)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnProperty(prefix = "banyan.spring.mvc", name = "enabled", matchIfMissing = true)
public class MvcWebExceptionAutoConfiguration {

    /**
     * MVC 参数与绑定异常处理。
     *
     * @param resultFactory 错误响应契约
     * @return web 异常 advice
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "banyan.spring.mvc.web-exception", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public BanyanWebExceptionAdvice banyanWebExceptionAdvice(ExceptionResultFactory resultFactory) {
        return new BanyanWebExceptionAdvice(resultFactory);
    }

    /**
     * 校验异常装配：只有存在 jakarta.validation 时才注册，避免缺类时加载失败。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "jakarta.validation.ConstraintViolationException")
    static class ValidationConfiguration {

        /**
         * 校验异常处理。
         *
         * @param resultFactory 错误响应契约
         * @return 校验异常 advice
         */
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "banyan.spring.mvc.web-exception", name = "enabled",
                havingValue = "true", matchIfMissing = true)
        BanyanConstraintViolationAdvice banyanConstraintViolationAdvice(
                ExceptionResultFactory resultFactory) {
            return new BanyanConstraintViolationAdvice(resultFactory);
        }
    }
}
