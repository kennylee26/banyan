package cn.kennylee.banyan.cache.service;

import cn.kennylee.banyan.cache.autoconfigure.CacheProperties;
import cn.kennylee.banyan.cache.provider.CacheProvider;
import cn.kennylee.banyan.cache.script.CacheScripts;
import cn.kennylee.banyan.cache.support.CacheKeys;
import cn.kennylee.banyan.cache.support.CacheValueMarker;
import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;
import cn.kennylee.banyan.commons.spring.json.JsonCodec;
import cn.kennylee.banyan.commons.util.Strings;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 默认缓存服务：负责 key 拼接、类型标记、JSON 编解码、读写开关与日志边界。
 *
 * @author kennylee
 * @since 0.1.0
 */
@Slf4j
public class DefaultCacheService implements CacheService {

    /** 删缓存锁 key 后缀。 */
    private static final String LOCK_KEY_SUFFIX = ":lock";

    /** 匹配全部 key 的通配符。 */
    private static final String WILDCARD_ALL = "*";

    /** key 分隔符。 */
    private static final String SEPARATOR = ":";

    /** 计数脚本约定：TTL 毫秒数不大于 0 表示不过期。 */
    private static final long NO_TTL_MILLIS = -1L;

    /** 缓存 Provider。 */
    private final CacheProvider provider;

    /** key 拼接器。 */
    private final CacheKeys keys;

    /** 配置。 */
    private final CacheProperties properties;

    /** JSON 编解码。 */
    private final JsonCodec jsonCodec;

    /** 穿透防护。 */
    private final CachePenetrationGuard penetrationGuard;

    /**
     * 构造实例。
     *
     * @param provider         缓存 Provider
     * @param keys             key 拼接器
     * @param properties       配置
     * @param jsonCodec        JSON 编解码
     * @param penetrationGuard 穿透防护
     */
    public DefaultCacheService(CacheProvider provider, CacheKeys keys, CacheProperties properties,
                               JsonCodec jsonCodec, CachePenetrationGuard penetrationGuard) {
        this.provider = provider;
        this.keys = keys;
        this.properties = properties;
        this.jsonCodec = jsonCodec;
        this.penetrationGuard = penetrationGuard;
    }

    @Override
    public <T> void put(String group, String key, T value, Duration ttl) {
        if (!isReadWriteEnabled()) {
            return;
        }
        String fullKey = keys.build(group, key);
        if (value == null) {
            provider.delete(fullKey);
            return;
        }
        String json = jsonCodec.toJson(value);
        provider.set(fullKey, CacheValueMarker.json(json, markerEnabled()), ttl);
        logOperation("put", fullKey, false);
    }

    @Override
    public <T> T get(String group, String key, Class<T> type) {
        if (!isReadWriteEnabled()) {
            return null;
        }
        String fullKey = keys.build(group, key);
        String stored = provider.get(fullKey);
        if (stored == null) {
            logOperation("get", fullKey, false);
            return null;
        }
        String json = CacheValueMarker.requireJson(stored, markerEnabled());
        logOperation("get", fullKey, true);
        return jsonCodec.fromJson(json, type);
    }

    @Override
    public void putRaw(String group, String key, String value, Duration ttl) {
        if (!isReadWriteEnabled()) {
            return;
        }
        String fullKey = keys.build(group, key);
        if (value == null) {
            provider.delete(fullKey);
            return;
        }
        provider.set(fullKey, CacheValueMarker.raw(value, markerEnabled()), ttl);
        logOperation("putRaw", fullKey, false);
    }

    @Override
    public String getRaw(String group, String key) {
        if (!isReadWriteEnabled()) {
            return null;
        }
        String fullKey = keys.build(group, key);
        String stored = provider.get(fullKey);
        if (stored == null) {
            logOperation("getRaw", fullKey, false);
            return null;
        }
        logOperation("getRaw", fullKey, true);
        return CacheValueMarker.requireRaw(stored, markerEnabled());
    }

    @Override
    public boolean delete(String group, String key) {
        return whenEnabled(() -> provider.delete(keys.build(group, key)), false);
    }

    @Override
    public boolean exists(String group, String key) {
        return whenEnabled(() -> provider.exists(keys.build(group, key)), false);
    }

    @Override
    public boolean expire(String group, String key, Duration ttl) {
        requireTtl(ttl, "expire");
        return whenEnabled(() -> provider.expire(keys.build(group, key), ttl), false);
    }

    @Override
    public boolean addExpireTime(String group, String key, Duration ttl) {
        requireTtl(ttl, "addExpireTime");
        return whenEnabled(() -> provider.addExpireTime(keys.build(group, key), ttl), false);
    }

    @Override
    public <T> List<T> batchGet(String group, Collection<String> keysInGroup, Class<T> type) {
        if (!isReadWriteEnabled() || keysInGroup == null || keysInGroup.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> fullKeys = new ArrayList<>(keysInGroup.size());
        keysInGroup.forEach(key -> fullKeys.add(keys.build(group, key)));
        List<String> storedValues = provider.batchGet(fullKeys);
        List<T> values = new ArrayList<>(storedValues.size());
        for (String stored : storedValues) {
            if (stored == null) {
                values.add(null);
            } else {
                values.add(jsonCodec.fromJson(
                        CacheValueMarker.requireJson(stored, markerEnabled()), type));
            }
        }
        logBatchOperation("batchGet", fullKeys, storedValues.stream().anyMatch(Objects::nonNull));
        return values;
    }

    @Override
    public <T> void batchPut(String group, Map<String, T> values, Duration ttl) {
        if (!isReadWriteEnabled() || values == null || values.isEmpty()) {
            return;
        }
        Map<String, String> payload = new LinkedHashMap<>(values.size());
        for (Map.Entry<String, T> entry : values.entrySet()) {
            String fullKey = keys.build(group, entry.getKey());
            if (entry.getValue() == null) {
                provider.delete(fullKey);
            } else {
                payload.put(fullKey, CacheValueMarker.json(
                        jsonCodec.toJson(entry.getValue()), markerEnabled()));
            }
        }
        if (!payload.isEmpty()) {
            provider.batchSet(payload, ttl);
        }
        logBatchOperation("batchPut", payload.keySet(), false);
    }

    @Override
    public long batchDelete(String group, Collection<String> keysInGroup) {
        if (!isReadWriteEnabled() || keysInGroup == null || keysInGroup.isEmpty()) {
            return 0;
        }
        List<String> fullKeys = new ArrayList<>(keysInGroup.size());
        keysInGroup.forEach(key -> fullKeys.add(keys.build(group, key)));
        return provider.batchDelete(fullKeys);
    }

    @Override
    public <T> T getOrLoad(String group, String key, Class<T> type, Supplier<T> loader, Duration ttl) {
        if (!isReadWriteEnabled()) {
            return loader.get();
        }
        T cached = get(group, key, type);
        if (cached != null) {
            return cached;
        }
        String fullKey = keys.build(group, key);
        T loaded = penetrationGuard.load(fullKey, loader);
        if (loaded != null) {
            put(group, key, loaded, ttl);
        }
        return loaded;
    }

    @Override
    public long increment(String group, String key, long delta) {
        return whenEnabled(() -> provider.increment(keys.build(group, key), delta), 0L);
    }

    @Override
    public long decrement(String group, String key, long delta) {
        return whenEnabled(() -> provider.decrement(keys.build(group, key), delta), 0L);
    }

    @Override
    public boolean checkAndIncrement(String group, String key, long delta, long expected, Duration ttl) {
        if (!isReadWriteEnabled()) {
            return false;
        }
        long ttlMillis = ttl == null ? NO_TTL_MILLIS : ttl.toMillis();
        try {
            Object result = provider.execute(CacheScripts.CHECK_AND_INCREMENT,
                    List.of(keys.build(group, key)), delta, ttlMillis, expected);
            return result instanceof Number number && number.longValue() == 1L;
        } catch (UnsupportedOperationException e) {
            // Provider 不支持 Lua 属于预期内的能力缺失；只记录降级说明，不打印堆栈以免误导排障
            log.warn(Messages.get(ErrorCode.CACHE_PROVIDER_UNSUPPORTED, "checkAndIncrement"));
            return false;
        }
    }

    @Override
    public void hashPut(String group, String key, String field, String value) {
        whenEnabled(() -> provider.hashPut(keys.build(group, key), field, value));
    }

    @Override
    public String hashGet(String group, String key, String field) {
        return whenEnabled(() -> provider.hashGet(keys.build(group, key), field), null);
    }

    @Override
    public void hashDelete(String group, String key, String field) {
        whenEnabled(() -> provider.hashDelete(keys.build(group, key), field));
    }

    @Override
    public Map<String, String> hashGetAll(String group, String key) {
        return whenEnabled(() -> provider.hashGetAll(keys.build(group, key)), Collections.emptyMap());
    }

    @Override
    public long listPush(String group, String key, String value) {
        return whenEnabled(() -> provider.listPush(keys.build(group, key), value), 0L);
    }

    @Override
    public List<String> listRange(String group, String key, long start, long end) {
        return whenEnabled(() -> provider.listRange(keys.build(group, key), start, end),
                Collections.emptyList());
    }

    @Override
    public long listSize(String group, String key) {
        return whenEnabled(() -> provider.listSize(keys.build(group, key)), 0L);
    }

    @Override
    public long setAdd(String group, String key, String... values) {
        return whenEnabled(() -> provider.setAdd(keys.build(group, key), values), 0L);
    }

    @Override
    public Set<String> setMembers(String group, String key) {
        return whenEnabled(() -> provider.setMembers(keys.build(group, key)), Collections.emptySet());
    }

    @Override
    public long setRemove(String group, String key, String... values) {
        return whenEnabled(() -> provider.setRemove(keys.build(group, key), values), 0L);
    }

    @Override
    public long zsetAdd(String group, String key, String member, double score) {
        return whenEnabled(() -> provider.zsetAdd(keys.build(group, key), member, score), 0L);
    }

    @Override
    public Set<String> zsetRange(String group, String key, long start, long end) {
        return whenEnabled(() -> provider.zsetRange(keys.build(group, key), start, end),
                Collections.emptySet());
    }

    @Override
    public boolean deleteWithLock(String group, String key) {
        if (!isReadWriteEnabled()) {
            return false;
        }
        String fullKey = keys.build(group, key);
        String lockKey = fullKey + LOCK_KEY_SUFFIX;
        String token = UUID.randomUUID().toString().replace("-", "");
        Duration lockTtl = properties.getLock().getTtl();
        if (!provider.tryLock(lockKey, token, lockTtl)) {
            log.warn(Messages.get(ErrorCode.CACHE_LOCK_NOT_ACQUIRED, fullKey));
            return false;
        }
        try {
            return provider.delete(fullKey);
        } finally {
            releaseLockQuietly(lockKey, token);
        }
    }

    @Override
    public long batchDeleteByPattern(String group, String keyPattern) {
        if (!isReadWriteEnabled()) {
            return 0;
        }
        String namespace = keys.namespace(group);
        String pattern = keyPattern == null ? "" : keyPattern.trim();
        if (Strings.isBlank(namespace) || Strings.isBlank(pattern) || WILDCARD_ALL.equals(pattern)
                || (pattern.contains(SEPARATOR) && !pattern.startsWith(namespace))) {
            log.warn(Messages.get(ErrorCode.CACHE_PATTERN_OUTSIDE_NAMESPACE, keyPattern));
            return 0;
        }
        String fullPattern = pattern.startsWith(namespace) ? pattern : namespace + pattern;
        int batchSize = Math.max(1, properties.getScan().getBatchSize());
        List<String> matched = provider.scanKeys(fullPattern, batchSize,
                Math.max(0, properties.getScan().getMaxDeletions()));
        long deleted = 0;
        for (int from = 0; from < matched.size(); from += batchSize) {
            deleted += provider.batchDelete(
                    matched.subList(from, Math.min(from + batchSize, matched.size())));
        }
        return deleted;
    }

    /** 读写开关。 */
    private boolean isReadWriteEnabled() {
        if (!properties.isReadWriteEnabled()) {
            log.debug(Messages.get(ErrorCode.CACHE_READ_WRITE_DISABLED));
            return false;
        }
        return true;
    }

    /** 类型标记开关。 */
    private boolean markerEnabled() {
        return properties.getTypeMarker().isEnabled();
    }

    /**
     * 释放锁失败只告警，避免 finally 抛异常覆盖删除结果；锁会在 TTL 到期后自然释放。
     *
     * @param lockKey 锁 key
     * @param token   持有者 token
     */
    private void releaseLockQuietly(String lockKey, String token) {
        try {
            provider.releaseLock(lockKey, token);
        } catch (RuntimeException e) {
            log.warn(Messages.get(ErrorCode.CACHE_LOCK_RELEASE_FAILED, lockKey), e);
        }
    }

    /** 校验必须非空的 TTL。 */
    private void requireTtl(Duration ttl, String operation) {
        if (ttl == null) {
            throw new IllegalArgumentException(Messages.get(ErrorCode.CACHE_TTL_REQUIRED, operation));
        }
    }

    /** 开关打开时执行并取返回值，关闭时返回明确的空结果。 */
    private <R> R whenEnabled(Supplier<R> action, R emptyResult) {
        return isReadWriteEnabled() ? action.get() : emptyResult;
    }

    /** 开关打开时执行写入。 */
    private void whenEnabled(Runnable action) {
        if (isReadWriteEnabled()) {
            action.run();
        }
    }

    /**
     * 输出不含值的操作日志。
     *
     * @param operation 操作名
     * @param key       完整 key
     * @param hit       是否命中
     */
    private void logOperation(String operation, String key, boolean hit) {
        if (log.isDebugEnabled()) {
            log.debug(Messages.get(ErrorCode.CACHE_OPERATION, operation, key, hit));
        }
    }

    /**
     * 输出批量操作日志：仅在 DEBUG 打开时才拼接 key 列表。
     *
     * @param operation 操作名
     * @param keys      完整 key 集合
     * @param hit       是否命中
     */
    private void logBatchOperation(String operation, Collection<String> keys, boolean hit) {
        if (log.isDebugEnabled()) {
            logOperation(operation, String.join(",", keys), hit);
        }
    }
}
