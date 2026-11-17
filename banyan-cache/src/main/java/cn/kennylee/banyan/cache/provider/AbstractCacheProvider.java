package cn.kennylee.banyan.cache.provider;

import cn.kennylee.banyan.commons.error.ErrorCode;
import cn.kennylee.banyan.commons.error.Messages;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provider 默认适配器：未实现的操作用明确异常拒绝，使用方按需覆写。
 *
 * @author kennylee
 * @since 0.1.0
 */
public abstract class AbstractCacheProvider implements CacheProvider {

    /**
     * 构造不支持异常。
     *
     * @param operation 操作名
     * @return 异常
     */
    protected UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException(
                Messages.get(ErrorCode.CACHE_PROVIDER_UNSUPPORTED, operation));
    }

    @Override
    public String get(String key) {
        throw unsupported("get");
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        throw unsupported("set");
    }

    @Override
    public boolean delete(String key) {
        throw unsupported("delete");
    }

    @Override
    public boolean exists(String key) {
        throw unsupported("exists");
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        throw unsupported("expire");
    }

    @Override
    public boolean addExpireTime(String key, Duration ttl) {
        throw unsupported("addExpireTime");
    }

    @Override
    public List<String> batchGet(Collection<String> keys) {
        throw unsupported("batchGet");
    }

    @Override
    public void batchSet(Map<String, String> values, Duration ttl) {
        throw unsupported("batchSet");
    }

    @Override
    public long batchDelete(Collection<String> keys) {
        throw unsupported("batchDelete");
    }

    @Override
    public Object execute(String script, List<String> keys, Object... args) {
        throw unsupported("execute");
    }

    @Override
    public boolean tryLock(String key, String token, Duration ttl) {
        throw unsupported("tryLock");
    }

    @Override
    public boolean releaseLock(String key, String token) {
        throw unsupported("releaseLock");
    }

    @Override
    public long increment(String key, long delta) {
        throw unsupported("increment");
    }

    @Override
    public long decrement(String key, long delta) {
        throw unsupported("decrement");
    }

    @Override
    public void hashPut(String key, String field, String value) {
        throw unsupported("hashPut");
    }

    @Override
    public String hashGet(String key, String field) {
        throw unsupported("hashGet");
    }

    @Override
    public void hashDelete(String key, String field) {
        throw unsupported("hashDelete");
    }

    @Override
    public Map<String, String> hashGetAll(String key) {
        throw unsupported("hashGetAll");
    }

    @Override
    public long listPush(String key, String value) {
        throw unsupported("listPush");
    }

    @Override
    public List<String> listRange(String key, long start, long end) {
        throw unsupported("listRange");
    }

    @Override
    public long listSize(String key) {
        throw unsupported("listSize");
    }

    @Override
    public long setAdd(String key, String... values) {
        throw unsupported("setAdd");
    }

    @Override
    public Set<String> setMembers(String key) {
        throw unsupported("setMembers");
    }

    @Override
    public long setRemove(String key, String... values) {
        throw unsupported("setRemove");
    }

    @Override
    public long zsetAdd(String key, String member, double score) {
        throw unsupported("zsetAdd");
    }

    @Override
    public Set<String> zsetRange(String key, long start, long end) {
        throw unsupported("zsetRange");
    }

    @Override
    public List<String> scanKeys(String pattern, long count, int maxKeys) {
        throw unsupported("scanKeys");
    }
}
