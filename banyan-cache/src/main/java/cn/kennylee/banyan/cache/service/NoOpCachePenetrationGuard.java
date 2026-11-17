package cn.kennylee.banyan.cache.service;

import java.util.function.Supplier;

/**
 * 穿透防护占位实现：直接调用加载器，不做任何额外处理。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class NoOpCachePenetrationGuard implements CachePenetrationGuard {

    @Override
    public <T> T load(String cacheKey, Supplier<T> loader) {
        return loader.get();
    }
}
