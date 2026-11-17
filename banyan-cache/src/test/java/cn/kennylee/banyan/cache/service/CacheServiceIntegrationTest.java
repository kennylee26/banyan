package cn.kennylee.banyan.cache.service;

import cn.kennylee.banyan.cache.CacheIntegrationTestBase;
import cn.kennylee.banyan.cache.CacheTestApplication;
import cn.kennylee.banyan.cache.autoconfigure.CacheProperties;
import cn.kennylee.banyan.cache.provider.CacheProvider;
import cn.kennylee.banyan.cache.provider.RedisTemplateCacheProvider;
import cn.kennylee.banyan.cache.support.CacheKeys;
import cn.kennylee.banyan.commons.spring.json.JacksonJsonCodec;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CacheService 集成测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = CacheTestApplication.class)
class CacheServiceIntegrationTest extends CacheIntegrationTestBase {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void cleanUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    /** 对象读写。 */
    @Test
    void putsAndGetsObject() {
        cacheService.put("cart", "u1", new User("alice", 30), Duration.ofMinutes(5));

        assertThat(cacheService.get("cart", "u1", User.class).getUserName()).isEqualTo("alice");
        // Redis 中是可读 JSON
        assertThat(stringRedisTemplate.opsForValue().get("banyan-test:cart:u1")).contains("alice");
    }

    /** Raw 与 Object 混用报错。 */
    @Test
    void rejectsMixedUsage() {
        cacheService.put("cart", "u2", new User("bob", 20), Duration.ofMinutes(5));

        assertThatThrownBy(() -> cacheService.getRaw("cart", "u2"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Raw 读写。 */
    @Test
    void putsAndGetsRaw() {
        cacheService.putRaw("cart", "u3", "plain", Duration.ofMinutes(5));

        assertThat(cacheService.getRaw("cart", "u3")).isEqualTo("plain");
    }

    /** 批量与过期。 */
    @Test
    void supportsBatchAndExpire() {
        cacheService.batchPut("cart", Map.of("a", new User("a", 1), "b", new User("b", 2)),
                Duration.ofMinutes(5));

        assertThat(cacheService.batchGet("cart", List.of("a", "b"), User.class))
                .extracting(User::getUserName)
                .containsExactly("a", "b");
        assertThat(cacheService.exists("cart", "a")).isTrue();
        assertThat(cacheService.expire("cart", "a", Duration.ofSeconds(30))).isTrue();
        assertThat(cacheService.addExpireTime("cart", "a", Duration.ofSeconds(30))).isTrue();
        assertThat(cacheService.batchDelete("cart", List.of("a", "b"))).isEqualTo(2);
    }

    /** expire / addExpireTime 拒绝 null TTL。 */
    @Test
    void rejectsNullTtl() {
        assertThatThrownBy(() -> cacheService.expire("cart", "u1", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cacheService.addExpireTime("cart", "u1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 读写开关关闭后不访问 Redis。 */
    @Test
    void readWriteSwitchShortCircuits() {
        CacheProperties properties = new CacheProperties();
        properties.setReadWriteEnabled(false);
        CacheService disabled = new DefaultCacheService(
                new RedisTemplateCacheProvider(stringRedisTemplate),
                new CacheKeys("banyan-test", 512),
                properties,
                new JacksonJsonCodec(),
                new NoOpCachePenetrationGuard());

        disabled.put("cart", "off", new User("x", 1), Duration.ofMinutes(5));

        assertThat(disabled.get("cart", "off", User.class)).isNull();
        assertThat(disabled.increment("cart", "counter", 1)).isZero();
        assertThat(stringRedisTemplate.hasKey("banyan-test:cart:off")).isFalse();
    }

    /** getOrLoad 只加载一次。 */
    @Test
    void loadsOnceThenCaches() {
        AtomicInteger loads = new AtomicInteger();

        User first = cacheService.getOrLoad("cart", "load", User.class, () -> {
            loads.incrementAndGet();
            return new User("loaded", 1);
        }, Duration.ofMinutes(5));
        User second = cacheService.getOrLoad("cart", "load", User.class, () -> {
            loads.incrementAndGet();
            return new User("loaded", 1);
        }, Duration.ofMinutes(5));

        assertThat(first.getUserName()).isEqualTo("loaded");
        assertThat(second.getUserName()).isEqualTo("loaded");
        assertThat(loads).hasValue(1);
    }

    /** 测试数据。 */
    @Data
    static class User {

        private String userName;

        private int age;

        User() {
        }

        User(String userName, int age) {
            this.userName = userName;
            this.age = age;
        }
    }
}
