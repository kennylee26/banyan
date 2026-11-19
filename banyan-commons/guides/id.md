# id：ID 生成

`id` 包只提供两件事：

- `IdProvider`：可替换的 ID 生成契约。
- `UuidIdProvider`：纯 JDK 的 UUID 默认实现。

```java
IdProvider provider = new UuidIdProvider();
String id = provider.nextId();
Long longId = provider.nextLongId();
```

该包不依赖 Spring，不注册 bean；使用方显式创建或注入自己的实现即可。
