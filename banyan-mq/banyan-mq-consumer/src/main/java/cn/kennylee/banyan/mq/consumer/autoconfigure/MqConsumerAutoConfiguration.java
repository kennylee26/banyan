package cn.kennylee.banyan.mq.consumer.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.mq.consumer.MqConsumer;
import cn.kennylee.banyan.mq.consumer.MqConsumerRegistrar;
import cn.kennylee.banyan.mq.core.MqMessageCodec;
import cn.kennylee.banyan.mq.core.StringMqMessageCodec;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.spring.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;

/**
 * 消费者自动装配：一次性扫描业务 bean 的注解方法并注册到逻辑实例后端。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration(afterName = {
        "cn.kennylee.banyan.mq.spring.MqCommonsAutoConfiguration",
        "cn.kennylee.banyan.mq.kafka.spring.autoconfigure.KafkaMqAutoConfiguration",
        "cn.kennylee.banyan.mq.rocketmq.apache.autoconfigure.RocketMqAutoConfiguration"
})
@ConditionalOnClass(name = "org.springframework.beans.factory.SmartInitializingSingleton")
@ConditionalOnExpression("${banyan.mq.enabled:true} and ${banyan.mq.consumer.enabled:true}")
public class MqConsumerAutoConfiguration implements SmartInitializingSingleton {

    /** 容器。 */
    private final ApplicationContext applicationContext;

    /** 配置。 */
    private final MqProperties properties;

    /**
     * 构造自动配置。
     *
     * @param applicationContext 容器
     * @param properties         配置
     */
    public MqConsumerAutoConfiguration(ApplicationContext applicationContext,
                                       MqProperties properties) {
        this.applicationContext = applicationContext;
        this.properties = properties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        MqInstanceRegistry registry = applicationContext.getBeanProvider(MqInstanceRegistry.class)
                .getIfAvailable();
        if (registry == null) {
            return;
        }

        MqMessageCodec codec = applicationContext.getBeanProvider(MqMessageCodec.class)
                .getIfAvailable(StringMqMessageCodec::new);
        MqConsumerRegistrar registrar = new MqConsumerRegistrar(registry, resolveSuffix(), codec,
                applicationContext.getEnvironment());
        log.info(Messages.get(ErrorCode.MQ_CONSUMER_SCAN_START, registry.instanceNames()));
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean = applicationContext.getBean(beanName);
            if (hasConsumerMethod(bean.getClass())) {
                registrar.register(bean);
            }
        }
        log.info(Messages.get(ErrorCode.MQ_CONSUMER_SCAN_END));
    }

    private String resolveSuffix() {
        if (!properties.getConsumer().isSuffixEnabled()) {
            return "";
        }
        return properties.effectiveSuffix(applicationContext.getEnvironment().getActiveProfiles());
    }

    static boolean hasConsumerMethod(Class<?> type) {
        for (Method method : ClassUtils.getUserClass(type).getDeclaredMethods()) {
            if (method.isAnnotationPresent(MqConsumer.class)) {
                return true;
            }
        }
        return false;
    }
}
