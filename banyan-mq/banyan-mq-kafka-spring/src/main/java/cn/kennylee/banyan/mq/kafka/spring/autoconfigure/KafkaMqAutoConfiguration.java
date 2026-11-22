package cn.kennylee.banyan.mq.kafka.spring.autoconfigure;

import cn.kennylee.banyan.mq.core.spi.MqBackend;
import cn.kennylee.banyan.mq.kafka.spring.KafkaMqBackend;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka 后端自动装配：默认开启，缺类时由缺类配置告警跳过。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
@ConditionalOnProperty(prefix = "banyan.mq", name = "enabled", matchIfMissing = true)
public class KafkaMqAutoConfiguration {

    /**
     * Kafka 后端。
     *
     * @param template        发送模板
     * @param consumerFactory 消费者工厂
     * @return 后端
     */
    @Bean
    @ConditionalOnMissingBean(KafkaMqBackend.class)
    @ConditionalOnBean({KafkaTemplate.class, ConsumerFactory.class})
    @ConditionalOnProperty(prefix = "banyan.mq.kafka", name = "enabled", matchIfMissing = true)
    public KafkaMqBackend kafkaMqBackend(KafkaTemplate<String, String> template,
                                         ConsumerFactory<String, String> consumerFactory) {
        return new KafkaMqBackend(template, consumerFactory);
    }
}
