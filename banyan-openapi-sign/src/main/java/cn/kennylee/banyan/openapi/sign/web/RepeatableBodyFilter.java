package cn.kennylee.banyan.openapi.sign.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * 把请求包装为可重复读：下游验签、解密与业务代码可以多次读取 body。
 *
 * <p>已经缓存过（属性存在）的请求不再重复包装。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class RepeatableBodyFilter extends OncePerRequestFilter {

    /** multipart 前缀：文件上传不缓存，避免把大文件读进内存。 */
    private static final String MULTIPART_PREFIX = "multipart/";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getAttribute(CachedBodyRequestWrapper.CACHED_BODY_ATTRIBUTE) != null
                || isMultipart(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(new CachedBodyRequestWrapper(request), response);
    }

    /** 是否为文件上传请求。 */
    private static boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith(MULTIPART_PREFIX);
    }
}
