# banyan

[English](README.md) | [简体中文](README.zh-CN.md)

An infrastructure toolkit for Spring Boot.

> A banyan's aerial roots reach the ground and grow into new trunks — one tree becomes a forest.

## Modules

| Module | Description |
|---|---|
| `banyan-bom` | Dependency version management |
| `banyan-commons` | Pure-JDK static utilities and pluggable provider interfaces; no Spring, no third-party deps |
| `banyan-commons-spring` | Spring-based common layer: replaceable interfaces, strategy registry and auto-configuration skeleton |
| `banyan-test` | Shared test support: embedded MongoDB / H2 setup and contract-test bases (test-support jar) |
| `banyan-cache` | Unified cache API with pluggable providers and an extensible cache-penetration guard placeholder |
| `banyan-trace` | Trace ID generation, propagation and log integration for servlet, reactive, dubbo and feign |
| `banyan-log` | Application lifecycle logging, optional logback snippets and pure-JDK log value tools |
| `banyan-data` | Parent module of the data access layer (Das) |
| `banyan-data-core` | Shared contracts: paging, sorting, query criteria, query strategy, validation |
| `banyan-data-mysql` | MySQL data access layer built on MyBatis-Plus |
| `banyan-data-mongodb` | MongoDB data access layer built on Spring Data MongoDB |

## Logging

`banyan-log` provides application lifecycle logging and an optional logback snippet set.

The template does **not** load automatically. Include it from your own `logback-spring.xml`:

```xml
<configuration>
    <include resource="banyan/logback/banyan-logback-spring.xml"/>

    <!-- Option A: define root yourself -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE-DAILY"/>
        <appender-ref ref="ERROR-FILE"/>
    </root>

    <!-- Option B: include the optional root snippet instead (it takes over root) -->
    <!-- <include resource="banyan/logback/banyan-logback-root.xml"/> -->
</configuration>
```

- `banyan.log.enabled=false` only disables the Java-side lifecycle beans; the XML template is disabled by not including it.
- `<springProperty>` requires the host file to be named `logback-spring.xml`.
- Tune the snippet with `banyan.log.logback.path`, `max-size`, `max-history`, `total-size-cap` and `trace-id-key`.
- Without `banyan-trace` on the classpath the trace field prints `N/A`; the module never generates or propagates trace IDs.
- Override the whole console/file pattern with `logging.pattern.console` / `logging.pattern.file`; custom variables use the `BANYAN_` prefix.

## Module guides

Some modules have their own `README.md` with usage examples and gotchas (Java package responsibilities stay in `package-info.java`):

| Guide | Covers |
|---|---|
| [`banyan-cache/README.md`](banyan-cache/README.md) | `put` vs `putRaw`, generic container reads, provider replacement, `banyan.cache.*` properties |
| [`banyan-log/README.md`](banyan-log/README.md) | Lifecycle events, `banyan.log.*`, logback host include and pattern override |
| [`banyan-trace/README.md`](banyan-trace/README.md) | MDC task decorator and executor factories |
| [`banyan-data-mysql/README.md`](banyan-data/banyan-data-mysql/README.md) | `BanyanQueryWrapper`, LIKE escaping boundary |

## Coordinates

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-data-mongodb</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Or manage versions with the BOM:

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

## Requirements

- Java 17
- Spring Boot 3.4.x

## Build

```bash
mvn clean install -DskipTests
```

Build a single module together with the modules it depends on:

```bash
mvn -pl banyan-data/banyan-data-mysql -am clean install -DskipTests
```

## Status

Early stage — the modules do not contain any code yet.

## License

[MIT](LICENSE)
