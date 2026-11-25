# banyan 配置总表

> 每个含 java config（`@ConfigurationProperties` 或用 `@ConditionalOnProperty` 读取 `banyan.*`）的模块都有一份显式 yml 配置参考。**新增或修改配置属性时，必须在同一次改动里同步更新对应 yml 与本文。**
> 本文是「可观察」总表；各模块 yml 是「可直接拷贝」的参考；两者不重复抄录。
> 既有 `banyan-mq/CONFIGURATION.md` 与各模块 README 里的配置表保留不动，本文用链接指过去。
> 维护规则正典见本地 `CLAUDE.md`「配置参考文件」一节（本地文件，不入库）。

## 配置前缀一览

| 前缀 | 模块 | 参考文件 | 说明 |
|---|---|---|---|
| `banyan.cache.*` | `banyan-cache` | [`banyan-cache.yml`](banyan-cache/banyan-cache.yml) | 缓存读写、类型标记、穿透防护、删缓存锁、批量扫描 |
| `banyan.trace.*` | `banyan-trace` | [`banyan-trace.yml`](banyan-trace/banyan-trace.yml) | TraceId 生成/传播，servlet / webflux / dubbo / feign 各端开关 |
| `banyan.log.*` | `banyan-log` | [`banyan-log.yml`](banyan-log/banyan-log.yml) | 生命周期日志；`logback.*` 由 XML 模板读取（见下文） |
| `banyan.json.*` | `banyan-commons-spring` | [`banyan-commons-spring.yml`](banyan-commons-spring/banyan-commons-spring.yml) | JSON 编解码实现选择（仅条件注解，无 Properties 类） |
| `banyan.openapi-sign.*` | `banyan-sign-openapi` | [`banyan-sign-openapi.yml`](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) | OpenAPI 验签/解密、header 名、密钥、错误码 |
| `banyan.data-mysql.*` | `banyan-data-mysql-mybatis` | [`banyan-data-mysql-mybatis.yml`](banyan-data/banyan-data-mysql-mybatis/banyan-data-mysql-mybatis.yml) | 分页、LIKE 转义、全表操作防护、批量更新列上限 |
| `banyan.data-mongodb.*` | `banyan-data-mongodb-springdata` | [`banyan-data-mongodb-springdata.yml`](banyan-data/banyan-data-mongodb-springdata/banyan-data-mongodb-springdata.yml) | MongoDB 数据访问开关（仅条件注解） |
| `banyan.mq.*` | `banyan-mq` | [`banyan-mq.yml`](banyan-mq/banyan-mq.yml) | 生产者/消费者、Kafka/RocketMQ 后端、逻辑实例路由 |
| `banyan.spring.feign.*` | `banyan-spring-feign` | [`banyan-spring-feign.yml`](banyan-spring/banyan-spring-feign/banyan-spring-feign.yml) | Feign header 转发、请求/响应日志 |
| `banyan.spring.mvc.*` | `banyan-spring-mvc` | [`banyan-spring-mvc.yml`](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) | 异常 advice、可替换错误响应、可选 Jackson 增强 |

## 全量配置项

| 模块 | 配置项 | 默认值 | 说明 | 来源 | 参考文件 |
|---|---|---|---|---|---|
| banyan-cache | `banyan.cache.enabled` | `true` | 总开关；false 时不注册任何 banyan-cache bean | `CacheProperties` / 条件注解 | [yml](banyan-cache/banyan-cache.yml) |
| banyan-cache | `banyan.cache.read-write-enabled` | `true` | 读写开关；false 时读返回空、写 no-op | `CacheProperties` | [yml](banyan-cache/banyan-cache.yml) |
| banyan-cache | `banyan.cache.max-key-length` | `512` | key 最大长度 | `CacheKeys.DEFAULT_MAX_KEY_LENGTH` | [yml](banyan-cache/banyan-cache.yml) |
| banyan-cache | `banyan.cache.type-marker.enabled` | `true` | 是否给本库写入的值附加类型标记 | `CacheProperties` | [yml](banyan-cache/banyan-cache.yml) |
| banyan-cache | `banyan.cache.penetration.enabled` | `false` | 穿透防护开关 | `CacheProperties` | [yml](banyan-cache/banyan-cache.yml) |
| banyan-cache | `banyan.cache.lock.ttl` | `5s` | 删缓存锁 TTL | `CacheProperties` | [yml](banyan-cache/banyan-cache.yml) |
| banyan-cache | `banyan.cache.scan.batch-size` | `500` | 单批扫描与删除数量 | `CacheProperties` | [yml](banyan-cache/banyan-cache.yml) |
| banyan-cache | `banyan.cache.scan.max-deletions` | `1000` | 单次调用最大删除数量 | `CacheProperties` | [yml](banyan-cache/banyan-cache.yml) |
| banyan-trace | `banyan.trace.enabled` | `true` | 总开关 | `TraceProperties` / 条件注解 | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.header-name` | `X-Trace-Id` | 传输 header 名 | `TraceProperties` | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.mdc-key` | `traceId` | 日志 MDC key | `TraceProperties` | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.accept-incoming` | `true` | 是否沿用上游传入的 TraceId | `TraceProperties` | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.accept-query-trace-id` | `false` | 是否允许从 query 参数兜底取 TraceId | `TraceProperties` | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.response-header.enabled` | `true` | 是否回写响应头 | `TraceProperties` | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.servlet.enabled` | `true` | 是否启用 Servlet 过滤器 | 条件注解 | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.servlet.filter-order` | `-2147483638` | 过滤器顺序（`HIGHEST_PRECEDENCE + 10`） | `TraceProperties` | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.webflux.enabled` | `true` | 是否启用 WebFlux 过滤器 | 条件注解 | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.webflux.context-propagation.enabled` | `true` | 是否开启 Reactor 自动上下文传播 | 条件注解 | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.feign.enabled` | `true` | 是否启用 Feign 出向透传 | 条件注解 | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.feign.response-enabled` | `true` | 是否启用响应侧 TraceId 补全 | 条件注解 | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.dubbo.enabled` | `true` | 是否启用 Dubbo 透传 | 条件注解 | [yml](banyan-trace/banyan-trace.yml) |
| banyan-trace | `banyan.trace.async.decorate-executors` | `true` | 是否自动给容器线程池补设装饰器 | `TraceProperties` | [yml](banyan-trace/banyan-trace.yml) |
| banyan-log | `banyan.log.enabled` | `true` | 总开关；只管 Java 侧能力 | `LogProperties` / 条件注解 | [yml](banyan-log/banyan-log.yml) |
| banyan-log | `banyan.log.lifecycle.enabled` | `true` | 生命周期日志开关 | `LogProperties` | [yml](banyan-log/banyan-log.yml) |
| banyan-log | `banyan.log.lifecycle.environment-enabled` | `true` | 是否输出环境快照 | `LogProperties` | [yml](banyan-log/banyan-log.yml) |
| banyan-log | `banyan.log.lifecycle.banner` | （空） | 自定义启动横幅；留空用消息资源 | `LogProperties` | [yml](banyan-log/banyan-log.yml) |
| banyan-commons-spring | `banyan.json.enabled` | `true` | 是否注册默认 JsonCodec | 条件注解 | [yml](banyan-commons-spring/banyan-commons-spring.yml) |
| banyan-commons-spring | `banyan.json.provider` | `jackson` | 编解码实现选择 | 条件注解 | [yml](banyan-commons-spring/banyan-commons-spring.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.enabled` | `true` | 总开关 | `OpenApiSignProperties` / 条件注解 | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.encrypted` | `true` | 是否要求并执行请求体解密 | `OpenApiSignProperties` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.sign-algorithm` | `HmacSHA256` | 签名算法标识 | `OpenApiSignProperties` / `HmacSha256SignAlgorithm.NAME` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.cipher` | `aes-gcm` | 加密算法标识 | `OpenApiSignProperties` / `AesGcmPayloadCipher.NAME` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.window` | `5m` | 验签时间窗口 | `OpenApiSignProperties` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.reveal-reason` | `false` | 是否在响应中返回具体失败原因 | `OpenApiSignProperties` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.headers.alg` | `X-Banyan-Alg` | 算法 header 名 | `OpenApiSignProperties` / `Signs` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.headers.key-id` | `X-Banyan-KeyId` | 密钥标识 header 名 | `OpenApiSignProperties` / `Signs` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.headers.timestamp` | `X-Banyan-Timestamp` | 时间戳 header 名 | `OpenApiSignProperties` / `Signs` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.headers.nonce` | `X-Banyan-Nonce` | nonce header 名 | `OpenApiSignProperties` / `Signs` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.headers.sign` | `X-Banyan-Sign` | 签名 header 名 | `OpenApiSignProperties` / `Signs` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.keys` | `{}` | keyId → secret 映射 | `OpenApiSignProperties` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.error.code` | `openapi.sign.invalid` | 响应错误码 | `OpenApiSignProperties` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-sign-openapi | `banyan.openapi-sign.error.message` | （空） | 通用失败文案；为空取消息资源 | `OpenApiSignProperties` | [yml](banyan-sign/banyan-sign-openapi/banyan-sign-openapi.yml) |
| banyan-data-mysql-mybatis | `banyan.data-mysql.enabled` | `true` | 总开关 | 条件注解 | [yml](banyan-data/banyan-data-mysql-mybatis/banyan-data-mysql-mybatis.yml) |
| banyan-data-mysql-mybatis | `banyan.data-mysql.max-update-columns` | `6` | 单条批量 UPDATE 列数上限，硬上限 32 | `BaseDas`（`Environment`） | [yml](banyan-data/banyan-data-mysql-mybatis/banyan-data-mysql-mybatis.yml) |
| banyan-data-mysql-mybatis | `banyan.data-mysql.pagination.enabled` | `true` | 是否启用分页 | `MyBatisDasProperties` | [yml](banyan-data/banyan-data-mysql-mybatis/banyan-data-mysql-mybatis.yml) |
| banyan-data-mysql-mybatis | `banyan.data-mysql.pagination.max-limit` | `-1` | 单页上限，-1 不限制 | `MyBatisDasProperties` | [yml](banyan-data/banyan-data-mysql-mybatis/banyan-data-mysql-mybatis.yml) |
| banyan-data-mysql-mybatis | `banyan.data-mysql.pagination.overflow` | `false` | 页码超出末页是否回首页 | `MyBatisDasProperties` | [yml](banyan-data/banyan-data-mysql-mybatis/banyan-data-mysql-mybatis.yml) |
| banyan-data-mysql-mybatis | `banyan.data-mysql.pagination.db-type` | （空） | 数据库类型，留空自动探测 | `MyBatisDasProperties` | [yml](banyan-data/banyan-data-mysql-mybatis/banyan-data-mysql-mybatis.yml) |
| banyan-data-mysql-mybatis | `banyan.data-mysql.like-escape.enabled` | `true` | 查询 DTO 产出的 like 条件是否转义 | `MyBatisDasProperties` | [yml](banyan-data/banyan-data-mysql-mybatis/banyan-data-mysql-mybatis.yml) |
| banyan-data-mysql-mybatis | `banyan.data-mysql.like-escape.inner-interceptor.enabled` | `false` | 是否启用全局 SQL 层转义 | `MyBatisDasProperties` | [yml](banyan-data/banyan-data-mysql-mybatis/banyan-data-mysql-mybatis.yml) |
| banyan-data-mysql-mybatis | `banyan.data-mysql.block-attack.enabled` | `false` | 是否启用全表更新/删除防护 | `MyBatisDasProperties` | [yml](banyan-data/banyan-data-mysql-mybatis/banyan-data-mysql-mybatis.yml) |
| banyan-data-mongodb-springdata | `banyan.data-mongodb.enabled` | `true` | 总开关 | 条件注解 | [yml](banyan-data/banyan-data-mongodb-springdata/banyan-data-mongodb-springdata.yml) |
| banyan-mq | `banyan.mq.enabled` | `true` | 总开关 | `MqProperties` / 条件注解 | [yml](banyan-mq/banyan-mq.yml) |
| banyan-mq | `banyan.mq.suffix` | （空） | 环境后缀；为空用首个激活 profile | `MqProperties` | [yml](banyan-mq/banyan-mq.yml) |
| banyan-mq | `banyan.mq.producer.enabled` | `true` | 生产者模块开关 | `MqProperties` / 条件注解 | [yml](banyan-mq/banyan-mq.yml) |
| banyan-mq | `banyan.mq.producer.tag-suffix-enabled` | `true` | 发送侧是否给 tag 追加环境后缀 | `MqProperties` | [yml](banyan-mq/banyan-mq.yml) |
| banyan-mq | `banyan.mq.consumer.enabled` | `true` | 消费者模块开关 | `MqProperties` / 条件注解 | [yml](banyan-mq/banyan-mq.yml) |
| banyan-mq | `banyan.mq.consumer.suffix-enabled` | `true` | 消费者是否给消费组与 tag 追加环境后缀 | `MqProperties` | [yml](banyan-mq/banyan-mq.yml) |
| banyan-mq | `banyan.mq.kafka.enabled` | `true` | Kafka 后端开关 | 条件注解 | [yml](banyan-mq/banyan-mq.yml) |
| banyan-mq | `banyan.mq.rocketmq.enabled` | `true` | RocketMQ 后端开关 | 条件注解 | [yml](banyan-mq/banyan-mq.yml) |
| banyan-mq | `banyan.mq.instances.<name>.type` | `{}` | 实例名→后端类型（`kafka` / `rocketmq`） | `MqProperties` | [yml](banyan-mq/banyan-mq.yml) |
| banyan-spring-feign | `banyan.spring.feign.enabled` | `true` | 总开关 | `FeignProperties` / 条件注解 | [yml](banyan-spring/banyan-spring-feign/banyan-spring-feign.yml) |
| banyan-spring-feign | `banyan.spring.feign.forward-headers.enabled` | `false` | 入站 header 转发，安全默认关闭 | `FeignProperties` / 条件注解 | [yml](banyan-spring/banyan-spring-feign/banyan-spring-feign.yml) |
| banyan-spring-feign | `banyan.spring.feign.forward-headers.clear-after-apply` | `true` | 应用后自动清理上下文 | `HeaderForwardOptions` | [yml](banyan-spring/banyan-spring-feign/banyan-spring-feign.yml) |
| banyan-spring-feign | `banyan.spring.feign.forward-headers.blocked-headers` | `[]` | 额外阻塞头 | `HeaderForwardOptions` | [yml](banyan-spring/banyan-spring-feign/banyan-spring-feign.yml) |
| banyan-spring-feign | `banyan.spring.feign.request-log.enabled` | `true` | 请求日志开关 | `FeignProperties` / 条件注解 | [yml](banyan-spring/banyan-spring-feign/banyan-spring-feign.yml) |
| banyan-spring-feign | `banyan.spring.feign.response-log.enabled` | `true` | 响应日志开关 | `FeignProperties` / 条件注解 | [yml](banyan-spring/banyan-spring-feign/banyan-spring-feign.yml) |
| banyan-spring-feign | `banyan.spring.feign.logging.mask-sensitive` | `true` | 是否对敏感 header 脱敏 | `LoggingOptions` | [yml](banyan-spring/banyan-spring-feign/banyan-spring-feign.yml) |
| banyan-spring-feign | `banyan.spring.feign.logging.masked-headers` | `[Authorization, Cookie, Set-Cookie]` | 需要脱敏的 header | `LoggingOptions` | [yml](banyan-spring/banyan-spring-feign/banyan-spring-feign.yml) |
| banyan-spring-feign | `banyan.spring.feign.logging.ignored-headers` | `[]` | 完全不打印的 header | `LoggingOptions` | [yml](banyan-spring/banyan-spring-feign/banyan-spring-feign.yml) |
| banyan-spring-feign | `banyan.spring.feign.logging.max-body-length` | `2048` | body 打印上限 | `LoggingOptions` | [yml](banyan-spring/banyan-spring-feign/banyan-spring-feign.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.enabled` | `true` | 总开关；false 时不注册任何本模块 bean | 条件注解 | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.web-exception.enabled` | `true` | MVC 参数/绑定/校验异常 advice | 条件注解 | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.feign-exception.enabled` | `true` | Feign 异常 advice（缺类跳过） | 条件注解 | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.fallback-exception.enabled` | `true` | 兜底 Exception advice | 条件注解 | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.jackson.enabled` | `true` | Jackson 增强总开关，默认开启；false 时恢复应用原有 JSON 输出 | 条件注解 | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.jackson.long-as-string` | `true` | Long 序列化为字符串，防 JS 精度丢失 | `MvcProperties` | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.jackson.enums-using-name` | `true` | 枚举按 name 序列化 | `MvcProperties` | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.jackson.big-decimal-as-plain` | `true` | BigDecimal 不用科学计数法 | `MvcProperties` | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.jackson.lenient-boolean` | `true` | 布尔接受 true/false、0/1、字符串 "true"/"false"/"0"/"1" | `MvcProperties` | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.jackson.date-format` | `yyyy-MM-dd HH:mm:ss` | Date 输出 + LocalDateTime 反序列化首选格式；空表示不处理日期 | `MvcProperties` | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.jackson.date-formats` | `[]` | LocalDateTime 反序列化的额外候选格式，按顺序尝试 | `MvcProperties` | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.jackson.local-date-format` | `yyyy-MM-dd` | LocalDate 输出 + 反序列化首选格式；不能带时间字段 | `MvcProperties` | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.jackson.local-date-formats` | `[]` | LocalDate 反序列化的额外候选格式，按顺序尝试 | `MvcProperties` | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |
| banyan-spring-mvc | `banyan.spring.mvc.jackson.time-zone` | （空） | 日期时区；空表示沿用应用默认 | `MvcProperties` | [yml](banyan-spring/banyan-spring-mvc/banyan-spring-mvc.yml) |

## 仅测试用 / 非本库属性

以下键**不属于**本库配置项，不出现在「全量配置项」表中：

| 键 | 说明 |
|---|---|
| `banyan.demo.enabled` | 仅 `banyan-test` 测试代码使用，不生成 yml、不纳入配置参考 |
| `banyan.log.logback.trace-id-key` / `path` / `max-size` / `max-history` / `total-size-cap` | 由 logback XML 模板的 `<springProperty>` 读取，Java 侧无默认值，默认值见 `banyan-log/src/main/resources/banyan/logback/*.xml` |
| `spring.jackson.*`（`date-format` / `time-zone` / `serialization.*` / `deserialization.*` / `mapper.*` / `parser.*` / `generator.*` / `default-property-inclusion` 等） | Spring Boot 原生配置，非本模块属性；`banyan-spring-mvc.yml` 顶部有注释示例 |
| `spring.mvc.format.*`（`date` / `time` / `date-time`） | Spring Boot 原生 MVC 入参时间格式，非本模块属性 |
| `spring.kafka.*` / `rocketmq.*` | 后端原生配置；banyan-mq 直接复用，详见 [`banyan-mq/CONFIGURATION.md`](banyan-mq/CONFIGURATION.md) |

## 维护约定

1. **新增 / 改名 / 删除配置属性** → 同一次改动里同步更新对应模块 `<artifactId>.yml` 与本文。
2. 只存在于条件注解（无 Properties 类）的 `banyan.*` 开关也要收录，来源列写「条件注解」。
3. 被继承的公共配置结构（`HeaderForwardOptions` / `LoggingOptions`）按实际生效的键列全。
4. 由 logback XML 读取的键只以注释列出，不编造 Java 侧默认值。
5. Spring Boot 原生键只在 yml 注释示例，不作为本库配置项计入本表。
6. 总表负责可观察、各模块 yml 负责可拷贝；不再生成合并的全量 `application-all.yml`。
