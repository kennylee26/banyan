# banyan

[English](README.md) | [简体中文](README.zh-CN.md)

An infrastructure toolkit for Spring Boot.

> A banyan's aerial roots reach the ground and grow into new trunks — one tree becomes a forest.

## Modules

| Module | Description |
|---|---|
| `banyan-bom` | Dependency version management |
| `banyan-commons` | Pure-JDK static utilities and pluggable provider interfaces; no Spring, no third-party deps (includes HMAC-SHA256 signing, replay guard, header propagation context and log sanitizing) |
| `banyan-commons-spring` | Spring-based common layer: replaceable interfaces, servlet header reading and auto-configuration skeleton |
| `banyan-test` | Shared test support: embedded MongoDB / H2 setup and contract-test bases (test-support jar) |
| `banyan-cache` | Unified cache API with pluggable providers and an extensible cache-penetration guard placeholder |
| `banyan-trace` | Trace ID generation, propagation and log integration for servlet, reactive, dubbo and feign |
| `banyan-log` | Application lifecycle logging, optional logback snippets and pure-JDK log value tools |
| `banyan-sign` | Parent module of the OpenAPI signing family |
| `banyan-sign-openapi` | Server-side verification and decryption: annotation driven, repeatable body, unified 401 |
| `banyan-sign-client` | Third-party copyable protocol core: AES-GCM encryption and HMAC-SHA256 signing, no banyan dependency |
| `banyan-sign-client-httpclient5` | Apache HttpClient 5 adapter: automatic encryption, signing and headers |
| `banyan-sign-client-resttemplate` | Spring RestTemplate adapter: automatic encryption, signing and headers |
| `banyan-sign-client-jdkhttp` | JDK HttpClient adapter: signs an existing `HttpRequest` |
| `banyan-data` | Parent module of the data access layer (Das) |
| `banyan-data-core` | Shared contracts: paging, sorting, query criteria, query strategy, validation |
| `banyan-data-mysql-mybatis` | MySQL data access layer built on MyBatis-Plus |
| `banyan-data-mongodb-springdata` | MongoDB data access layer built on Spring Data MongoDB |
| `banyan-mq` | Parent module of the messaging layer |
| `banyan-mq-core` | Shared messaging contracts: message model, send/consume contracts, instance routing, backend SPI and capabilities |
| `banyan-mq-commons-spring` | Spring shared infrastructure: unified properties, default JSON codec, instance registry |
| `banyan-mq-producer` | Producer facade: sync send, send after commit, delay message, instance routing |
| `banyan-mq-consumer` | Method-level `@MqConsumer`, multiple consumers per class, env suffix isolation, retry semantics |
| `banyan-mq-kafka-spring` | Kafka backend built on spring-kafka |
| `banyan-mq-rocketmq-apache` | RocketMQ backend built on rocketmq-spring-boot-starter |
| `banyan-spring` | Parent module of Spring Boot application capabilities (compile-scope Spring deps, enabled switches) |
| `banyan-spring-feign` | Feign capability: header forwarding, request/response logging and auto-configuration |
| `banyan-spring-mvc` | Spring MVC capability: ordered exception advices, replaceable error response and optional Jackson enhancements |
| `banyan-excel` | Parent module of the spreadsheet export layer |
| `banyan-excel-exporter` | Zero-dependency export contracts: batch source, row writer SPI, export flow and result |
| `banyan-excel-exporter-fastexcel` | FastExcel backend: xlsx templates, automatic sheet splitting and CSV output |

## Configuration reference

Every module with a java config has a `<artifactId>.yml` reference in its module directory; [`CONFIGURATION.md`](CONFIGURATION.md) is the full table of all `banyan.*` keys. Adding or changing a java config requires updating both in the same change. These files are documentation only — they are not packaged into the jar.

## Module README convention

Every module README starts with what the module brings (a short capability list and who should use it), then numbered sections (`一、二、三…`) for introduction, configuration and usage, and ends with a `能力 → 代码位置` table linking each capability to its source package, key types and config file. New modules and new capabilities must keep both the capability list and that table in sync.

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
| [`banyan-commons/README.md`](banyan-commons/README.md) | commons module guide; [crypto signing](banyan-commons/guides/crypto.md), [utilities](banyan-commons/guides/util.md), [error codes](banyan-commons/guides/error.md), [IDs](banyan-commons/guides/id.md) |
| [`banyan-cache/README.md`](banyan-cache/README.md) | `put` vs `putRaw`, generic container reads, provider replacement, `banyan.cache.*` properties |
| [`banyan-log/README.md`](banyan-log/README.md) | Lifecycle events, `banyan.log.*`, logback host include and pattern override |
| [`banyan-trace/README.md`](banyan-trace/README.md) | MDC task decorator and executor factories |
| [`banyan-sign/README.md`](banyan-sign/README.md) | Signing module family overview and dependency direction |
| [`banyan-sign/banyan-sign-openapi/README.md`](banyan-sign/banyan-sign-openapi/README.md) | `@OpenApiSigned`, configuration, 401 body, replaceable key and cipher |
| [`banyan-sign/banyan-sign-client/README.md`](banyan-sign/banyan-sign-client/README.md) | Third-party client: protocol core, HttpClient5 and RestTemplate usage, [integration guide](banyan-sign/banyan-sign-client/THIRD-PARTY-GUIDE.md) |
| [`banyan-data/README.md`](banyan-data/README.md) | Data access layer contracts, MySQL and MongoDB quick start, module dependency direction |
| [`banyan-data-mysql-mybatis/README.md`](banyan-data/banyan-data-mysql-mybatis/README.md) | `BanyanQueryWrapper`, LIKE escaping boundary |
| [`banyan-mq/README.md`](banyan-mq/README.md) | `@MqConsumer`, producer/consumer split, instance routing, capability differences |
| [`banyan-excel/README.md`](banyan-excel/README.md) | Spreadsheet export family overview and module dependency direction |
| [`banyan-excel/banyan-excel-exporter/README.md`](banyan-excel/banyan-excel-exporter/README.md) | Zero-dependency export contracts: batch source, cursor, limits and result |
| [`banyan-excel/banyan-excel-exporter-fastexcel/README.md`](banyan-excel/banyan-excel-exporter-fastexcel/README.md) | FastExcel backend: xlsx templates, automatic sheet splitting, CSV |

## Coordinates

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-data-mongodb-springdata</artifactId>
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
mvn -pl banyan-data/banyan-data-mysql-mybatis -am clean install -DskipTests
```

## Status

Early stage — the modules do not contain any code yet.

## License

[MIT](LICENSE)
