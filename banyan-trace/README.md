# banyan-trace

# trace.thread 用法

Java package 的职责说明仍在 `package-info.java`，本文只写异步场景的两种入口。

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
