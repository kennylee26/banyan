package cn.kennylee.banyan.commons.spring.thread;

import cn.kennylee.banyan.commons.util.Strings;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 线程池工具：只负责创建与设置装饰器，不含任何日志上下文或业务语义。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class TaskExecutors {

    /** 默认线程名前缀。 */
    private static final String DEFAULT_THREAD_NAME_PREFIX = "banyan-";

    /** 默认核心线程数。 */
    private static final int DEFAULT_CORE_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    /** 默认最大线程数。 */
    private static final int DEFAULT_MAX_SIZE = DEFAULT_CORE_SIZE * 4;

    /** 默认队列容量。 */
    private static final int DEFAULT_QUEUE_CAPACITY = 1000;

    /** 默认空闲线程存活秒数。 */
    private static final int DEFAULT_KEEP_ALIVE_SECONDS = 60;

    private TaskExecutors() {
    }

    /**
     * 按默认参数创建并初始化线程池。
     *
     * @param threadNamePrefix 线程名前缀，空白时使用默认值
     * @return 已初始化的线程池
     */
    public static ThreadPoolTaskExecutor newPool(String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(DEFAULT_CORE_SIZE);
        executor.setMaxPoolSize(DEFAULT_MAX_SIZE);
        executor.setQueueCapacity(DEFAULT_QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(DEFAULT_KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix(Strings.isNotBlank(threadNamePrefix)
                ? threadNamePrefix : DEFAULT_THREAD_NAME_PREFIX);
        executor.initialize();
        return executor;
    }

    /**
     * 给线程池设置任务装饰器；装饰器为 null 时不修改。
     *
     * @param executor  线程池
     * @param decorator 任务装饰器，可为 null
     * @return 原线程池，便于链式调用
     */
    public static ThreadPoolTaskExecutor decorate(ThreadPoolTaskExecutor executor, TaskDecorator decorator) {
        if (executor != null && decorator != null) {
            executor.setTaskDecorator(decorator);
        }
        return executor;
    }
}
