package cn.kennylee.banyan.data.mysql.mybatis.autoconfigure;

import cn.kennylee.banyan.data.core.query.DasDbType;
import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.data.core.query.DefaultDasQueryStrategyRegistry;
import cn.kennylee.banyan.data.mysql.mybatis.strategy.MyBatisDasQueryStrategy;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * MySQL 侧自动配置。注册表用 {@code @ConditionalOnMissingBean} 共享；默认策略以 bean 形式注册，
 * 使用方定义同类型 bean 时让位，注册时也不会覆盖已存在的 MYSQL 策略。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.baomidou.mybatisplus.core.mapper.BaseMapper")
@ConditionalOnProperty(prefix = "banyan.data-mysql", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MyBatisDasProperties.class)
public class MyBatisDasAutoConfiguration implements SmartInitializingSingleton, ApplicationContextAware {

    /** 应用上下文。 */
    private ApplicationContext applicationContext;

    /**
     * 注册查询策略注册表。
     *
     * @return 策略注册表
     */
    @Bean
    @ConditionalOnMissingBean(DasQueryStrategyRegistry.class)
    public DasQueryStrategyRegistry dasQueryStrategyRegistry() {
        return new DefaultDasQueryStrategyRegistry(Map.of());
    }

    /**
     * 注册默认的 MySQL 查询策略；是否转义由配置决定。
     *
     * @param properties 配置
     * @return 查询策略
     */
    @Bean
    @ConditionalOnMissingBean(MyBatisDasQueryStrategy.class)
    public MyBatisDasQueryStrategy<?> myBatisDasQueryStrategy(MyBatisDasProperties properties) {
        return new MyBatisDasQueryStrategy<>(properties.getLikeEscape().isEnabled());
    }

    /**
     * 注入应用上下文。
     *
     * @param applicationContext 应用上下文
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 容器内单例初始化完成后，把容器中的 MySQL 策略注册进注册表；已存在 MYSQL 策略时不覆盖。
     */
    @Override
    public void afterSingletonsInstantiated() {
        DasQueryStrategyRegistry registry = applicationContext.getBean(DasQueryStrategyRegistry.class);
        if (registry.containsStrategy(DasDbType.MYSQL)) {
            return;
        }
        applicationContext.getBeansOfType(MyBatisDasQueryStrategy.class).values().stream()
                .findFirst()
                .ifPresent(strategy -> registry.register(DasDbType.MYSQL, strategy));
    }
}
