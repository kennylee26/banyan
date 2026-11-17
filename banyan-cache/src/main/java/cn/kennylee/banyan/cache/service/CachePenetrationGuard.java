package cn.kennylee.banyan.cache.service;

import java.util.function.Supplier;

/**
 * 缓存穿透防护扩展点：默认 NoOp，使用方可提供真实实现。
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface CachePenetrationGuard {

    /**
     * 在缓存未命中时加载数据。
     *
     * @param cacheKey 完整缓存 key
     * @param loader   数据加载器
     * @param <T>      数据类型
     * @return 加载结果，可为 null
     */
    <T> T load(String cacheKey, Supplier<T> loader);
}
