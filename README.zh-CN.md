# banyan

[English](README.md) | [简体中文](README.zh-CN.md)

基于 Spring Boot 的基础设施工具箱。

> 榕树的气根垂到地上就能长成新的树干，一棵树最终长成一片林子。

## 模块

| 模块 | 说明 |
|---|---|
| `banyan-bom` | 依赖版本管理 |
| `banyan-commons` | 纯 JDK 静态工具与可插拔能力接口，零 Spring、零第三方 |
| `banyan-commons-spring` | 依赖 Spring 的公共层：可替换接口、策略注册器与装配骨架 |
| `banyan-test` | 公共测试装备：内嵌 MongoDB / H2 配置、契约测试基类（test-support jar） |
| `banyan-cache` | 统一缓存接口，Provider 可插拔，内置可扩展的穿透防护占位 |
| `banyan-trace` | Trace ID 生成、传播与日志集成，兼容 servlet、reactive、dubbo、feign |
| `banyan-data` | 数据访问层（Das）父模块 |
| `banyan-data-core` | 公共契约：分页、排序、查询条件、查询策略、校验 |
| `banyan-data-mysql` | 基于 MyBatis-Plus 的 MySQL 数据访问层 |
| `banyan-data-mongodb` | 基于 Spring Data MongoDB 的数据访问层 |

## 坐标

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-data-mongodb</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

也可以用 BOM 统一管理版本：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.kennylee.banyan</groupId>
            <artifactId>banyan-bom</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

## 环境要求

- Java 17
- Spring Boot 3.4.x

## 构建

```bash
mvn clean install -DskipTests
```

只构建单个模块（含其依赖的模块）：

```bash
mvn -pl banyan-data/banyan-data-mysql -am clean install -DskipTests
```

## 状态

早期阶段，模块目前不含任何代码。

## License

[MIT](LICENSE)
