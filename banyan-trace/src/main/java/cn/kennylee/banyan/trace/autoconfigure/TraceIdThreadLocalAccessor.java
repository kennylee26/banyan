package cn.kennylee.banyan.trace.autoconfigure;

import cn.kennylee.banyan.trace.context.TraceContext;
import io.micrometer.context.ThreadLocalAccessor;

/**
 * 把 TraceId 线程上下文接入 Reactor 自动上下文传播。
 *
 * @author kennylee
 * @since 0.1.0
 */
class TraceIdThreadLocalAccessor implements ThreadLocalAccessor<String> {

    @Override
    public Object key() {
        return TraceContext.CONTEXT_KEY;
    }

    @Override
    public String getValue() {
        return TraceContext.getTraceId();
    }

    @Override
    public void setValue(String value) {
        TraceContext.setTraceId(value);
    }

    @Override
    public void setValue() {
        TraceContext.clear();
    }
}
