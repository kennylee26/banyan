# banyan-data-mysql-mybatis

基于 MyBatis-Plus 的 MySQL 数据访问层实现（`banyan-data-core` 契约的 MySQL 落地）。引入后得到 `BaseDas` 分页/CRUD 模板、自研查询 Wrapper、主键策略与可逐个开关的拦截器。

## 这个模块带来什么

- **统一数据访问模板**：`BaseDas` 实现 `AbstractDas` 的 `countByQuery` / `queryPage` / `applySorts` 三个差异点，save / findPage 流程与 MongoDB 实现共用，返回统一 `PageResult`。
- **可直接传给 `selectPage` 的查询 Wrapper**：`BanyanQueryWrapper<T>` 继承 MyBatis-Plus 本体 `QueryWrapper<T>`，不用链式代理容器，也不引入被禁用的 `mybatis-plus-join`。
- **拦截器按需开关**：分页、LIKE 转义（策略 DTO 层 / 全局 SQL 层）、全表更新删除防护，各自独立 `enabled`，默认只开分页与策略层转义。
- **主键策略跟随 MyBatis-Plus**：基类不声明 id 字段，使用方在自己的实体上用 `@TableId` 决定 AUTO / ASSIGN_UUID / INPUT 等。

适合用 MyBatis-Plus 且想复用 `banyan-data-core` 公共契约的项目。Java package 的职责说明仍在各自的 `package-info.java`，本文只写为什么自研、和 LIKE 转义的边界。

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
