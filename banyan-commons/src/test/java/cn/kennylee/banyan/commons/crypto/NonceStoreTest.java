package cn.kennylee.banyan.commons.crypto;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内存 nonce 存储测试：去重、过期清理与并发安全。
 *
 * @author kennylee
 * @since 0.1.0
 */
class NonceStoreTest {

    /** 窗口 TTL。 */
    private static final long TTL = 300_000L;

    private final MutableClock clock = new MutableClock(Instant.ofEpochMilli(1700000000000L));

    private final InMemoryNonceStore store = new InMemoryNonceStore(clock);

    /** 同一 nonce 第二次被拒。 */
    @Test
    void rejectsDuplicateNonce() {
        // when
        assertTrue(store.putIfAbsent("app1:abc", TTL));
        assertFalse(store.putIfAbsent("app1:abc", TTL));
    }

    /** 不同 nonce 互不影响。 */
    @Test
    void acceptsDifferentNonces() {
        // when
        assertTrue(store.putIfAbsent("app1:abc", TTL));
        assertTrue(store.putIfAbsent("app1:def", TTL));
    }

    /** 过期后同一 nonce 可再次使用。 */
    @Test
    void expiresEntries() {
        // when
        assertTrue(store.putIfAbsent("app1:abc", TTL));

        clock.advance(Duration.ofMillis(TTL + 1));

        assertTrue(store.putIfAbsent("app1:abc", TTL));
    }

    /** 恰好到达 TTL 即视为过期。 */
    @Test
    void expiresExactlyAtTtl() {
        // when
        assertTrue(store.putIfAbsent("app1:edge", TTL));

        clock.advance(Duration.ofMillis(TTL));

        assertTrue(store.putIfAbsent("app1:edge", TTL));
    }

    /** 并发提交同一 nonce 至多一个成功。 */
    @Test
    void allowsOnlyOneConcurrentWinner() throws InterruptedException {
        // given
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger winners = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        // when
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        if (store.putIfAbsent("app1:race", TTL)) {
                            winners.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        // then
        assertEquals(1, winners.get());
    }
}
