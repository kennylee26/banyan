package cn.kennylee.banyan.trace.thread;

import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

/**
 * 异步任务装饰器：把提交时的 MDC 快照与 TraceId 传递到执行线程，执行后恢复执行线程原有上下文。
 * <p>同时传递 MDC 与线程上下文；两者都为空时仍然包装。不引入 TransmittableThreadLocal。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        TraceContextSnapshot captured = TraceContextSnapshot.capture();
        return () -> {
            TraceContextSnapshot previous = TraceContextSnapshot.capture();
            try {
                captured.restore();
                runnable.run();
            } finally {
                previous.restore();
            }
        };
    }
}
