package cn.kennylee.banyan.commons.spring.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 读取当前 Servlet 请求的入站 header。
 *
 * <p>唯一依赖 Spring Web 与 Servlet API 的类；两者在本模块是 {@code provided + optional}，
 * 不向使用方传递。classpath 缺少必要类时 {@link #inbound()} 返回空表并打一次 WARN，
 * 不抛错，符合「缺必需类则跳过」的约定。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public final class ServletHeaders {

    /** 用于判断 Spring Web 是否在 classpath 的探测类名。 */
    private static final String PROBE_CLASS =
            "org.springframework.web.context.request.RequestContextHolder";

    /** 可用性缓存；null 表示尚未探测。 */
    private static volatile Boolean available;

    private ServletHeaders() {
    }

    /**
     * 当前 classpath 是否具备读取入站 header 的能力。
     *
     * @return true 表示 Spring Web 与 Servlet API 都在
     */
    public static boolean available() {
        Boolean cached = available;
        if (cached == null) {
            cached = probe();
            available = cached;
        }
        return cached;
    }

    /**
     * 读取当前 Servlet 请求的 header；不在请求线程内或缺类时返回空表。
     *
     * @return header 表，保持入站顺序
     */
    public static Map<String, String> inbound() {
        if (!available()) {
            return new LinkedHashMap<>();
        }
        return read();
    }

    /** 实际读取，只有确认类齐备后才会执行到 Spring / Servlet 类型。 */
    private static Map<String, String> read() {
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

    /** 探测 Spring Web 是否可用，缺类时打一次 WARN。 */
    private static boolean probe() {
        ClassLoader loader = ServletHeaders.class.getClassLoader();
        try {
            Class.forName(PROBE_CLASS, false, loader);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            log.warn("classpath 缺少 Spring Web，跳过入站 header 读取：{}", PROBE_CLASS);
            return false;
        }
    }
}
