package cn.kennylee.banyan.spring.feign.autoconfigure;

import cn.kennylee.banyan.spring.feign.interceptor.FeignRequestLogInterceptor;
import cn.kennylee.banyan.spring.feign.interceptor.ForwardHeadersInterceptor;
import cn.kennylee.banyan.spring.feign.logging.LoggingFeignDecoderCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Feign 能力自动装配：日志默认开启，header 转发默认关闭。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(name = "feign.RequestInterceptor")
@ConditionalOnProperty(prefix = "banyan.spring.feign", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(FeignProperties.class)
public class FeignAutoConfiguration {

    /**
     * 请求日志拦截器。
     *
     * @param properties 配置
     * @return 拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "banyan.spring.feign.request-log", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public FeignRequestLogInterceptor feignRequestLogInterceptor(FeignProperties properties) {
        return new FeignRequestLogInterceptor(properties.getLogging());
    }

    /**
     * 响应日志解码器包装器。
     *
     * @param properties 配置
     * @return FeignBuilderCustomizer
     */
    @Bean
    @ConditionalOnMissingBean(LoggingFeignDecoderCustomizer.class)
    @ConditionalOnProperty(prefix = "banyan.spring.feign.response-log", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public LoggingFeignDecoderCustomizer loggingFeignDecoderCustomizer(
            FeignProperties properties) {
        return new LoggingFeignDecoderCustomizer(properties.getLogging());
    }

    /**
     * 入站 header 转发拦截器，默认关闭。
     *
     * @param properties 配置
     * @return 拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "banyan.spring.feign.forward-headers", name = "enabled",
            havingValue = "true")
    public ForwardHeadersInterceptor forwardHeadersInterceptor(FeignProperties properties) {
        return new ForwardHeadersInterceptor(properties);
    }
}
