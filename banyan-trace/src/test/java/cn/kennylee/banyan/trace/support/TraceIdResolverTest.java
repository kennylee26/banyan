package cn.kennylee.banyan.trace.support;

import cn.kennylee.banyan.trace.context.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TraceId 解析测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class TraceIdResolverTest {

    @AfterEach
    void cleanUp() {
        TraceContext.clear();
        MDC.clear();
    }

    /** 先取线程上下文，再取 MDC。 */
    @Test
    void resolvesFromContextThenMdc() {
        // given
        MDC.put("traceId", "m-1");

        // when
        assertThat(TraceIdResolver.resolve("traceId")).isEqualTo("m-1");

        TraceContext.setTraceId("c-1");
        assertThat(TraceIdResolver.resolve("traceId")).isEqualTo("c-1");
    }

    /** 沿用上游 header。 */
    @Test
    void acceptsIncomingHeader() {
        // when
        assertThat(TraceIdResolver.resolveIncoming("up-1", (String) null, true)).isEqualTo("up-1");
    }

    /** 不接受上游时重新生成。 */
    @Test
    void regeneratesWhenIncomingRejected() {
        // when
        assertThat(TraceIdResolver.resolveIncoming("up-1", "q-1", false))
                .matches("[0-9a-f]{32}");
    }

    /** header 缺失时使用 query 兜底。 */
    @Test
    void acceptsQueryWhenHeaderMissing() {
        // when
        assertThat(TraceIdResolver.resolveIncoming(null, "q-1", true)).isEqualTo("q-1");
    }

    /** header 优先于 query。 */
    @Test
    void headerWinsOverQuery() {
        // when
        assertThat(TraceIdResolver.resolveIncoming("up-1", "q-1", true)).isEqualTo("up-1");
    }
}
