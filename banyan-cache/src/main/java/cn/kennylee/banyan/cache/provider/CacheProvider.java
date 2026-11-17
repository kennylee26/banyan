package cn.kennylee.banyan.cache.provider;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 缓存操作 Provider：只收发 String，屏蔽具体 Redis 客户端。
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface CacheProvider {

    /**
     * 读取字符串值。
     *
     * @param key 完整 key
     * @return 值；不存在时返回 null
     */
    String get(String key);

    /**
     * 写入字符串值。
     *
     * @param key   完整 key
     * @param value 值
     * @param ttl   过期时间，null 或非正数表示不过期
     */
    void set(String key, String value, Duration ttl);

    /**
     * 删除 key。
     *
     * @param key 完整 key
     * @return 是否删除成功
     */
    boolean delete(String key);

    /**
     * 判断 key 是否存在。
     *
     * @param key 完整 key
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 设置过期时间。
     *
     * @param key 完整 key
     * @param ttl 过期时间
     * @return 是否设置成功
     */
    boolean expire(String key, Duration ttl);

    /**
     * 在剩余过期时间上追加时长。
     *
     * @param key 完整 key
     * @param ttl 追加时长
     * @return 是否追加成功
     */
    boolean addExpireTime(String key, Duration ttl);

    /**
     * 批量读取。
     *
     * @param keys 完整 key 集合
     * @return 与入参顺序一致的值列表，缺失元素为 null
     */
    List<String> batchGet(Collection<String> keys);

    /**
     * 批量写入。
     *
     * @param values key 与值的映射
     * @param ttl    过期时间，null 或非正数表示不过期
     */
    void batchSet(Map<String, String> values, Duration ttl);

    /**
     * 批量删除。
     *
     * @param keys 完整 key 集合
     * @return 实际删除数量
     */
    long batchDelete(Collection<String> keys);

    /**
     * 执行 Lua 脚本。
     *
     * <p>本库内置脚本都返回整数；参数在交给 Redis 前统一转成字符串，
     * 实现方只需按字符串传递脚本参数。不支持脚本时抛
     * {@link UnsupportedOperationException}。</p>
     *
     * @param script 脚本内容
     * @param keys   脚本 keys
     * @param args   脚本参数，按字符串传入
     * @return 脚本返回值；整数脚本返回 {@link Long}，key 不存在时可能为 null
     */
    Object execute(String script, List<String> keys, Object... args);

    /**
     * 尝试获取锁。
     *
     * @param key   锁 key
     * @param token 持有者 token
     * @param ttl   锁 TTL
     * @return 是否获取成功
     */
    boolean tryLock(String key, String token, Duration ttl);

    /**
     * 释放锁：只有 token 匹配才删除。
     *
     * @param key   锁 key
     * @param token 持有者 token
     * @return 是否释放成功
     */
    boolean releaseLock(String key, String token);

    /**
     * 自增。
     *
     * @param key   完整 key
     * @param delta 增量
     * @return 自增后的值
     */
    long increment(String key, long delta);

    /**
     * 自减。
     *
     * @param key   完整 key
     * @param delta 减量
     * @return 自减后的值
     */
    long decrement(String key, long delta);

    /**
     * hash 写入。
     *
     * @param key   完整 key
     * @param field 字段
     * @param value 值
     */
    void hashPut(String key, String field, String value);

    /**
     * hash 读取。
     *
     * @param key   完整 key
     * @param field 字段
     * @return 值；不存在时返回 null
     */
    String hashGet(String key, String field);

    /**
     * hash 删除字段。
     *
     * @param key   完整 key
     * @param field 字段
     */
    void hashDelete(String key, String field);

    /**
     * hash 读取全部字段。
     *
     * @param key 完整 key
     * @return 字段与值映射
     */
    Map<String, String> hashGetAll(String key);

    /**
     * list 追加。
     *
     * @param key   完整 key
     * @param value 值
     * @return 列表长度
     */
    long listPush(String key, String value);

    /**
     * list 范围读取。
     *
     * @param key   完整 key
     * @param start 起始下标
     * @param end   结束下标
     * @return 元素列表
     */
    List<String> listRange(String key, long start, long end);

    /**
     * list 长度。
     *
     * @param key 完整 key
     * @return 长度
     */
    long listSize(String key);

    /**
     * set 添加。
     *
     * @param key    完整 key
     * @param values 值
     * @return 新增数量
     */
    long setAdd(String key, String... values);

    /**
     * set 读取全部成员。
     *
     * @param key 完整 key
     * @return 成员集合
     */
    Set<String> setMembers(String key);

    /**
     * set 移除。
     *
     * @param key    完整 key
     * @param values 值
     * @return 移除数量
     */
    long setRemove(String key, String... values);

    /**
     * zset 添加。
     *
     * @param key    完整 key
     * @param member 成员
     * @param score  分值
     * @return 新增数量
     */
    long zsetAdd(String key, String member, double score);

    /**
     * zset 按分值范围读取。
     *
     * @param key   完整 key
     * @param start 起始下标
     * @param end   结束下标
     * @return 成员集合
     */
    Set<String> zsetRange(String key, long start, long end);

    /**
     * 按 pattern 扫描 key，结果最多 {@code maxKeys} 个。
     *
     * @param pattern 匹配模式
     * @param count   单批扫描数量
     * @param maxKeys 最多返回的 key 数量
     * @return 匹配的 key 列表
     */
    List<String> scanKeys(String pattern, long count, int maxKeys);

    /**
     * 按 pattern 扫描 key，不限制返回数量。
     *
     * @param pattern 匹配模式
     * @param count   单批扫描数量
     * @return 匹配的 key 列表
     */
    default List<String> scanKeys(String pattern, long count) {
        return scanKeys(pattern, count, Integer.MAX_VALUE);
    }
}
