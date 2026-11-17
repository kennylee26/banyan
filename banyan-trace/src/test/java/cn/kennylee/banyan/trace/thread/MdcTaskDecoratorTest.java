package cn.kennylee.banyan.trace.thread;

import cn.kennylee.banyan.trace.context.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异步任务装饰器测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void cleanUp() {
        TraceContext.clear();
        MDC.clear();
    }

    /** 子线程同时拿到 MDC 与 TraceId。 */
    @Test
    void propagatesMdcAndTraceId() throws Exception {
        TraceContext.setTraceId("t-1");
        MDC.put("traceId", "t-1");
        MDC.put("tenant", "h-1");

        AtomicReference<String> context = new AtomicReference<>();
        AtomicReference<String> mdcTrace = new AtomicReference<>();
        AtomicReference<String> mdcTenant = new AtomicReference<>();
        Runnable wrapped = decorator.decorate(() -> {
            context.set(TraceContext.getTraceId());
            mdcTrace.set(MDC.get("traceId"));
            mdcTenant.set(MDC.get("tenant"));
        });

        Thread thread = new Thread(wrapped);
        thread.start();
        thread.join();

        assertThat(context).hasValue("t-1");
        assertThat(mdcTrace).hasValue("t-1");
        assertThat(mdcTenant).hasValue("h-1");
    }

    /** 执行结束后恢复子线程原有上下文，不残留父任务的值。 */
    @Test
    void restoresPreviousChildContext() {
        TraceContext.setTraceId("parent");
        MDC.put("traceId", "parent");
        Runnable wrapped = decorator.decorate(() -> {
        });

        TraceContext.setTraceId("child-before");
        MDC.put("traceId", "child-before");
        wrapped.run();

        assertThat(TraceContext.getTraceId()).isEqualTo("child-before");
        assertThat(MDC.get("traceId")).isEqualTo("child-before");
    }

    /** 上下文为空时仍然包装并执行。 */
    @Test
    void wrapsEvenWhenContextEmpty() {
        AtomicBoolean ran = new AtomicBoolean();
        Runnable wrapped = decorator.decorate(() -> ran.set(true));

        wrapped.run();

        assertThat(ran).isTrue();
        assertThat(TraceContext.getTraceId()).isNull();
    }

    /** 不污染父线程的宿主 MDC。 */
    @Test
    void keepsParentHostMdc() {
        MDC.put("tenant", "h-1");
        Runnable wrapped = decorator.decorate(() -> {
        });

        wrapped.run();

        assertThat(MDC.get("tenant")).isEqualTo("h-1");
    }
}
