package cn.kennylee.banyan.commons.spring.web;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * header 传播规则：读取当前入站 header，并按阻塞名单剔除。
 *
 * <p>默认阻塞连接与鉴权相关的头，避免自动把入站凭据带出去。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class HeaderRules {

    /** 默认阻塞头。 */
    private static final Set<String> DEFAULT_BLOCKED = Set.of(
            HttpHeaders.COOKIE, HttpHeaders.SET_COOKIE, HttpHeaders.CONTENT_LENGTH,
            HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.AUTHORIZATION);

    private HeaderRules() {
    }

    /**
     * 读取当前 Servlet 请求的 header；不在请求线程内时返回空表。
     *
     * @return header 表，保持入站顺序
     */
    public static Map<String, String> inboundHeaders() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return new LinkedHashMap<>();
        }
        HttpServletRequest request = servletAttributes.getRequest();
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
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

    /**
     * 剔除默认阻塞头与额外阻塞头，匹配忽略大小写。
     *
     * @param headers      待处理的 header 表，原地修改
     * @param extraBlocked 额外阻塞头，可为 null
     */
    public static void removeBlocked(@NonNull Map<String, String> headers,
                                     Collection<String> extraBlocked) {
        Set<String> blocked = new HashSet<>();
        DEFAULT_BLOCKED.forEach(name -> blocked.add(name.toLowerCase(Locale.ROOT)));
        if (extraBlocked != null) {
            extraBlocked.forEach(name -> blocked.add(name.toLowerCase(Locale.ROOT)));
        }
        headers.keySet().removeIf(name -> blocked.contains(name.toLowerCase(Locale.ROOT)));
    }
}
