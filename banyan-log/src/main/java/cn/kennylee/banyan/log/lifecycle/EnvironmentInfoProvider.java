package cn.kennylee.banyan.log.lifecycle;

import org.springframework.context.ApplicationContext;

/**
 * 环境信息采集扩展点：默认实现只依赖 JDK，使用方可声明自己的 bean 替换。
 *
 * @author kennylee
 * @since 0.1.0
 */
@FunctionalInterface
public interface EnvironmentInfoProvider {

    /**
     * 采集运行时环境信息。
     *
     * @param context 当前应用上下文
     * @return 环境信息；取不到的字段为 null
     */
    EnvironmentInfo collect(ApplicationContext context);
}
