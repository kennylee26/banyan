package cn.kennylee.banyan.trace.filter;

import cn.kennylee.banyan.trace.context.TraceContext;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器：返回当前线程上下文与 MDC 中的 TraceId。
 *
 * @author kennylee
 * @since 0.1.0
 */
@RestController
public class TraceTestController {

    /** MDC key。 */
    private static final String MDC_KEY = "traceId";

    /**
     * 返回当前 TraceId。
     *
     * @return 上下文与 MDC 中的值
     */
    @GetMapping("/trace")
    public Map<String, String> trace() {
        Map<String, String> result = new HashMap<>(4);
        result.put("context", TraceContext.getTraceId());
        result.put("mdc", MDC.get(MDC_KEY));
        return result;
    }
}
