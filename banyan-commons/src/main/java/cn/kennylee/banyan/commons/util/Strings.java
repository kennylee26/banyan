package cn.kennylee.banyan.commons.util;

/**
 * 字符串工具。只收本库实际需要的少量能力，不追求与第三方工具库对齐。
 *
 * @author kennylee
 * @since 0.1.0
 */
public final class Strings {

    private Strings() {
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param cs 参数
     * @return 是否成功
     */
    public static boolean isBlank(CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return true;
        }
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否非空白。
     *
     * @param cs 参数
     * @return 是否成功
     */
    public static boolean isNotBlank(CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * 驼峰转下划线。连续大写视为一个词（URL -> url）；单个大写前若为小写或数字则插入下划线（userName -> user_name）。
     *
     * @param value 值
     * @return 字符串
     */
    public static String toUnderlineCase(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && (Character.isLowerCase(value.charAt(i - 1)) || Character.isDigit(value.charAt(i - 1)))) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 下划线转驼峰（user_name -> userName）。不含下划线时原样返回。
     *
     * @param value 值
     * @return 字符串
     */
    public static String toCamelCase(String value) {
        if (value == null || value.indexOf('_') < 0) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value.length());
        boolean upperNext = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '_') {
                upperNext = true;
                continue;
            }
            sb.append(upperNext ? Character.toUpperCase(c) : c);
            upperNext = false;
        }
        return sb.toString();
    }
}
