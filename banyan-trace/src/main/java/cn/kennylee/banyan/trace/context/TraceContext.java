package cn.kennylee.banyan.trace.context;

import cn.kennylee.banyan.commons.id.IdProvider;
import cn.kennylee.banyan.commons.id.UuidIdProvider;
import cn.kennylee.banyan.commons.util.Strings;
import org.slf4j.MDC;

import java.util.Map;

/**
 * Trace 上下文：持有当前线程的 TraceId 与日志 MDC。
 * <p>生成策略默认使用 32 位无横线 UUID，可通过 {@link #setIdProvider(IdProvider)} 替换。
 * {@link #start(String, String)} 返回入口前的 MDC 快照，{@link #stop(Map)} 恢复该快照并移除
 * TraceId，不会清空宿主在 MDC 中放置的其它键。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class TraceContext {

    /** 响应式上下文 key，与上下文传播的 ThreadLocalAccessor 共用。 */
    public static final String CONTEXT_KEY = "banyan.traceId";

    /** 线程上下文中的 TraceId。 */
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    /** ID 生成器。 */
    private static volatile IdProvider idProvider = new UuidIdProvider();

    private TraceContext() {
    }

    /**
     * 替换 ID 生成器。
     *
     * @param provider ID 生成器，为 null 时忽略
     */
    public static void setIdProvider(IdProvider provider) {
        if (provider != null) {
            idProvider = provider;
        }
    }

    /**
     * 生成新的 TraceId。
     *
     * @return 32 位无横线标识
     */
    public static String generateTraceId() {
        return idProvider.nextId();
    }

    /**
     * 获取当前线程的 TraceId。
     *
     * @return TraceId；不存在时返回 null
     */
    public static String getTraceId() {
        return TRACE_ID.get();
    }

    /**
     * 设置当前线程的 TraceId，不写 MDC。
     *
     * @param traceId TraceId，可为 null
     */
    public static void setTraceId(String traceId) {
        if (traceId == null) {
            TRACE_ID.remove();
        } else {
            TRACE_ID.set(traceId);
        }
    }

    /**
     * 进入一次 Trace 上下文：写入线程上下文与 MDC，并返回入口前的 MDC 快照。
     *
     * @param traceId TraceId
     * @param mdcKey  MDC key，空白时不写 MDC
     * @return 入口前的 MDC 快照；原本为空时返回 null
     */
    public static Map<String, String> start(String traceId, String mdcKey) {
        Map<String, String> snapshot = MDC.getCopyOfContextMap();
        setTraceId(traceId);
        if (Strings.isNotBlank(mdcKey) && Strings.isNotBlank(traceId)) {
            MDC.put(mdcKey, traceId);
        }
        return snapshot;
    }

    /**
     * 退出一次 Trace 上下文：移除 TraceId 并恢复入口前的 MDC 快照。
     *
     * @param snapshot {@link #start(String, String)} 返回的快照；为 null 时清空 MDC
     */
    public static void stop(Map<String, String> snapshot) {
        TRACE_ID.remove();
        if (snapshot == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(snapshot);
        }
    }

    /**
     * 仅移除当前线程的 TraceId，不处理 MDC。
     */
    public static void clear() {
        TRACE_ID.remove();
    }
}
