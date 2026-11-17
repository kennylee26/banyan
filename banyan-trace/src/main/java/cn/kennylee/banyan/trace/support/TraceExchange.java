package cn.kennylee.banyan.trace.support;

import cn.kennylee.banyan.commons.util.Strings;
import org.springframework.web.server.ServerWebExchange;

/**
 * 响应式请求属性读写：把 TraceId 存进 exchange attributes，供同一请求内的 Filter / 控制器读取。
 * <p>响应式链会切换线程，请求对象比线程上下文更可靠。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class TraceExchange {

    /** exchange 属性 key。 */
    public static final String ATTRIBUTE_KEY = TraceExchange.class.getName() + ".traceId";

    private TraceExchange() {
    }

    /**
     * 写入 TraceId。
     *
     * @param exchange 请求交换对象
     * @param traceId  TraceId
     */
    public static void put(ServerWebExchange exchange, String traceId) {
        exchange.getAttributes().put(ATTRIBUTE_KEY, traceId);
    }

    /**
     * 读取 TraceId：优先 exchange 属性，其次请求头。
     *
     * @param exchange   请求交换对象
     * @param headerName 请求头名
     * @return TraceId；不存在时返回 null
     */
    public static String get(ServerWebExchange exchange, String headerName) {
        Object attribute = exchange.getAttributes().get(ATTRIBUTE_KEY);
        if (attribute instanceof String traceId && Strings.isNotBlank(traceId)) {
            return traceId;
        }
        return exchange.getRequest().getHeaders().getFirst(headerName);
    }
}
