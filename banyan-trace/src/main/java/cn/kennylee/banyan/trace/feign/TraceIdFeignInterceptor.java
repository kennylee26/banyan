package cn.kennylee.banyan.trace.feign;

import cn.kennylee.banyan.commons.util.Strings;
import cn.kennylee.banyan.trace.autoconfigure.TraceProperties;
import cn.kennylee.banyan.trace.support.TraceIdResolver;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.lang.NonNull;

/**
 * Feign 出向拦截器：把当前 TraceId 写入请求头，已有则跳过。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class TraceIdFeignInterceptor implements RequestInterceptor {

    /** 配置。 */
    private final TraceProperties properties;

    /**
     * 构造实例。
     *
     * @param properties 配置
     */
    public TraceIdFeignInterceptor(TraceProperties properties) {
        this.properties = properties;
    }

    @Override
    public void apply(@NonNull RequestTemplate template) {
        String headerName = properties.getHeaderName();
        boolean alreadyPresent = template.headers().keySet().stream()
                .anyMatch(existing -> existing.equalsIgnoreCase(headerName));
        if (alreadyPresent) {
            return;
        }
        String traceId = TraceIdResolver.resolve(properties.getMdcKey());
        if (Strings.isNotBlank(traceId)) {
            template.header(headerName, traceId);
        }
    }
}
