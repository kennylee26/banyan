package cn.kennylee.banyan.mq.spring;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.spring.json.JsonCodec;
import cn.kennylee.banyan.mq.core.MqMessageCodec;
import cn.kennylee.banyan.mq.core.StringMqMessageCodec;
import cn.kennylee.banyan.mq.core.instance.MqInstanceRegistry;
import cn.kennylee.banyan.mq.core.spi.MqBackend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 消息 Spring 层共享装配：默认编解码与逻辑实例路由。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(prefix = "banyan.mq", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MqProperties.class)
public class MqCommonsAutoConfiguration {

    /**
     * 默认编解码器：有 JsonCodec 时用 JSON，否则 String 直通。
     *
     * @param jsonCodec JSON 编解码
     * @return 编解码器
     */
    @Bean
    @ConditionalOnMissingBean
    public MqMessageCodec mqMessageCodec(ObjectProvider<JsonCodec> jsonCodec) {
        JsonCodec json = jsonCodec.getIfAvailable();
        return json == null ? new StringMqMessageCodec() : new JsonMqMessageCodec(json);
    }

    /**
     * 逻辑实例路由。
     *
     * @param backends   后端实现
     * @param properties 配置
     * @return 路由
     */
    @Bean
    @ConditionalOnMissingBean
    public MqInstanceRegistry mqInstanceRegistry(ObjectProvider<MqBackend> backends,
                                                 MqProperties properties) {
        Map<String, String> mapping = new LinkedHashMap<>();
        properties.getInstances().forEach((name, instance) -> mapping.put(name, instance.getType()));
        MqInstanceRegistry registry = new MqInstanceRegistry(
                backends.orderedStream().toList(), mapping);
        log.info(Messages.get(ErrorCode.MQ_INSTANCE_ASSEMBLED, registry.instanceNames()));
        return registry;
    }
}
