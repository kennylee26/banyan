# banyan-spring-mvc

Spring MVC 应用能力扩展。引入后自动装配，把**接口异常统一转成中性错误响应**，避免每个 Controller 各自 try-catch 或返回五花八门的错误体。

## 一、模块能力

- **统一异常处理**：按优先级分级的 4 个 advice 覆盖 MVC 参数/绑定/校验异常、Feign 调用异常与兜底 `Exception`，默认返回 HTTP 200 + `ErrorResponse(code, message)`，不回显堆栈或 SQL。
- **响应体形状可替换**：通过 `ExceptionResultFactory` 契约换成自己的 `Result`，不绑死本库的结构。
- **不改变成功响应**：不注册 `ResponseBodyAdvice`，正常返回值仍按应用原有方式渲染。
- **Jackson 增强默认开启**：`Long` 转字符串（防前端精度丢失）、枚举按 `name()`、BigDecimal 不用科学计数法；`java.util.Date` 与 `LocalDateTime` 自动按 `yyyy-MM-dd HH:mm:ss` 双向转换。可用 `banyan.spring.mvc.jackson.enabled=false` 完全恢复应用原有 JSON 输出。
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
| `banyan.spring.mvc.jackson.enabled` | `true` | Jackson 增强总开关，默认开启；`false` 时恢复应用原有 JSON 输出 |
| `banyan.spring.mvc.jackson.long-as-string` | `true` | Long 序列化为字符串，防 JS 精度丢失 |
| `banyan.spring.mvc.jackson.enums-using-name` | `true` | 枚举按 `name()` 序列化 |
| `banyan.spring.mvc.jackson.big-decimal-as-plain` | `true` | BigDecimal 不使用科学计数法 |
| `banyan.spring.mvc.jackson.date-format` | `yyyy-MM-dd HH:mm:ss` | `Date` 与 `LocalDateTime` 的输入/输出格式；空表示不处理日期 |
| `banyan.spring.mvc.jackson.time-zone` | （空） | 日期时区；空表示沿用应用默认 |

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

本模块默认开启，引入后立即统一这几项：

- `Long` → JSON 字符串，避免前端精度丢失。
- 枚举按 `name()` 序列化。
- BigDecimal 不使用科学计数法。
- `java.util.Date` 与 `LocalDateTime` 按 `banyan.spring.mvc.jackson.date-format`（默认 `yyyy-MM-dd HH:mm:ss`）双向转换；`LocalDate` / `LocalTime` 保持 Jackson 默认 ISO 格式。

配置示例：

```yaml
banyan:
  spring:
    mvc:
      jackson:
        enabled: true
        date-format: yyyy-MM-dd HH:mm:ss
        time-zone: Asia/Shanghai
```

其余配置（属性包含策略、`SerializationFeature` / `DeserializationFeature` 等）仍用 Spring Boot 原生 `spring.jackson.*`。优先级：

- 本模块的 `date-format` / `time-zone` 显式配置优先于 `spring.jackson.date-format` / `spring.jackson.time-zone`（本模块 customizer 排序在 Boot 之后）。
- 不希望被本模块改动时，设置 `banyan.spring.mvc.jackson.enabled=false`，JSON 输出与未引入本模块时一致。

本模块只注册一个 `Jackson2ObjectMapperBuilderCustomizer`（排序晚于 Boot 自身），不替换容器 `ObjectMapper`、不声明 `HttpMessageConverters` bean。使用方可以关闭本模块开关后自行追加 customizer。

## 七、Jackson 设计取舍

- **日期格式由本模块统一**：Boot 的 `spring.jackson.date-format` 只作用于 `java.util.Date`，不作用于 `LocalDateTime`；要让两者格式一致，只能在模块内注册 JSR-310 的 `LocalDateTimeSerializer` / `LocalDateTimeDeserializer`。
- **默认开启**：引入即统一 JSON 输出，省去逐个 DTO 加 `@JsonFormat` / `@JsonSerialize`。代价是会改变既有接口的 JSON 形态（如 Long 变字符串），因此提供 `enabled=false` 一键恢复，并在本文与配置参考中显著标注。
- 其余 Jackson feature 仍复用 `spring.jackson.*`，本模块不重复定义等价的 `banyan.*` 键。
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
