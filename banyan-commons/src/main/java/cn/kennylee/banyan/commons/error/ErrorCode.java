package cn.kennylee.banyan.commons.error;

/**
 * 库内错误码。每个错误码对应一个资源文件中的消息 key。
 *
 * @author kennylee
 * @since 0.1.0
 */
public enum ErrorCode {

    /** 读取 Bean 属性失败。 */
    BEAN_READ_FAILED("banyan.commons.bean-read-failed"),

    /** JSON 序列化失败。 */
    JSON_SERIALIZE_FAILED("banyan.json.serialize-failed"),

    /** JSON 反序列化失败。 */
    JSON_DESERIALIZE_FAILED("banyan.json.deserialize-failed"),

    /** 未注册查询策略。 */
    STRATEGY_NOT_REGISTERED("banyan.query.strategy-not-registered"),

    /** 不支持的查询操作符。 */
    UNSUPPORTED_OPERATION("banyan.query.unsupported-operation"),

    /** IN / NOT_IN 条件值不是集合。 */
    INVALID_COLLECTION_VALUE("banyan.query.invalid-collection-value"),

    /** 查询字段在实体上不存在。 */
    UNKNOWN_PROPERTY("banyan.query.unknown-property"),

    /** 读取最大页码校验字段失败。 */
    MAX_PAGE_READ_FAILED("banyan.validation.max-page-read-failed"),

    /** 表名为空。 */
    TABLE_NAME_REQUIRED("banyan.sql.table-name-required"),

    /** 列名为空。 */
    COLUMNS_REQUIRED("banyan.sql.columns-required"),

    /** 行数不合法。 */
    ROWS_POSITIVE("banyan.sql.rows-positive"),

    /** 主键列名为空。 */
    ID_COLUMN_REQUIRED("banyan.sql.id-column-required"),

    /** 更新列与取值行数不匹配。 */
    COLUMN_VALUE_COUNT_MISMATCH("banyan.sql.column-value-count-mismatch"),

    /** 批量保存失败。 */
    BATCH_SAVE_FAILED("banyan.mysql.batch-save-failed"),

    /** 填充主键失败。 */
    ID_FILL_FAILED("banyan.mysql.id-fill-failed"),

    /** 多值批量插入失败。 */
    MULTI_VALUE_INSERT_FAILED("banyan.mysql.multi-value-insert-failed"),

    /** 未找到表信息。 */
    TABLE_INFO_NOT_FOUND("banyan.mysql.table-info-not-found"),

    /** 数据库方言判断失败。 */
    DIALECT_DETECT_FAILED("banyan.mysql.dialect-detect-failed"),

    /** 回填生成键失败。 */
    GENERATED_KEY_FILL_FAILED("banyan.mysql.generated-key-fill-failed"),

    /** 缺少必需依赖类，自动配置未装载。 */
    AUTO_CONFIG_SKIPPED("banyan.autoconfig.skipped"),

    /** MongoDB 批量插入忽略错误。 */
    MONGO_BATCH_IGNORED_ERRORS("banyan.mongodb.batch-ignored-errors"),

    /** 页码超出最大允许值。 */
    MAX_PAGE_EXCEEDED("banyan.validation.max-page-exceeded"),

    /** 批量按主键更新的入参为 null。 */
    BATCH_UPDATE_NULL_ARG("banyan.data.batch-update-null-arg"),

    /** 批量按主键更新的记录缺少主键。 */
    BATCH_UPDATE_ID_REQUIRED("banyan.data.batch-update-id-required"),

    /** 批量按主键更新的记录主键重复。 */
    BATCH_UPDATE_DUPLICATE_ID("banyan.data.batch-update-duplicate-id"),

    /** 批量按主键更新的记录没有可更新字段。 */
    BATCH_UPDATE_NO_UPDATABLE_FIELD("banyan.data.batch-update-no-updatable-field"),

    /** 批量更新因方言不支持而降级。 */
    BATCH_UPDATE_DIALECT_FALLBACK("banyan.data.batch-update-dialect-fallback"),

    /** 批量更新因字段并集超限而降级。 */
    BATCH_UPDATE_COLUMNS_FALLBACK("banyan.data.batch-update-columns-fallback"),

    /** 批量更新由调用方显式关闭单条批量 SQL。 */
    BATCH_UPDATE_SKIP_BY_CALLER("banyan.data.batch-update-skip-by-caller"),

    /** 批量保存因方言不支持而降级。 */
    BATCH_SAVE_DIALECT_FALLBACK("banyan.data.batch-save-dialect-fallback"),

    /** 批量保存由调用方显式关闭单条批量 SQL。 */
    BATCH_SAVE_SKIP_BY_CALLER("banyan.data.batch-save-skip-by-caller"),

    /** 批量保存走单条多值 INSERT。 */
    BATCH_SAVE_MULTI_VALUE("banyan.data.batch-save-multi-value"),

    /** 批量更新的列数上限配置非法。 */
    BATCH_UPDATE_MAX_COLUMNS_INVALID("banyan.data.batch-update-max-columns-invalid"),

    /** 批量更新失败。 */
    BATCH_UPDATE_FAILED("banyan.data.batch-update-failed"),

    /** 批量执行失败后回滚也失败。 */
    BATCH_ROLLBACK_FAILED("banyan.data.batch-rollback-failed"),

    /** 全局 LIKE 转义遇到不支持的参数形态。 */
    LIKE_ESCAPE_UNSUPPORTED_PARAM("banyan.mysql.like-escape-unsupported-param"),

    /** 使用方已有任务装饰器时跳过自动兜底。 */
    TRACE_CUSTOM_DECORATOR_SKIPPED("banyan.trace.custom-decorator-skipped"),

    /** 缓存组名或 key 非法。 */
    CACHE_KEY_INVALID("banyan.cache.key-invalid"),

    /** 缓存 key 超长。 */
    CACHE_KEY_TOO_LONG("banyan.cache.key-too-long"),

    /** 缓存值类型与期望不符。 */
    CACHE_VALUE_TYPE_MISMATCH("banyan.cache.value-type-mismatch"),

    /** 批量删除 pattern 不在命名空间内。 */
    CACHE_PATTERN_OUTSIDE_NAMESPACE("banyan.cache.pattern-outside-namespace"),

    /** Provider 不支持该操作。 */
    CACHE_PROVIDER_UNSUPPORTED("banyan.cache.provider-unsupported"),

    /** 穿透开关开启但无实现。 */
    CACHE_PENETRATION_NOOP("banyan.cache.penetration-noop"),

    /** 未获取到缓存删除锁。 */
    CACHE_LOCK_NOT_ACQUIRED("banyan.cache.lock-not-acquired"),

    /** 读写开关关闭，操作被跳过。 */
    CACHE_READ_WRITE_DISABLED("banyan.cache.read-write-disabled"),

    /** 缓存操作日志模板，参数为操作名、key、是否命中。 */
    CACHE_OPERATION("banyan.cache.operation"),

    /** 需要非空 TTL 的缓存操作收到 null。 */
    CACHE_TTL_REQUIRED("banyan.cache.ttl-required"),

    /** 释放缓存锁失败。 */
    CACHE_LOCK_RELEASE_FAILED("banyan.cache.lock-release-failed"),

    /** 应用启动横幅。 */
    LOG_BANNER("banyan.log.banner"),

    /** 应用优雅停机提示。 */
    LOG_SHUTDOWN("banyan.log.shutdown"),

    /** 应用启动失败。 */
    LOG_STARTUP_FAILED("banyan.log.startup-failed"),

    /** 环境快照标题。 */
    LOG_ENV_TITLE("banyan.log.env.title"),

    /** 环境快照：应用与容器。 */
    LOG_ENV_RUNTIME("banyan.log.env.runtime"),

    /** 环境快照：平台信息。 */
    LOG_ENV_PLATFORM("banyan.log.env.platform"),

    /** 环境快照：内存信息。 */
    LOG_ENV_MEMORY("banyan.log.env.memory"),

    /** 签名请求参数非法。 */
    SIGN_REQUEST_INVALID("banyan.sign.request-invalid"),

    /** 签名密钥本身非法（keyId 或 secret 为空）。 */
    SIGN_KEY_INVALID("banyan.sign.key-invalid"),

    /** 请求体解密失败。 */
    PAYLOAD_DECRYPT_FAILED("banyan.openapi-sign.decrypt-failed"),

    /** OpenAPI 请求验签失败（对外统一文案）。 */
    OPENAPI_SIGN_INVALID("banyan.openapi-sign.invalid"),

    /** 请求签名格式非法。 */
    OPENAPI_SIGN_MALFORMED("banyan.openapi-sign.malformed"),

    /** 未知 keyId。 */
    OPENAPI_SIGN_UNKNOWN_KEY("banyan.openapi-sign.unknown-key"),

    /** 签名算法不匹配。 */
    OPENAPI_SIGN_ALG_MISMATCH("banyan.openapi-sign.alg-mismatch"),

    /** 请求已过期。 */
    OPENAPI_SIGN_EXPIRED("banyan.openapi-sign.expired"),

    /** 请求重放。 */
    OPENAPI_SIGN_REPLAY("banyan.openapi-sign.replay"),

    /** 签名不匹配。 */
    OPENAPI_SIGN_SIGNATURE_MISMATCH("banyan.openapi-sign.signature-mismatch"),

    /** 验签失败日志模板，参数为原因、方法、路径、keyId。 */
    OPENAPI_SIGN_VERIFY_FAILED("banyan.openapi-sign.verify-failed"),

    /** 请求体包装器不支持异步读取。 */
    OPENAPI_SIGN_ASYNC_UNSUPPORTED("banyan.openapi-sign.async-unsupported"),

    /** 消息配置错误。 */
    MQ_CONFIGURATION_INVALID("banyan.mq.configuration-invalid"),
    /** tag 数组元素里出现了分隔符。 */
    MQ_TAG_SEPARATOR_NOT_ALLOWED("banyan.mq.tag-separator-not-allowed"),

    /** 通配 tag 与其它 tag 混用。 */
    MQ_TAG_WILDCARD_MIXED("banyan.mq.tag-wildcard-mixed"),

    /** topic 为空。 */
    MQ_TOPIC_REQUIRED("banyan.mq.topic-required"),

    /** payload 为空。 */
    MQ_PAYLOAD_REQUIRED("banyan.mq.payload-required"),

    /** 编解码器不支持的类型。 */
    MQ_CODEC_UNSUPPORTED("banyan.mq.codec-unsupported"),

    /** 消息发送失败。 */
    MQ_SEND_FAILED("banyan.mq.send-failed"),

    /** 消息消费失败。 */
    MQ_CONSUME_FAILED("banyan.mq.consume-failed"),

    /** 实例装配完成。 */
    MQ_INSTANCE_ASSEMBLED("banyan.mq.instance-assembled"),

    /** 生产者已装配。 */
    MQ_PRODUCER_ASSEMBLED("banyan.mq.producer-assembled"),

    /** 消费者扫描开始。 */
    MQ_CONSUMER_SCAN_START("banyan.mq.consumer-scan-start"),

    /** 消费者已注册。 */
    MQ_CONSUMER_REGISTERED("banyan.mq.consumer-registered"),

    /** 消费者扫描完成。 */
    MQ_CONSUMER_SCAN_END("banyan.mq.consumer-scan-end"),

    /** Kafka 不支持消费者级重试。 */
    MQ_KAFKA_RETRY_UNSUPPORTED("banyan.mq.kafka-retry-unsupported"),

    /** Kafka 消费者容器启动。 */
    MQ_KAFKA_CONSUMER_STARTED("banyan.mq.kafka-consumer-started"),

    /** RocketMQ 消费者容器启动。 */
    MQ_ROCKETMQ_CONSUMER_STARTED("banyan.mq.rocketmq-consumer-started"),

    /** 发送消息日志。 */
    MQ_SEND_DEBUG("banyan.mq.send-debug"),

    /** Kafka 发送日志。 */
    MQ_KAFKA_SEND_DEBUG("banyan.mq.kafka-send-debug"),

    /** RocketMQ 发送日志。 */
    MQ_ROCKETMQ_SEND_DEBUG("banyan.mq.rocketmq-send-debug"),

    /** MVC 请求参数不合法。 */
    SPRING_MVC_BAD_REQUEST("banyan.spring.mvc.bad-request"),

    /** MVC 参数校验失败。 */
    SPRING_MVC_VALIDATION_FAILED("banyan.spring.mvc.validation-failed"),

    /** Feign 调用异常。 */
    SPRING_MVC_FEIGN_ERROR("banyan.spring.mvc.feign-error"),

    /** 未预期的内部异常。 */
    SPRING_MVC_INTERNAL_ERROR("banyan.spring.mvc.internal-error"),

    /** 请求处理异常日志模板，参数为异常类型。 */
    SPRING_MVC_EXCEPTION_HANDLED("banyan.spring.mvc.exception-handled");

    /** 资源文件中的消息 key。 */
    private final String messageKey;

    ErrorCode(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * 获取资源文件中的消息 key。
     *
     * @return 消息 key
     */
    public String messageKey() {
        return messageKey;
    }
}
