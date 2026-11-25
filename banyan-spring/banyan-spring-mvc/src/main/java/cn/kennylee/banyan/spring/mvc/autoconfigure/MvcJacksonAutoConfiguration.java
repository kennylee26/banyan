package cn.kennylee.banyan.spring.mvc.autoconfigure;

import cn.kennylee.banyan.spring.mvc.jackson.BanyanJacksonCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 可选 Jackson 增强装配，默认关闭。
 *
 * @author kennylee
 * @since 0.1.0
 */
@AutoConfiguration(after = MvcCoreAutoConfiguration.class)
@ConditionalOnClass(name = {
        "com.fasterxml.jackson.databind.ObjectMapper",
        "org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer"})
@ConditionalOnProperty(prefix = "banyan.spring.mvc", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MvcProperties.class)
public class MvcJacksonAutoConfiguration {

    /**
     * Jackson 便捷开关 customizer，默认关闭。
     *
     * @param properties 配置
     * @return Jackson customizer
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "banyan.spring.mvc.jackson", name = "enabled",
            havingValue = "true")
    public BanyanJacksonCustomizer banyanJacksonCustomizer(MvcProperties properties) {
        return new BanyanJacksonCustomizer(properties.getJackson());
    }
}
