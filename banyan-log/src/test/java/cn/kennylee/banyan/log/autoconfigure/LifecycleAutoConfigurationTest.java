package cn.kennylee.banyan.log.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.log.lifecycle.EnvironmentInfoProvider;
import cn.kennylee.banyan.log.lifecycle.JdkEnvironmentInfoProvider;
import cn.kennylee.banyan.log.lifecycle.LifecycleLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 日志自动装配测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@ExtendWith(OutputCaptureExtension.class)
class LifecycleAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    LogAutoConfiguration.class,
                    LogMissingClassAutoConfiguration.class));

    /** 恢复默认语言。 */
    @AfterEach
    void resetLocale() {
        Messages.setLocale(Locale.getDefault());
    }

    /** 默认装配。 */
    @Test
    void loadsByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(LogProperties.class);
            assertThat(context).hasSingleBean(EnvironmentInfoProvider.class);
            assertThat(context).hasSingleBean(LifecycleLogger.class);
        });
    }

    /** 关闭总开关。 */
    @Test
    void skipsWhenDisabled() {
        runner.withPropertyValues("banyan.log.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(LifecycleLogger.class);
                    assertThat(context).doesNotHaveBean(EnvironmentInfoProvider.class);
                });
    }

    /** 关闭生命周期子开关。 */
    @Test
    void skipsLifecycleWhenDisabled() {
        runner.withPropertyValues("banyan.log.lifecycle.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(LifecycleLogger.class));
    }

    /** 自定义 Provider 让位。 */
    @Test
    void backsOffWhenCustomProviderExists() {
        EnvironmentInfoProvider custom = new JdkEnvironmentInfoProvider();

        runner.withBean(EnvironmentInfoProvider.class, () -> custom)
                .run(context -> {
                    assertThat(context).hasSingleBean(EnvironmentInfoProvider.class);
                    assertThat(context.getBean(EnvironmentInfoProvider.class)).isSameAs(custom);
                });
    }

    /** 缺 Spring Boot 配置属性类时只告警不崩。 */
    @Test
    void warnsWhenSpringBootConfigClassMissing(CapturedOutput output) {
        Messages.setLocale(Locale.SIMPLIFIED_CHINESE);
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(
                        "org.springframework.boot.context.properties.ConfigurationProperties"))
                .withConfiguration(AutoConfigurations.of(LogMissingClassAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(LifecycleLogger.class);
                });

        assertThat(output).contains(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED,
                "Spring Boot 配置属性", "banyan-log"));
    }

    /** 缺 Spring 事件类时提示使用 Spring 事件能力不可用。 */
    @Test
    void warnsWhenSpringEventClassMissing(CapturedOutput output) {
        Messages.setLocale(Locale.SIMPLIFIED_CHINESE);

        new LogMissingClassAutoConfiguration.SpringEventMissing();

        assertThat(output).contains(Messages.get(ErrorCode.AUTO_CONFIG_SKIPPED,
                "Spring 事件", "banyan-log"));
    }
}
