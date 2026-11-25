# banyan-spring-mvc

Spring MVC 应用能力扩展。引入后自动装配，把**接口异常统一转成中性错误响应**，避免每个 Controller 各自 try-catch 或返回五花八门的错误体。

## 一、模块能力

- **统一异常处理**：按优先级分级的 4 个 advice 覆盖 MVC 参数/绑定/校验异常、Feign 调用异常与兜底 `Exception`，默认返回 HTTP 200 + `ErrorResponse(code, message)`，不回显堆栈或 SQL。
- **响应体形状可替换**：通过 `ExceptionResultFactory` 契约换成自己的 `Result`，不绑死本库的结构。
- **不改变成功响应**：不注册 `ResponseBodyAdvice`，正常返回值仍按应用原有方式渲染。
- **可选 Jackson 增强**：默认关闭；开启后可让 `Long` 转字符串（防前端精度丢失）、枚举按 name、BigDecimal 不用科学计数法。日期格式等仍用 Spring Boot 原生 `spring.jackson.*`。
- **缺类可降级**：Feign / `jakarta.validation` / Jackson 是可选增强，缺失时静默跳过；核心类缺失才 WARN，应用不崩。

适合「想要统一异常响应、但不想被某个 Result 结构绑死」的 Spring Boot Web 应用。若你已有完整的全局异常处理，引入前先确认开关与优先级是否冲突。各能力的源码位置见文末「八、能力 → 代码位置」。

## 二、引入

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-spring-mvc</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 三、配置

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

## 四、异常 advice

四个 advice 按 `@Order` 分级，全部通过 `ExceptionResultFactory` 生成响应体，默认 **HTTP 200 + body 带 code/message**：

| advice | 处理 | order |
|---|---|---|
| `BanyanWebExceptionAdvice` | `MissingServletRequestParameterException`、`MethodArgumentTypeMismatchException`、`HttpMessageNotReadableException`、`MaxUploadSizeExceededException`、`MethodArgumentNotValidException`、`BindException`、`IllegalArgumentException` | 1 |
| `BanyanConstraintViolationAdvice` | `ConstraintViolationException`（有 `jakarta.validation` 才装配） | 1 |
| `BanyanFeignExceptionAdvice` | `FeignException`、`DecodeException`（有 Feign 才装配） | 2 |
| `BanyanFallbackExceptionAdvice` | 兜底 `Exception` | 最低 |

- 不注册任何成功响应的 `ResponseBodyAdvice`，正常返回值仍由应用原有消息转换器渲染。
- 不处理 Sql / MyBatis 异常，避免把 SQL 与数据库信息回显给调用方。

## 五、替换错误响应结构

默认响应体是 `ErrorResponse(code, message)`。想用自己的 `Result`，声明一个 `ExceptionResultFactory` bean 即可，默认实现自动让位：

```java
@Bean
ExceptionResultFactory exceptionResultFactory() {
    return context -> MyResult.fail(context.code(), context.message());
}
```

`MvcErrorContext` 提供 `code`、`message` 与原始异常，便于自定义实现决定是否附带更多信息。

## 六、Jackson 增强与 `spring.jackson.*` 的关系

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

## 七、Jackson 设计取舍

- 不重复定义日期格式、时区、feature 布尔开关与包含策略：这些直接用 `spring.jackson.*`（Spring Boot 3.4.5 原生提供），本模块不为它们另起 `banyan.*` 键。
- 三个便捷开关是**显式覆盖**语义：仅在 `jackson.enabled=true` 时生效，会覆盖等价的原生 feature 设置；未开启时不改动任何 JSON 输出。
- 只注册一个 `Jackson2ObjectMapperBuilderCustomizer`，不替换容器 `ObjectMapper`、不声明 `HttpMessageConverters` bean，因此不会影响应用已有的 converter 配置。

## 八、能力 → 代码位置

| 能力 | 源码位置 | 关键类型 |
|---|---|---|
| 按优先级分级的异常 advice | [`handler/`](src/main/java/cn/kennylee/banyan/spring/mvc/handler/) | `BanyanWebExceptionAdvice`、`BanyanConstraintViolationAdvice`、`BanyanFeignExceptionAdvice`、`BanyanFallbackExceptionAdvice` |
| 错误响应契约与默认实现 | [`error/`](src/main/java/cn/kennylee/banyan/spring/mvc/error/) | `ExceptionResultFactory`、`DefaultExceptionResultFactory`、`ErrorResponse`、`MvcErrorContext` |
| 可选 Jackson 增强 | [`jackson/`](src/main/java/cn/kennylee/banyan/spring/mvc/jackson/) | `BanyanJacksonCustomizer` |
| 配置属性 | [`MvcProperties.java`](src/main/java/cn/kennylee/banyan/spring/mvc/autoconfigure/MvcProperties.java) | `MvcProperties` |
| 自动装配与开关 | [`autoconfigure/`](src/main/java/cn/kennylee/banyan/spring/mvc/autoconfigure/) | 5 个 `*AutoConfiguration`，注册清单见 [`AutoConfiguration.imports`](src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports) |
| 配置参考 | [`banyan-spring-mvc.yml`](banyan-spring-mvc.yml) | 全部键与默认值 |
