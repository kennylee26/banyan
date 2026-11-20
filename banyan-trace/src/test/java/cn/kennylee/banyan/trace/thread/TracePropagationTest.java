package cn.kennylee.banyan.trace.thread;

import cn.kennylee.banyan.trace.autoconfigure.TraceProperties;
import cn.kennylee.banyan.trace.context.TraceContext;
import cn.kennylee.banyan.trace.support.TraceSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异步生命周期工具测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class TracePropagationTest {

    @BeforeEach
    void setUp() {
        TraceProperties properties = new TraceProperties();
        properties.setHeaderName("X-Trace-Id");
        properties.setMdcKey("traceId");
        TraceSettings.init(properties);
    }

    @AfterEach
    void cleanUp() {
        TraceContext.clear();
        MDC.clear();
    }

    /** 读取当前 TraceId。 */
    @Test
    void readsCurrentTraceId() {
        // given
        TraceContext.setTraceId("p-1");

        // when
        assertThat(TracePropagation.resolveCurrentTraceId()).isEqualTo("p-1");
    }

    /** header 名与 HTTP 配置一致。 */
    @Test
    void exposesConfiguredHeaderName() {
        // when
        assertThat(TracePropagation.resolveHeaderName()).isEqualTo("X-Trace-Id");
    }

    /** 包装 Runnable 后子线程可读到 TraceId。 */
    @Test
    void wrapsRunnableWithTraceId() throws Exception {
        // given
        TraceContext.setTraceId("p-2");
        AtomicReference<String> seen = new AtomicReference<>();

        // when
        Thread thread = new Thread(TracePropagation.wrap(() -> seen.set(TraceContext.getTraceId())));
        thread.start();
        thread.join();

        // then
        assertThat(seen).hasValue("p-2");
    }

    /** 包装 Callable 后返回值与环境隔离。 */
    @Test
    void wrapsCallable() throws Exception {
        // given
        TraceContext.setTraceId("p-3");

        // when
        String seen = TracePropagation.wrap(() -> TraceContext.getTraceId()).call();

        // then
        assertThat(seen).isEqualTo("p-3");
        assertThat(TraceContext.getTraceId()).isEqualTo("p-3");
    }
}
