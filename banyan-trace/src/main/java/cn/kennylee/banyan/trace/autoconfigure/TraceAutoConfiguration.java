package cn.kennylee.banyan.trace.autoconfigure;

import cn.kennylee.banyan.trace.feign.FeignResponseTraceInterceptor;
import cn.kennylee.banyan.trace.feign.TraceIdFeignInterceptor;
import cn.kennylee.banyan.trace.filter.ReactiveTraceFilter;
import cn.kennylee.banyan.trace.filter.ServletTraceFilter;
import cn.kennylee.banyan.trace.thread.TraceExecutorBeanPostProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/**
 * Trace 自动配置：按技术栈注册入向过滤器，缺类时跳过。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "banyan.trace", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TraceProperties.class)
public class TraceAutoConfiguration {

    /**
     * Servlet 入向过滤器。
     *
     * @param properties 配置
     * @return 过滤器
     */
    @Bean
    @ConditionalOnClass(name = "jakarta.servlet.Filter")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "banyan.trace.servlet", name = "enabled", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ServletTraceFilter servletTraceFilter(TraceProperties properties) {
        return new ServletTraceFilter(properties);
    }

    /**
     * WebFlux 入向过滤器。
     *
     * @param properties 配置
     * @return 过滤器
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.web.server.WebFilter")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnProperty(prefix = "banyan.trace.webflux", name = "enabled", matchIfMissing = true)
    @ConditionalOnMissingBean
    public ReactiveTraceFilter reactiveTraceFilter(TraceProperties properties) {
        return new ReactiveTraceFilter(properties);
    }

    /**
     * Feign 出向拦截器；缺少 Feign 时整块跳过。
           *
      * @author kennylee
      * @since 0.1.0
      */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    static class FeignConfiguration {

        /**
         * 注册 Feign 出向拦截器。
         *
         * @param properties 配置
         * @return 拦截器
         */
        @Bean
        @ConditionalOnProperty(prefix = "banyan.trace.feign", name = "enabled", matchIfMissing = true)
        @ConditionalOnMissingBean
        TraceIdFeignInterceptor traceIdFeignInterceptor(TraceProperties properties) {
            return new TraceIdFeignInterceptor(properties);
        }

        /**
         * 注册 Feign 响应侧 TraceId 补全拦截器。
         *
         * @param properties 配置
         * @return 拦截器
         */
        @Bean
        @ConditionalOnProperty(prefix = "banyan.trace.feign", name = "response-enabled",
                matchIfMissing = true)
        @ConditionalOnMissingBean
        FeignResponseTraceInterceptor feignResponseTraceInterceptor(TraceProperties properties) {
            return new FeignResponseTraceInterceptor(properties);
        }
    }

    /**
     * 启动期把配置写入 Dubbo SPI 可读取的静态持有者。
     *
     * @param properties 配置
     * @return 初始化器
     */
    @Bean
    @ConditionalOnMissingBean
    public TraceSettingsInitializer traceConfigHolderInitializer(TraceProperties properties) {
        return new TraceSettingsInitializer(properties);
    }

    /**
     * 线程池兜底装饰。
     *
     * @param properties       配置
     * @param customDecorators 使用方声明的任务装饰器
     * @return 后置处理器
     */
    @Bean
    @ConditionalOnClass(name = "org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor")
    public static TraceExecutorBeanPostProcessor traceExecutorBeanPostProcessor(
            TraceProperties properties, ObjectProvider<TaskDecorator> customDecorators) {
        return new TraceExecutorBeanPostProcessor(properties, customDecorators);
    }
}
