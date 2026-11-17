package cn.kennylee.banyan.data.mysql.support;

/**
 * LIKE 特殊字符转义。策略层与全局 LIKE 拦截器共用同一份规则，避免两条路径漂移。
 * <p>字符集固定为 {@link #SPECIAL_CHARS}：转义符反斜杠本身与 SQL 标准通配符 {@code %}、{@code _}。
 * 转义规则是标准非幂等转义：先处理反斜杠，再处理通配符，因此用户输入本身含反斜杠时仍按字面量保留。
 * 也正因非幂等，调用方必须保证同一个原始值只调用一次。</p>
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class LikeEscaper {

    /** LIKE 特殊字符：转义符反斜杠与 SQL 标准通配符。 */
    static final String SPECIAL_CHARS = "\\%_";

    private LikeEscaper() {
    }

    /**
     * 转义 LIKE 值中的特殊字符。
     *
     * @param value 原始值，可为 null
     * @return 转义后的值；null 与空串原样返回
     */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder escaped = new StringBuilder(value.length() + SPECIAL_CHARS.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (SPECIAL_CHARS.indexOf(current) >= 0) {
                escaped.append('\\');
            }
            escaped.append(current);
        }
        return escaped.toString();
    }
}
