package cn.kennylee.banyan.trace.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

/**
 * Trace 配置。所有开关与参数集中在此，避免散读环境变量。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "banyan.trace")
public class TraceProperties {

    /** 过滤器与后置处理器的默认顺序，不抢最高优先级。 */
    public static final int DEFAULT_FILTER_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    /** 默认传输 header 名。 */
    public static final String DEFAULT_HEADER_NAME = "X-Trace-Id";

    /** 默认 MDC key。 */
    public static final String DEFAULT_MDC_KEY = "traceId";

    /** 模块总开关。 */
    private boolean enabled = true;

    /** 传输 header 名。 */
    private String headerName = DEFAULT_HEADER_NAME;

    /** 日志 MDC key。 */
    private String mdcKey = DEFAULT_MDC_KEY;

    /** 是否沿用上游传入的 TraceId。 */
    private boolean acceptIncoming = true;

    /** 是否允许从 query 参数兜底取 TraceId，默认关闭。 */
    private boolean acceptQueryTraceId;

    /** 响应头回写配置。 */
    private ResponseHeader responseHeader = new ResponseHeader();

    /** Servlet 配置。 */
    private Servlet servlet = new Servlet();

    /** WebFlux 配置。 */
    private Webflux webflux = new Webflux();

    /** Feign 配置。 */
    private Feign feign = new Feign();

    /** Dubbo 配置。 */
    private Dubbo dubbo = new Dubbo();

    /** 异步传递配置。 */
    private Async async = new Async();

    /**
     * 响应头回写配置。
           *
      * @author kennylee
      * @since 0.1.0
      */
    @Getter
    @Setter
    public static class ResponseHeader {

        /** 是否回写响应头。 */
        private boolean enabled = true;
    }

    /**
     * Servlet 配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Servlet {

        /** 是否启用 Servlet 过滤器。 */
        private boolean enabled = true;

        /** 过滤器顺序，默认不抢最高优先级。 */
        private int filterOrder = DEFAULT_FILTER_ORDER;
    }

    /**
     * WebFlux 配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Webflux {

        /** 是否启用 WebFlux 过滤器。 */
        private boolean enabled = true;

        /** 响应式上下文传播配置。 */
        private ContextPropagation contextPropagation = new ContextPropagation();
    }

    /**
     * 响应式上下文传播配置。
           *
      * @author kennylee
      * @since 0.1.0
      */
    @Getter
    @Setter
    public static class ContextPropagation {

        /** 是否开启 Reactor 自动上下文传播，默认开启。 */
        private boolean enabled = true;
    }

    /**
     * Feign 配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Feign {

        /** 是否启用 Feign 出向透传。 */
        private boolean enabled = true;
    }

    /**
     * Dubbo 配置。
     *
     * @author kennylee
     * @since 0.1.0
     */
    @Getter
    @Setter
    public static class Dubbo {

        /** 是否启用 Dubbo 提供端与消费端透传。 */
        private boolean enabled = true;
    }

    /**
     * 异步传递配置。
           *
      * @author kennylee
      * @since 0.1.0
      */
    @Getter
    @Setter
    public static class Async {

        /** 是否自动给容器中的线程池补设装饰器。 */
        private boolean decorateExecutors = true;
    }
}
