# banyan-spring-feign

Feign 应用能力扩展。引入后自动装配请求/响应日志与 header 转发能力，不用自己写 `RequestInterceptor` / `Decoder`。

## 这个模块带来什么

- **请求/响应 DEBUG 日志**：默认开启，敏感 header 默认脱敏，可配置忽略头、脱敏名单与 body 截断长度。
- **入站 header 转发**：默认**关闭**（避免自动带出 Cookie / Authorization 等敏感头）；开启后转发入站 header，支持额外阻塞头与自定义头。
- **单次请求级上下文**：`HeaderContextHolder` 支持按调用禁用转发、加自定义/阻塞头、控制清理时机；默认每次 apply 后自动清理，避免异步串 header。
- **透明响应日志解码器**：包装当前 `Decoder`，只打印响应、不判断业务返回体、不破坏原解码结果或异常。
- **traceId 配合**：`banyan-trace` 在 classpath 有 Feign 时自动出向透传 `X-Trace-Id`、响应侧补全，两模块互不依赖。

适合「用了 Spring Cloud OpenFeign、想统一日志与 header 传播」的应用。只发简单调用、不需要日志与转发的话，这个包带来的价值有限。

```xml
<dependency>
    <groupId>cn.kennylee.banyan</groupId>
    <artifactId>banyan-spring-feign</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 配置

| 配置项 | 默认 | 说明 |
|---|---|---|
| `banyan.spring.feign.enabled` | `true` | 总开关 |
| `banyan.spring.feign.forward-headers.enabled` | `false` | 转发入站 header，安全默认关闭 |
| `banyan.spring.feign.forward-headers.clear-after-apply` | `true` | 每次 apply 后自动清理上下文 |
| `banyan.spring.feign.forward-headers.blocked-headers` | 空 | 额外阻塞头 |
| `banyan.spring.feign.request-log.enabled` | `true` | DEBUG 请求日志 |
| `banyan.spring.feign.response-log.enabled` | `true` | DEBUG 响应日志 |
| `banyan.spring.feign.logging.mask-sensitive` | `true` | 敏感值脱敏 |
| `banyan.spring.feign.logging.masked-headers` | `Authorization,Cookie,Set-Cookie` | 脱敏名单 |
| `banyan.spring.feign.logging.ignored-headers` | 空 | 完全不打印的 header |
| `banyan.spring.feign.logging.max-body-length` | `2048` | body 打印上限 |

默认阻塞头：`Cookie`、`Set-Cookie`、`Content-Length`、`Content-Type`、`Accept`、`Authorization`。

## 单次 header 上下文

```java
HeaderContextHolder.addCustomHeader("X-Tenant", "t1");
HeaderContextHolder.addBlockedHeader("X-Internal");
HeaderContextHolder.disablePropagation();      // 单次关闭传播
HeaderContextHolder.setClearAtOnce(false);     // 自行控制 clear()
```

`HeaderContextHolder` 位于 `banyan-commons`（包 `cn.kennylee.banyan.commons.http`），
不绑定 Feign，后续其它出站客户端能力共用同一套上下文。

默认 apply 后自动 `clear()`，避免异步/线程池串 header；同一线程里串联多个出站能力时，
先 `setClearAtOnce(false)`，由调用方统一 `clear()`，避免前一个能力清掉后一个要用的 header。

## traceId

trace 能力在 `banyan-trace`：

- 出向：classpath 有 `feign.RequestInterceptor` 时自动写入 `X-Trace-Id`。
- 响应侧：当前线程无 traceId 且响应头带 `X-Trace-Id` 时临时补全，finally 恢复。
- 开关：`banyan.trace.feign.enabled` / `banyan.trace.feign.response-enabled`。

## 响应日志解码器

`LoggingFeignDecoderCustomizer` 通过 `FeignBuilderCustomizer` 包装当前 Decoder，不判断业务返回体、不改变原解码结果或异常；body 只读取一次后重建 Response 再委托。

需要关闭时：`banyan.spring.feign.response-log.enabled=false`。
