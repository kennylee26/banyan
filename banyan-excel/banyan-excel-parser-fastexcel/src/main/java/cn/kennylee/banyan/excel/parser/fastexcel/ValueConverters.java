package cn.kennylee.banyan.excel.parser.fastexcel;

import cn.kennylee.banyan.commons.error.BanyanException;
import cn.kennylee.banyan.commons.error.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.BiFunction;

/**
 * 单元格字符串到字段类型的转换。
 *
 * <p>支持常见基本类型、包装类型、BigDecimal、LocalDate、LocalDateTime 与枚举；其它类型视为不支持。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
final class ValueConverters {

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyyMMdd")
    };

    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss")
    };

    private ValueConverters() {
    }

    /**
     * 把单元格字符串转换成目标类型。
     *
     * @param raw  单元格字符串
     * @param type 目标字段类型
     * @return 转换后的值；字符串类型保留原值，其它类型的空串返回 {@code null}
     */
    static Object convert(String raw, Class<?> type) {
        if (type == String.class) {
            return raw;
        }
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            if (type == int.class || type == Integer.class) {
                return Integer.valueOf(value);
            }
            if (type == long.class || type == Long.class) {
                return Long.valueOf(value);
            }
            if (type == double.class || type == Double.class) {
                return Double.valueOf(value);
            }
            if (type == float.class || type == Float.class) {
                return Float.valueOf(value);
            }
            if (type == short.class || type == Short.class) {
                return Short.valueOf(value);
            }
            if (type == byte.class || type == Byte.class) {
                return Byte.valueOf(value);
            }
            if (type == boolean.class || type == Boolean.class) {
                return parseBoolean(value);
            }
            if (type == BigDecimal.class) {
                return new BigDecimal(value);
            }
            if (type == LocalDate.class) {
                return parseLocalDate(value);
            }
            if (type == LocalDateTime.class) {
                return parseLocalDateTime(value);
            }
            if (type.isEnum()) {
                return parseEnum(value, type);
            }
        } catch (RuntimeException e) {
            throw new BanyanException(ErrorCode.PARSE_FAILED, e);
        }
        throw new BanyanException(ErrorCode.PARSE_CONFIG_INVALID, "fieldType:" + type.getSimpleName());
    }

    private static boolean parseBoolean(String value) {
        if ("1".equals(value) || "是".equals(value)) {
            return true;
        }
        if ("0".equals(value) || "否".equals(value)) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    private static LocalDate parseLocalDate(String value) {
        return parseWithFallback(value, DATE_FORMATTERS, LocalDate::parse, "无法解析日期");
    }

    private static LocalDateTime parseLocalDateTime(String value) {
        return parseWithFallback(value, DATE_TIME_FORMATTERS, LocalDateTime::parse, "无法解析日期时间");
    }

    private static <T> T parseWithFallback(String value, DateTimeFormatter[] formatters,
                                           BiFunction<String, DateTimeFormatter, T> parser,
                                           String message) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return parser.apply(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一个格式
            }
        }
        throw new BanyanException(ErrorCode.PARSE_FAILED, new IllegalArgumentException(message + ": " + value));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnum(String value, Class<?> type) {
        return Enum.valueOf((Class<? extends Enum>) type.asSubclass(Enum.class), value);
    }
}
