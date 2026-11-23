package cn.kennylee.banyan.spring.feign.autoconfigure;

import cn.kennylee.banyan.spring.feign.interceptor.FeignRequestLogInterceptor;
import cn.kennylee.banyan.spring.feign.interceptor.ForwardHeadersInterceptor;
import cn.kennylee.banyan.spring.feign.logging.LoggingFeignDecoderCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class FeignAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(FeignAutoConfiguration.class));

    /** 默认开启请求/响应日志，header 转发关闭。 */
    @Test
    void registersDefaultCapabilities() {
        // given
        // when
        runner.run(context -> {
            // then
            assertThat(context).hasSingleBean(FeignRequestLogInterceptor.class);
            assertThat(context).hasSingleBean(LoggingFeignDecoderCustomizer.class);
            assertThat(context).doesNotHaveBean(ForwardHeadersInterceptor.class);
        });
    }

    /** 总开关关闭时不注册任何 bean。 */
    @Test
    void skipsAllWhenDisabled() {
        // given
        // when
        runner.withPropertyValues("banyan.spring.feign.enabled=false").run(context -> {
            // then
            assertThat(context).doesNotHaveBean(FeignRequestLogInterceptor.class);
            assertThat(context).doesNotHaveBean(LoggingFeignDecoderCustomizer.class);
            assertThat(context).doesNotHaveBean(ForwardHeadersInterceptor.class);
        });
    }

    /** 显式开启 header 转发。 */
    @Test
    void registersForwardHeadersWhenEnabled() {
        // given
        // when
        runner.withPropertyValues("banyan.spring.feign.forward-headers.enabled=true")
                .run(context -> {
                    // then
                    assertThat(context).hasSingleBean(ForwardHeadersInterceptor.class);
                });
    }

    /** 两个日志开关都能独立关闭。 */
    @Test
    void disablesLoggingTogglesIndependently() {
        // given
        // when
        runner.withPropertyValues(
                "banyan.spring.feign.request-log.enabled=false",
                "banyan.spring.feign.response-log.enabled=false").run(context -> {
            // then
            assertThat(context).doesNotHaveBean(FeignRequestLogInterceptor.class);
            assertThat(context).doesNotHaveBean(LoggingFeignDecoderCustomizer.class);
        });
    }

    /** 缺少 Feign 时整块跳过。 */
    @Test
    void skipsWhenFeignMissing() {
        // given
        // when
        runner.withClassLoader(new FilteredClassLoader("feign.RequestInterceptor"))
                .run(context -> {
                    // then
                    assertThat(context).doesNotHaveBean(FeignRequestLogInterceptor.class);
                    assertThat(context).doesNotHaveBean(LoggingFeignDecoderCustomizer.class);
                });
    }

    /** 用户已有其它 customizer 时，日志 customizer 仍可共存。 */
    @Test
    void coexistsWithUserCustomizer() {
        // given
        // when
        runner.withUserConfiguration(CustomConfiguration.class).run(context -> {
            // then
            assertThat(context).hasSingleBean(LoggingFeignDecoderCustomizer.class);
            assertThat(context.getBeansOfType(FeignBuilderCustomizer.class)).hasSize(2);
        });
    }

    /** 用户配置。 */
    @Configuration(proxyBeanMethods = false)
    static class CustomConfiguration {

        /** 自定义 FeignBuilderCustomizer。 */
        @Bean
        FeignBuilderCustomizer customizer() {
            return builder -> {
            };
        }
    }
}
