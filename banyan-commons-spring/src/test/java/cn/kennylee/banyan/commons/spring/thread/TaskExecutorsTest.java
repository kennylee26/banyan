package cn.kennylee.banyan.commons.spring.thread;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 通用线程工具测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
class TaskExecutorsTest {

    /** 创建已初始化的线程池并保留线程名前缀。 */
    @Test
    void createsInitializedPoolWithPrefix() {
        ThreadPoolTaskExecutor executor = TaskExecutors.newPool("banyan-test-");
        try {
            assertThat(executor.getThreadNamePrefix()).isEqualTo("banyan-test-");
            assertThat(executor.getCorePoolSize()).isPositive();
            assertThat(executor.getThreadPoolExecutor()).isNotNull();
        } finally {
            executor.shutdown();
        }
    }

    /** 设置装饰器后，任务执行时装饰器生效。 */
    @Test
    void appliesDecorator() throws Exception {
        AtomicBoolean decorated = new AtomicBoolean();
        TaskDecorator decorator = runnable -> () -> {
            decorated.set(true);
            runnable.run();
        };
        ThreadPoolTaskExecutor executor = TaskExecutors.decorate(
                TaskExecutors.newPool("banyan-test-"), decorator);
        try {
            executor.submit(() -> {
            }).get();
            assertThat(decorated).isTrue();
        } finally {
            executor.shutdown();
        }
    }

    /** 装饰器为 null 时返回原线程池，不报错。 */
    @Test
    void keepsExecutorWhenDecoratorIsNull() {
        ThreadPoolTaskExecutor executor = TaskExecutors.newPool("banyan-test-");
        try {
            assertThat(TaskExecutors.decorate(executor, null)).isSameAs(executor);
        } finally {
            executor.shutdown();
        }
    }
}
