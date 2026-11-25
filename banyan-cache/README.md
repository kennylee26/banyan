# banyan-cache

Redis 缓存封装：统一 `CacheService` API、可插拔 `CacheProvider`、自动装配与配置开关。
Java package 的职责说明仍在各自的 `package-info.java`，本文只写使用方式与容易踩的坑。

## 一、模块能力

- **统一缓存读写 API**：引入后直接注入 `CacheService` 即可按 `appName:group:key` 读写，对象自动 JSON 序列化、支持 TTL 与泛型容器还原，不用自己拼 key 或操作 `StringRedisTemplate`。
- **开关一键降级**：`banyan.cache.enabled=false` 不注册任何缓存 bean；`read-write-enabled=false` 让所有读返回空、所有写 no-op 且不访问 Redis，调用代码不用改。
- **两种写入格式与类型校验**：`put` 存带 `BANYAN:JSON:` 前缀的 JSON，`putRaw` 存裸串；类型标记开启时混用 `put`/`getRaw` 会抛明确异常，不会静默读到错位数据。
- **批量删除与带锁删除**：`batchDeleteByPattern` 用 SCAN 按命名空间逐批删、不用 `KEYS`；`deleteWithLock` 用随机 token + Lua 比较后释放，只删自己的锁。
- **Provider 可替换、缺类不崩**：声明自己的 `CacheProvider` bean 即整体让位；classpath 缺 `StringRedisTemplate` / Jackson 等必需类时只打 WARN、不注册 bean，应用照常启动。

适合已经用 Redis、想统一 value API 并保留降级开关的 Spring Boot 应用；若你已有自己的缓存门面、或需要可重入且带看门狗续期的分布式锁，引入前先确认默认 `CacheProvider` 与 `deleteWithLock` 的边界是否满足。各能力的源码位置见文末「十一、能力 → 代码位置」。

## 二、快速上手

引入依赖后默认自动装配，直接注入 `CacheService`：

```java
@Autowired
private CacheService cache;

Duration ttl = Duration.ofMinutes(5);
cache.put("cart", "u1", new User("alice", 30), ttl);
User user = cache.get("cart", "u1", User.class);
```

key 统一拼成 `appName:group:key`，`appName` 读 `spring.application.name`（读不到为空、不报错）；
group 可为空，空时省掉该段与那个冒号；group / key 含 `:` 抛参数异常。

## 三、一套 value API，两种写入格式

`put` **无条件**把值序列化成 JSON 再存（Redis String 本身只存字符串）；`putRaw` 存裸字符串。
两者不可混用于同一个 key：类型标记开启时会抛明确异常。

```java
cache.put("cart", "u1", new User("alice", 30), ttl);
// Redis: BANYAN:JSON:{"userName":"alice","age":30}

cache.put("order", "count", 123, ttl);   // BANYAN:JSON:123
Integer n = cache.get("order", "count", Integer.class);

cache.putRaw("token", "abc", "plain-text", ttl);  // BANYAN:RAW:plain-text
String raw = cache.getRaw("token", "abc");
```

注意：`put("g", "k", "hello", ttl)` 存的是 `BANYAN:JSON:"hello"`（**带引号**），
不是裸串。需要不含引号的裸串用 `putRaw`。
`banyan.cache.type-marker.enabled=false` 时两侧都不写前缀，
但也不再校验格式，混用 `put`/`getRaw` 会读到错位数据。

## 四、容器类型：泛型还原

`get(..., List.class)` 只能拿回 `List<LinkedHashMap>`，元素会退化。用以下任一方式：

```java
// 1) 便捷方法（推荐，不用写匿名类）
List<User> users = cache.getForList("gen", "list", User.class);
Set<Long> ids = cache.getForSet("order", "ids", Long.class);
Map<String, User> byName = cache.getForMap("gen", "map", User.class);

// 2) 完整 Type 引用（嵌套泛型 / 自定义 key 类型时用）
List<User> users2 = cache.get("gen", "list",
        new ParameterizedTypeReference<List<User>>() {});
```

`batchGet` 与 `getOrLoad` 同样有 `ParameterizedTypeReference` 重载。
`java.lang.reflect.Type` 只在 `CacheServiceImpl` 内部使用，不在公开签名里。
`getFor*` 在 key 不存在或读写开关关闭时返回**空容器**而不是 null；`get(..., Class)` 返回 null。

## 五、批量删除按 pattern

`batchDeleteByPattern` 的 pattern 相对于组命名空间，用 SCAN 逐批删除：

```java
cache.batchDeleteByPattern("order", "u*");   // 只删 appName:order:u*
```

- `"*"` 或命名空间外的绝对 pattern 被拒绝并 WARN，不抛异常
- 单批大小 `banyan.cache.scan.batch-size`、总上限 `scan.max-deletions`
- 不用 `KEYS`

## 六、带锁删除

`deleteWithLock` 用 `SET NX PX` + 随机 token + Lua 比较后释放，只删自己的锁。
**非可重入、无看门狗续期**，仅用于删缓存这类短临界区；临界区耗时必须小于
`banyan.cache.lock.ttl`（默认 5s）。释放失败只 WARN，不覆盖删除结果。

## 七、替换 Provider

声明一个 `CacheProvider` bean，自动配置即整体让位（`@ConditionalOnMissingBean`）：

```java
@Bean
CacheProvider cacheProvider() {
    return new MyCacheProvider(...);
}
```

最省事的写法是继承 `AbstractCacheProvider`，只覆写你支持的方法；
未覆写的方法抛 `UnsupportedOperationException`（文案走 `CACHE_PROVIDER_UNSUPPORTED`），
调用方会得到明确「不支持」异常，不会静默成功或半执行。

契约要点：只收发 `String` 值（对象由上层 `JsonCodec` 转 JSON）、接口不暴露
`RedisTemplate`、`execute` 的整数脚本返回 `Long` 且参数统一转字符串、
`scanKeys(pattern, count, maxKeys)` 受上限约束。
默认实现 `RedisTemplateCacheProvider` 用 `StringRedisTemplate`，Redis 中是可读文本，
Lua 文本集中在 `cache.script.CacheScripts`。

## 八、配置项（`banyan.cache.*`）

| 属性 | 默认值 | 说明 |
|---|---|---|
| `enabled` | `true` | 装配总开关；false 时不注册任何缓存 bean |
| `read-write-enabled` | `true` | 读写开关；关闭后所有方法不访问 Redis |
| `max-key-length` | `512` | 完整 key 长度上限，超长抛 `IllegalArgumentException` |
| `type-marker.enabled` | `true` | 值前缀标记；关闭后不校验格式 |
| `penetration.enabled` | `false` | 穿透防护占位；开启但无自定义实现时退回 NoOp 并 WARN |
| `lock.ttl` | `5s` | `deleteWithLock` 的锁 TTL |
| `scan.batch-size` | `500` | SCAN 单批扫描与删除数量 |
| `scan.max-deletions` | `1000` | 单次 `batchDeleteByPattern` 最大删除数 |

```yaml
banyan:
  cache:
    read-write-enabled: false
    scan:
      batch-size: 200
      max-deletions: 500
```

`banyan.cache.read-write-enabled=false` 时所有读返回空、所有写 no-op，
`getFor*` 返回空容器，`getOrLoad` 直接执行 loader，任何方法都不访问 Redis。

完整默认值与注释见 [`banyan-cache.yml`](banyan-cache.yml)，全仓总表见 [`CONFIGURATION.md`](../CONFIGURATION.md)。

## 九、装配顺序与让位

`CacheAutoConfiguration` 用 `@AutoConfiguration(after = JsonAutoConfiguration.class)`
保证 `JsonCodec` 已注册，再按 `@ConditionalOnBean(JsonCodec.class)` 装配 `CacheService`。
去掉 `after` 会导致 `CacheService` 静默缺失——`CacheAutoConfigurationTest.loadsByDefault` 守着这条。

以下 bean 由使用方声明时，默认实现不注册：`CacheProvider`、`CacheService`、
`CachePenetrationGuard`、`CacheKeys`。

## 十、缺类降级

classpath 缺少 `StringRedisTemplate` / Jackson / `JavaTimeModule` / Boot 配置属性能力时，
`CacheMissingClassAutoConfiguration` 只打 WARN（走 `AUTO_CONFIG_SKIPPED` 消息资源），
不注册 bean，应用启动成功；运行时初始化失败仍快速失败。

## 十一、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 缓存读写 API 与泛型还原 | [`service/`](src/main/java/cn/kennylee/banyan/cache/service/) | `CacheService`、`CacheServiceImpl`、`CachePenetrationGuard`、`NoOpCachePenetrationGuard` |
| 可插拔 Provider 契约与默认实现 | [`provider/`](src/main/java/cn/kennylee/banyan/cache/provider/) | `CacheProvider`、`AbstractCacheProvider`、`RedisTemplateCacheProvider` |
| key 拼接与值类型标记 | [`support/`](src/main/java/cn/kennylee/banyan/cache/support/) | `CacheKeys`、`CacheValueMarker` |
| 删缓存锁与批量扫描脚本 | [`script/`](src/main/java/cn/kennylee/banyan/cache/script/) | `CacheScripts` |
| 配置属性 | [`CacheProperties.java`](src/main/java/cn/kennylee/banyan/cache/autoconfigure/CacheProperties.java) | `CacheProperties` |
| 自动装配与缺类降级 | [`autoconfigure/`](src/main/java/cn/kennylee/banyan/cache/autoconfigure/) | `CacheAutoConfiguration`、`CacheMissingClassAutoConfiguration`，注册清单见 [`AutoConfiguration.imports`](src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports) |
| 配置参考 | [`banyan-cache.yml`](banyan-cache.yml) | 全部键与默认值 |
