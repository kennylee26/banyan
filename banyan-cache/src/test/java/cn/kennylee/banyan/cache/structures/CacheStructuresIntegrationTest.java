package cn.kennylee.banyan.cache.structures;

import cn.kennylee.banyan.cache.CacheIntegrationTestBase;
import cn.kennylee.banyan.cache.CacheTestApplication;
import cn.kennylee.banyan.cache.autoconfigure.CacheProperties;
import cn.kennylee.banyan.cache.provider.AbstractCacheProvider;
import cn.kennylee.banyan.cache.provider.CacheProvider;
import cn.kennylee.banyan.cache.provider.RedisTemplateCacheProvider;
import cn.kennylee.banyan.cache.service.CacheService;
import cn.kennylee.banyan.cache.service.CacheServiceImpl;
import cn.kennylee.banyan.cache.service.NoOpCachePenetrationGuard;
import cn.kennylee.banyan.cache.support.CacheKeys;
import cn.kennylee.banyan.commons.spring.json.JacksonJsonCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 结构、计数、锁与 SCAN 集成测试。
 *
 * @author kennylee
 * @since 0.1.0
 */
@SpringBootTest(classes = CacheTestApplication.class)
@ExtendWith(OutputCaptureExtension.class)
class CacheStructuresIntegrationTest extends CacheIntegrationTestBase {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private CacheProvider provider;

    private CacheProperties properties;

    private CacheService service;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        provider = new RedisTemplateCacheProvider(stringRedisTemplate);
        properties = new CacheProperties();
        service = new CacheServiceImpl(provider, new CacheKeys("banyan-test", 512),
                properties, new JacksonJsonCodec(), new NoOpCachePenetrationGuard());
    }

    /** 结构与计数。 */
    @Test
    void supportsStructuresAndCounters() {
        assertThat(service.increment("order", "c1", 2)).isEqualTo(2);
        assertThat(service.decrement("order", "c1", 1)).isEqualTo(1);

        service.hashPut("order", "h1", "f1", "v1");
        assertThat(service.hashGet("order", "h1", "f1")).isEqualTo("v1");
        assertThat(service.hashGetAll("order", "h1")).containsEntry("f1", "v1");

        service.listPush("order", "l1", "a");
        service.listPush("order", "l1", "b");
        assertThat(service.listRange("order", "l1", 0, -1)).containsExactly("a", "b");
        assertThat(service.listSize("order", "l1")).isEqualTo(2);

        service.setAdd("order", "s1", "x", "y");
        assertThat(service.setMembers("order", "s1")).containsExactlyInAnyOrder("x", "y");
        assertThat(service.setRemove("order", "s1", "x")).isEqualTo(1);

        service.zsetAdd("order", "z1", "m1", 1.0);
        assertThat(service.zsetRange("order", "z1", 0, -1)).containsExactly("m1");
    }

    /** 原子自增判等。 */
    @Test
    void checkAndIncrementIsAtomic() throws Exception {
        int threads = 4;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger matched = new AtomicInteger();
        AtomicInteger threadSeq = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads,
                runnable -> new Thread(runnable, "cache-check-incr-" + threadSeq.incrementAndGet()));
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        if (service.checkAndIncrement("order", "hit", 1, 2, Duration.ofMinutes(1))) {
                            matched.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(matched).hasValue(1);
    }

    /** 释放锁失败不覆盖删除结果。 */
    @Test
    void releaseFailureKeepsDeleteResult(CapturedOutput output) {
        CacheService brokenRelease = new CacheServiceImpl(new AbstractCacheProvider() {

            @Override
            public boolean tryLock(String key, String token, Duration ttl) {
                return true;
            }

            @Override
            public boolean delete(String key) {
                return true;
            }

            @Override
            public boolean releaseLock(String key, String token) {
                throw new UnsupportedOperationException("no lua");
            }
        }, new CacheKeys("banyan-test", 512), properties, new JacksonJsonCodec(),
                new NoOpCachePenetrationGuard());

        assertThat(brokenRelease.deleteWithLock("order", "k1")).isTrue();
        assertThat(output).contains("banyan-test:order:k1:lock");
    }

    /** 带锁删除只认自己的锁。 */
    @Test
    void deletesWithLockOnlyWhenLockAcquired() {
        service.putRaw("order", "target", "v", Duration.ofMinutes(1));
        String lockKey = "banyan-test:order:target:lock";
        assertThat(provider.tryLock(lockKey, "other-owner", Duration.ofSeconds(30))).isTrue();

        assertThat(service.deleteWithLock("order", "target")).isFalse();
        assertThat(service.exists("order", "target")).isTrue();

        provider.releaseLock(lockKey, "other-owner");
        assertThat(service.deleteWithLock("order", "target")).isTrue();
        assertThat(service.exists("order", "target")).isFalse();
    }

    /** SCAN 批量删除：全库 pattern 被拒，带组前缀可删。 */
    @Test
    void batchDeleteByPatternStaysInsideNamespace() {
        service.putRaw("order", "u1", "a", null);
        service.putRaw("order", "u2", "b", null);

        assertThat(service.batchDeleteByPattern("order", "*")).isZero();
        assertThat(service.batchDeleteByPattern("order", "banyan-test:other:u*")).isZero();
        assertThat(service.exists("order", "u1")).isTrue();

        assertThat(service.batchDeleteByPattern("order", "u*")).isEqualTo(2);
    }

    /** pattern 合法性的各个分支都被拒绝且不访问 Redis。 */
    @Test
    void rejectsInvalidPatternsWithoutDeleting() {
        service.putRaw("order", "u1", "a", null);

        assertThat(service.batchDeleteByPattern("order", null)).isZero();
        assertThat(service.batchDeleteByPattern("order", "  ")).isZero();
        assertThat(service.batchDeleteByPattern("order", "*")).isZero();
        assertThat(service.batchDeleteByPattern("order", "banyan-test:other:u*")).isZero();
        assertThat(service.exists("order", "u1")).isTrue();
    }

    /** 无 appName 且 group 为空时命名空间为空，pattern 被拒。 */
    @Test
    void rejectsPatternWhenNamespaceEmpty() {
        CacheService noNamespace = new CacheServiceImpl(provider, new CacheKeys(null, 512),
                properties, new JacksonJsonCodec(), new NoOpCachePenetrationGuard());

        assertThat(noNamespace.batchDeleteByPattern("", "u*")).isZero();
    }

    /** 读写开关关闭时不扫描也不删除。 */
    @Test
    void skipsPatternDeletionWhenReadWriteDisabled() {
        properties.setReadWriteEnabled(false);
        service.putRaw("order", "u1", "a", null);

        assertThat(service.batchDeleteByPattern("order", "u*")).isZero();
    }

    /** 多批删除：匹配数超过单批大小时按批累加删除数。 */
    @Test
    void deletesInMultipleBatches() {
        properties.getScan().setBatchSize(2);
        for (int i = 1; i <= 5; i++) {
            service.putRaw("order", "b" + i, "v", null);
        }

        assertThat(service.batchDeleteByPattern("order", "b*")).isEqualTo(5);
    }

    /** 删除上限生效。 */
    @Test
    void batchDeleteHonorsMaxDeletions() {
        properties.getScan().setMaxDeletions(1);
        service.putRaw("order", "x1", "a", null);
        service.putRaw("order", "x2", "b", null);

        assertThat(service.batchDeleteByPattern("order", "x*")).isEqualTo(1);
    }

    /** Provider 不支持 Lua 时 checkAndIncrement 降级为 false 并 WARN。 */
    @Test
    void unsupportedLuaDegradesWithWarn(CapturedOutput output) {
        CacheService noLua = new CacheServiceImpl(new AbstractCacheProvider() {
        }, new CacheKeys("banyan-test", 512), properties, new JacksonJsonCodec(),
                new NoOpCachePenetrationGuard());

        assertThat(noLua.checkAndIncrement("order", "x", 1, 1, null)).isFalse();
        assertThat(output).contains("checkAndIncrement");
    }
}
