package cn.kennylee.banyan.cache.autoconfigure;

import cn.kennylee.banyan.cache.provider.CacheProvider;
import cn.kennylee.banyan.cache.provider.RedisTemplateCacheProvider;
import cn.kennylee.banyan.cache.service.CachePenetrationGuard;
import cn.kennylee.banyan.cache.service.CacheService;
import cn.kennylee.banyan.cache.service.CacheServiceImpl;
import cn.kennylee.banyan.cache.service.NoOpCachePenetrationGuard;
import cn.kennylee.banyan.cache.support.CacheKeys;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.spring.json.JsonAutoConfiguration;
import cn.kennylee.banyan.commons.spring.json.JsonCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存自动配置：默认装配 CacheService / Provider / 配置，缺类时由缺类配置告警跳过。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration(after = JsonAutoConfiguration.class)
@ConditionalOnClass(name = {
        "org.springframework.data.redis.core.StringRedisTemplate",
        "com.fasterxml.jackson.databind.ObjectMapper",
        "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule"})
@ConditionalOnProperty(prefix = "banyan.cache", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    /** 应用名属性。 */
    private static final String APP_NAME_PROPERTY = "spring.application.name";

    /**
     * 默认 Provider。
     *
     * @param stringRedisTemplate Redis 模板
     * @return Provider
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheProvider cacheProvider(StringRedisTemplate stringRedisTemplate) {
        return new RedisTemplateCacheProvider(stringRedisTemplate);
    }

    /**
     * key 拼接器。
     *
     * @param environment 环境配置
     * @param properties  缓存配置
     * @return 拼接器
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheKeys cacheKeys(Environment environment, CacheProperties properties) {
        return new CacheKeys(environment.getProperty(APP_NAME_PROPERTY), properties.getMaxKeyLength());
    }

    /**
     * 穿透防护占位。
     *
     * @param properties 缓存配置
     * @return 防护实现
     */
    @Bean
    @ConditionalOnMissingBean
    public CachePenetrationGuard cachePenetrationGuard(CacheProperties properties) {
        if (properties.getPenetration().isEnabled()) {
            log.warn(Messages.get(ErrorCode.CACHE_PENETRATION_NOOP));
        }
        return new NoOpCachePenetrationGuard();
    }

    /**
     * 缓存服务。
     *
     * @param provider          Provider
     * @param keys              key 拼接器
     * @param properties        缓存配置
     * @param jsonCodec         JSON 编解码
     * @param penetrationGuard  穿透防护
     * @return 缓存服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(JsonCodec.class)
    public CacheService cacheService(CacheProvider provider, CacheKeys keys, CacheProperties properties,
                                     JsonCodec jsonCodec, CachePenetrationGuard penetrationGuard) {
        return new CacheServiceImpl(provider, keys, properties, jsonCodec, penetrationGuard);
    }
}
