package cn.kennylee.banyan.trace.dubbo;

import cn.kennylee.banyan.trace.autoconfigure.TraceProperties;
import cn.kennylee.banyan.trace.context.TraceContext;
import cn.kennylee.banyan.trace.support.TraceSettings;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Dubbo 出入口透传测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class DubboTraceFilterTest {

    private Invoker<?> invoker;

    private Invocation invocation;

    private Result result;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        invoker = mock(Invoker.class);
        invocation = mock(Invocation.class);
        result = mock(Result.class);
        when(invoker.invoke(any())).thenReturn(result);
        TraceSettings.init(new TraceProperties());
    }

    @AfterEach
    void cleanUp() {
        TraceContext.clear();
    }

    /** 两个过滤器都通过 @Activate 默认激活。 */
    @Test
    void filtersAreActivated() {
        assertThat(DubboTraceConsumerFilter.class
                .isAnnotationPresent(org.apache.dubbo.common.extension.Activate.class)).isTrue();
        assertThat(DubboTraceProviderFilter.class
                .isAnnotationPresent(org.apache.dubbo.common.extension.Activate.class)).isTrue();
    }

    /** 消费端把当前 TraceId 写入 attachment。 */
    @Test
    void consumerWritesAttachment() {
        TraceContext.setTraceId("d-1");

        new DubboTraceConsumerFilter().invoke(invoker, invocation);

        verify(invocation).setAttachment("X-Trace-Id", "d-1");
    }

    /** 提供端读取 attachment 并写入线程上下文，结束后清理。 */
    @Test
    void providerReadsAttachmentAndRestoresContext() {
        when(invocation.getAttachment("X-Trace-Id")).thenReturn("d-2");
        AtomicReference<String> seen = new AtomicReference<>();
        when(invoker.invoke(any())).thenAnswer(call -> {
            seen.set(TraceContext.getTraceId());
            return result;
        });

        new DubboTraceProviderFilter().invoke(invoker, invocation);

        assertThat(seen).hasValue("d-2");
        assertThat(TraceContext.getTraceId()).isNull();
    }

    /** 提供端在上游无值时补生成。 */
    @Test
    void providerGeneratesWhenMissing() {
        when(invocation.getAttachment("X-Trace-Id")).thenReturn(null);
        AtomicReference<String> seen = new AtomicReference<>();
        when(invoker.invoke(any())).thenAnswer(call -> {
            seen.set(TraceContext.getTraceId());
            return result;
        });

        new DubboTraceProviderFilter().invoke(invoker, invocation);

        assertThat(seen.get()).matches("[0-9a-f]{32}");
    }

    /** 开关关闭时两端完全放行。 */
    @Test
    void disabledSkipsBothSides() {
        TraceProperties properties = new TraceProperties();
        properties.getDubbo().setEnabled(false);
        TraceSettings.init(properties);
        TraceContext.setTraceId("d-3");

        new DubboTraceConsumerFilter().invoke(invoker, invocation);
        new DubboTraceProviderFilter().invoke(invoker, invocation);

        verify(invocation, never()).setAttachment(any(), any());
        assertThat(TraceContext.getTraceId()).isEqualTo("d-3");
    }
}
