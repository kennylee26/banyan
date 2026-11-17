package cn.kennylee.banyan.trace.thread;

import cn.kennylee.banyan.trace.context.TraceContext;
import org.slf4j.MDC;

import java.util.Map;

/**
 * 线程上下文快照：捕获与还原 MDC 和 TraceId，供装饰器与生命周期工具共用。
 *
 * @author kennylee
 * @since 0.1.0
 */
final class TraceContextSnapshot {

    /** MDC 快照。 */
    private final Map<String, String> mdc;

    /** TraceId。 */
    private final String traceId;

    private TraceContextSnapshot(Map<String, String> mdc, String traceId) {
        this.mdc = mdc;
        this.traceId = traceId;
    }

    /**
     * 捕获当前线程的上下文。
     *
     * @return 快照
     */
    static TraceContextSnapshot capture() {
        return new TraceContextSnapshot(MDC.getCopyOfContextMap(), TraceContext.getTraceId());
    }

    /**
     * 把快照还原到当前线程。
     */
    void restore() {
        if (mdc == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(mdc);
        }
        TraceContext.setTraceId(traceId);
    }
}
