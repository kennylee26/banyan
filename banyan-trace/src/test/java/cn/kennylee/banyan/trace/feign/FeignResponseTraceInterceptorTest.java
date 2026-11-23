package cn.kennylee.banyan.trace.feign;

import cn.kennylee.banyan.trace.autoconfigure.TraceProperties;
import cn.kennylee.banyan.trace.context.TraceContext;
import feign.InvocationContext;
import feign.InvocationContexts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Feign 响应侧 TraceId 补全测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class FeignResponseTraceInterceptorTest {

    @AfterEach
    void clear() {
        TraceContext.clear();
        MDC.clear();
    }

    /** 当前线程无 traceId 时临时使用响应头的值，finally 后恢复。 */
    @Test
    void setsAndRestoresTraceIdFromResponseHeader() throws Exception {
        // given
        TraceProperties properties = new TraceProperties();
        FeignResponseTraceInterceptor interceptor =
                new FeignResponseTraceInterceptor(properties);
        InvocationContext context = InvocationContexts.of(
                Map.of(properties.getHeaderName(),
                        Collections.singletonList("trace-from-provider")));

        // when
        Object result = interceptor.intercept(context, chain -> "ok");

        // then
        assertEquals("ok", result);
        assertNull(TraceContext.getTraceId());
        assertNull(MDC.get(properties.getMdcKey()));
    }

    /** 当前已有 traceId 时保持原值。 */
    @Test
    void keepsExistingTraceId() throws Exception {
        // given
        TraceContext.setTraceId("local");
        TraceProperties properties = new TraceProperties();
        FeignResponseTraceInterceptor interceptor =
                new FeignResponseTraceInterceptor(properties);
        InvocationContext context = InvocationContexts.of(
                Map.of(properties.getHeaderName(), Collections.singletonList("provider")));

        // when
        interceptor.intercept(context, chain -> "ok");

        // then
        assertEquals("local", TraceContext.getTraceId());
    }

}
