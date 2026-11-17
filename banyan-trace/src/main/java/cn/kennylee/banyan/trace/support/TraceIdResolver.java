package cn.kennylee.banyan.trace.support;

import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.trace.context.TraceContext;
import org.slf4j.MDC;

import java.util.function.Supplier;

/**
 * TraceId 解析与入向取值。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class TraceIdResolver {

    private TraceIdResolver() {
    }

    /**
     * 按「线程上下文 → MDC」顺序取当前 TraceId。
     *
     * @param mdcKey MDC key，可为空
     * @return TraceId；都没有时返回 null
     */
    public static String resolve(String mdcKey) {
        String traceId = TraceContext.getTraceId();
        if (Strings.isNotBlank(traceId)) {
            return traceId;
        }
        return Strings.isBlank(mdcKey) ? null : MDC.get(mdcKey);
    }

    /**
     * 计算本次请求使用的 TraceId，不使用 query 兜底。
     *
     * @param headerValue    请求头中的值
     * @param acceptIncoming 是否沿用上游
     * @return TraceId
     */
    public static String resolveIncoming(String headerValue, boolean acceptIncoming) {
        return resolveIncoming(headerValue, (String) null, acceptIncoming);
    }

    /**
     * 计算本次请求使用的 TraceId，query 值按需惰性读取。
     *
     * @param headerValue    请求头中的值
     * @param querySupplier  query 取值器，可为 null
     * @param acceptIncoming 是否沿用上游
     * @return TraceId
     */
    public static String resolveIncoming(String headerValue, Supplier<String> querySupplier,
                                         boolean acceptIncoming) {
        return resolveIncoming(headerValue, querySupplier == null ? null : querySupplier.get(), acceptIncoming);
    }

    /**
     * 计算本次请求使用的 TraceId：允许沿用且上游有值则沿用，否则新建。
     *
     * @param headerValue    请求头中的值
     * @param queryValue     query 参数中的值，可为 null
     * @param acceptIncoming 是否沿用上游
     * @return TraceId
     */
    public static String resolveIncoming(String headerValue, String queryValue, boolean acceptIncoming) {
        if (acceptIncoming && Strings.isNotBlank(headerValue)) {
            return headerValue;
        }
        if (acceptIncoming && Strings.isNotBlank(queryValue)) {
            return queryValue;
        }
        return TraceContext.generateTraceId();
    }
}
