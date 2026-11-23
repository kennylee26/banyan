package cn.kennylee.banyan.spring.feign.interceptor;

import cn.kennylee.banyan.spring.feign.FeignContext;
import cn.kennylee.banyan.spring.feign.FeignContextHolder;
import cn.kennylee.banyan.spring.feign.autoconfigure.FeignProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 入站 header 转发拦截器：默认由配置关闭，开启后按阻塞名单与单次上下文转发。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class ForwardHeadersInterceptor implements RequestInterceptor, Ordered {

    /** 默认阻塞头。 */
    private static final Set<String> DEFAULT_BLOCKED = Set.of(
            HttpHeaders.COOKIE, HttpHeaders.SET_COOKIE, HttpHeaders.CONTENT_LENGTH,
            HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION);

    /** 配置。 */
    private final FeignProperties properties;

    /**
     * 构造拦截器。
     *
     * @param properties 配置
     */
    public ForwardHeadersInterceptor(FeignProperties properties) {
        this.properties = properties;
    }

    @Override
    public void apply(@NonNull RequestTemplate template) {
        FeignContext context = FeignContextHolder.current();
        try {
            if (!FeignContextHolder.isForwardEnabled()) {
                log.debug("当前 Feign 上下文或全局开关禁用转发 header");
                return;
            }
            Map<String, String> headers = originHeaders();
            removeBlocked(headers, context);
            headers.putAll(context.getCustomHeaders());
            headers.forEach(template::header);
        } finally {
            if (properties.getForwardHeaders().isClearAfterApply()
                    && context.isClearAtOnce()) {
                FeignContextHolder.clear();
            }
        }
    }

    @Override
    public int getOrder() {
        return FeignRequestLogInterceptor.ORDER - 1;
    }

    /** 读取当前 Servlet 请求的 header。 */
    private static Map<String, String> originHeaders() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return new LinkedHashMap<>();
        }
        HttpServletRequest request = servletAttributes.getRequest();
        Enumeration<String> names = request.getHeaderNames();
        Map<String, String> headers = new LinkedHashMap<>();
        if (names == null) {
            return headers;
        }
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String value = request.getHeader(name);
            if (value != null && !value.isEmpty()) {
                headers.put(name, value);
            }
        }
        return headers;
    }

    /** 应用默认 + 配置 + 单次的阻塞头。 */
    private void removeBlocked(Map<String, String> headers, FeignContext context) {
        Set<String> blocked = new HashSet<>();
        DEFAULT_BLOCKED.forEach(name -> blocked.add(name.toLowerCase(Locale.ROOT)));
        properties.getForwardHeaders().getBlockedHeaders()
                .forEach(name -> blocked.add(name.toLowerCase(Locale.ROOT)));
        context.getBlockedHeaders().forEach(name -> blocked.add(name.toLowerCase(Locale.ROOT)));
        headers.keySet().removeIf(name -> blocked.contains(name.toLowerCase(Locale.ROOT)));
    }
}
