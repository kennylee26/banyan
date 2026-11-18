package cn.kennylee.banyan.cache.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 统一缓存服务：一套 value 主 API + 类型化结构方法族，所有 key 都带组名。
 *
 * <p>{@code put} 一律把值序列化成 JSON 文本再存（Redis String 本身只存字符串），
 * {@code get} 按调用方给出的类型反序列化回来；{@code putRaw} / {@code getRaw}
 * 是绕过 JSON、直接存取原始字符串的显式逃生口。两套方法不可混用于同一个 key。</p>
 *
 * <p>下方示例中的 {@code BANYAN:JSON:} / {@code BANYAN:RAW:} 前缀是
 * {@code banyan.cache.type-marker.enabled=true}（默认）时的形态；关闭该开关后
 * 写入的是不带前缀的裸值，读取时也不再校验格式。</p>
 *
 * <p>{@code get} 提供 {@code Class} / {@link ParameterizedTypeReference} 两种入口，
 * 并配套 {@code getForList} / {@code getForSet} / {@code getForMap} 容器便捷方法；
 * {@code batchGet} 与 {@code getOrLoad} 同样支持泛型类型。所有入口共用同一套
 * 读写开关与类型标记语义。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public interface CacheService {

    /**
     * 写入对象，统一序列化为 JSON 文本后存储。
     *
     * <p>无论值是 POJO、数字、字符串还是容器，都走同一条 JSON 路径（示例均为
     * 类型标记开启时的形态）：</p>
     *
     * <pre>{@code
     * Duration ttl = Duration.ofMinutes(5);
     * cache.put("cart", "u1", new User("alice", 30), ttl);
     * // Redis 中的值：BANYAN:JSON:{"userName":"alice","age":30}
     *
     * cache.put("order", "count", 123, ttl);
     * // Redis 中的值：BANYAN:JSON:123
     *
     * cache.put("order", "ids", List.of(1, 2), ttl);
     * // Redis 中的值：BANYAN:JSON:[1,2]
     * }</pre>
     *
     * <p>注意：字符串值也会被 JSON 编码（{@code put("g", "k", "hello", ttl)}
     * 存的是 {@code BANYAN:JSON:"hello"}，带引号）。需要不含引号的裸串请用
     * {@link #putRaw}。关闭 {@code banyan.cache.type-marker.enabled} 后
     * 则不写前缀，示例里的 Redis 值去掉 {@code BANYAN:JSON:} 即可。</p>
     *
     * @param group 组名，可为空
     * @param key   业务 key
     * @param value 对象；为 null 时删除
     * @param ttl   过期时间，null 或非正数表示不过期
     * @param <T>   对象类型
     */
    <T> void put(String group, String key, T value, Duration ttl);

    /**
     * 读取对象，按 {@code Class} 反序列化。
     *
     * <pre>{@code
     * User user = cache.get("cart", "u1", User.class);
     * Integer count = cache.get("order", "count", Integer.class);  // -> 123
     * String text = cache.get("g", "k", String.class);            // -> hello（去掉 JSON 引号）
     * }</pre>
     *
     * <p>容器类型请用 {@link #get(String, String, ParameterizedTypeReference)}，
     * 否则元素会退化成 {@code LinkedHashMap} 等中间类型。</p>
     *
     * @param group 组名，可为空
     * @param key   业务 key
     * @param type  对象类型
     * @param <T>   对象类型
     * @return 对象；不存在时返回 null
     */
    <T> T get(String group, String key, Class<T> type);

    /**
     * 读取带泛型信息的对象，例如 {@code List<User>}、{@code Map<String, User>}。
     *
     * <pre>{@code
     * Duration ttl = Duration.ofMinutes(5);
     * cache.put("gen", "list", List.of(new User("alice", 30)), ttl);
     * List<User> users = cache.get("gen", "list",
     *         new ParameterizedTypeReference<List<User>>() {});
     * }</pre>
     *
     * @param group 组名，可为空
     * @param key   业务 key
     * @param type  含泛型参数的目标类型
     * @param <T>   对象类型
     * @return 对象；不存在时返回 null
     */
    <T> T get(String group, String key, ParameterizedTypeReference<T> type);

    /**
     * 读取 {@code List<E>}，无需手写 {@code ParameterizedTypeReference}。
     *
     * <pre>{@code
     * cache.put("gen", "list", List.of(new User("alice", 30)), Duration.ofMinutes(5));
     * List<User> users = cache.getForList("gen", "list", User.class);
     * }</pre>
     *
     * <p>与 {@link #get(String, String, Class)} 不同：key 不存在或读写开关关闭时
     * 返回空列表而不是 null，便于直接遍历。</p>
     *
     * @param group       组名，可为空
     * @param key         业务 key
     * @param elementType 元素类型
     * @param <E>         元素类型
     * @return 列表；不存在时返回空列表
     */
    default <E> List<E> getForList(String group, String key, Class<E> elementType) {
        ParameterizedTypeReference<List<E>> type = ParameterizedTypeReference.forType(
                ResolvableType.forClassWithGenerics(List.class, elementType).getType());
        List<E> result = get(group, key, type);
        return result == null ? List.of() : result;
    }

    /**
     * 读取 {@code Set<E>}，无需手写 {@code ParameterizedTypeReference}。
     *
     * <pre>{@code
     * Set<Long> ids = cache.getForSet("order", "ids", Long.class);
     * }</pre>
     *
     * <p>key 不存在或读写开关关闭时返回空集合而不是 null。</p>
     *
     * @param group       组名，可为空
     * @param key         业务 key
     * @param elementType 元素类型
     * @param <E>         元素类型
     * @return 集合；不存在时返回空集合
     */
    default <E> Set<E> getForSet(String group, String key, Class<E> elementType) {
        ParameterizedTypeReference<Set<E>> type = ParameterizedTypeReference.forType(
                ResolvableType.forClassWithGenerics(Set.class, elementType).getType());
        Set<E> result = get(group, key, type);
        return result == null ? Set.of() : result;
    }

    /**
     * 读取 {@code Map<String, V>}，无需手写 {@code ParameterizedTypeReference}。
     *
     * <p>JSON 对象的 key 只能是字符串，因此这里固定 key 为 {@code String}；
     * 需要其它 key 类型或嵌套泛型请用
     * {@link #get(String, String, ParameterizedTypeReference)}。</p>
     *
     * <pre>{@code
     * Map<String, User> byName = cache.getForMap("gen", "map", User.class);
     * }</pre>
     *
     * <p>key 不存在或读写开关关闭时返回空 Map 而不是 null。</p>
     *
     * @param group      组名，可为空
     * @param key         业务 key
     * @param valueType   value 类型
     * @param <V>         value 类型
     * @return Map；不存在时返回空 Map
     */
    default <V> Map<String, V> getForMap(String group, String key, Class<V> valueType) {
        ParameterizedTypeReference<Map<String, V>> type = ParameterizedTypeReference.forType(
                ResolvableType.forClassWithGenerics(Map.class, String.class, valueType).getType());
        Map<String, V> result = get(group, key, type);
        return result == null ? Map.of() : result;
    }

    /**
     * 写入原始字符串，不参与 JSON 编解码，值原样存入 Redis。
     *
     * <p>适用于与外部系统共用、或需要保持裸串格式（不含 JSON 引号）的 key
     * （示例均为类型标记开启时的形态）：</p>
     *
     * <pre>{@code
     * Duration ttl = Duration.ofMinutes(5);
     * cache.putRaw("token", "abc", "plain-text", ttl);
     * // Redis 中的值：BANYAN:RAW:plain-text
     * cache.getRaw("token", "abc");  // -> plain-text
     *
     * // 对比：put 会带上 JSON 引号
     * cache.put("token", "abc", "plain-text", ttl);
     * // Redis 中的值：BANYAN:JSON:"plain-text"
     * }</pre>
     *
     * <p>关闭 {@code banyan.cache.type-marker.enabled} 后两侧都不写前缀，
     * 分别为 {@code plain-text} 与 {@code "plain-text"}。</p>
     *
     * <p>与 {@link #put} / {@link #get} 不可混用于同一 key：类型标记开启时会抛出明确异常，
     * 关闭后由调用方自行保证格式一致。</p>
     *
     * @param group 组名，可为空
     * @param key   业务 key
     * @param value 原始字符串；为 null 时删除
     * @param ttl   过期时间，null 或非正数表示不过期
     */
    void putRaw(String group, String key, String value, Duration ttl);

    /**
     * 读取原始字符串，不做 JSON 解码。
     *
     * <pre>{@code
     * String raw = cache.getRaw("token", "abc");
     * }</pre>
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
     * 批量读取带泛型信息的对象，例如 {@code List<User>}。
     *
     * <pre>{@code
     * List<List<User>> result = cache.batchGet("gen", List.of("l1", "l2"),
     *         new ParameterizedTypeReference<List<User>>() {});
     * }</pre>
     *
     * @param group 组名
     * @param keys  业务 key 集合
     * @param type  含泛型参数的目标类型
     * @param <T>   对象类型
     * @return 与入参顺序一致的结果，缺失元素为 null
     */
    <T> List<T> batchGet(String group, Collection<String> keys, ParameterizedTypeReference<T> type);


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
     * 读缓存，未命中时加载并回写；按带泛型信息的类型还原，例如 {@code List<User>}。
     *
     * <pre>{@code
     * List<User> users = cache.getOrLoad("gen", "list",
     *         new ParameterizedTypeReference<List<User>>() {},
     *         () -> userMapper.listAll(), Duration.ofMinutes(5));
     * }</pre>
     *
     * @param group  组名
     * @param key    业务 key
     * @param type   含泛型参数的目标类型
     * @param loader 加载器
     * @param ttl    回写过期时间
     * @param <T>    对象类型
     * @return 对象
     */
    <T> T getOrLoad(String group, String key, ParameterizedTypeReference<T> type,
                    Supplier<T> loader, Duration ttl);

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
