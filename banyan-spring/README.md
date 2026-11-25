# banyan-spring

Spring Boot 应用能力扩展家族。与 `banyan-commons` / 传统 starter 不同，这个家族**强依赖 Spring 生态，引入即用**；每个能力仍提供 `enabled` 开关与 AutoConfiguration。

## 一、模块能力

- **引入即用的应用能力**：子模块 `compile` 强依赖 Spring / Spring Cloud / 具体客户端，使用方不需要再手工拼依赖。
- **每个能力独立可关**：各有 `banyan.spring.<capability>.enabled` 总开关，默认自动装配。
- **缺类可降级**：缺少 classpath 必需类时跳过装配并打 WARN，应用不崩。
- **横切能力不混进来**：traceId 这类仍放 `banyan-trace` 等模块，通过 `@ConditionalOnClass` 自动增强，不反向依赖本家族。

适合想直接拿到某个 Spring 应用能力（Feign 日志与 header 转发、MVC 统一异常响应）的项目；若你已有完整实现、或只想按需引入轻量工具类，先看对应子模块 README 再决定。当前两个子模块见「二、子模块」。

## 二、子模块

| 模块 | 职责 |
|---|---|
| [`banyan-spring-feign`](banyan-spring-feign/README.md) | Feign header 转发、请求/响应日志与自动装配 |
| [`banyan-spring-mvc`](banyan-spring-mvc/README.md) | Spring MVC 分级异常 advice、可替换错误响应与可选 Jackson 增强 |

## 三、约定

- 所有子模块统一依赖 [`banyan-commons-spring`](../banyan-commons-spring)（父 POM `<dependencies>` 统一声明），公共能力放那一层，能力包只写胶水。
- 子模块可以 `compile` 依赖 Spring / Spring Cloud / 具体客户端，使用方不需要再手工补依赖。
- 版本只在根 POM `<properties>` 与 `banyan-bom` 维护。
- 每个能力有 `banyan.spring.<capability>.enabled` 总开关；缺类时跳过装配并打 WARN。
- 横切能力（如 traceId）仍放 `banyan-trace` 等模块，通过 `@ConditionalOnClass` 自动增强，不反向依赖本家族。

## 四、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| Feign header 转发与日志 | [`banyan-spring-feign/`](banyan-spring-feign/) | `FeignRequestLogInterceptor`、`ForwardHeadersInterceptor`、`LoggingFeignDecoderCustomizer` |
| MVC 异常 advice 与错误响应 | [`banyan-spring-mvc/`](banyan-spring-mvc/) | `BanyanWebExceptionAdvice`、`ExceptionResultFactory`、`BanyanJacksonCustomizer` |
| 公共层 | [`banyan-commons-spring/`](../banyan-commons-spring/) | 各能力包共用的接口与装配骨架 |
| 配置参考 | [`banyan-spring-feign.yml`](banyan-spring-feign/banyan-spring-feign.yml)、[`banyan-spring-mvc.yml`](banyan-spring-mvc/banyan-spring-mvc.yml) | 两个能力的全部 `banyan.spring.*` 键与默认值 |

全仓配置项总表见 [`../CONFIGURATION.md`](../CONFIGURATION.md)。本家族是聚合 POM，自身不含代码与 java config。
