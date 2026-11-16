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
    MAX_PAGE_EXCEEDED("banyan.validation.max-page-exceeded");

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
