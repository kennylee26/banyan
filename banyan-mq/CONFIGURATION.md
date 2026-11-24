# banyan-mq 配置参考

## `banyan.mq.*`

| 配置 | 默认值 | 说明 |
|---|---|---|
| `banyan.mq.enabled` | `true` | 总开关；`false` 时不注册任何 banyan-mq bean |
| `banyan.mq.suffix` | 首个激活 profile | 环境后缀；为空时用首个激活 profile，没有 profile 时不追加 |
| `banyan.mq.producer.enabled` | `true` | 生产者模块开关 |
| `banyan.mq.producer.tag-suffix-enabled` | `true` | 发送侧是否默认给 tag 追加环境后缀 |
| `banyan.mq.consumer.enabled` | `true` | 消费者模块开关 |
| `banyan.mq.consumer.suffix-enabled` | `true` | 消费者是否给消费组与 tag 追加环境后缀 |
| `banyan.mq.kafka.enabled` | `true` | Kafka 后端开关 |
| `banyan.mq.rocketmq.enabled` | `true` | RocketMQ 后端开关 |
| `banyan.mq.instances.<name>.type` | 实例名 | 逻辑实例绑定的后端类型：`kafka` 或 `rocketmq` |

说明：

- 未声明 `instances` 时，每个后端自动生成一个同名实例（实例名 = 后端类型），单后端项目零配置。
- 声明了 `instances` 后，业务通过实例名路由；未声明的实例在启动期报错。
- 使用后端不支持的能力（例如 Kafka 上的 tag / 延迟）在启动期或发送前报明确错误，不静默忽略。

### 多后端示例

```yaml
banyan:
  mq:
    enabled: true
    producer:
      enabled: true
    consumer:
      enabled: true
      suffix-enabled: true
    instances:
      member-kafka:
        type: kafka
      member-rocketmq:
        type: rocketmq
```

### 同类型多实例

`instances.<name>.type` 只能区分后端类型。多个 Kafka 集群这类场景，在 Java 配置里显式声明「实例名到后端实现」的映射：

```java
@Bean
MqInstanceRegistry mqInstanceRegistry(MqBackend memberKafkaBackend, MqBackend paymentKafkaBackend) {
    return new MqInstanceRegistry(Map.of(
            "member-kafka", memberKafkaBackend,
            "payment-kafka", paymentKafkaBackend));
}
```

每个后端实现由使用方用各自的 `KafkaTemplate` / `ConsumerFactory` 构造，连接配置仍放在各自的原生命名空间或自定义 bean 里。

### `@MqConsumer` 属性

| 属性 | 默认值 | 说明 |
|---|---|---|
| `instance` | 空 | 逻辑实例名；为空时使用唯一默认实例。支持 Spring 占位符 |
| `topic` | 必填 | topic，支持 Spring 占位符 |
| `tag` | `{"*"}` | tag 数组；空数组或只有 `*` 表示不按 tag 过滤，且不追加后缀、不校验后端 tag 能力。多个 tag 由框架拼成后端表达式，支持 Spring 占位符 |
| `group` | 类名_方法名Consumer | 消费组，支持 Spring 占位符 |
| `concurrency` | `0` | 并发数，`0` 用后端默认；Kafka 映射容器并发数，RocketMQ 同时设置 `consumeThreadNumber` / `consumeThreadMax` |
| `maxRetryTimes` | `-1` | 最大重试次数，`-1` 用后端默认；RocketMQ 映射 `maxReconsumeTimes`，Kafka 无内建重试，该属性不生效并打印 WARN |
| `groupSuffixEnabled` | `true` | 是否给消费组追加环境后缀 |
| `tagSuffixEnabled` | `true` | 是否给每个 tag 追加环境后缀 |

`concurrency` 与 `maxRetryTimes` 是数值属性，Java 注解上不能写占位符。

## Kafka 原生配置（`spring.kafka.*`）

banyan-mq 直接复用 Spring Kafka 的配置，不另造一套。

| 配置 | 说明 |
|---|---|
| `spring.kafka.bootstrap-servers` | Kafka 地址，必填 |
| `spring.kafka.producer.key-serializer` | 默认 `StringSerializer` |
| `spring.kafka.producer.value-serializer` | 默认 `StringSerializer` |
| `spring.kafka.producer.acks` | 发送确认级别 |
| `spring.kafka.producer.retries` | 发送重试次数 |
| `spring.kafka.consumer.group-id` | 消费组，注解未指定 group 时使用 |
| `spring.kafka.consumer.auto-offset-reset` | 无 offset 时的起始位置，建议 `earliest` |
| `spring.kafka.consumer.key-deserializer` | 默认 `StringDeserializer` |
| `spring.kafka.consumer.value-deserializer` | 默认 `StringDeserializer` |
| `spring.kafka.consumer.enable-auto-commit` | 建议 `false`，由容器管理提交 |

## RocketMQ 原生配置（`rocketmq.*`）

| 配置 | 说明 |
|---|---|
| `rocketmq.name-server` | nameserver 地址，必填 |
| `rocketmq.producer.group` | 生产者组 |
| `rocketmq.producer.send-message-timeout` | 发送超时，毫秒 |
| `rocketmq.producer.retry-times-when-send-failed` | 发送失败重试次数 |
| `rocketmq.consumer.group` | 消费者组，仅在使用 starter 自带容器时生效；banyan-mq 以自己的消费者定义为准 |
| `rocketmq.consumer.topic` | 同上 |

## 占位符与环境变量

示例配置统一使用 `${环境变量:默认值}`，发布到公开仓库时不携带本地地址：

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:localhost:9876}
server:
  port: ${SERVER_PORT:0}
```

`server.port` 默认 `0`（随机）；需要固定时用 `SERVER_PORT` 指定。

## 日志

启动期会打印：

- `banyan-mq 实例装配完成: instances=[...]`
- `banyan-mq 生产者已装配: instances=[...], tagSuffix=..., tagSuffixEnabled=...`
- `banyan-mq 消费者扫描开始/完成`
- `注册消费者: instance=..., topic=..., tag=..., group=..., concurrency=..., maxRetryTimes=...`
- `Kafka 消费者容器启动: topic=..., group=..., concurrency=...`
- `RocketMQ 消费者容器启动: topic=..., tag=..., group=...`

发送侧使用 DEBUG：

- `发送消息: instance=..., topic=..., tag=..., delay=...`
- `Kafka 发送: topic=..., key=...`
- `RocketMQ 发送: topic=..., tag=..., key=...`
