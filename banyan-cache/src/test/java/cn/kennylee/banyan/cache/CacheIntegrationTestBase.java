package cn.kennylee.banyan.cache;

import cn.kennylee.banyan.cache.support.EmbeddedRedisSupport;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 内嵌 Redis 集成测试基类。
 *
 * @author kennylee
 * @since 0.1.0
 */
public abstract class CacheIntegrationTestBase {

    static {
        EmbeddedRedisSupport.start();
    }

    /**
     * 注入 Redis 连接与 appName。
     *
     * @param registry 动态属性注册表
     */
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", EmbeddedRedisSupport::port);
        registry.add("spring.application.name", () -> "banyan-test");
    }
}
