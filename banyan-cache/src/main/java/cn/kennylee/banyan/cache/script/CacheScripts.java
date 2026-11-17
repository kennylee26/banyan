package cn.kennylee.banyan.cache.script;

/**
 * 缓存 Lua 脚本常量：集中管理，避免散落在业务方法里。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class CacheScripts {

    /** 释放锁：只有 token 匹配才删除。KEYS[1]=锁 key，ARGV[1]=token。 */
    public static final String RELEASE_LOCK = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """;

    /** 自增并判等：ARGV[1]=增量，ARGV[2]=TTL 毫秒（<=0 不过期），ARGV[3]=期望值。 */
    public static final String CHECK_AND_INCREMENT = """
            local value = redis.call('incrby', KEYS[1], ARGV[1])
            if tonumber(ARGV[2]) > 0 then
                redis.call('pexpire', KEYS[1], ARGV[2])
            end
            if value == tonumber(ARGV[3]) then
                return 1
            end
            return 0
            """;

    private CacheScripts() {
    }
}
