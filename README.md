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
| `banyan-data` | Parent module of the data access layer (Das) |
| `banyan-data-core` | Shared contracts: paging, sorting, query criteria, query strategy, validation |
| `banyan-data-mysql` | MySQL data access layer built on MyBatis-Plus |
| `banyan-data-mongodb` | MongoDB data access layer built on Spring Data MongoDB |

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
