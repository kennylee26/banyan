package cn.kennylee.banyan.commons.spring.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * JsonCodec 自动配置。默认开启；使用方可通过 {@code banyan.json.enabled=false} 关闭，
 * 或通过 {@code banyan.json.provider} 显式选择实现。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnProperty(prefix = "banyan.json", name = "enabled", matchIfMissing = true)
public class JsonAutoConfiguration {

    /**
     * 注册 JsonCodec 默认实现（Jackson）。
     *
     * @return JsonCodec 实例
     */
    @Bean
    @ConditionalOnMissingBean(JsonCodec.class)
    @ConditionalOnProperty(prefix = "banyan.json", name = "provider", havingValue = "jackson", matchIfMissing = true)
    public JsonCodec jsonCodec() {
        return new JacksonJsonCodec();
    }
}
