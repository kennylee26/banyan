package cn.kennylee.banyan.trace.thread;

import cn.kennylee.banyan.trace.support.TraceIdResolver;
import cn.kennylee.banyan.trace.support.TraceSettings;

import java.util.concurrent.Callable;

/**
 * 任意异步的生命周期工具：让 MQ 或其它异步场景用与 HTTP 相同的 header 名、同一段捕获/还原逻辑传递 TraceId。
 * <p>不内置任何 MQ 客户端。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class TracePropagation {

    /** 复用装饰器的捕获/还原语义。 */
    private static final MdcTaskDecorator DECORATOR = new MdcTaskDecorator();

    private TracePropagation() {
    }

    /**
     * 读取当前 TraceId。
     *
     * @return TraceId；不存在时返回 null
     */
    public static String resolveCurrentTraceId() {
        return TraceIdResolver.resolve(TraceSettings.getMdcKey());
    }

    /**
     * 读取配置的传输 header 名，供 MQ 生产者写入消息头。
     *
     * @return header 名
     */
    public static String resolveHeaderName() {
        return TraceSettings.getHeaderName();
    }

    /**
     * 包装 Runnable：执行时还原提交时的 TraceId 与 MDC，结束后恢复。
     *
     * @param runnable 原任务
     * @return 包装后的任务
     */
    public static Runnable wrap(Runnable runnable) {
        return DECORATOR.decorate(runnable);
    }

    /**
     * 包装 Callable：执行时还原提交时的 TraceId 与 MDC，结束后恢复。
     *
     * @param callable 原任务
     * @param <V>      返回值类型
     * @return 包装后的任务
     */
    public static <V> Callable<V> wrap(Callable<V> callable) {
        TraceContextSnapshot captured = TraceContextSnapshot.capture();
        return () -> {
            TraceContextSnapshot previous = TraceContextSnapshot.capture();
            try {
                captured.restore();
                return callable.call();
            } finally {
                previous.restore();
            }
        };
    }
}
