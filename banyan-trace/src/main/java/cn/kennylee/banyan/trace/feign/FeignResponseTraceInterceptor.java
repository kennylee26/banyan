package cn.kennylee.banyan.trace.feign;

import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.trace.autoconfigure.TraceProperties;
import cn.kennylee.banyan.trace.context.TraceContext;
import feign.InvocationContext;
import feign.ResponseInterceptor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;

/**
 * Feign 响应拦截器：当前线程没有 TraceId 时，临时使用响应头中的值，并在 finally 恢复。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class FeignResponseTraceInterceptor implements ResponseInterceptor {

    /** 配置。 */
    private final TraceProperties properties;

    /**
     * 构造拦截器。
     *
     * @param properties 配置
     */
    public FeignResponseTraceInterceptor(TraceProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object intercept(InvocationContext context, Chain chain) throws Exception {
        String original = TraceContext.getTraceId();
        Map<String, String> snapshot = null;
        boolean temporary = false;
        if (Strings.isBlank(original)) {
            String header = first(context.response().headers().get(properties.getHeaderName()));
            if (Strings.isNotBlank(header)) {
                snapshot = TraceContext.start(header, properties.getMdcKey());
                temporary = true;
                log.debug("从 Feign 响应头补全 TraceId: {}", header);
            }
        }
        try {
            return chain.next(context);
        } finally {
            if (temporary) {
                TraceContext.stop(snapshot);
            } else {
                TraceContext.setTraceId(original);
            }
        }
    }

    /** 取响应头第一个非空值。 */
    private static String first(Collection<String> values) {
        if (values == null) {
            return null;
        }
        return values.stream().filter(Strings::isNotBlank).findFirst().orElse(null);
    }
}
