package cn.kennylee.banyan.log.support;

import java.util.regex.Pattern;

/**
 * 日志值工具：显式调用的脱敏与截断，不注册 bean、没有开关、不自动作用于任何日志。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class LogValues {

    /** 默认截断长度。 */
    private static final int DEFAULT_MAX_LENGTH = 256;

    /** 截断后缀。 */
    private static final String TRUNCATED_SUFFIX = "...";

    /** 脱敏占位。 */
    private static final String MASK = "****";

    /** 邮箱用户名脱敏占位。 */
    private static final String EMAIL_MASK = "***";

    /** 11 位手机号。 */
    private static final Pattern MOBILE = Pattern.compile("^1\\d{10}$");

    /** 手机号保留的前缀长度。 */
    private static final int MOBILE_PREFIX = 3;

    /** 手机号保留的后缀长度。 */
    private static final int MOBILE_SUFFIX = 4;

    /** 邮箱分隔符。 */
    private static final String EMAIL_SEPARATOR = "@";

    /** 邮箱用户名最短长度（至少一个字符）。 */
    private static final int MIN_LOCAL_PART_LENGTH = 1;

    /** 末尾字符偏移。 */
    private static final int LAST_CHAR_OFFSET = 1;

    private LogValues() {
    }

    /**
     * 对手机号与邮箱做部分遮蔽，其它文本原样返回。
     *
     * @param value 原始值，可为 null
     * @return 处理后的值；入参为 null 时返回 null
     */
    public static String mask(String value) {
        if (value == null) {
            return null;
        }
        if (MOBILE.matcher(value).matches()) {
            return value.substring(0, MOBILE_PREFIX) + MASK
                    + value.substring(value.length() - MOBILE_SUFFIX);
        }
        int at = value.indexOf(EMAIL_SEPARATOR);
        if (at >= MIN_LOCAL_PART_LENGTH && at < value.length() - LAST_CHAR_OFFSET) {
            return value.charAt(0) + EMAIL_MASK + value.substring(at);
        }
        return value;
    }

    /**
     * 按默认长度截断。
     *
     * @param value 原始值，可为 null
     * @return 截断后的值；入参为 null 时返回 null
     */
    public static String truncate(String value) {
        return truncate(value, DEFAULT_MAX_LENGTH);
    }

    /**
     * 按指定长度截断，超长时附加 {@code ...}。
     *
     * @param value     原始值，可为 null
     * @param maxLength 保留的最大字符数
     * @return 截断后的值；入参为 null 时返回 null
     */
    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        int limit = Math.max(0, maxLength);
        return value.length() <= limit ? value : value.substring(0, limit) + TRUNCATED_SUFFIX;
    }
}
