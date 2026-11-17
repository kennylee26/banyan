package cn.kennylee.banyan.trace.filter;

import cn.kennylee.banyan.trace.autoconfigure.TraceProperties;
import cn.kennylee.banyan.trace.context.TraceContext;
import cn.kennylee.banyan.trace.support.TraceExchange;
import cn.kennylee.banyan.trace.support.TraceIdResolver;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFlux 入向过滤器：读取或生成 TraceId，写入请求头、exchange 属性、Reactor 上下文与响应头。
 * <p>响应式链会切换线程，本过滤器不直接写 MDC；需要线程上下文可用时由上下文传播能力负责。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class ReactiveTraceFilter implements WebFilter, Ordered {

    /** 配置。 */
    private final TraceProperties properties;

    /**
     * 构造实例。
     *
     * @param properties 配置
     */
    public ReactiveTraceFilter(TraceProperties properties) {
        this.properties = properties;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String headerValue = exchange.getRequest().getHeaders().getFirst(properties.getHeaderName());
        String traceId = TraceIdResolver.resolveIncoming(headerValue,
                () -> properties.isAcceptQueryTraceId()
                        ? exchange.getRequest().getQueryParams().getFirst(properties.getMdcKey()) : null,
                properties.isAcceptIncoming());

        ServerWebExchange mutated = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(properties.getHeaderName(), traceId)))
                .build();
        TraceExchange.put(mutated, traceId);
        if (properties.getResponseHeader().isEnabled()) {
            mutated.getResponse().getHeaders().set(properties.getHeaderName(), traceId);
        }
        return chain.filter(mutated)
                .contextWrite(context -> context.put(TraceContext.CONTEXT_KEY, traceId));
    }

    @Override
    public int getOrder() {
        return TraceProperties.DEFAULT_FILTER_ORDER;
    }
}
