# banyan-spring

Spring Boot 应用能力扩展家族。与 `banyan-commons` / 传统 starter 不同，这个家族**强依赖 Spring 生态，引入即用**；每个能力仍提供 `enabled` 开关与 AutoConfiguration。

| 模块 | 职责 |
|---|---|
| [`banyan-spring-feign`](banyan-spring-feign/README.md) | Feign header 转发、请求/响应日志与自动装配 |

## 约定

- 所有子模块统一依赖 [`banyan-commons-spring`](../banyan-commons-spring)（父 POM `<dependencies>` 统一声明），公共能力放那一层，能力包只写胶水。
- 子模块可以 `compile` 依赖 Spring / Spring Cloud / 具体客户端，使用方不需要再手工补依赖。
- 版本只在根 POM `<properties>` 与 `banyan-bom` 维护。
- 每个能力有 `banyan.spring.<capability>.enabled` 总开关；缺类时跳过装配并打 WARN。
- 横切能力（如 traceId）仍放 `banyan-trace` 等模块，通过 `@ConditionalOnClass` 自动增强，不反向依赖本家族。
