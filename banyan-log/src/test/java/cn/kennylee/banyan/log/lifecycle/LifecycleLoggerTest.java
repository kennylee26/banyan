package cn.kennylee.banyan.log.lifecycle;

import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.log.autoconfigure.LogProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 生命周期日志行为测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@ExtendWith(OutputCaptureExtension.class)
class LifecycleLoggerTest {

    private LogProperties properties;

    private LifecycleLogger logger;

    private GenericApplicationContext context;

    /** 准备上下文与监听器。 */
    @BeforeEach
    void setUp() {
        Messages.setLocale(Locale.SIMPLIFIED_CHINESE);
        properties = new LogProperties();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "order");
        environment.setActiveProfiles("dev");
        context = new GenericApplicationContext();
        context.setEnvironment(environment);
        logger = new LifecycleLogger(properties, new JdkEnvironmentInfoProvider(), environment);
    }

    /** 恢复语言。 */
    @AfterEach
    void resetLocale() {
        Messages.setLocale(Locale.getDefault());
    }

    /** 启动完成输出横幅与环境快照。 */
    @Test
    void logsStartupBannerAndSnapshot(CapturedOutput output) {
        // when
        logger.onReady(readyEvent());

        // then
        assertThat(output).contains("order").contains("运行环境").contains("CPU");
    }

    /** 停机日志只输出一次。 */
    @Test
    void logsShutdownOnce(CapturedOutput output) {
        // when
        logger.onClosed(closedEvent());
        logger.onClosed(closedEvent());

        // then
        assertThat(countOccurrences(output.getOut(), "优雅停机")).isEqualTo(1);
    }

    /** 启动失败输出 ERROR 与异常。 */
    @Test
    void logsFailure(CapturedOutput output) {
        // given
        ApplicationFailedEvent event = mock(ApplicationFailedEvent.class);
        when(event.getApplicationContext()).thenReturn(context);
        when(event.getException()).thenReturn(new IllegalStateException("boom"));

        // when
        logger.onFailure(event);

        // then
        assertThat(output).contains("启动失败").contains("boom");
    }

    /** 关闭环境快照后只剩横幅。 */
    @Test
    void skipsSnapshotWhenEnvironmentDisabled(CapturedOutput output) {
        // given
        properties.getLifecycle().setEnvironmentEnabled(false);

        // when
        logger.onReady(readyEvent());

        // then
        assertThat(output).contains("order").doesNotContain("运行环境");
    }

    /** 自定义横幅覆盖消息资源。 */
    @Test
    void usesCustomBannerWhenConfigured(CapturedOutput output) {
        // given
        properties.getLifecycle().setBanner("custom {0} at {1}");

        // when
        logger.onReady(readyEvent());

        // then
        assertThat(output).contains("custom order at dev");
    }

    /** 字节按 1024 进制换算为 MB。 */
    @Test
    void convertsBytesToMegabytes() {
        // when
        assertThat(logger.toMegabytes(2L * 1024 * 1024)).isEqualTo(2L);
    }

    /** 自定义横幅中的单引号需双写才能保留为字面量。 */
    @Test
    void doublesSingleQuoteInCustomBanner(CapturedOutput output) {
        // given
        properties.getLifecycle().setBanner("It''s {0}");

        // when
        logger.onReady(readyEvent());

        // then
        assertThat(output).contains("It's order");
    }

    /** 构造启动完成事件。 */
    private ApplicationReadyEvent readyEvent() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        when(event.getApplicationContext()).thenReturn(context);
        return event;
    }

    /** 构造上下文关闭事件。 */
    private ContextClosedEvent closedEvent() {
        ContextClosedEvent event = mock(ContextClosedEvent.class);
        when(event.getApplicationContext()).thenReturn(context);
        return event;
    }

    /** 统计子串出现次数。 */
    private long countOccurrences(String text, String token) {
        return text.lines()
                .filter(line -> line.contains(token))
                .count();
    }
}
