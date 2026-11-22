package cn.kennylee.banyan.mq.producer.autoconfigure;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.mq.core.MqMessageCodec;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.producer.MqProducer;
import cn.kennylee.banyan.mq.spring.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 生产者自动装配：默认开启，总开关与模块开关都可关闭，缺事务类时跳过。
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
@ConditionalOnClass(name = "org.springframework.transaction.support.TransactionSynchronization")
@ConditionalOnExpression("${banyan.mq.enabled:true} and ${banyan.mq.producer.enabled:true}")
public class MqProducerAutoConfiguration {

    /**
     * 发送门面。
     *
     * @param registry    逻辑实例路由
     * @param codec       编解码器
     * @param properties  配置
     * @param environment 环境配置
     * @return 发送门面
     */
    @Bean
    @ConditionalOnMissingBean
    public MqProducer mqProducer(MqInstanceRegistry registry, MqMessageCodec codec,
                                 MqProperties properties, Environment environment) {
        String tagSuffix = properties.effectiveSuffix(environment.getActiveProfiles());
        boolean tagSuffixEnabled = properties.getProducer().isTagSuffixEnabled();
        log.info(Messages.get(ErrorCode.MQ_PRODUCER_ASSEMBLED,
                registry.instanceNames(), tagSuffix, tagSuffixEnabled));
        return new MqProducer(registry, null, codec, tagSuffix, tagSuffixEnabled);
    }
}
