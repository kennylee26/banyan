# banyan-data-mysql

# data-mysql.wrapper 用法

Java package 的职责说明仍在 `package-info.java`，本文只写为什么自研、和 LIKE 转义的边界。

## 为什么不用链式代理容器

`BanyanQueryWrapper<T>` 直接继承 MyBatis-Plus 本体的 `QueryWrapper<T>`，
因此可以作为 `Wrapper<T>` 直接传给 `BaseMapper.selectPage`：

```java
BanyanQueryWrapper<User> wrapper = new BanyanQueryWrapper<>(User.class)
        .eq(User::getUserName, "alice")
        .ge(User::getAge, 18);

Page<User> page = userMapper.selectPage(new Page<>(1, 20), wrapper);
```

链式代理型容器（`QueryChainWrapper` 等）不能传给 `selectPage`；
`mybatis-plus-join` 在本项目明确禁用。本类不实现连表 JOIN，不生成带别名的 SELECT 列。

## LIKE 特殊字符边界（重要）

本类的 `like` / `notLike` 是**原生透传**，不承诺转义 `%` / `_` / `\`。
用户搜索里带这些字符时，需要走下面两条路径之一：

1. **查询策略 DTO**（默认开启）：受 `banyan.data-mysql.like-escape.enabled` 控制，
   由框架产出的 like 条件会做转义，并避免 count + data 共用 wrapper 时的二次转义。
2. **全局 SQL 层拦截器**：`banyan.data-mysql.like-escape.inner-interceptor.enabled`
   （默认**显式 false**），对未经策略 DTO 的 like 参数兜底。

已经转义过的值不会被二次加工。若两条路径都关闭，特殊字符会按 SQL 通配符原样匹配。

## 相关配置（`banyan.data-mysql.*`）

| 属性 | 默认值 | 说明 |
|---|---|---|
| `pagination.enabled` | `true` | 分页拦截器 |
| `pagination.max-limit` | `-1` | 单页上限，-1 不限制 |
| `pagination.overflow` | `false` | 页码超末页时是否回首页 |
| `like-escape.enabled` | `true` | 查询策略 DTO 层转义 |
| `like-escape.inner-interceptor.enabled` | `false` | 全局 SQL 层转义 |
| `block-attack.enabled` | `false` | 全表更新/删除防护 |

自定义 `InnerInterceptor` 可以直接声明为 bean，框架自带的拦截器可逐个关闭。
