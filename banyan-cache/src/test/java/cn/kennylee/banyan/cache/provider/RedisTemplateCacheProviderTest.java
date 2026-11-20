package cn.kennylee.banyan.cache.provider;

import cn.kennylee.banyan.cache.CacheIntegrationTestBase;
import cn.kennylee.banyan.cache.CacheTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisTemplate Provider 集成测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = CacheTestApplication.class)
class RedisTemplateCacheProviderTest extends CacheIntegrationTestBase {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private CacheProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RedisTemplateCacheProvider(stringRedisTemplate);
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    /** 写入后直接读 Redis 是可读字符串。 */
    @Test
    void storesReadableString() {
        // given
        provider.set("k1", "{\"name\":\"alice\"}", Duration.ofSeconds(30));

        // when
        assertThat(provider.get("k1")).isEqualTo("{\"name\":\"alice\"}");
        assertThat(stringRedisTemplate.opsForValue().get("k1")).isEqualTo("{\"name\":\"alice\"}");
    }

    /** 存在性与过期。 */
    @Test
    void supportsExistsAndExpire() {
        // given
        provider.set("k2", "v", null);

        // when
        assertThat(provider.exists("k2")).isTrue();
        assertThat(provider.expire("k2", Duration.ofSeconds(30))).isTrue();
        assertThat(stringRedisTemplate.getExpire("k2")).isPositive();
        assertThat(provider.delete("k2")).isTrue();
        assertThat(provider.exists("k2")).isFalse();
    }

    /** 批量读写删。 */
    @Test
    void supportsBatchOperations() {
        // given
        provider.batchSet(Map.of("k3", "a", "k4", "b"), null);

        // when
        assertThat(provider.batchGet(List.of("k3", "k4"))).containsExactly("a", "b");
        assertThat(provider.batchDelete(List.of("k3", "k4"))).isEqualTo(2);
    }

    /** 结构与计数。 */
    @Test
    void supportsStructuresAndCounters() {
        // when
        assertThat(provider.increment("c1", 2)).isEqualTo(2L);

        provider.hashPut("h1", "f1", "v1");
        assertThat(provider.hashGet("h1", "f1")).isEqualTo("v1");
        assertThat(provider.hashGetAll("h1")).containsEntry("f1", "v1");

        provider.listPush("l1", "a");
        provider.listPush("l1", "b");
        assertThat(provider.listRange("l1", 0, -1)).containsExactly("a", "b");

        provider.setAdd("s1", "x", "y");
        assertThat(provider.setMembers("s1")).containsExactlyInAnyOrder("x", "y");

        provider.zsetAdd("z1", "m1", 1.0);
        assertThat(provider.zsetRange("z1", 0, -1)).containsExactly("m1");
    }

    /** 追加过期时间：持久 key 不能被凭空加上 TTL。 */
    @Test
    void addExpireTimeRequiresExistingTtl() {
        // given
        provider.set("persist", "v", null);

        // when
        assertThat(provider.addExpireTime("persist", Duration.ofSeconds(30))).isFalse();
        assertThat(stringRedisTemplate.getExpire("persist")).isEqualTo(-1L);

        provider.set("ttl", "v", Duration.ofSeconds(30));
        assertThat(provider.addExpireTime("ttl", Duration.ofSeconds(30))).isTrue();
        assertThat(stringRedisTemplate.getExpire("ttl")).isGreaterThan(30L);
    }

    /** SCAN 前缀查询。 */
    @Test
    void scansKeysByPattern() {
        // given
        provider.set("scan:a", "1", null);
        provider.set("scan:b", "2", null);

        // when
        assertThat(provider.scanKeys("scan:*", 100)).containsExactlyInAnyOrder("scan:a", "scan:b");
    }

    /** SCAN 返回数量受上限约束。 */
    @Test
    void scanKeysHonorsMaxKeys() {
        // given
        provider.set("cap:a", "1", null);
        provider.set("cap:b", "2", null);
        provider.set("cap:c", "3", null);

        // when
        assertThat(provider.scanKeys("cap:*", 100, 2)).hasSize(2);
    }
}
