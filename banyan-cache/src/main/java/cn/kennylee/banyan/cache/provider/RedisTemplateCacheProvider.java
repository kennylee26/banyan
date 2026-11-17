package cn.kennylee.banyan.cache.provider;

import cn.kennylee.banyan.cache.script.CacheScripts;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 基于 {@link StringRedisTemplate} 的默认 Provider：只收发 String，Redis 中存放可读文本。
 *
 * @author kennylee
 * @since 0.1.0
 */
public class RedisTemplateCacheProvider extends AbstractCacheProvider {

    /** Redis 返回的剩余 TTL：key 不存在。 */
    private static final long TTL_KEY_NOT_EXIST = -2L;

    /** Redis 返回的剩余 TTL：key 存在但没有过期时间。 */
    private static final long TTL_NO_EXPIRE = -1L;

    /** Redis 模板。 */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造实例。
     *
     * @param stringRedisTemplate 字符串 Redis 模板
     */
    public RedisTemplateCacheProvider(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            stringRedisTemplate.opsForValue().set(key, value);
        } else {
            stringRedisTemplate.opsForValue().set(key, value, ttl);
        }
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        return Boolean.TRUE.equals(stringRedisTemplate.expire(key, ttl));
    }

    /**
     * 追加到剩余 TTL 上。key 不存在或没有设置过期时间时返回 false，不会给持久 key 凭空加 TTL。
     *
     * @param key 完整 key
     * @param ttl 追加时长
     * @return 是否追加成功
     */
    @Override
    public boolean addExpireTime(String key, Duration ttl) {
        Long current = stringRedisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
        if (current == null || current == TTL_KEY_NOT_EXIST || current == TTL_NO_EXPIRE) {
            return false;
        }
        return Boolean.TRUE.equals(
                stringRedisTemplate.expire(key, Duration.ofMillis(current + ttl.toMillis())));
    }

    @Override
    public List<String> batchGet(Collection<String> keys) {
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
        return values == null ? new ArrayList<>() : values;
    }

    @Override
    public void batchSet(Map<String, String> values, Duration ttl) {
        stringRedisTemplate.opsForValue().multiSet(values);
        if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
            values.keySet().forEach(key -> stringRedisTemplate.expire(key, ttl));
        }
    }

    @Override
    public long batchDelete(Collection<String> keys) {
        Long deleted = stringRedisTemplate.delete(keys);
        return deleted == null ? 0 : deleted;
    }

    @Override
    public Object execute(String script, List<String> keys, Object... args) {
        // 本库内置脚本均返回整数；Lettuce 对 Object 结果类型的整数返回不支持
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        // StringRedisTemplate 的参数序列化器只接受 String，统一转成字符串
        Object[] stringArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            stringArgs[i] = args[i] == null ? null : String.valueOf(args[i]);
        }
        return stringRedisTemplate.execute(redisScript, keys, stringArgs);
    }

    @Override
    public boolean tryLock(String key, String token, Duration ttl) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, token, ttl));
    }

    @Override
    public boolean releaseLock(String key, String token) {
        Object released = execute(CacheScripts.RELEASE_LOCK, List.of(key), token);
        return released instanceof Number number && number.longValue() > 0;
    }

    @Override
    public long increment(String key, long delta) {
        return nullSafe(stringRedisTemplate.opsForValue().increment(key, delta));
    }

    @Override
    public long decrement(String key, long delta) {
        return nullSafe(stringRedisTemplate.opsForValue().increment(key, -delta));
    }

    @Override
    public void hashPut(String key, String field, String value) {
        stringRedisTemplate.opsForHash().put(key, field, value);
    }

    @Override
    public String hashGet(String key, String field) {
        Object value = stringRedisTemplate.opsForHash().get(key, field);
        return value == null ? null : String.valueOf(value);
    }

    @Override
    public void hashDelete(String key, String field) {
        stringRedisTemplate.opsForHash().delete(key, field);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> hashGetAll(String key) {
        return (Map<String, String>) (Map<?, ?>) stringRedisTemplate.opsForHash().entries(key);
    }

    @Override
    public long listPush(String key, String value) {
        return nullSafe(stringRedisTemplate.opsForList().rightPush(key, value));
    }

    @Override
    public List<String> listRange(String key, long start, long end) {
        List<String> values = stringRedisTemplate.opsForList().range(key, start, end);
        return values == null ? new ArrayList<>() : values;
    }

    @Override
    public long listSize(String key) {
        return nullSafe(stringRedisTemplate.opsForList().size(key));
    }

    @Override
    public long setAdd(String key, String... values) {
        return nullSafe(stringRedisTemplate.opsForSet().add(key, values));
    }

    @Override
    public Set<String> setMembers(String key) {
        Set<String> members = stringRedisTemplate.opsForSet().members(key);
        return members == null ? Set.of() : members;
    }

    @Override
    public long setRemove(String key, String... values) {
        return nullSafe(stringRedisTemplate.opsForSet().remove(key, (Object[]) values));
    }

    @Override
    public long zsetAdd(String key, String member, double score) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForZSet().add(key, member, score)) ? 1 : 0;
    }

    @Override
    public Set<String> zsetRange(String key, long start, long end) {
        Set<String> members = stringRedisTemplate.opsForZSet().range(key, start, end);
        return members == null ? Set.of() : members;
    }

    @Override
    public List<String> scanKeys(String pattern, long count, int maxKeys) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(count).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext() && keys.size() < maxKeys) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    /**
     * 未初始化连接时返回 0。
     *
     * @param value Redis 返回值
     * @return 非空值
     */
    private long nullSafe(Long value) {
        return Objects.requireNonNullElse(value, 0L);
    }

}
