package cn.kennylee.banyan.spring.mvc.autoconfigure;

import cn.kennylee.banyan.spring.mvc.error.DefaultExceptionResultFactory;
import cn.kennylee.banyan.spring.mvc.error.ExceptionResultFactory;
import cn.kennylee.banyan.spring.mvc.error.MvcErrorContext;
import cn.kennylee.banyan.spring.mvc.handler.BanyanConstraintViolationAdvice;
import cn.kennylee.banyan.spring.mvc.handler.BanyanFallbackExceptionAdvice;
import cn.kennylee.banyan.spring.mvc.handler.BanyanFeignExceptionAdvice;
import cn.kennylee.banyan.spring.mvc.handler.BanyanWebExceptionAdvice;
import cn.kennylee.banyan.spring.mvc.jackson.BanyanJacksonCustomizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动装配测试：开关、缺类降级、可替换契约。
 *
 * @author kennylee
 * @since 0.1.0
 */
@ExtendWith(OutputCaptureExtension.class)
class MvcAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MvcCoreAutoConfiguration.class,
                    MvcWebExceptionAutoConfiguration.class,
                    MvcFeignExceptionAutoConfiguration.class,
                    MvcJacksonAutoConfiguration.class,
                    MvcMissingClassAutoConfiguration.class));

    /** 默认装配异常链，Jackson 增强默认关闭。 */
    @Test
    void registersDefaultCapabilities() {
        // when
        runner.run(context -> {
            // then
            assertThat(context).hasSingleBean(ExceptionResultFactory.class);
            assertThat(context.getBean(ExceptionResultFactory.class))
                    .isInstanceOf(DefaultExceptionResultFactory.class);
            assertThat(context).hasSingleBean(BanyanFallbackExceptionAdvice.class);
            assertThat(context).hasSingleBean(BanyanWebExceptionAdvice.class);
            assertThat(context).hasSingleBean(BanyanConstraintViolationAdvice.class);
            assertThat(context).hasSingleBean(BanyanFeignExceptionAdvice.class);
            assertThat(context).doesNotHaveBean(BanyanJacksonCustomizer.class);
        });
    }

    /** 总开关关闭时全部不注册。 */
    @Test
    void skipsAllWhenDisabled() {
        // when
        runner.withPropertyValues("banyan.spring.mvc.enabled=false").run(context -> {
            // then
            assertThat(context).doesNotHaveBean(ExceptionResultFactory.class);
            assertThat(context).doesNotHaveBean(BanyanFallbackExceptionAdvice.class);
            assertThat(context).doesNotHaveBean(BanyanWebExceptionAdvice.class);
            assertThat(context).doesNotHaveBean(BanyanConstraintViolationAdvice.class);
            assertThat(context).doesNotHaveBean(BanyanFeignExceptionAdvice.class);
            assertThat(context).doesNotHaveBean(BanyanJacksonCustomizer.class);
        });
    }

    /** 只关闭兜底处理器。 */
    @Test
    void disablesFallbackIndependently() {
        // when
        runner.withPropertyValues("banyan.spring.mvc.fallback-exception.enabled=false").run(context -> {
            // then
            assertThat(context).doesNotHaveBean(BanyanFallbackExceptionAdvice.class);
            assertThat(context).hasSingleBean(BanyanWebExceptionAdvice.class);
        });
    }

    /** 关闭 web 异常同时关闭校验异常。 */
    @Test
    void disablesWebAndValidationTogether() {
        // when
        runner.withPropertyValues("banyan.spring.mvc.web-exception.enabled=false").run(context -> {
            // then
            assertThat(context).doesNotHaveBean(BanyanWebExceptionAdvice.class);
            assertThat(context).doesNotHaveBean(BanyanConstraintViolationAdvice.class);
        });
    }

    /** 关闭 Feign 异常。 */
    @Test
    void disablesFeignIndependently() {
        // when
        runner.withPropertyValues("banyan.spring.mvc.feign-exception.enabled=false").run(context -> {
            // then
            assertThat(context).doesNotHaveBean(BanyanFeignExceptionAdvice.class);
        });
    }

    /** 开启 Jackson 增强。 */
    @Test
    void registersJacksonCustomizerWhenEnabled() {
        // when
        runner.withPropertyValues("banyan.spring.mvc.jackson.enabled=true").run(context -> {
            // then
            assertThat(context).hasSingleBean(BanyanJacksonCustomizer.class);
        });
    }

    /** 缺少 Feign 时静默跳过。 */
    @Test
    void skipsFeignAdviceWhenFeignMissing() {
        // when
        runner.withClassLoader(new FilteredClassLoader("feign.FeignException")).run(context -> {
            // then
            assertThat(context).doesNotHaveBean(BanyanFeignExceptionAdvice.class);
            assertThat(context).hasSingleBean(BanyanWebExceptionAdvice.class);
        });
    }

    /** 缺少校验 API 时静默跳过校验 advice。 */
    @Test
    void skipsValidationAdviceWhenValidationMissing() {
        // when
        runner.withClassLoader(new FilteredClassLoader("jakarta.validation.ConstraintViolationException"))
                .run(context -> {
                    // then
                    assertThat(context).doesNotHaveBean(BanyanConstraintViolationAdvice.class);
                    assertThat(context).hasSingleBean(BanyanWebExceptionAdvice.class);
                });
    }

    /** 缺少 Jackson 时即使开启也不注册。 */
    @Test
    void skipsJacksonWhenJacksonMissing() {
        // when
        runner.withClassLoader(new FilteredClassLoader("com.fasterxml.jackson.databind.ObjectMapper"))
                .withPropertyValues("banyan.spring.mvc.jackson.enabled=true")
                .run(context -> {
                    // then
                    assertThat(context).doesNotHaveBean(BanyanJacksonCustomizer.class);
                });
    }

    /** 缺少 Spring MVC 核心类时整块跳过并 WARN。 */
    @Test
    void skipsAllWhenSpringMvcMissing(CapturedOutput output) {
        // when
        runner.withClassLoader(new FilteredClassLoader("org.springframework.web.servlet.DispatcherServlet"))
                .run(context -> {
                    // then
                    assertThat(context).doesNotHaveBean(ExceptionResultFactory.class);
                    assertThat(context).doesNotHaveBean(BanyanFallbackExceptionAdvice.class);
                    assertThat(context).doesNotHaveBean(BanyanWebExceptionAdvice.class);
                });

        // then
        assertThat(output).contains("banyan-spring-mvc");
    }

    /** 使用方替换错误响应实现时默认实现让位。 */
    @Test
    void userFactoryReplacesDefault() {
        // when
        runner.withUserConfiguration(UserFactoryConfiguration.class).run(context -> {
            // then
            assertThat(context).hasSingleBean(ExceptionResultFactory.class);
            assertThat(context.getBean(ExceptionResultFactory.class))
                    .isInstanceOf(UserExceptionResultFactory.class);
        });
    }

    /** 使用方 customizer 可与本模块 customizer 共存。 */
    @Test
    void userCustomizerCoexists() {
        // when
        runner.withUserConfiguration(UserCustomizerConfiguration.class)
                .withPropertyValues("banyan.spring.mvc.jackson.enabled=true")
                .run(context -> {
                    // then
                    assertThat(context).hasSingleBean(BanyanJacksonCustomizer.class);
                    assertThat(context.getBeansOfType(Jackson2ObjectMapperBuilderCustomizer.class))
                            .hasSize(2);
                });
    }

    /**
     * 使用方错误响应实现。
     *
     * @author kennylee
     * @since 0.1.0
     */
    static class UserExceptionResultFactory implements ExceptionResultFactory {

        @Override
        public Object create(MvcErrorContext context) {
            return "custom";
        }
    }

    /**
     * 使用方配置：自定义错误响应实现。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class UserFactoryConfiguration {

        /**
         * 注册使用方实现。
         *
         * @return 使用方错误响应实现
         */
        @Bean
        ExceptionResultFactory userExceptionResultFactory() {
            return new UserExceptionResultFactory();
        }
    }

    /**
     * 使用方配置：自定义 Jackson customizer。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class UserCustomizerConfiguration {

        /**
         * 注册使用方 customizer。
         *
         * @return 使用方 customizer
         */
        @Bean
        Jackson2ObjectMapperBuilderCustomizer userCustomizer() {
            return builder -> {
            };
        }
    }
}
