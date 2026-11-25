package cn.kennylee.banyan.spring.mvc.autoconfigure;

import cn.kennylee.banyan.spring.mvc.error.DefaultExceptionResultFactory;
import cn.kennylee.banyan.spring.mvc.error.ExceptionResultFactory;
import cn.kennylee.banyan.spring.mvc.handler.BanyanFallbackExceptionAdvice;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * 核心装配：错误响应契约与兜底异常处理。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@ConditionalOnProperty(prefix = "banyan.spring.mvc", name = "enabled", matchIfMissing = true)
public class MvcCoreAutoConfiguration {

    /**
     * 默认错误响应契约，使用方可提供自己的实现替换。
     *
     * @return 错误响应契约
     */
    @Bean
    @ConditionalOnMissingBean
    public ExceptionResultFactory exceptionResultFactory() {
        return new DefaultExceptionResultFactory();
    }

    /**
     * 兜底异常处理。
     *
     * @param resultFactory 错误响应契约
     * @return 兜底 advice
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "banyan.spring.mvc.fallback-exception", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public BanyanFallbackExceptionAdvice banyanFallbackExceptionAdvice(
            ExceptionResultFactory resultFactory) {
        return new BanyanFallbackExceptionAdvice(resultFactory);
    }
}
