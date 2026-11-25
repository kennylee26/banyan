# banyan-log

应用生命周期日志、可选 logback 输出片段与纯 JDK 日志值工具。

## 一、模块能力

- **开箱即用的生命周期日志**：应用启动横幅 + 运行环境快照、优雅停机、启动失败堆栈，默认 INFO/ERROR，不打印任何业务数据。
- **可选 logback 片段**：一组 `<included>` 资源提供控制台 / 按天文件 / 错误文件 appender，宿主显式 include 才生效，不覆盖宿主已有的日志配置。
- **日志值工具**：`LogValues` 的手机号/邮箱遮蔽与超长文本截断，纯静态 JDK 方法，不调用就不生效。
- **与 trace 弱耦合**：只读取 trace 已写入 MDC 的 key 用于日志 pattern，**不依赖 `banyan-trace`**、不生成也不传播 TraceId；未引入时该字段显示 `N/A`。

适合想统一「启动/停机日志 + 文件输出格式」的项目；日志主体 pattern 仍来自 Spring Boot 默认配置。Java package 的职责说明仍在各自的 `package-info.java`，本文只写使用方式与容易踩的坑。各能力的源码位置见文末「四、能力 → 代码位置」。

## 二、生命周期日志

| 事件 | 级别 | 内容 |
|---|---|---|
| `ApplicationReadyEvent` | INFO | 启动横幅 + 环境快照 |
| `ContextClosedEvent` | INFO | 优雅停机（每个上下文只打一次） |
| `ApplicationFailedEvent` | ERROR | 启动失败 + 异常堆栈 |

环境快照字段：应用名、active profiles、Web 容器类型与版本、Servlet 规范版本、
CPU 核数、Java 与 VM 版本、时区、Max / Total / Free / Usable 内存。
不打印请求体、响应体、参数值或任何业务数据。

### 2.1 配置项（`banyan.log.*`）

| 属性 | 默认值 | 说明 |
|---|---|---|
| `enabled` | `true` | 总开关；只管 Java 侧 bean，**不影响 XML 模板** |
| `lifecycle.enabled` | `true` | 生命周期日志开关 |
| `lifecycle.environment-enabled` | `true` | 关掉后只剩横幅与停机日志 |
| `lifecycle.banner` | 空 | 自定义启动横幅，走 `MessageFormat` |

```yaml
banyan:
  log:
    lifecycle:
      environment-enabled: false
      banner: "It''s {0} running on {1}"
```

**自定义横幅的坑**：`MessageFormat` 里单引号会吞掉后续占位符，
需要输出单引号必须双写（`It''s {0}`）。占位符 `{0}` = 应用名，`{1}` = profiles。
文案缺省回落 `Messages` 消息资源（中英）。

### 2.2 替换环境信息采集

```java
@Bean
EnvironmentInfoProvider environmentInfoProvider() {
    return context -> new EnvironmentInfo(...);  // 自己组装快照
}
```

默认实现 `JdkEnvironmentInfoProvider` 只用 `Runtime` / `System` / JDK：
容器类型来自 Spring Boot 的 `WebServer` 抽象，版本用 `ClassLoader.loadClass` 反射读取
（只加载不初始化，取不到就省略字段），Servlet 版本反射读取，不 compile 依赖 servlet / webflux。
探测失败一律省略字段或显示 `N/A`，不抛异常、不 WARN、不阻断启动。

### 2.3 明确不做的事

- 不碰 JVM 默认未捕获异常处理器、不注册 OOM hook、不叠加 shutdown hook
- 不用跨 ApplicationContext 共享的静态状态（停机去重用实例级 `AtomicBoolean`）
- 不自带 `application` 配置、不覆盖宿主日志配置

## 三、logback 片段

这些是纯 `<included>` 片段资源（在 `src/main/resources/banyan/logback/`），
**不会自动生效**。宿主必须显式 include。

### 3.1 最小可用配置

在宿主自己的 `logback-spring.xml` 里：

```xml
<configuration>
    <!-- 主片段：定义 BANYAN_ 变量 + CONSOLE / FILE-DAILY / ERROR-FILE 三个 appender -->
    <include resource="banyan/logback/banyan-logback-spring.xml"/>

    <!-- 方案 A：自己组装 root（推荐，级别与 appender 完全由宿主决定） -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE-DAILY"/>
        <appender-ref ref="ERROR-FILE"/>
    </root>

    <!-- 方案 B：改用可选 root 片段（引入即把 root 交给本库） -->
    <!-- <include resource="banyan/logback/banyan-logback-root.xml"/> -->

    <logger name="com.example" level="DEBUG"/>
</configuration>
```

主片段**不含** `<root>` / `<logger>` / `<contextName>`，也不自动 include root 片段。
只写 include 不写 root 会没有任何输出。

### 3.2 为什么文件名带 `-spring`

`springProperty` / `springProfile` 需要 Spring Boot 的 `LoggingSystem` 解析，
所以宿主文件名必须是 `logback-spring.xml`。本库主片段叫
`banyan-logback-spring.xml`——带 `-spring` 只表示语法能力，
**不叫** `logback-spring.xml` 以规避被 Boot 自动识别为主配置。

### 3.3 日志格式

主体 pattern 来自 Spring Boot 3.4.5 的 `defaults.xml`，本库不重定义
`CONSOLE_LOG_PATTERN` / `FILE_LOG_PATTERN` / `LOG_FILE`（避免覆盖宿主同名变量）。
traceId 通过 Boot 的 `LOG_CORRELATION_PATTERN` 钩子注入，默认形如：

```
2026-11-19T08:55:56.584+08:00  INFO 12345 --- [main] [traceId=N/A] com.example.Foo : hello
```

有 trace 时显示 `[traceId=abc123]`，未引 `banyan-trace` 时显示 `[traceId=N/A]`。
需要整体替换 pattern 用 `logging.pattern.console` / `logging.pattern.file`。

### 3.4 可覆盖的值

| 属性 | 默认值 | 用途 |
|---|---|---|
| `banyan.log.logback.path` | `logs` | 日志目录 |
| `banyan.log.logback.max-size` | `100MB` | 单文件上限 |
| `banyan.log.logback.max-history` | `30` | 保留天数 |
| `banyan.log.logback.total-size-cap` | `10GB` | 总容量上限 |
| `banyan.log.logback.trace-id-key` | `traceId` | MDC 中 TraceId 的 key |
| `logging.pattern.console` / `logging.pattern.file` | Boot 默认 | 整体替换控制台 / 文件 pattern |

若使用方改了 `banyan.trace.mdc-key`，同步设置 `banyan.log.logback.trace-id-key` 即可对齐。
自定义变量一律 `BANYAN_` 前缀；`%esb` 转换符由 Spring Boot 3.4.5 `defaults.xml` 内置，
本库不分发自定义 Converter。

### 3.5 依赖与禁用

- 片段是纯 XML，本模块不声明 logback / janino 编译依赖；不用 `<if>`（缺 janino 时
  logback 会静默跳过条件分支），按 profile 或条件装配由宿主决定 include 哪些片段。
- 片段里不内置任何 dev / prod 具体 profile 名，也不夹带 APM 占位符。
- `banyan.log.enabled=false` 只关 Java 侧生命周期 bean。
  **XML 模板的禁用方式就是不写 include**，没有第二套开关。

## 四、日志值工具

`LogValues.mask(...)` / `truncate(...)` 是纯静态 JDK 工具，无接口、无 bean、无开关，
**不调用就不生效**，不会自动作用于任何日志。手机号、邮箱做部分遮蔽，
超长文本截断并附加 `...`，null 输入返回 null。

## 五、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 生命周期日志与环境快照 | [`lifecycle/`](src/main/java/cn/kennylee/banyan/log/lifecycle/) | `LifecycleLogger`、`EnvironmentInfoProvider`、`JdkEnvironmentInfoProvider`、`EnvironmentInfo` |
| logback 片段 | [`resources/banyan/logback/`](src/main/resources/banyan/logback/) | `banyan-logback-spring.xml`、`banyan-logback-root.xml` 及各 `*-appender.xml` |
| 日志值工具 | [`support/`](src/main/java/cn/kennylee/banyan/log/support/) | `LogValues` |
| 配置属性 | [`LogProperties.java`](src/main/java/cn/kennylee/banyan/log/autoconfigure/LogProperties.java) | `LogProperties` |
| 自动装配与缺类降级 | [`autoconfigure/`](src/main/java/cn/kennylee/banyan/log/autoconfigure/) | `LogAutoConfiguration`、`LogMissingClassAutoConfiguration`，注册清单见 [`AutoConfiguration.imports`](src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports) |
| 配置参考 | [`banyan-log.yml`](banyan-log.yml) | 全部键、logback 键与默认值 |
