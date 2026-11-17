package cn.kennylee.banyan.trace.thread;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.trace.autoconfigure.TraceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 线程池兜底：给容器中尚未设置装饰器的 {@link ThreadPoolTaskExecutor} 补上 TraceId 传递装饰器。
 * <p>只处理线程池执行器；使用方已声明 {@link TaskDecorator} bean 时整体跳过，不覆盖使用方配置。
 * 受 {@code banyan.trace.async.decorate-executors} 控制。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class TraceExecutorBeanPostProcessor implements BeanPostProcessor, Ordered {

    /** 配置。 */
    private final TraceProperties properties;

    /** 使用方声明的任务装饰器。 */
    private final ObjectProvider<TaskDecorator> customDecorators;

    /**
     * 构造实例。
     *
     * @param properties       配置
     * @param customDecorators 使用方声明的任务装饰器
     */
    public TraceExecutorBeanPostProcessor(TraceProperties properties,
                                          ObjectProvider<TaskDecorator> customDecorators) {
        this.properties = properties;
        this.customDecorators = customDecorators;
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
        if (!properties.getAsync().isDecorateExecutors()) {
            return bean;
        }
        if (!(bean instanceof ThreadPoolTaskExecutor executor)) {
            return bean;
        }
        if (customDecorators.getIfAvailable() != null) {
            log.info(Messages.get(ErrorCode.TRACE_CUSTOM_DECORATOR_SKIPPED, beanName));
            return bean;
        }
        // 线程池无公开的 getTaskDecorator；使用方若直接设置过装饰器，本库无法识别
        executor.setTaskDecorator(new MdcTaskDecorator());
        return bean;
    }

    @Override
    public int getOrder() {
        return TraceProperties.DEFAULT_FILTER_ORDER;
    }
}
