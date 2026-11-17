package cn.kennylee.banyan.cache.service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 统一缓存服务：一套 value 主 API + 类型化结构方法族，所有 key 都带组名。
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface CacheService {

    /**
     * 写入对象（JSON 序列化）。
     *
     * @param group 组名，可为空
     * @param key   业务 key
     * @param value 对象；为 null 时删除
     * @param ttl   过期时间，null 或非正数表示不过期
     * @param <T>   对象类型
     */
    <T> void put(String group, String key, T value, Duration ttl);

    /**
     * 读取对象。
     *
     * @param group 组名，可为空
     * @param key   业务 key
     * @param type  对象类型
     * @param <T>   对象类型
     * @return 对象；不存在时返回 null
     */
    <T> T get(String group, String key, Class<T> type);

    /**
     * 写入原始字符串（不参与 JSON 编解码）。
     *
     * <p>与 {@link #put} / {@link #get} 不可混用于同一 key：类型标记开启时会抛出明确异常，
     * 关闭后由调用方自行保证格式一致。</p>
     *
     * @param group 组名，可为空
     * @param key   业务 key
     * @param value 原始字符串；为 null 时删除
     * @param ttl   过期时间
     */
    void putRaw(String group, String key, String value, Duration ttl);

    /**
     * 读取原始字符串。
     *
     * @param group 组名，可为空
     * @param key   业务 key
     * @return 原始字符串；不存在时返回 null
     */
    String getRaw(String group, String key);

    /**
     * 删除。
     *
     * @param group 组名
     * @param key   业务 key
     * @return 是否删除成功
     */
    boolean delete(String group, String key);

    /**
     * 判断存在。
     *
     * @param group 组名
     * @param key   业务 key
     * @return 是否存在
     */
    boolean exists(String group, String key);

    /**
     * 设置过期时间。
     *
     * @param group 组名
     * @param key   业务 key
     * @param ttl   过期时间，必须非空
     * @return 是否设置成功
     * @throws IllegalArgumentException ttl 为 null
     */
    boolean expire(String group, String key, Duration ttl);

    /**
     * 在剩余过期时间上追加时长。
     *
     * @param group 组名
     * @param key   业务 key
     * @param ttl   追加时长，必须非空
     * @return 是否追加成功；key 不存在或没有过期时间时返回 false
     * @throws IllegalArgumentException ttl 为 null
     */
    boolean addExpireTime(String group, String key, Duration ttl);

    /**
     * 批量读取对象。
     *
     * @param group 组名
     * @param keys  业务 key 集合
     * @param type  对象类型
     * @param <T>   对象类型
     * @return 与入参顺序一致的结果，缺失元素为 null
     */
    <T> List<T> batchGet(String group, Collection<String> keys, Class<T> type);

    /**
     * 批量写入对象。
     *
     * @param group  组名
     * @param values 业务 key 与对象映射
     * @param ttl    过期时间
     * @param <T>    对象类型
     */
    <T> void batchPut(String group, Map<String, T> values, Duration ttl);

    /**
     * 批量删除。
     *
     * @param group 组名
     * @param keys  业务 key 集合
     * @return 实际删除数量
     */
    long batchDelete(String group, Collection<String> keys);

    /**
     * 读缓存，未命中时加载并回写。
     *
     * <p>读写开关关闭时直接执行 {@code loader} 并返回其结果，不访问缓存。</p>
     *
     * @param group  组名
     * @param key    业务 key
     * @param type   对象类型
     * @param loader 加载器
     * @param ttl    回写过期时间
     * @param <T>    对象类型
     * @return 对象
     */
    <T> T getOrLoad(String group, String key, Class<T> type, Supplier<T> loader, Duration ttl);

    /**
     * 自增。
     *
     * @param group 组名
     * @param key   业务 key
     * @param delta 增量
     * @return 自增后的值
     */
    long increment(String group, String key, long delta);

    /**
     * 自减。
     *
     * @param group 组名
     * @param key   业务 key
     * @param delta 减量
     * @return 自减后的值
     */
    long decrement(String group, String key, long delta);

    /**
     * 原子自增并判断是否等于期望值。
     *
     * @param group    组名
     * @param key      业务 key
     * @param delta    增量
     * @param expected 期望值
     * @param ttl      计数键过期时间，可为 null
     * @return 自增后是否等于期望值
     */
    boolean checkAndIncrement(String group, String key, long delta, long expected, Duration ttl);

    /**
     * 原子自增并判断是否等于期望值，计数键不过期。
     *
     * @param group    组名
     * @param key      业务 key
     * @param delta    增量
     * @param expected 期望值
     * @return 自增后是否等于期望值
     */
    default boolean checkAndIncrement(String group, String key, long delta, long expected) {
        return checkAndIncrement(group, key, delta, expected, null);
    }

    /**
     * hash 写入。
     *
     * @param group 组名
     * @param key   业务 key
     * @param field 字段
     * @param value 值
     */
    void hashPut(String group, String key, String field, String value);

    /**
     * hash 读取。
     *
     * @param group 组名
     * @param key   业务 key
     * @param field 字段
     * @return 值；不存在时返回 null
     */
    String hashGet(String group, String key, String field);

    /**
     * hash 删除字段。
     *
     * @param group 组名
     * @param key   业务 key
     * @param field 字段
     */
    void hashDelete(String group, String key, String field);

    /**
     * hash 读取全部。
     *
     * @param group 组名
     * @param key   业务 key
     * @return 字段与值映射
     */
    Map<String, String> hashGetAll(String group, String key);

    /**
     * list 追加。
     *
     * @param group 组名
     * @param key   业务 key
     * @param value 值
     * @return 列表长度
     */
    long listPush(String group, String key, String value);

    /**
     * list 范围读取。
     *
     * @param group 组名
     * @param key   业务 key
     * @param start 起始下标
     * @param end   结束下标
     * @return 元素列表
     */
    List<String> listRange(String group, String key, long start, long end);

    /**
     * list 长度。
     *
     * @param group 组名
     * @param key   业务 key
     * @return 长度
     */
    long listSize(String group, String key);

    /**
     * set 添加。
     *
     * @param group  组名
     * @param key    业务 key
     * @param values 值
     * @return 新增数量
     */
    long setAdd(String group, String key, String... values);

    /**
     * set 读取成员。
     *
     * @param group 组名
     * @param key   业务 key
     * @return 成员集合
     */
    Set<String> setMembers(String group, String key);

    /**
     * set 移除。
     *
     * @param group  组名
     * @param key    业务 key
     * @param values 值
     * @return 移除数量
     */
    long setRemove(String group, String key, String... values);

    /**
     * zset 添加。
     *
     * @param group  组名
     * @param key    业务 key
     * @param member 成员
     * @param score  分值
     * @return 新增数量
     */
    long zsetAdd(String group, String key, String member, double score);

    /**
     * zset 范围读取。
     *
     * @param group 组名
     * @param key   业务 key
     * @param start 起始下标
     * @param end   结束下标
     * @return 成员集合
     */
    Set<String> zsetRange(String group, String key, long start, long end);

    /**
     * 带锁删除：获取锁失败时不删除并输出 WARN。
     *
     * <p>锁为 Redis 原生 {@code SET NX PX}，<b>非可重入、无看门狗续期</b>，
     * 只用于删除缓存这类短临界区；锁 TTL 由 {@code banyan.cache.lock.ttl} 控制（默认 5 秒），
     * 临界区耗时必须小于该 TTL。</p>
     *
     * @param group 组名
     * @param key   业务 key
     * @return 是否删除成功
     */
    boolean deleteWithLock(String group, String key);

    /**
     * 按 pattern 批量删除，pattern 必须落在当前命名空间内。
     *
     * <p>{@code keyPattern} 相对于组命名空间，例如 {@code u*} 会拼成
     * {@code appName:group:u*}；绝对 pattern 若不在当前命名空间内会被拒绝并输出 WARN。</p>
     *
     * @param group      组名
     * @param keyPattern 业务 key 的匹配模式
     * @return 实际删除数量
     */
    long batchDeleteByPattern(String group, String keyPattern);
}
