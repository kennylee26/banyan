package cn.kennylee.banyan.data.core.query;

import cn.kennylee.banyan.commons.util.Strings;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 查询操作符。字段名以对应后缀结尾即表示该操作符，未匹配任何后缀时按 {@link #EQ} 处理。
 * <p>后缀按长度优先匹配，保证 {@code NotLike} / {@code NotIn} / {@code PreLike} / {@code SufLike} 不会被较短后缀误判。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public enum CondOperation {

    /** 相等。 */
    EQ(""),
    /** 不等。 */
    NE("Ne"),
    /** 大于。 */
    GT("Gt"),
    /** 大于等于。 */
    GE("Ge"),
    /** 小于。 */
    LT("Lt"),
    /** 小于等于。 */
    LE("Le"),
    /** 全模糊。 */
    LIKE("Like"),
    /** 前模糊。 */
    PRE_LIKE("PreLike"),
    /** 后模糊。 */
    SUF_LIKE("SufLike"),
    /** 不包含。 */
    NOT_LIKE("NotLike"),
    /** 集合包含。 */
    IN("In"),
    /** 集合排除。 */
    NOT_IN("NotIn"),
    /** 为空。 */
    IS_NULL("IsNull"),
    /** 非空。 */
    IS_NOT_NULL("IsNotNull");

    private static final List<CondOperation> BY_SUFFIX_LENGTH = Arrays.stream(values())
            .filter(op -> !op.suffix.isEmpty())
            .sorted(Comparator.comparingInt((CondOperation op) -> op.suffix.length()).reversed())
            .toList();

    /** 操作符后缀，{@link #EQ} 为空串。 */
    private final String suffix;

    CondOperation(String suffix) {
        this.suffix = suffix;
    }

    /**
     * 获取操作符后缀。
     *
     * @return 后缀；{@link #EQ} 返回空串
     */
    public String suffix() {
        return suffix;
    }

    /**
     * 获取小写名称。
     *
     * @return 小写枚举名
     */
    public String lowerCase() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * 判断字段名是否以该后缀结尾（忽略大小写）。
     *
     * @param field 字段名
     * @return 是否匹配
     */
    public boolean isEndWithIgnoreCase(String field) {
        if (suffix.isEmpty() || field == null || field.length() < suffix.length()) {
            return false;
        }
        return field.regionMatches(true, field.length() - suffix.length(), suffix, 0, suffix.length());
    }

    /**
     * 截取后缀后的驼峰属性名。
     *
     * @param field 字段名
     * @return 驼峰属性名
     */
    public String subFieldNameCamelCase(String field) {
        if (!isEndWithIgnoreCase(field)) {
            return field;
        }
        return field.substring(0, field.length() - suffix.length());
    }

    /**
     * 截取后缀后的下划线列名。
     *
     * @param field 字段名
     * @return 下划线列名
     */
    public String subFieldNameUnderlineCase(String field) {
        return Strings.toUnderlineCase(subFieldNameCamelCase(field));
    }

    /**
     * 判断字段是否为范围条件（Gt / Ge / Lt / Le）。
     *
     * @param field 字段名
     * @return 是否为范围字段
     */
    public static boolean isRangeField(String field) {
        CondOperation operation = match(field);
        return operation == GT || operation == GE || operation == LT || operation == LE;
    }

    /**
     * 按字段名解析查询操作符。
     *
     * @param field 字段名
     * @return 匹配到的操作符；未匹配时返回 {@link #EQ}
     */
    public static CondOperation match(String field) {
        if (field == null) {
            return EQ;
        }
        for (CondOperation operation : BY_SUFFIX_LENGTH) {
            if (operation.isEndWithIgnoreCase(field)) {
                return operation;
            }
        }
        return EQ;
    }
}
