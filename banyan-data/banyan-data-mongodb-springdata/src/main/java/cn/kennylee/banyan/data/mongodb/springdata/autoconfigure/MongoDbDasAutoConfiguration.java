package cn.kennylee.banyan.data.mongodb.springdata.autoconfigure;

import cn.kennylee.banyan.data.core.query.DasDbType;
import cn.kennylee.banyan.data.core.query.DasQueryStrategyRegistry;
import cn.kennylee.banyan.data.core.query.DefaultDasQueryStrategyRegistry;
import cn.kennylee.banyan.data.mongodb.springdata.strategy.MongodbDasQueryStrategy;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * MongoDB 侧自动配置。注册表用 {@code @ConditionalOnMissingBean} 共享；默认策略以 bean 形式注册，
 * 使用方定义同类型 bean 时让位，注册时也不会覆盖已存在的 MONGODB 策略。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.data.mongodb.core.MongoTemplate")
@ConditionalOnProperty(prefix = "banyan.data-mongodb", name = "enabled", matchIfMissing = true)
public class MongoDbDasAutoConfiguration implements SmartInitializingSingleton, ApplicationContextAware {

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
     * 注册默认的 MongoDB 查询策略。
     *
     * @return 查询策略
     */
    @Bean
    @ConditionalOnMissingBean(MongodbDasQueryStrategy.class)
    public MongodbDasQueryStrategy<?> mongodbDasQueryStrategy() {
        return new MongodbDasQueryStrategy<>();
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
     * 容器内单例初始化完成后，把容器中的 MongoDB 策略注册进注册表；已存在 MONGODB 策略时不覆盖。
     */
    @Override
    public void afterSingletonsInstantiated() {
        DasQueryStrategyRegistry registry = applicationContext.getBean(DasQueryStrategyRegistry.class);
        if (registry.containsStrategy(DasDbType.MONGODB)) {
            return;
        }
        applicationContext.getBeansOfType(MongodbDasQueryStrategy.class).values().stream()
                .findFirst()
                .ifPresent(strategy -> registry.register(DasDbType.MONGODB, strategy));
    }
}
