package cn.kennylee.banyan.trace.filter;

import cn.kennylee.banyan.trace.context.TraceContext;
import cn.kennylee.banyan.trace.support.TraceExchange;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * WebFlux 测试控制器：返回线程上下文、exchange 属性与 Reactor 上下文中的 TraceId。
 *
 * @author kennylee
 * @since 0.1.0
 */
@RestController
public class ReactiveTraceTestController {

    /**
     * 返回各来源的 TraceId。
     *
     * @param exchange 请求交换对象
     * @return 各来源的 TraceId
     */
    @GetMapping("/reactive-trace")
    public Mono<Map<String, String>> trace(ServerWebExchange exchange) {
        return Mono.deferContextual(context -> {
            Map<String, String> result = new HashMap<>(4);
            result.put("context", String.valueOf(TraceContext.getTraceId()));
            result.put("attribute", String.valueOf(exchange.getAttributes().get(TraceExchange.ATTRIBUTE_KEY)));
            result.put("reactor", String.valueOf(context.getOrDefault(TraceContext.CONTEXT_KEY, "none")));
            return Mono.just(result);
        });
    }
}
