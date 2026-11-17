package cn.kennylee.banyan.trace.thread;

import cn.kennylee.banyan.commons.spring.thread.TaskExecutors;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 带 TraceId 传递语义的线程池工厂：在通用线程工具之上挂载 {@link MdcTaskDecorator}。
 * <p>自建线程池请走本类，否则异步任务里没有 TraceId。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class TraceTaskExecutors {

    private TraceTaskExecutors() {
    }

    /**
     * 创建已带 TraceId 传递的线程池。
     *
     * @param threadNamePrefix 线程名前缀
     * @return 线程池
     */
    public static ThreadPoolTaskExecutor create(String threadNamePrefix) {
        return TaskExecutors.decorate(TaskExecutors.newPool(threadNamePrefix), new MdcTaskDecorator());
    }

    /**
     * 给已有线程池挂上 TraceId 传递装饰器。
     *
     * @param executor 线程池
     * @return 原线程池
     */
    public static ThreadPoolTaskExecutor decorate(ThreadPoolTaskExecutor executor) {
        return TaskExecutors.decorate(executor, new MdcTaskDecorator());
    }
}
