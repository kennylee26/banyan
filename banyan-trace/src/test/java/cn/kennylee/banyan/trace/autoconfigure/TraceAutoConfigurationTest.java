package cn.kennylee.banyan.trace.autoconfigure;

import cn.kennylee.banyan.trace.feign.TraceIdFeignInterceptor;
import cn.kennylee.banyan.trace.filter.ReactiveTraceFilter;
import cn.kennylee.banyan.trace.filter.ServletTraceFilter;
import cn.kennylee.banyan.trace.thread.TraceExecutorBeanPostProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trace 自动装配测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@ExtendWith(OutputCaptureExtension.class)
class TraceAutoConfigurationTest {

    private final WebApplicationContextRunner servletRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    TraceAutoConfiguration.class,
                    TraceContextPropagationAutoConfiguration.class,
                    TraceMissingClassAutoConfiguration.class));

    /** 默认 Servlet 装配。 */
    @Test
    void loadsServletByDefault() {
        // when
        servletRunner.run(context -> {
            assertThat(context).hasSingleBean(TraceProperties.class);
            assertThat(context).hasSingleBean(ServletTraceFilter.class);
            assertThat(context).hasSingleBean(TraceSettingsInitializer.class);
            assertThat(context).hasSingleBean(TraceExecutorBeanPostProcessor.class);
            assertThat(context).doesNotHaveBean(ReactiveTraceFilter.class);
        });
    }

    /** 关闭总开关。 */
    @Test
    void skipsEverythingWhenDisabled() {
        // when
        servletRunner.withPropertyValues("banyan.trace.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ServletTraceFilter.class);
                    assertThat(context).doesNotHaveBean(TraceProperties.class);
                });
    }

    /** 关闭 Servlet 子开关。 */
    @Test
    void skipsServletWhenDisabled() {
        // when
        servletRunner.withPropertyValues("banyan.trace.servlet.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ServletTraceFilter.class);
                    assertThat(context).hasSingleBean(TraceProperties.class);
                });
    }

    /** 响应式装配。 */
    @Test
    void loadsReactiveWhenReactiveApplication() {
        // when
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TraceAutoConfiguration.class,
                        TraceContextPropagationAutoConfiguration.class,
                        TraceMissingClassAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(ReactiveTraceFilter.class);
                    assertThat(context).doesNotHaveBean(ServletTraceFilter.class);
                });
    }

    /** 关闭 WebFlux 子开关。 */
    @Test
    void skipsReactiveWhenDisabled() {
        // when
        new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TraceAutoConfiguration.class,
                        TraceMissingClassAutoConfiguration.class))
                .withPropertyValues("banyan.trace.webflux.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ReactiveTraceFilter.class));
    }

    /** 缺少 Feign 时跳过出向拦截器。 */
    @Test
    void skipsFeignWhenClassMissing() {
        // when
        servletRunner.withClassLoader(new FilteredClassLoader("feign.RequestInterceptor"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(TraceIdFeignInterceptor.class);
                });
    }

    /** 使用方已有自定义过滤器时让位。 */
    @Test
    void backsOffWhenCustomFilterExists() {
        // when
        servletRunner.withBean(ServletTraceFilter.class,
                        () -> new ServletTraceFilter(new TraceProperties()))
                .run(context -> assertThat(context.getBeansOfType(ServletTraceFilter.class)).hasSize(1));
    }

    /** 缺少 Spring Boot 配置属性能力时只告警。 */
    @Test
    void warnsWhenSpringBootConfigClassMissing(CapturedOutput output) {
        // when
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(
                        "org.springframework.boot.context.properties.ConfigurationProperties"))
                .withConfiguration(AutoConfigurations.of(TraceMissingClassAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed());

        // then
        assertThat(output).contains("banyan-trace");
    }

    /** 默认开启响应式上下文传播。 */
    @Test
    void enablesContextPropagationByDefault() {
        // when
        servletRunner.run(context -> {
            assertThat(context.getBeansOfType(
                    TraceContextPropagationAutoConfiguration.WebfluxEnabled.class)).hasSize(1);
            assertThat(context.getBeansOfType(
                    TraceContextPropagationAutoConfiguration.WebfluxEnabled.Enabled.class)).hasSize(1);
        });
    }

    /** 关闭传播开关。 */
    @Test
    void skipsContextPropagationWhenSwitchDisabled() {
        // when
        servletRunner.withPropertyValues("banyan.trace.webflux.context-propagation.enabled=false")
                .run(context -> assertThat(context.getBeansOfType(
                        TraceContextPropagationAutoConfiguration.WebfluxEnabled.Enabled.class)).isEmpty());
    }

    /** 关闭 WebFlux 时传播也关闭。 */
    @Test
    void skipsContextPropagationWhenWebfluxDisabled() {
        // when
        servletRunner.withPropertyValues("banyan.trace.webflux.enabled=false")
                .run(context -> assertThat(context.getBeansOfType(
                        TraceContextPropagationAutoConfiguration.WebfluxEnabled.class)).isEmpty());
    }

    /** 缺少 context-propagation 类时整块跳过。 */
    @Test
    void skipsContextPropagationWhenClassMissing() {
        // when
        servletRunner.withClassLoader(new FilteredClassLoader("io.micrometer.context.ContextRegistry"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeansOfType(
                            TraceContextPropagationAutoConfiguration.WebfluxEnabled.class)).isEmpty();
                });
    }

    /** 缺少 Servlet 能力时告警。 */
    @Test
    void warnsWhenServletMissing(CapturedOutput output) {
        // when
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader("jakarta.servlet.Filter"))
                .withConfiguration(AutoConfigurations.of(TraceMissingClassAutoConfiguration.class))
                .run(context -> assertThat(context).hasNotFailed());

        // then
        assertThat(output).contains("Servlet");
    }

    /** 缺少 Dubbo 能力时告警。 */
    @Test
    void warnsWhenDubboMissing(CapturedOutput output) {
        // when
        servletRunner.withClassLoader(new FilteredClassLoader("org.apache.dubbo.rpc.Filter"))
                .run(context -> assertThat(context).hasNotFailed());

        // then
        assertThat(output).contains("Dubbo");
    }
}
