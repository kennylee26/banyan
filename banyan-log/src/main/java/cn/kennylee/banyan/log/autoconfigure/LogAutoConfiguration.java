package cn.kennylee.banyan.log.autoconfigure;

import cn.kennylee.banyan.log.lifecycle.EnvironmentInfoProvider;
import cn.kennylee.banyan.log.lifecycle.JdkEnvironmentInfoProvider;
import cn.kennylee.banyan.log.lifecycle.LifecycleLogger;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 日志自动配置：注册纯 JDK 的环境信息 Provider 与生命周期事件监听。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(name = {
        "org.springframework.boot.context.properties.ConfigurationProperties",
        "org.springframework.context.ApplicationEvent"})
@ConditionalOnProperty(prefix = "banyan.log", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(LogProperties.class)
public class LogAutoConfiguration {

    /**
     * 纯 JDK 环境信息采集器。
     *
     * @return Provider
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "banyan.log.lifecycle", name = "enabled", matchIfMissing = true)
    public EnvironmentInfoProvider environmentInfoProvider() {
        return new JdkEnvironmentInfoProvider();
    }

    /**
     * 生命周期日志监听器。
     *
     * @param properties  配置
     * @param provider    环境信息采集器
     * @param environment Spring 环境
     * @return 监听器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "banyan.log.lifecycle", name = "enabled", matchIfMissing = true)
    public LifecycleLogger lifecycleLogger(LogProperties properties, EnvironmentInfoProvider provider,
                                           Environment environment) {
        return new LifecycleLogger(properties, provider, environment);
    }
}
