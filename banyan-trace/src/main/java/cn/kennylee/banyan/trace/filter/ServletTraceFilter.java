package cn.kennylee.banyan.trace.filter;

import cn.kennylee.banyan.trace.autoconfigure.TraceProperties;
import cn.kennylee.banyan.trace.context.TraceContext;
import cn.kennylee.banyan.trace.support.TraceIdResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Servlet 入向过滤器：读取或生成 TraceId，写入 MDC 与响应头，请求结束后恢复入口前的 MDC 快照。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class ServletTraceFilter extends OncePerRequestFilter implements Ordered {

    /** 配置。 */
    private final TraceProperties properties;

    /**
     * 构造实例。
     *
     * @param properties 配置
     */
    public ServletTraceFilter(TraceProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String headerValue = request.getHeader(properties.getHeaderName());
        String traceId = TraceIdResolver.resolveIncoming(headerValue,
                () -> properties.isAcceptQueryTraceId() ? request.getParameter(properties.getMdcKey()) : null,
                properties.isAcceptIncoming());
        Map<String, String> mdcSnapshot = TraceContext.start(traceId, properties.getMdcKey());
        try {
            if (properties.getResponseHeader().isEnabled()) {
                response.setHeader(properties.getHeaderName(), traceId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.stop(mdcSnapshot);
        }
    }

    @Override
    public int getOrder() {
        return properties.getServlet().getFilterOrder();
    }
}
