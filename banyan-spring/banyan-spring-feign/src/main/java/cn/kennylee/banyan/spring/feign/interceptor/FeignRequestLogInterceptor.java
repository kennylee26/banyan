package cn.kennylee.banyan.spring.feign.interceptor;

import cn.kennylee.banyan.spring.feign.autoconfigure.FeignProperties;
import cn.kennylee.banyan.spring.feign.logging.FeignLogSupport;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Feign 请求日志拦截器：DEBUG 打印 method、URL、header 与 body。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class FeignRequestLogInterceptor implements RequestInterceptor, Ordered {

    /** 执行顺序。 */
    public static final int ORDER = 0;

    /** 日志配置。 */
    private final FeignProperties.Logging logging;

    /**
     * 构造拦截器。
     *
     * @param logging 日志配置
     */
    public FeignRequestLogInterceptor(FeignProperties.Logging logging) {
        this.logging = logging;
    }

    @Override
    public void apply(@NonNull RequestTemplate template) {
        if (!log.isDebugEnabled()) {
            return;
        }
        Map<String, String> headers = new LinkedHashMap<>();
        template.headers().forEach((name, values) ->
                headers.put(name, first(values)));
        log.debug("Feign 请求: method={}, url={}, headers={}, body={}",
                template.method(), template.url(),
                FeignLogSupport.headers(headers, logging),
                FeignLogSupport.body(FeignLogSupport.utf8(template.body()), logging));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    /** 取多值 header 的第一个值。 */
    private static String first(Collection<String> values) {
        return values == null || values.isEmpty() ? "" : values.iterator().next();
    }
}
