package cn.kennylee.banyan.trace.thread;

import cn.kennylee.banyan.commons.spring.thread.TaskExecutors;
import cn.kennylee.banyan.trace.context.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 带 TraceId 传递的线程池工厂测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class TraceTaskExecutorsTest {

    @AfterEach
    void cleanUp() {
        TraceContext.clear();
    }

    /** 工厂创建的线程池自动带装饰器。 */
    @Test
    void factoryPropagatesTraceId() throws Exception {
        TraceContext.setTraceId("e-1");
        ThreadPoolTaskExecutor executor = TraceTaskExecutors.create("trace-test-");
        try {
            AtomicReference<String> seen = new AtomicReference<>();
            executor.submit(() -> seen.set(TraceContext.getTraceId())).get();
            assertThat(seen).hasValue("e-1");
        } finally {
            executor.shutdown();
        }
    }

    /** 就地包装已有线程池。 */
    @Test
    void decoratesExistingExecutor() throws Exception {
        TraceContext.setTraceId("e-2");
        ThreadPoolTaskExecutor executor = TraceTaskExecutors.decorate(
                TaskExecutors.newPool("trace-test-"));
        try {
            AtomicReference<String> seen = new AtomicReference<>();
            executor.submit(() -> seen.set(TraceContext.getTraceId())).get();
            assertThat(seen).hasValue("e-2");
        } finally {
            executor.shutdown();
        }
    }
}
