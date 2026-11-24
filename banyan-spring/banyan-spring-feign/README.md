# banyan-spring-feign

Feign 应用能力扩展：引入后自动装配请求/响应日志；header 转发默认关闭，需要显式开启。

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
HeaderContextHolder.disableForwardHeaders();   // 单次关闭转发
HeaderContextHolder.setClearAtOnce(false);     // 自行控制 clear()
```

`HeaderContextHolder` 位于 `banyan-commons-spring`（包 `cn.kennylee.banyan.commons.spring.web`），
不绑定 Feign，后续其它出站客户端能力共用同一套上下文。

默认 apply 后自动 `clear()`，避免异步/线程池串 header。

## traceId

trace 能力在 `banyan-trace`：

- 出向：classpath 有 `feign.RequestInterceptor` 时自动写入 `X-Trace-Id`。
- 响应侧：当前线程无 traceId 且响应头带 `X-Trace-Id` 时临时补全，finally 恢复。
- 开关：`banyan.trace.feign.enabled` / `banyan.trace.feign.response-enabled`。

## 响应日志解码器

`LoggingFeignDecoderCustomizer` 通过 `FeignBuilderCustomizer` 包装当前 Decoder，不判断业务返回体、不改变原解码结果或异常；body 只读取一次后重建 Response 再委托。

需要关闭时：`banyan.spring.feign.response-log.enabled=false`。
