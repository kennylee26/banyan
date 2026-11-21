# banyan

[English](README.md) | [简体中文](README.zh-CN.md)

基于 Spring Boot 的基础设施工具箱。

> 榕树的气根垂到地上就能长成新的树干，一棵树最终长成一片林子。

## 模块

| 模块 | 说明 |
|---|---|
| `banyan-bom` | 依赖版本管理 |
| `banyan-commons` | 纯 JDK 静态工具与可插拔能力接口，零 Spring、零第三方（内含 HMAC-SHA256 签名与防重放） |
| `banyan-commons-spring` | 依赖 Spring 的公共层：可替换接口、策略注册器与装配骨架 |
| `banyan-test` | 公共测试装备：内嵌 MongoDB / H2 配置、契约测试基类（test-support jar） |
| `banyan-cache` | 统一缓存接口，Provider 可插拔，内置可扩展的穿透防护占位 |
| `banyan-trace` | Trace ID 生成、传播与日志集成，兼容 servlet、reactive、dubbo、feign |
| `banyan-log` | 应用生命周期日志、可选 logback 输出片段与纯 JDK 日志值工具 |
| `banyan-openapi-sign` | OpenAPI 请求验签与解密：注解驱动、可重复读 body、统一 401 |
| `banyan-data` | 数据访问层（Das）父模块 |
| `banyan-data-core` | 公共契约：分页、排序、查询条件、查询策略、校验 |
| `banyan-data-mysql-mybatis` | 基于 MyBatis-Plus 的 MySQL 数据访问层 |
| `banyan-data-mongodb-springdata` | 基于 Spring Data MongoDB 的数据访问层 |

## 日志

`banyan-log` 提供应用生命周期日志，以及一组可选的 logback 输出片段。

模板**不会自动生效**，必须在自己的 `logback-spring.xml` 里显式 include：

```xml
<configuration>
    <include resource="banyan/logback/banyan-logback-spring.xml"/>

    <!-- 方案 A：自己定义 root -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE-DAILY"/>
        <appender-ref ref="ERROR-FILE"/>
    </root>

    <!-- 方案 B：改用可选 root 片段（引入即把 root 交给本库） -->
    <!-- <include resource="banyan/logback/banyan-logback-root.xml"/> -->
</configuration>
```

- `banyan.log.enabled=false` 只管 Java 侧生命周期 bean；XML 模板的禁用方式是不 include。
- `<springProperty>` 要求宿主文件名必须是 `logback-spring.xml`。
- 可用 `banyan.log.logback.path`、`max-size`、`max-history`、`total-size-cap`、`trace-id-key` 调整片段。
- 未引入 `banyan-trace` 时 trace 字段显示 `N/A`；本模块不生成、不传播 TraceId。
- 可用 `logging.pattern.console` / `logging.pattern.file` 整体替换控制台/文件 pattern；本库自定义变量统一 `BANYAN_` 前缀。

## 模块用法文档

以下模块根目录有 `README.md`，写用法、示例与坑（Java package 职责仍由 `package-info.java` 承担）：

| 文档 | 内容 |
|---|---|
| [`banyan-commons/README.md`](banyan-commons/README.md) | commons 模块导航；[crypto 签名与防重放](banyan-commons/guides/crypto.md)、[常用工具](banyan-commons/guides/util.md)、[错误码](banyan-commons/guides/error.md)、[ID](banyan-commons/guides/id.md) |
| [`banyan-cache/README.md`](banyan-cache/README.md) | `put` 与 `putRaw`、泛型容器读取、Provider 替换、`banyan.cache.*` 配置 |
| [`banyan-log/README.md`](banyan-log/README.md) | 生命周期事件、`banyan.log.*`、logback 宿主 include 与 pattern 覆盖 |
| [`banyan-trace/README.md`](banyan-trace/README.md) | MDC 任务装饰器与线程池工厂 |
| [`banyan-openapi-sign/README.md`](banyan-openapi-sign/README.md) | `@OpenApiSigned`、配置项、401 错误体、可替换密钥与加密实现 |
| [`banyan-data/README.md`](banyan-data/README.md) | 数据访问层公共契约、MySQL 与 MongoDB 快速接入、模块依赖方向 |
| [`banyan-data-mysql-mybatis/README.md`](banyan-data/banyan-data-mysql-mybatis/README.md) | `BanyanQueryWrapper`、LIKE 转义边界 |

## 坐标

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-data-mongodb-springdata</artifactId>
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
mvn -pl banyan-data/banyan-data-mysql-mybatis -am clean install -DskipTests
```

## 状态

早期阶段，模块目前不含任何代码。

## License

[MIT](LICENSE)
