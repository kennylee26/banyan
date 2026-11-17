package cn.kennylee.banyan.trace.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trace 上下文测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class TraceContextTest {

    @AfterEach
    void cleanUp() {
        TraceContext.clear();
        MDC.clear();
    }

    /** 生成 32 位无横线且互不相同的标识。 */
    @Test
    void generatesUniqueUuidWithoutHyphen() {
        String first = TraceContext.generateTraceId();
        String second = TraceContext.generateTraceId();

        assertThat(first).hasSize(32).doesNotContain("-");
        assertThat(second).isNotEqualTo(first);
    }

    /** start 写入线程上下文与 MDC，stop 恢复入口前的 MDC 快照。 */
    @Test
    void startAndStopRestoreHostMdc() {
        MDC.put("tenant", "t1");

        Map<String, String> snapshot = TraceContext.start("trace-1", "traceId");

        assertThat(TraceContext.getTraceId()).isEqualTo("trace-1");
        assertThat(MDC.get("traceId")).isEqualTo("trace-1");
        assertThat(MDC.get("tenant")).isEqualTo("t1");

        TraceContext.stop(snapshot);

        assertThat(TraceContext.getTraceId()).isNull();
        assertThat(MDC.get("traceId")).isNull();
        assertThat(MDC.get("tenant")).isEqualTo("t1");
    }

    /** 入口前没有 MDC 时，stop 后 MDC 为空。 */
    @Test
    void stopClearsMdcWhenNoPreviousSnapshot() {
        Map<String, String> snapshot = TraceContext.start("trace-2", "traceId");

        TraceContext.stop(snapshot);

        assertThat(TraceContext.getTraceId()).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    /** 可替换生成策略。 */
    @Test
    void supportsCustomIdProvider() {
        TraceContext.setIdProvider(new cn.kennylee.banyan.commons.id.IdProvider() {
            @Override
            public String nextId() {
                return "custom-id";
            }

            @Override
            public long nextLongId() {
                return 1L;
            }
        });

        assertThat(TraceContext.generateTraceId()).isEqualTo("custom-id");
        TraceContext.setIdProvider(new cn.kennylee.banyan.commons.id.UuidIdProvider());
    }

    /** 直接设置与读取。 */
    @Test
    void setsAndGetsTraceId() {
        TraceContext.setTraceId("trace-3");

        assertThat(TraceContext.getTraceId()).isEqualTo("trace-3");
    }
}
