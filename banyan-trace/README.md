# banyan-trace

Trace ID 生成、传播与日志集成。引入后自动在请求入口生成/沿用 TraceId，写入 MDC 与响应头，并跨 Servlet、WebFlux、Dubbo、Feign 与线程池传递。

## 这个模块带来什么

- **入口生成与传播**：Servlet Filter / WebFlux Filter 从 header 或 query 取 TraceId（或生成新的），放入 MDC 并回写响应头，请求结束清理。
- **跨线程/跨调用**：自动装饰容器里的 `ThreadPoolTaskExecutor`，也可手动创建带 `MdcTaskDecorator` 的线程池；Feign 出向写 header、响应侧可临时补全；Dubbo 提供端/消费端透传。
- **可选增强、有则生效**：Servlet / WebFlux / Feign / Dubbo 相关依赖都是 `provided + optional`，classpath 缺哪个就跳过哪个并 WARN，不拖垮应用。
- **不生成业务语义**：只管 TraceId 本身，不打印请求体/响应体，不依赖 `banyan-log`（`banyan-log` 只读取它写入 MDC 的 key）。

适合需要全链路追踪标识、但还没上完整 APM 的 Spring Boot 应用。Java package 的职责说明仍在各自的 `package-info.java`，本文只写使用方式与容易踩的坑。

## 线程池：异步场景的两种入口

## 为什么要显式用工厂

TraceId 存在 `ThreadLocal` + MDC 里，普通线程池提交任务时不会自动带到子线程。
`MdcTaskDecorator` 负责在任务执行前恢复 TraceId、执行后清理。

## 入口一：直接创建带装饰的线程池

```java
ThreadPoolTaskExecutor executor = TraceTaskExecutors.create("order-worker-");
executor.initialize();
// ... 正常使用 executor
```

`create` 内部走 `TaskExecutors.newPool`（commons-spring），线程名有前缀、参数按场景设置，
并自动挂上 `MdcTaskDecorator`。

## 入口二：装饰已有线程池

```java
ThreadPoolTaskExecutor existing = ...; // 使用方自己创建/配置的
TraceTaskExecutors.decorate(existing);
```

注意：装饰会覆盖 executor 上原有的 `TaskDecorator`。若使用方已经声明了自定义的
`TaskDecorator`，框架的自动装配会跳过（`banyan.trace.executor.decorate-executors` 可控）
并 WARN，不会悄悄覆盖（`banyan.trace.async.decorate-executors` 可控）。

## 自动装配的兜底

`TraceExecutorBeanPostProcessor` 会对容器里的 `ThreadPoolTaskExecutor` bean 自动补装饰，
受 `banyan.trace.async.decorate-executors`（默认 `true`）控制。
需要完全自己控制线程池时把它设为 false，再用上面的入口手动装饰。

## 相关配置（`banyan.trace.*`）

| 属性 | 默认值 | 说明 |
|---|---|---|
| `enabled` | `true` | 总开关 |
| `accept-incoming` | `true` | 是否接受外部传入的 TraceId |
| `accept-query-trace-id` | `false` | 是否从 query 参数兜底取 TraceId |
| `async.decorate-executors` | `true` | 自动装饰容器中的线程池 |
| `feign.enabled` | `true` | classpath 有 Feign 时自动写 TraceId 出向 header |
| `feign.response-enabled` | `true` | 当前线程没有 TraceId 时，从 Feign 响应头临时补全并在 finally 恢复 |

`banyan.trace.mdc-key` 默认 `traceId`。若修改它，logback 模板侧的
`banyan.log.logback.trace-id-key` 需同步对齐。

## Feign

`banyan-trace` 的 Feign 依赖是 `provided + optional`，属于「有则增强」：

- `TraceIdFeignInterceptor`：请求头没有 TraceId 时，把当前上下文值写入 `X-Trace-Id`。
- `FeignResponseTraceInterceptor`：当前线程没有 TraceId 且响应头带 `X-Trace-Id` 时，
  在调用链内临时设置，finally 恢复入口前的上下文；不改变已有 TraceId。

不需要 `banyan-spring-feign` 也能生效；两个模块互不依赖。
