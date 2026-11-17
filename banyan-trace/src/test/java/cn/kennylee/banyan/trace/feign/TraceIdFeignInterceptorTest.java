package cn.kennylee.banyan.trace.feign;

import cn.kennylee.banyan.trace.autoconfigure.TraceProperties;
import cn.kennylee.banyan.trace.context.TraceContext;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feign 出向透传测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class TraceIdFeignInterceptorTest {

    private final TraceIdFeignInterceptor interceptor =
            new TraceIdFeignInterceptor(new TraceProperties());

    @AfterEach
    void cleanUp() {
        TraceContext.clear();
        MDC.clear();
    }

    /** 当前上下文有值时写入请求头。 */
    @Test
    void addsHeaderFromContext() {
        TraceContext.setTraceId("t-1");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("X-Trace-Id")).containsExactly("t-1");
    }

    /** 线程上下文为空时回退到 MDC。 */
    @Test
    void fallsBackToMdc() {
        MDC.put("traceId", "t-2");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers().get("X-Trace-Id")).containsExactly("t-2");
    }

    /** 已存在的头不覆盖。 */
    @Test
    void keepsExistingHeader() {
        TraceContext.setTraceId("t-3");
        RequestTemplate template = new RequestTemplate();
        template.header("X-Trace-Id", "upstream");

        interceptor.apply(template);

        assertThat(template.headers().get("X-Trace-Id")).containsExactly("upstream");
    }

    /** 已有 header 大小写不同也不覆盖。 */
    @Test
    void keepsExistingHeaderIgnoringCase() {
        TraceContext.setTraceId("t-4");
        RequestTemplate template = new RequestTemplate();
        template.header("x-trace-id", "upstream");

        interceptor.apply(template);

        assertThat(template.headers()).hasSize(1);
        assertThat(template.headers().get("x-trace-id")).containsExactly("upstream");
    }

    /** 没有 TraceId 时不写头。 */
    @Test
    void skipsWhenNoTraceId() {
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("X-Trace-Id");
    }
}
