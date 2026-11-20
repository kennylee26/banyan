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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        // given
        cacheService.put("cart", "u1", new User("alice", 30), Duration.ofMinutes(5));

        // when
        assertThat(cacheService.get("cart", "u1", User.class).getUserName()).isEqualTo("alice");
        // Redis 中是可读 JSON
        assertThat(stringRedisTemplate.opsForValue().get("banyan-test:cart:u1")).contains("alice");
    }

    /** 泛型容器按 Type 还原元素类型。 */
    @Test
    void restoresGenericContainers() {
        // given
        List<User> users = List.of(new User("alice", 30), new User("bob", 20));
        Map<String, User> byName = Map.of("a", new User("alice", 30));
        Set<User> unique = Set.of(new User("bob", 20));

        cacheService.put("gen", "list", users, Duration.ofMinutes(5));
        cacheService.put("gen", "map", byName, Duration.ofMinutes(5));
        cacheService.put("gen", "set", unique, Duration.ofMinutes(5));

        // when
        assertThat(cacheService.get("gen", "list", new ParameterizedTypeReference<List<User>>() {
        })).extracting(User::getUserName).containsExactly("alice", "bob");
        assertThat(cacheService.get("gen", "map", new ParameterizedTypeReference<Map<String, User>>() {
        }).get("a")).extracting(User::getAge).isEqualTo(30);
        assertThat(cacheService.get("gen", "set", new ParameterizedTypeReference<Set<User>>() {
        })).extracting(User::getUserName).containsExactly("bob");

        cacheService.batchPut("gen", Map.of(
                "l1", users,
                "l2", List.of(new User("carol", 40))), Duration.ofMinutes(5));
        assertThat(cacheService.batchGet("gen", List.of("l1", "l2"),
                new ParameterizedTypeReference<List<User>>() {
                })).hasSize(2);
        assertThat(cacheService.batchGet("gen", List.of("l1", "l2"),
                new ParameterizedTypeReference<List<User>>() {
                }).get(0)).extracting(User::getUserName).containsExactly("alice", "bob");
        assertThat(cacheService.batchGet("gen", List.of("l1", "l2"),
                new ParameterizedTypeReference<List<User>>() {
                }).get(1)).extracting(User::getUserName).containsExactly("carol");
    }

    /** getOrLoad 泛型重载能还原容器元素类型，且只加载一次。 */
    @Test
    void getOrLoadRestoresGenericContainers() {
        // given
        AtomicInteger loads = new AtomicInteger();
        ParameterizedTypeReference<List<User>> type = new ParameterizedTypeReference<List<User>>() {
        };

        // when
        List<User> first = cacheService.getOrLoad("gen", "lazy", type, () -> {
            loads.incrementAndGet();
            return List.of(new User("alice", 30), new User("bob", 20));
        }, Duration.ofMinutes(5));
        List<User> second = cacheService.getOrLoad("gen", "lazy", type, () -> {
            loads.incrementAndGet();
            return List.of();
        }, Duration.ofMinutes(5));

        // then
        assertThat(first).extracting(User::getUserName).containsExactly("alice", "bob");
        assertThat(second).extracting(User::getUserName).containsExactly("alice", "bob");
        assertThat(loads).hasValue(1);
    }

    /** 便捷方法无需匿名类即可还原容器元素类型。 */
    @Test
    void getsContainersWithoutTypeReference() {
        // given
        List<User> users = List.of(new User("alice", 30), new User("bob", 20));
        cacheService.put("easy", "list", users, Duration.ofMinutes(5));
        cacheService.put("easy", "set", Set.of(new User("bob", 20)), Duration.ofMinutes(5));
        cacheService.put("easy", "map", Map.of("a", new User("alice", 30)),
                Duration.ofMinutes(5));

        // when
        assertThat(cacheService.getForList("easy", "list", User.class))
                .extracting(User::getUserName).containsExactly("alice", "bob");
        assertThat(cacheService.getForSet("easy", "set", User.class))
                .extracting(User::getUserName).containsExactly("bob");
        assertThat(cacheService.getForMap("easy", "map", User.class).get("a"))
                .extracting(User::getAge).isEqualTo(30);
    }

    /** key 不存在时便捷方法返回空容器而不是 null。 */
    @Test
    void convenienceGettersReturnEmptyContainersWhenMissing() {
        // when
        assertThat(cacheService.getForList("missing", "k", User.class)).isEmpty();
        assertThat(cacheService.getForSet("missing", "k", User.class)).isEmpty();
        assertThat(cacheService.getForMap("missing", "k", User.class)).isEmpty();
    }

    /** 读写开关关闭时便捷方法返回空容器而不是 null，且不访问 Redis。 */
    @Test
    void convenienceGettersReturnEmptyContainersWhenDisabled() {
        // given
        CacheProperties properties = new CacheProperties();
        properties.setReadWriteEnabled(false);
        CacheService disabled = new CacheServiceImpl(
                new RedisTemplateCacheProvider(stringRedisTemplate),
                new CacheKeys("banyan-test", 512),
                properties,
                new JacksonJsonCodec(),
                new NoOpCachePenetrationGuard());

        // when
        assertThat(disabled.getForList("g", "k", User.class)).isEmpty();
        assertThat(disabled.getForSet("g", "k", User.class)).isEmpty();
        assertThat(disabled.getForMap("g", "k", User.class)).isEmpty();
        assertThat(stringRedisTemplate.hasKey("banyan-test:g:k")).isFalse();
    }

    /** Raw 与 Object 混用报错。 */
    @Test
    void rejectsMixedUsage() {
        // given
        cacheService.put("cart", "u2", new User("bob", 20), Duration.ofMinutes(5));

        // when
        assertThatThrownBy(() -> cacheService.getRaw("cart", "u2"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** Raw 读写。 */
    @Test
    void putsAndGetsRaw() {
        // given
        cacheService.putRaw("cart", "u3", "plain", Duration.ofMinutes(5));

        // when
        assertThat(cacheService.getRaw("cart", "u3")).isEqualTo("plain");
    }

    /** 批量与过期。 */
    @Test
    void supportsBatchAndExpire() {
        // given
        cacheService.batchPut("cart", Map.of("a", new User("a", 1), "b", new User("b", 2)),
                Duration.ofMinutes(5));

        // when
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
        // when
        assertThatThrownBy(() -> cacheService.expire("cart", "u1", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cacheService.addExpireTime("cart", "u1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 读写开关关闭后不访问 Redis。 */
    @Test
    void readWriteSwitchShortCircuits() {
        // given
        CacheProperties properties = new CacheProperties();
        properties.setReadWriteEnabled(false);
        CacheService disabled = new CacheServiceImpl(
                new RedisTemplateCacheProvider(stringRedisTemplate),
                new CacheKeys("banyan-test", 512),
                properties,
                new JacksonJsonCodec(),
                new NoOpCachePenetrationGuard());

        disabled.put("cart", "off", new User("x", 1), Duration.ofMinutes(5));

        // when
        assertThat(disabled.get("cart", "off", User.class)).isNull();
        assertThat(disabled.increment("cart", "counter", 1)).isZero();
        assertThat(stringRedisTemplate.hasKey("banyan-test:cart:off")).isFalse();
    }

    /** getOrLoad 只加载一次。 */
    @Test
    void loadsOnceThenCaches() {
        // given
        AtomicInteger loads = new AtomicInteger();

        // when
        User first = cacheService.getOrLoad("cart", "load", User.class, () -> {
            loads.incrementAndGet();
            return new User("loaded", 1);
        }, Duration.ofMinutes(5));
        User second = cacheService.getOrLoad("cart", "load", User.class, () -> {
            loads.incrementAndGet();
            return new User("loaded", 1);
        }, Duration.ofMinutes(5));

        // then
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
