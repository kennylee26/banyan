# banyan-data

数据访问层（Das）父模块：把分页、排序、查询条件与查询策略收敛到一套公共契约，业务代码面向 `banyan-data-core` 编程，具体数据库实现按需引入子模块。

## 一、模块能力

- **一套公共契约**：`banyan-data-core` 定义 `Identifiable` / `DasOperations` / `AbstractDas` 与分页、排序、查询条件、查询策略，业务代码不绑具体数据库。
- **两套可替换实现**：MySQL（MyBatis-Plus）与 MongoDB（Spring Data MongoDB）各自实现同一契约，`findPage` 统一返回 `PageResult`。
- **模板方法避免重复**：save / findPage 流程在 `AbstractDas`，实现模块只写 `countByQuery` / `queryPage` / `applySorts` 三个差异点。
- **依赖方向单向**：实现模块依赖 `data-core`，`data-core` 不依赖任何实现模块，也不注册 bean、不做自动装配。

适合同时或先后接入多种数据库、希望数据访问 API 保持一致的项目；只用单一数据库且已有成熟 DAO 层时，引入前先评估公共契约是否带来额外约束。各模块源码位置见文末「六、能力 → 代码位置」。

## 二、模块

| 模块 | 角色 | 依赖 |
|---|---|---|
| `banyan-data-core` | 公共契约：`Identifiable`、`DasOperations`、`AbstractDas`、`PageDto`、`PageResult`、`SortDto`、`QueryCriteria`、`DasQueryStrategyRegistry` | `banyan-commons` + `spring-core`（`provided + optional`） |
| `banyan-data-mysql-mybatis` | MySQL 实现（MyBatis-Plus） | `banyan-data-core` + `banyan-commons-spring` |
| `banyan-data-mongodb-springdata` | MongoDB 实现（Spring Data MongoDB） | `banyan-data-core` + `banyan-commons-spring` |

依赖方向单向：实现模块依赖 `banyan-data-core`；`banyan-data-core` 不依赖任何实现模块，也不注册 bean、不做自动装配。

## 三、公共契约

- `PageDto`：`pageNo` / `pageSize` / `searchCount` / `sorts`
- `PageResult<T>`：`pageNo` / `pageSize` / `total` / `totalPage` / `data`
- `QueryCriteria`：实体属性名 + 条件操作符 + 值
- `DasOperations<T, P>`：`findPage`、`save`、`saveBatch`、`findById`、`findAll`、`removeById`、`count` 等
- `AbstractDas<T, P, Q>`：分页模板，子类只实现 `countByQuery` / `queryPage` / `applySorts`
- `DasQueryStrategyRegistry`：按数据库类型选择查询策略，由容器管理，不是静态单例

排序一律在数据库层执行，不在 DAS 或 Service 层用 `stream().sorted()` 重排。

## 四、MySQL（MyBatis-Plus）

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-data-mysql-mybatis</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

实体继承 `BaseEo<P>`，主键由使用方自己声明；没有 `@TableId` 时按全局 `ASSIGN_ID` 分配雪花 ID：

```java
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("users")
public class UserEo extends BaseEo<Long> {

    private Long id;

    @TableField("user_name")
    private String userName;
}
```

数据访问类继承 `BaseDas`，只需提供 Mapper 与实体类型：

```java
public class UserDas extends BaseDas<UserEo, Long> {

    private final UserMapper userMapper;

    public UserDas(UserMapper userMapper, DasQueryStrategyRegistry registry) {
        super(registry);
        this.userMapper = userMapper;
    }

    @Override
    public BaseMapper<UserEo> getBaseMapper() {
        return userMapper;
    }

    @Override
    public Class<UserEo> getEntityClass() {
        return UserEo.class;
    }

    @Override
    public Class<? extends BaseMapper<UserEo>> getMapperClass() {
        return UserMapper.class;
    }
}
```

分页：

```java
PageDto pageDto = new PageDto();
pageDto.setPageNo(1);
pageDto.setPageSize(20);
pageDto.setSorts(List.of(SortDto.of("userName", SortDirection.DESC)));

PageResult<UserEo> page = userDas.findPage(pageDto);
```

配置项（`banyan.data-mysql.*`）与 LIKE 转义边界见
[`banyan-data-mysql-mybatis/README.md`](banyan-data-mysql-mybatis/README.md)。

## 五、MongoDB（Spring Data MongoDB）

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-data-mongodb-springdata</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

实体实现 `MongoDbEo<P>`，默认主键 `ObjectId`；数据访问类继承 `AbstractMongoDas`：

```java
@Data
@Document(collection = "users")
public class UserEo implements MongoDbEo<ObjectId> {

    @Id
    private ObjectId id;

    private String userName;
}

public class UserDas extends AbstractMongoDas<UserEo, ObjectId> {

    public UserDas(MongoTemplate mongoTemplate, DasQueryStrategyRegistry registry) {
        super(mongoTemplate, registry);
    }

    @Override
    public Class<UserEo> getEntityClass() {
        return UserEo.class;
    }
}
```

## 六、约定

- 不引入 `mybatis-plus-join`；`BanyanQueryWrapper` 继承 MyBatis-Plus 本体 `QueryWrapper`，可直接传给 `selectPage`，不实现连表 JOIN。
- 手写多值 INSERT 不经过 MyBatis-Plus，补主键时读取 `TableInfo.getIdType()`，与框架插入路径保持一致。
- 排序字段支持实体属性名与数据库列名，按实体 `@TableField` 映射解析；无法识别时原样透传。
- 测试不依赖外部服务：MySQL 用 H2，MongoDB 用 flapdoodle 内嵌实例，均为 `test` scope。

## 七、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 公共契约与模板方法 | [`banyan-data-core/`](banyan-data-core/) | `Identifiable`、`DasOperations`、`AbstractDas`、`PageResult`、`DasQueryStrategyRegistry` |
| MySQL 实现 | [`banyan-data-mysql-mybatis/`](banyan-data-mysql-mybatis/) | `BaseDas`、`BanyanQueryWrapper`、`MyBatisDasQueryStrategy`；[模块 README](banyan-data-mysql-mybatis/README.md) |
| MongoDB 实现 | [`banyan-data-mongodb-springdata/`](banyan-data-mongodb-springdata/) | `AbstractMongoDas`、`MongoDbDasHelper`（该模块暂无独立 README） |
| MySQL 配置参考 | [`banyan-data-mysql-mybatis.yml`](banyan-data-mysql-mybatis/banyan-data-mysql-mybatis.yml) | 全部 `banyan.data-mysql.*` 键与默认值 |
| MongoDB 配置参考 | [`banyan-data-mongodb-springdata.yml`](banyan-data-mongodb-springdata/banyan-data-mongodb-springdata.yml) | `banyan.data-mongodb.*` 键与默认值 |
| 全仓配置总表 | [`../CONFIGURATION.md`](../CONFIGURATION.md) | 所有 `banyan.*` 键 |

本模块是聚合 POM，自身不含 `src` 与 java config；配置属性在两个实现模块里。
