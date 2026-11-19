package cn.kennylee.banyan.commons.crypto;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 纯 JDK 的内存 nonce 存储：单实例开箱可用，采用惰性清理——每次写入前移除已过期条目，
 * 没有后续写入时过期条目会暂时保留到下一次写入。
 *
 * <p>多实例部署时各实例内存不共享，必须由使用方提供分布式 {@link NonceStore} 实现。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public class InMemoryNonceStore implements NonceStore {

    /** 最小存活时间，避免 TTL 为 0 时立即过期。 */
    private static final long MIN_TTL_MILLIS = 1L;

    /** nonce → 过期时间（毫秒）。 */
    private final ConcurrentHashMap<String, Long> entries = new ConcurrentHashMap<>();

    /** 时钟。 */
    private final Clock clock;

    /**
     * 使用系统时钟构造实例。
     */
    public InMemoryNonceStore() {
        this(Clock.systemUTC());
    }

    /**
     * 构造实例。
     *
     * @param clock 时钟
     */
    public InMemoryNonceStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean putIfAbsent(String nonceKey, long ttlMillis) {
        long now = clock.millis();
        purgeExpired(now);
        long expiresAt = now + Math.max(MIN_TTL_MILLIS, ttlMillis);
        return entries.putIfAbsent(nonceKey, expiresAt) == null;
    }

    /** 清理已过期条目。 */
    private void purgeExpired(long now) {
        entries.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
