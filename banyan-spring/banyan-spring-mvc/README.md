# banyan-spring-mvc

Spring MVC 应用能力扩展：按优先级分级的异常 advice、可替换错误响应契约，以及默认关闭的可选 Jackson 增强。引入后自动装配，不改变成功响应。

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-spring-mvc</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 配置

| 配置项 | 默认 | 说明 |
|---|---|---|
| `banyan.spring.mvc.enabled` | `true` | 总开关；`false` 时不注册本模块任何 bean |
| `banyan.spring.mvc.web-exception.enabled` | `true` | MVC 参数/绑定/校验异常 advice |
| `banyan.spring.mvc.feign-exception.enabled` | `true` | Feign 异常 advice；classpath 无 `feign.FeignException` 时静默跳过 |
| `banyan.spring.mvc.fallback-exception.enabled` | `true` | 兜底 `Exception` advice |
| `banyan.spring.mvc.jackson.enabled` | `false` | Jackson 增强总开关，默认关闭 |
| `banyan.spring.mvc.jackson.long-as-string` | `true` | Long 序列化为字符串；仅当 `jackson.enabled=true` 生效 |
| `banyan.spring.mvc.jackson.enums-using-name` | `true` | 枚举按 `name()` 序列化；仅当 `jackson.enabled=true` 生效 |
| `banyan.spring.mvc.jackson.big-decimal-as-plain` | `true` | BigDecimal 不使用科学计数法；仅当 `jackson.enabled=true` 生效 |

完整默认值与注释见 [`banyan-spring-mvc.yml`](banyan-spring-mvc.yml)，全仓总表见 [`CONFIGURATION.md`](../../CONFIGURATION.md)。

## 异常 advice

四个 advice 按 `@Order` 分级，全部通过 `ExceptionResultFactory` 生成响应体，默认 **HTTP 200 + body 带 code/message**：

| advice | 处理 | order |
|---|---|---|
| `BanyanWebExceptionAdvice` | `MissingServletRequestParameterException`、`MethodArgumentTypeMismatchException`、`HttpMessageNotReadableException`、`MaxUploadSizeExceededException`、`MethodArgumentNotValidException`、`BindException`、`IllegalArgumentException` | 1 |
| `BanyanConstraintViolationAdvice` | `ConstraintViolationException`（有 `jakarta.validation` 才装配） | 1 |
| `BanyanFeignExceptionAdvice` | `FeignException`、`DecodeException`（有 Feign 才装配） | 2 |
| `BanyanFallbackExceptionAdvice` | 兜底 `Exception` | 最低 |

- 不注册任何成功响应的 `ResponseBodyAdvice`，正常返回值仍由应用原有消息转换器渲染。
- 不处理 Sql / MyBatis 异常，避免把 SQL 与数据库信息回显给调用方。

## 替换错误响应结构

默认响应体是 `ErrorResponse(code, message)`。想用自己的 `Result`，声明一个 `ExceptionResultFactory` bean 即可，默认实现自动让位：

```java
@Bean
ExceptionResultFactory exceptionResultFactory() {
    return context -> MyResult.fail(context.code(), context.message());
}
```

`MvcErrorContext` 提供 `code`、`message` 与原始异常，便于自定义实现决定是否附带更多信息。

## Jackson 增强与 `spring.jackson.*` 的关系

本模块**不重复定义**日期格式、时区、feature 布尔开关、属性包含策略——这些直接用 Spring Boot 原生配置：

```yaml
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
    serialization:
      write-dates-as-timestamps: false
    deserialization:
      fail-on-unknown-properties: false
```

本模块只补 Boot 未直接给的三个便捷开关，且默认关闭：

```yaml
banyan:
  spring:
    mvc:
      jackson:
        enabled: true
```

- `big-decimal-as-plain` 等价 `spring.jackson.generator.write-bigdecimal-as-plain`。
- `enums-using-name=true` 是 Jackson 默认行为（强制按 `name()`，可覆盖被 `spring.jackson.datatype.enum.write-enums-using-to-string` 改写的设置）。
- `long-as-string` 无直接等价 Boot 键（Boot 的 `write-numbers-as-strings` 作用于所有数字），是本模块真正补的能力。

本模块只注册一个 `Jackson2ObjectMapperBuilderCustomizer`（排序晚于 Boot 自身），不替换容器 `ObjectMapper`、不声明 `HttpMessageConverters` bean。使用方可以关闭本模块开关后自行追加 customizer。

## 设计依据

Jackson 定制的取舍见 `docs/adr/0010-spring-mvc-jackson-customization.md`（本地设计文档，不入库）。
