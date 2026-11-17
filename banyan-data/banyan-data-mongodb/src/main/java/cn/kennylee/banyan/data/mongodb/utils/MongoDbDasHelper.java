package cn.kennylee.banyan.data.mongodb.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

/**
 * MongoDB 查询辅助：模糊正则与时间戳换算。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class MongoDbDasHelper {

    /** Mongo 主键字段名。 */
    public static final String MONGO_ID = "_id";

    /** Mongo 文档类型字段名，由 Spring Data 写入，不属于业务字段。 */
    public static final String MONGO_CLASS = "_class";

    /** 时间戳换算使用的固定时区（+8）。 */
    public static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.ofHours(8);

    private MongoDbDasHelper() {
    }

    /**
     * LocalDateTime 按固定 +8 偏移转 epoch 毫秒（沿用原工程语义）。
     *
     * @param value 值
     * @return 结果对象
     */
    public static Object toEpochMilliIfNecessary(Object value) {
        if (value instanceof LocalDateTime time) {
            return time.toInstant(DEFAULT_ZONE_OFFSET).toEpochMilli();
        }
        return value;
    }

    /**
     * 构造全模糊正则。
     *
     * @param value 值
     * @return 正则
     */
    public static Pattern toLikeRegex(Object value) {
        return Pattern.compile(Pattern.quote(String.valueOf(value)), Pattern.CASE_INSENSITIVE);
    }

    /**
     * 构造前模糊正则。
     *
     * @param value 值
     * @return 正则
     */
    public static Pattern toPreLikeRegex(Object value) {
        return Pattern.compile("^" + Pattern.quote(String.valueOf(value)), Pattern.CASE_INSENSITIVE);
    }

    /**
     * 构造后模糊正则。
     *
     * @param value 值
     * @return 正则
     */
    public static Pattern toSufLikeRegex(Object value) {
        return Pattern.compile(Pattern.quote(String.valueOf(value)) + "$", Pattern.CASE_INSENSITIVE);
    }
}
