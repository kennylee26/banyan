package cn.kennylee.banyan.mq.rocketmq.apache.autoconfigure;

import cn.kennylee.banyan.mq.core.spi.MqBackend;
import cn.kennylee.banyan.mq.rocketmq.apache.RocketMqBackend;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * RocketMQ 后端自动装配：默认开启，缺类时由缺类配置告警跳过。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration(afterName = {
        "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration"
})
@ConditionalOnClass(name = "org.apache.rocketmq.spring.core.RocketMQTemplate")
@ConditionalOnProperty(prefix = "banyan.mq", name = "enabled", matchIfMissing = true)
public class RocketMqAutoConfiguration {

    /**
     * RocketMQ 后端。
     *
     * @param template           发送模板
     * @param applicationContext 容器
     * @param environment        环境配置
     * @return 后端
     */
    @Bean
    @ConditionalOnMissingBean(RocketMqBackend.class)
    @ConditionalOnBean(RocketMQTemplate.class)
    @ConditionalOnProperty(prefix = "banyan.mq.rocketmq", name = "enabled", matchIfMissing = true)
    public RocketMqBackend rocketMqBackend(RocketMQTemplate template,
                                           ApplicationContext applicationContext,
                                           Environment environment) {
        String nameServer = environment.getProperty("rocketmq.name-server", "");
        return new RocketMqBackend(template, nameServer, applicationContext);
    }
}
