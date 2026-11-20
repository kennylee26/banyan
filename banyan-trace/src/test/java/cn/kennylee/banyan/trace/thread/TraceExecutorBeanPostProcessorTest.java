package cn.kennylee.banyan.trace.thread;

import cn.kennylee.banyan.commons.spring.thread.TaskExecutors;
import cn.kennylee.banyan.trace.autoconfigure.TraceProperties;
import cn.kennylee.banyan.trace.context.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 线程池兜底装饰测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class TraceExecutorBeanPostProcessorTest {

    @AfterEach
    void cleanUp() {
        TraceContext.clear();
    }

    /** 自建线程池被补设装饰器。 */
    @Test
    void decoratesExecutorWithoutCustomDecorator() throws Exception {
        // given
        TraceProperties properties = new TraceProperties();
        @SuppressWarnings("unchecked")
        ObjectProvider<TaskDecorator> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        TraceExecutorBeanPostProcessor processor =
                new TraceExecutorBeanPostProcessor(properties, provider);
        ThreadPoolTaskExecutor executor = TaskExecutors.newPool("bpp-");
        try {
            processor.postProcessAfterInitialization(executor, "executor");
            TraceContext.setTraceId("b-1");
            AtomicReference<String> seen = new AtomicReference<>();

            // when
            executor.submit(() -> seen.set(TraceContext.getTraceId())).get();

            // then
            assertThat(seen).hasValue("b-1");
        } finally {
            executor.shutdown();
        }
    }

    /** 使用方已有装饰器 bean 时整体跳过。 */
    @Test
    void skipsWhenCustomDecoratorExists() throws Exception {
        // given
        TraceProperties properties = new TraceProperties();
        @SuppressWarnings("unchecked")
        ObjectProvider<TaskDecorator> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(runnable -> runnable);
        TraceExecutorBeanPostProcessor processor =
                new TraceExecutorBeanPostProcessor(properties, provider);
        ThreadPoolTaskExecutor executor = TaskExecutors.newPool("bpp-");
        try {
            processor.postProcessAfterInitialization(executor, "executor");
            TraceContext.setTraceId("b-2");
            AtomicReference<String> seen = new AtomicReference<>();

            // when
            executor.submit(() -> seen.set(TraceContext.getTraceId())).get();

            // then
            assertThat(seen.get()).isNull();
        } finally {
            executor.shutdown();
        }
    }

    /** 兜底开关关闭时不修改线程池。 */
    @Test
    void skipsWhenDisabled() throws Exception {
        // given
        TraceProperties properties = new TraceProperties();
        properties.getAsync().setDecorateExecutors(false);
        @SuppressWarnings("unchecked")
        ObjectProvider<TaskDecorator> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        TraceExecutorBeanPostProcessor processor =
                new TraceExecutorBeanPostProcessor(properties, provider);
        ThreadPoolTaskExecutor executor = TaskExecutors.newPool("bpp-");
        try {
            processor.postProcessAfterInitialization(executor, "executor");
            TraceContext.setTraceId("b-3");
            AtomicReference<String> seen = new AtomicReference<>();

            // when
            executor.submit(() -> seen.set(TraceContext.getTraceId())).get();

            // then
            assertThat(seen.get()).isNull();
        } finally {
            executor.shutdown();
        }
    }
}
